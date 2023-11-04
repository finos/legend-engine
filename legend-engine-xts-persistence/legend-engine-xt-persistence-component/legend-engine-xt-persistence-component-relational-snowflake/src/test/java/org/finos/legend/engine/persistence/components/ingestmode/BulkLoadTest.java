// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.persistence.components.ingestmode;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.digest.NoDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UDFBasedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.relational.snowflake.logicalplan.datasets.SnowflakeStagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.snowflake.SnowflakeSink;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.common.StatisticName.*;

public class BulkLoadTest
{
    private static final String APPEND_TIME = "append_time";

    private static Field col1 = Field.builder()
            .name("col_int")
            .type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty()))
            .build();
    private static Field col2 = Field.builder()
            .name("col_integer")
            .type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty()))
            .build();
    private static Field col3 = Field.builder()
            .name("col_bigint")
            .type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty()))
            .columnNumber(4)
            .build();
    private static Field col4 = Field.builder()
            .name("col_variant")
            .type(FieldType.of(DataType.VARIANT, Optional.empty(), Optional.empty()))
            .columnNumber(5)
            .build();

    private List filesList = Arrays.asList("/path/xyz/file1.csv", "/path/xyz/file2.csv");

    protected final ZonedDateTime fixedZonedDateTime_2000_01_01 = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    protected final Clock fixedClock_2000_01_01 = Clock.fixed(fixedZonedDateTime_2000_01_01.toInstant(), ZoneOffset.UTC);

    @Test
    public void testBulkLoadWithDigestNotGeneratedColumnNumbersDerived()
    {
        BulkLoad bulkLoad = BulkLoad.builder()
                .batchIdField("batch_id")
                .digestGenStrategy(NoDigestGenStrategy.builder().build())
                .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
                .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
                .stagedFilesDatasetProperties(
                        SnowflakeStagedFilesDatasetProperties.builder()
                           .location("my_location")
                           .fileFormat("my_file_format")
                           .addAllFiles(filesList).build())
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2)).build())
                .build();

        Dataset mainDataset = DatasetDefinition.builder()
                .database("my_db").name("my_name").alias("my_alias")
                .schema(SchemaDefinition.builder().build())
                .build();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(bulkLoad)
                .relationalSink(SnowflakeSink.get())
                .collectStatistics(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .bulkLoadTaskIdValue("task123")
                .batchIdPattern("{NEXT_BATCH_ID}")
                .build();

        GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"my_db\".\"my_name\"(\"col_int\" INTEGER,\"col_integer\" INTEGER,\"batch_id\" INTEGER,\"append_time\" DATETIME)";
        String expectedIngestSql = "COPY INTO \"my_db\".\"my_name\" " +
                "(\"col_int\", \"col_integer\", \"batch_id\", \"append_time\") " +
                "FROM " +
                "(SELECT legend_persistence_stage.$1 as \"col_int\",legend_persistence_stage.$2 as \"col_integer\",{NEXT_BATCH_ID},'2000-01-01 00:00:00' " +
                "FROM my_location (FILE_FORMAT => 'my_file_format', PATTERN => '(/path/xyz/file1.csv)|(/path/xyz/file2.csv)') as legend_persistence_stage)" +
                " on_error = 'ABORT_STATEMENT'";

        String expectedMetadataIngestSql = "INSERT INTO bulk_load_batch_metadata (\"batch_id\", \"table_name\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\", \"batch_source_info\") " +
                "(SELECT {NEXT_BATCH_ID},'my_name','2000-01-01 00:00:00',SYSDATE(),'{BULK_LOAD_BATCH_STATUS_PLACEHOLDER}',PARSE_JSON('{\"files\":[\"/path/xyz/file1.csv\",\"/path/xyz/file2.csv\"],\"task_id\":\"task123\"}'))";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals(expectedMetadataIngestSql, metadataIngestSql.get(0));

        Assertions.assertEquals("SELECT 0 as \"rowsDeleted\"", statsSql.get(ROWS_DELETED));
        Assertions.assertEquals("SELECT 0 as \"rowsTerminated\"", statsSql.get(ROWS_TERMINATED));
        Assertions.assertEquals("SELECT 0 as \"rowsUpdated\"", statsSql.get(ROWS_UPDATED));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM \"my_db\".\"my_name\" as my_alias WHERE my_alias.\"append_time\" = '2000-01-01 00:00:00'", statsSql.get(ROWS_INSERTED));
    }

    @Test
    public void testBulkLoadWithDigestNotGeneratedColumnNumbersProvided()
    {
        BulkLoad bulkLoad = BulkLoad.builder()
                .batchIdField("batch_id")
                .digestGenStrategy(NoDigestGenStrategy.builder().build())
                .auditing(NoAuditing.builder().build())
                .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
                .stagedFilesDatasetProperties(
                        SnowflakeStagedFilesDatasetProperties.builder()
                                .location("my_location")
                                .fileFormat("my_file_format")
                                .addAllFiles(filesList).build())
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col3, col4)).build())
                .alias("t")
                .build();

        Dataset mainDataset = DatasetDefinition.builder()
                .database("my_db").name("my_name").alias("my_alias")
                .schema(SchemaDefinition.builder().build())
                .build();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(bulkLoad)
                .relationalSink(SnowflakeSink.get())
                .collectStatistics(true)
                .bulkLoadTaskIdValue("task123")
                .build();

        GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"my_db\".\"my_name\"(\"col_bigint\" BIGINT,\"col_variant\" VARIANT,\"batch_id\" INTEGER)";
        String expectedIngestSql = "COPY INTO \"my_db\".\"my_name\" " +
                "(\"col_bigint\", \"col_variant\", \"batch_id\") " +
                "FROM " +
                "(SELECT t.$4 as \"col_bigint\",TO_VARIANT(PARSE_JSON(t.$5)) as \"col_variant\",(SELECT COALESCE(MAX(bulk_load_batch_metadata.\"batch_id\"),0)+1 FROM bulk_load_batch_metadata as bulk_load_batch_metadata WHERE UPPER(bulk_load_batch_metadata.\"table_name\") = 'MY_NAME') " +
                "FROM my_location (FILE_FORMAT => 'my_file_format', PATTERN => '(/path/xyz/file1.csv)|(/path/xyz/file2.csv)') as t) " +
                "on_error = 'ABORT_STATEMENT'";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));

        Assertions.assertEquals("SELECT 0 as \"rowsDeleted\"", statsSql.get(ROWS_DELETED));
        Assertions.assertEquals("SELECT 0 as \"rowsTerminated\"", statsSql.get(ROWS_TERMINATED));
        Assertions.assertEquals("SELECT 0 as \"rowsUpdated\"", statsSql.get(ROWS_UPDATED));
    }

    @Test
    public void testBulkLoadWithUpperCaseConversionAndNoTaskId()
    {
        BulkLoad bulkLoad = BulkLoad.builder()
                .batchIdField("batch_id")
                .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestField("digest").digestUdfName("LAKEHOUSE_MD5").build())
                .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
                .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
                .stagedFilesDatasetProperties(
                        SnowflakeStagedFilesDatasetProperties.builder()
                                .location("my_location")
                                .fileFormat("my_file_format")
                                .addAllFiles(filesList).build())
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2)).build())
                .build();

        Dataset mainDataset = DatasetDefinition.builder()
                .database("my_db").name("my_name").alias("my_alias")
                .schema(SchemaDefinition.builder().build())
                .build();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(bulkLoad)
                .relationalSink(SnowflakeSink.get())
                .collectStatistics(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .caseConversion(CaseConversion.TO_UPPER)
                .build();

        GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"MY_DB\".\"MY_NAME\"(\"COL_INT\" INTEGER," +
                "\"COL_INTEGER\" INTEGER,\"DIGEST\" VARCHAR,\"BATCH_ID\" INTEGER,\"APPEND_TIME\" DATETIME)";
        String expectedIngestSql = "COPY INTO \"MY_DB\".\"MY_NAME\" " +
                "(\"COL_INT\", \"COL_INTEGER\", \"DIGEST\", \"BATCH_ID\", \"APPEND_TIME\") " +
                "FROM " +
                "(SELECT legend_persistence_stage.$1 as \"COL_INT\",legend_persistence_stage.$2 as \"COL_INTEGER\"," +
                "LAKEHOUSE_MD5(OBJECT_CONSTRUCT('COL_INT',legend_persistence_stage.$1,'COL_INTEGER',legend_persistence_stage.$2))," +
                "(SELECT COALESCE(MAX(BULK_LOAD_BATCH_METADATA.\"BATCH_ID\"),0)+1 FROM BULK_LOAD_BATCH_METADATA as BULK_LOAD_BATCH_METADATA WHERE UPPER(BULK_LOAD_BATCH_METADATA.\"TABLE_NAME\") = 'MY_NAME'),'2000-01-01 00:00:00' " +
                "FROM my_location (FILE_FORMAT => 'my_file_format', " +
                "PATTERN => '(/path/xyz/file1.csv)|(/path/xyz/file2.csv)') as legend_persistence_stage) " +
                "on_error = 'ABORT_STATEMENT'";

        String expectedMetadataIngestSql = "INSERT INTO BULK_LOAD_BATCH_METADATA (\"BATCH_ID\", \"TABLE_NAME\", \"BATCH_START_TS_UTC\", \"BATCH_END_TS_UTC\", \"BATCH_STATUS\", \"BATCH_SOURCE_INFO\") " +
            "(SELECT (SELECT COALESCE(MAX(BULK_LOAD_BATCH_METADATA.\"BATCH_ID\"),0)+1 FROM BULK_LOAD_BATCH_METADATA as BULK_LOAD_BATCH_METADATA WHERE UPPER(BULK_LOAD_BATCH_METADATA.\"TABLE_NAME\") = 'MY_NAME')," +
            "'MY_NAME','2000-01-01 00:00:00',SYSDATE(),'{BULK_LOAD_BATCH_STATUS_PLACEHOLDER}',PARSE_JSON('{\"files\":[\"/path/xyz/file1.csv\",\"/path/xyz/file2.csv\"]}'))";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals(expectedMetadataIngestSql, metadataIngestSql.get(0));

        Assertions.assertEquals("SELECT 0 as \"ROWSDELETED\"", statsSql.get(ROWS_DELETED));
        Assertions.assertEquals("SELECT 0 as \"ROWSTERMINATED\"", statsSql.get(ROWS_TERMINATED));
        Assertions.assertEquals("SELECT 0 as \"ROWSUPDATED\"", statsSql.get(ROWS_UPDATED));
        Assertions.assertEquals("SELECT COUNT(*) as \"ROWSINSERTED\" FROM \"MY_DB\".\"MY_NAME\" as my_alias WHERE my_alias.\"APPEND_TIME\" = '2000-01-01 00:00:00'", statsSql.get(ROWS_INSERTED));
    }

    @Test
    public void testBulkLoadDigestColumnNotProvided()
    {
        try
        {
            BulkLoad bulkLoad = BulkLoad.builder()
                    .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestUdfName("LAKEHOUSE_MD5").build())
                    .batchIdField("batch_id")
                    .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
                    .build();
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e.getMessage().contains("Cannot build UDFBasedDigestGenStrategy, some of required attributes are not set [digestField]"));
        }
    }

    @Test
    public void testBulkLoadDigestUDFNotProvided()
    {
        try
        {
            BulkLoad bulkLoad = BulkLoad.builder()
                    .batchIdField("batch_id")
                    .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestField("digest").build())
                    .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
                    .build();
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e.getMessage().contains("Cannot build UDFBasedDigestGenStrategy, some of required attributes are not set [digestUdfName]"));
        }
    }

    @Test
    public void testBulkLoadStagedFilesDatasetNotProvided()
    {
        try
        {
            BulkLoad bulkLoad = BulkLoad.builder()
                    .batchIdField("batch_id")
                    .digestGenStrategy(NoDigestGenStrategy.builder().build())
                    .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
                    .build();

            Dataset stagingDataset = DatasetDefinition.builder()
                    .database("my_db").name("my_stage").alias("my_alias")
                    .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2)).build())
                    .build();

            Dataset mainDataset = DatasetDefinition.builder()
                    .database("my_db").name("my_name").alias("my_alias")
                    .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2)).build())
                    .build();

            RelationalGenerator generator = RelationalGenerator.builder()
                    .ingestMode(bulkLoad)
                    .relationalSink(SnowflakeSink.get())
                    .collectStatistics(true)
                    .executionTimestampClock(fixedClock_2000_01_01)
                    .bulkLoadTaskIdValue("batch123")
                    .build();

            GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagingDataset));
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e.getMessage().contains("Only StagedFilesDataset are allowed under Bulk Load"));
        }
    }

    @Test
    public void testBulkLoadWithDigest()
    {
        BulkLoad bulkLoad = BulkLoad.builder()
                .batchIdField("batch_id")
                .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestField("digest").digestUdfName("LAKEHOUSE_UDF").build())
                .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
                .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
                .stagedFilesDatasetProperties(
                        SnowflakeStagedFilesDatasetProperties.builder()
                                .location("my_location")
                                .fileFormat("my_file_format")
                                .addAllFiles(filesList).build())
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2)).build())
                .build();

        Dataset mainDataset = DatasetDefinition.builder()
                .database("my_db").name("my_name").alias("my_alias")
                .schema(SchemaDefinition.builder().build())
                .build();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(bulkLoad)
                .relationalSink(SnowflakeSink.get())
                .collectStatistics(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .bulkLoadTaskIdValue("task123")
                .build();

        GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"my_db\".\"my_name\"(\"col_int\" INTEGER,\"col_integer\" INTEGER,\"digest\" VARCHAR,\"batch_id\" INTEGER,\"append_time\" DATETIME)";

        String expectedIngestSql = "COPY INTO \"my_db\".\"my_name\" " +
                "(\"col_int\", \"col_integer\", \"digest\", \"batch_id\", \"append_time\") " +
                "FROM " +
                "(SELECT legend_persistence_stage.$1 as \"col_int\",legend_persistence_stage.$2 as \"col_integer\"," +
                "LAKEHOUSE_UDF(OBJECT_CONSTRUCT('col_int',legend_persistence_stage.$1,'col_integer',legend_persistence_stage.$2))," +
                "(SELECT COALESCE(MAX(bulk_load_batch_metadata.\"batch_id\"),0)+1 FROM bulk_load_batch_metadata as bulk_load_batch_metadata WHERE UPPER(bulk_load_batch_metadata.\"table_name\") = 'MY_NAME'),'2000-01-01 00:00:00' " +
                "FROM my_location (FILE_FORMAT => 'my_file_format', " +
                "PATTERN => '(/path/xyz/file1.csv)|(/path/xyz/file2.csv)') as legend_persistence_stage) " +
                "on_error = 'ABORT_STATEMENT'";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));

        Assertions.assertEquals("SELECT 0 as \"rowsDeleted\"", statsSql.get(ROWS_DELETED));
        Assertions.assertEquals("SELECT 0 as \"rowsTerminated\"", statsSql.get(ROWS_TERMINATED));
        Assertions.assertEquals("SELECT 0 as \"rowsUpdated\"", statsSql.get(ROWS_UPDATED));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM \"my_db\".\"my_name\" as my_alias WHERE my_alias.\"append_time\" = '2000-01-01 00:00:00'", statsSql.get(ROWS_INSERTED));
    }
}
