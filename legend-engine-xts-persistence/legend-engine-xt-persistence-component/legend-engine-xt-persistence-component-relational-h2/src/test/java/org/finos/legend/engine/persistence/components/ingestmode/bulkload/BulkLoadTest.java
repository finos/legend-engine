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

package org.finos.legend.engine.persistence.components.ingestmode.bulkload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.FileFormatType;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoad;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.digest.NoDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UDFBasedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.api.DataError;
import org.finos.legend.engine.persistence.components.relational.api.DryRunResult;
import org.finos.legend.engine.persistence.components.relational.api.ErrorCategory;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.IngestStatus;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2DigestUtil;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.h2.logicalplan.datasets.H2StagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.util.ValidationCategory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.TestUtils.mainTableName;
import static org.finos.legend.engine.persistence.components.TestUtils.testDatabaseName;
import static org.finos.legend.engine.persistence.components.TestUtils.testSchemaName;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;

public class BulkLoadTest extends BaseTest
{
    private static final String APPEND_TIME = "append_time";
    private static final String DIGEST = "digest";
    private static final String DIGEST_UDF = "LAKEHOUSE_MD5";
    private static final String BATCH_ID = "batch_id";
    private static final String EVENT_ID_1 = "xyz123";
    private static final String EVENT_ID_2 = "abc987";
    private static final Map<String, Object> ADDITIONAL_METADATA = Collections.singletonMap("watermark", "my_watermark_value");
    private static final String COL_INT = "col_int";
    private static final String COL_STRING = "col_string";
    private static final String COL_DECIMAL = "col_decimal";
    private static final String COL_DATETIME = "col_datetime";
    private static final String ingestRunId = "075605e3-bada-47d7-9ae9-7138f392fe22";

    private static Field col1 = Field.builder()
            .name(COL_INT)
            .type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty()))
            .build();
    private static Field col2 = Field.builder()
            .name(COL_STRING)
            .type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty()))
            .build();
    private static Field col3 = Field.builder()
            .name(COL_DECIMAL)
            .type(FieldType.of(DataType.DECIMAL, 5, 2))
            .build();
    private static Field col4 = Field.builder()
        .name(COL_DATETIME)
        .type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty()))
        .build();
    private static Field col2NonNullable = Field.builder()
        .name(COL_STRING)
        .type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty()))
        .nullable(false)
        .build();
    private static Field col3NonNullable = Field.builder()
        .name(COL_DECIMAL)
        .type(FieldType.of(DataType.DECIMAL, 5, 2))
        .nullable(false)
        .build();

    protected final ZonedDateTime fixedZonedDateTime_2000_01_01 = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    protected final Clock fixedClock_2000_01_01 = Clock.fixed(fixedZonedDateTime_2000_01_01.toInstant(), ZoneOffset.UTC);

    @Test
    public void testBulkLoadWithDigestNotGeneratedAuditEnabledNoBulkLoadEventId() throws Exception
    {
        String filePath = "src/test/resources/data/bulk-load/input/staged_file1.csv";

        BulkLoad bulkLoad = BulkLoad.builder()
                .batchIdField(BATCH_ID)
                .digestGenStrategy(NoDigestGenStrategy.builder().build())
                .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
                .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
                .stagedFilesDatasetProperties(
                        H2StagedFilesDatasetProperties.builder()
                            .fileFormat(FileFormatType.CSV)
                            .addAllFilePaths(Collections.singletonList(filePath)).build())
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
                .build();

        Dataset mainDataset = DatasetDefinition.builder()
                .database(testDatabaseName).group(testSchemaName).name(mainTableName).alias("my_alias")
                .schema(SchemaDefinition.builder().build())
                .build();

        Datasets datasets = Datasets.of(mainDataset, stagedFilesDataset);

        // Verify SQLs using generator
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(bulkLoad)
                .relationalSink(H2Sink.get())
                .collectStatistics(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .batchIdPattern("{NEXT_BATCH_ID_PATTERN}")
                .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"TEST_DB\".\"TEST\".\"main\"" +
            "(\"col_int\" INTEGER,\"col_string\" VARCHAR,\"col_decimal\" DECIMAL(5,2),\"col_datetime\" TIMESTAMP,\"batch_id\" INTEGER,\"append_time\" TIMESTAMP)";

        String expectedIngestSql = "INSERT INTO \"TEST_DB\".\"TEST\".\"main\" " +
                "(\"col_int\", \"col_string\", \"col_decimal\", \"col_datetime\", \"batch_id\", \"append_time\") " +
                "SELECT CONVERT(\"col_int\",INTEGER),CONVERT(\"col_string\",VARCHAR),CONVERT(\"col_decimal\",DECIMAL(5,2)),CONVERT(\"col_datetime\",TIMESTAMP)," +
                "{NEXT_BATCH_ID_PATTERN},'2000-01-01 00:00:00.000000' FROM CSVREAD('src/test/resources/data/bulk-load/input/staged_file1.csv'," +
                "'col_int,col_string,col_decimal,col_datetime',NULL)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM \"TEST_DB\".\"TEST\".\"main\" as my_alias WHERE my_alias.\"batch_id\" = {NEXT_BATCH_ID_PATTERN}", statsSql.get(ROWS_INSERTED));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{COL_INT, COL_STRING, COL_DECIMAL, COL_DATETIME, BATCH_ID, APPEND_TIME};

        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.ROWS_INSERTED.name(), 3);
        expectedStats.put(StatisticName.FILES_LOADED.name(), 1);
        expectedStats.put(StatisticName.ROWS_WITH_ERRORS.name(), 0);

        String expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table1.csv";

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.empty());
        executePlansAndVerifyResults(ingestor, datasets, schema, expectedDataPath, expectedStats, false);

        Map<String, Object> appendMetadata = h2Sink.executeQuery("select * from batch_metadata").get(0);
        verifyBulkLoadMetadata(appendMetadata, filePath, 1, Optional.empty(), Optional.empty());
    }

    @Test
    public void testBulkLoadWithDigestNotGeneratedAuditDisabled() throws Exception
    {
        String filePath = "src/test/resources/data/bulk-load/input/staged_file2.csv";

        BulkLoad bulkLoad = BulkLoad.builder()
            .digestGenStrategy(NoDigestGenStrategy.builder().build())
            .auditing(NoAuditing.builder().build())
            .batchIdField(BATCH_ID)
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList(filePath)).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database(testDatabaseName).group(testSchemaName).name(mainTableName).alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainDataset, stagedFilesDataset);

        // Verify SQLs using generator
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(H2Sink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .putAllAdditionalMetadata(ADDITIONAL_METADATA)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"TEST_DB\".\"TEST\".\"main\"" +
            "(\"col_int\" INTEGER,\"col_string\" VARCHAR,\"col_decimal\" DECIMAL(5,2),\"col_datetime\" TIMESTAMP,\"batch_id\" INTEGER)";

        String expectedIngestSql = "INSERT INTO \"TEST_DB\".\"TEST\".\"main\" " +
                "(\"col_int\", \"col_string\", \"col_decimal\", \"col_datetime\", \"batch_id\") " +
                "SELECT CONVERT(\"col_int\",INTEGER),CONVERT(\"col_string\",VARCHAR),CONVERT(\"col_decimal\",DECIMAL(5,2)),CONVERT(\"col_datetime\",TIMESTAMP)," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM \"batch_metadata\" as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM CSVREAD('src/test/resources/data/bulk-load/input/staged_file2.csv','col_int,col_string,col_decimal,col_datetime',NULL)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM \"TEST_DB\".\"TEST\".\"main\" as my_alias WHERE my_alias.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM \"batch_metadata\" as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')", statsSql.get(ROWS_INSERTED));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{COL_INT, COL_STRING, COL_DECIMAL, COL_DATETIME, BATCH_ID};

        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.FILES_LOADED.name(), 1);
        expectedStats.put(StatisticName.ROWS_WITH_ERRORS.name(), 0);
        expectedStats.put(StatisticName.ROWS_INSERTED.name(), 3);

        String expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table2.csv";

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.empty(), ADDITIONAL_METADATA);
        executePlansAndVerifyResults(ingestor, datasets, schema, expectedDataPath, expectedStats, false);
        Map<String, Object> appendMetadata = h2Sink.executeQuery("select * from batch_metadata").get(0);
        verifyBulkLoadMetadata(appendMetadata, filePath, 1, Optional.empty(), Optional.of(ADDITIONAL_METADATA));
    }

    @Test
    public void testBulkLoadWithDigestGeneratedAuditEnabled() throws Exception
    {
        // Register UDF
        H2DigestUtil.registerMD5Udf(h2Sink, DIGEST_UDF);

        String filePath = "src/test/resources/data/bulk-load/input/staged_file3.csv";

        BulkLoad bulkLoad = BulkLoad.builder()
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestUdfName(DIGEST_UDF).digestField(DIGEST).build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .batchIdField(BATCH_ID)
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList(filePath)).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database(testDatabaseName).group(testSchemaName).name(mainTableName).alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainDataset, stagedFilesDataset);

        // Verify SQLs using generator
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(H2Sink.get())
            .collectStatistics(true)
            .bulkLoadEventIdValue(EVENT_ID_1)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"TEST_DB\".\"TEST\".\"main\"" +
            "(\"col_int\" INTEGER,\"col_string\" VARCHAR,\"col_decimal\" DECIMAL(5,2),\"col_datetime\" TIMESTAMP,\"digest\" VARCHAR,\"batch_id\" INTEGER,\"append_time\" TIMESTAMP)";

        String expectedIngestSql = "INSERT INTO \"TEST_DB\".\"TEST\".\"main\" " +
                "(\"col_int\", \"col_string\", \"col_decimal\", \"col_datetime\", \"digest\", \"batch_id\", \"append_time\") " +
                "SELECT CONVERT(\"col_int\",INTEGER),CONVERT(\"col_string\",VARCHAR),CONVERT(\"col_decimal\",DECIMAL(5,2)),CONVERT(\"col_datetime\",TIMESTAMP)," +
                "LAKEHOUSE_MD5(ARRAY['col_int','col_string','col_decimal','col_datetime'],ARRAY[\"col_int\",\"col_string\",\"col_decimal\",\"col_datetime\"])," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM \"batch_metadata\" as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'),'2000-01-01 00:00:00.000000' FROM CSVREAD('src/test/resources/data/bulk-load/input/staged_file3.csv','col_int,col_string,col_decimal,col_datetime',NULL)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM \"TEST_DB\".\"TEST\".\"main\" as my_alias WHERE my_alias.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM \"batch_metadata\" as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')", statsSql.get(ROWS_INSERTED));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{COL_INT, COL_STRING, COL_DECIMAL, COL_DATETIME, DIGEST, BATCH_ID, APPEND_TIME};

        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.ROWS_INSERTED.name(), 3);
        expectedStats.put(StatisticName.FILES_LOADED.name(), 1);
        expectedStats.put(StatisticName.ROWS_WITH_ERRORS.name(), 0);

        String expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table3.csv";

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.of(EVENT_ID_1));
        executePlansAndVerifyResults(ingestor, datasets, schema, expectedDataPath, expectedStats, false);
        Map<String, Object> appendMetadata = h2Sink.executeQuery("select * from batch_metadata").get(0);
        verifyBulkLoadMetadata(appendMetadata, filePath, 1, Optional.of(EVENT_ID_1), Optional.empty());
    }

    @Test
    public void testBulkLoadWithDigestGeneratedWithFieldsToExcludeAuditEnabled() throws Exception
    {
        // Register UDF
        H2DigestUtil.registerMD5Udf(h2Sink, DIGEST_UDF);

        String filePath = "src/test/resources/data/bulk-load/input/staged_file3.csv";

        BulkLoad bulkLoad = BulkLoad.builder()
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestUdfName(DIGEST_UDF).digestField(DIGEST).addAllFieldsToExcludeFromDigest(Arrays.asList(col2.name(), col4.name())).build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .batchIdField(BATCH_ID)
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList(filePath)).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database(testDatabaseName).group(testSchemaName).name(mainTableName).alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainDataset, stagedFilesDataset);

        Map<String, Object> additionalMetadata = new HashMap<>();
        additionalMetadata.put("watermark", "my_watermark_value");
        additionalMetadata.put("external_uuid", "my_external_uuid");

        // Verify SQLs using generator
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(H2Sink.get())
            .collectStatistics(true)
            .bulkLoadEventIdValue(EVENT_ID_1)
            .putAllAdditionalMetadata(additionalMetadata)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"TEST_DB\".\"TEST\".\"main\"" +
            "(\"col_int\" INTEGER,\"col_string\" VARCHAR,\"col_decimal\" DECIMAL(5,2),\"col_datetime\" TIMESTAMP,\"digest\" VARCHAR,\"batch_id\" INTEGER,\"append_time\" TIMESTAMP)";

        String expectedIngestSql = "INSERT INTO \"TEST_DB\".\"TEST\".\"main\" " +
            "(\"col_int\", \"col_string\", \"col_decimal\", \"col_datetime\", \"digest\", \"batch_id\", \"append_time\") " +
            "SELECT CONVERT(\"col_int\",INTEGER),CONVERT(\"col_string\",VARCHAR),CONVERT(\"col_decimal\",DECIMAL(5,2)),CONVERT(\"col_datetime\",TIMESTAMP)," +
            "LAKEHOUSE_MD5(ARRAY['col_int','col_decimal'],ARRAY[\"col_int\",\"col_decimal\"])," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM \"batch_metadata\" as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'),'2000-01-01 00:00:00.000000' FROM CSVREAD('src/test/resources/data/bulk-load/input/staged_file3.csv','col_int,col_string,col_decimal,col_datetime',NULL)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM \"TEST_DB\".\"TEST\".\"main\" as my_alias WHERE my_alias.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM \"batch_metadata\" as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')", statsSql.get(ROWS_INSERTED));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{COL_INT, COL_STRING, COL_DECIMAL, COL_DATETIME, DIGEST, BATCH_ID, APPEND_TIME};

        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.ROWS_INSERTED.name(), 3);
        expectedStats.put(StatisticName.FILES_LOADED.name(), 1);
        expectedStats.put(StatisticName.ROWS_WITH_ERRORS.name(), 0);

        String expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table6.csv";

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.of(EVENT_ID_1), additionalMetadata);
        executePlansAndVerifyResults(ingestor, datasets, schema, expectedDataPath, expectedStats, false);
        Map<String, Object> appendMetadata = h2Sink.executeQuery("select * from batch_metadata").get(0);
        verifyBulkLoadMetadata(appendMetadata, filePath, 1, Optional.of(EVENT_ID_1), Optional.of(additionalMetadata));
    }

    @Test
    public void testBulkLoadWithDigestGeneratedAuditEnabledUpperCase() throws Exception
    {
        // Register UDF
        H2DigestUtil.registerMD5Udf(h2Sink, DIGEST_UDF);

        String filePath = "src/test/resources/data/bulk-load/input/staged_file4.csv";

        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestUdfName(DIGEST_UDF).digestField(DIGEST).build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList(filePath)).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database(testDatabaseName).group(testSchemaName).name(mainTableName).alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainDataset, stagedFilesDataset);

        Map<String, Object> additionalMetadata = new HashMap<>();
        additionalMetadata.put("watermark", "my_watermark_value");
        additionalMetadata.put("external_uuid", "my_external_uuid");

        // Verify SQLs using generator
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(H2Sink.get())
            .collectStatistics(true)
            .bulkLoadEventIdValue(EVENT_ID_1)
            .putAllAdditionalMetadata(additionalMetadata)
            .executionTimestampClock(fixedClock_2000_01_01)
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"TEST_DB\".\"TEST\".\"MAIN\"" +
            "(\"COL_INT\" INTEGER,\"COL_STRING\" VARCHAR,\"COL_DECIMAL\" DECIMAL(5,2),\"COL_DATETIME\" TIMESTAMP,\"DIGEST\" VARCHAR,\"BATCH_ID\" INTEGER,\"APPEND_TIME\" TIMESTAMP)";

        String expectedIngestSql = "INSERT INTO \"TEST_DB\".\"TEST\".\"MAIN\" " +
                "(\"COL_INT\", \"COL_STRING\", \"COL_DECIMAL\", \"COL_DATETIME\", \"DIGEST\", \"BATCH_ID\", \"APPEND_TIME\") " +
                "SELECT CONVERT(\"COL_INT\",INTEGER),CONVERT(\"COL_STRING\",VARCHAR),CONVERT(\"COL_DECIMAL\",DECIMAL(5,2)),CONVERT(\"COL_DATETIME\",TIMESTAMP)," +
                "LAKEHOUSE_MD5(ARRAY['COL_INT','COL_STRING','COL_DECIMAL','COL_DATETIME'],ARRAY[\"COL_INT\",\"COL_STRING\",\"COL_DECIMAL\",\"COL_DATETIME\"])," +
                "(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM \"BATCH_METADATA\" as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN'),'2000-01-01 00:00:00.000000' " +
                "FROM CSVREAD('src/test/resources/data/bulk-load/input/staged_file4.csv','COL_INT,COL_STRING,COL_DECIMAL,COL_DATETIME',NULL)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"ROWSINSERTED\" FROM \"TEST_DB\".\"TEST\".\"MAIN\" as my_alias WHERE my_alias.\"BATCH_ID\" = (SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM \"BATCH_METADATA\" as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN')", statsSql.get(ROWS_INSERTED));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{COL_INT.toUpperCase(), COL_STRING.toUpperCase(), COL_DECIMAL.toUpperCase(), COL_DATETIME.toUpperCase(), DIGEST.toUpperCase(), BATCH_ID.toUpperCase(), APPEND_TIME.toUpperCase()};

        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.ROWS_INSERTED.name(), 3);
        expectedStats.put(StatisticName.FILES_LOADED.name(), 1);
        expectedStats.put(StatisticName.ROWS_WITH_ERRORS.name(), 0);

        String expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table4.csv";

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.TO_UPPER, Optional.of(EVENT_ID_1), additionalMetadata);
        executePlansAndVerifyForCaseConversion(ingestor, datasets, schema, expectedDataPath, expectedStats);
        Map<String, Object> appendMetadata = h2Sink.executeQuery("select * from BATCH_METADATA").get(0);
        verifyBulkLoadMetadataForUpperCase(appendMetadata, filePath, 1, Optional.of(EVENT_ID_1), Optional.of(additionalMetadata));
    }

    @Test
    public void testBulkLoadWithDigestNotGeneratedAuditDisabledTwoBatches() throws Exception
    {
        String filePath = "src/test/resources/data/bulk-load/input/staged_file2.csv";

        BulkLoad bulkLoad = BulkLoad.builder()
            .digestGenStrategy(NoDigestGenStrategy.builder().build())
            .auditing(NoAuditing.builder().build())
            .batchIdField(BATCH_ID)
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList(filePath)).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database(testDatabaseName).group(testSchemaName).name(mainTableName).alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainDataset, stagedFilesDataset);


        // Verify execution using ingestor (first batch)
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{COL_INT, COL_STRING, COL_DECIMAL, COL_DATETIME, BATCH_ID};

        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.FILES_LOADED.name(), 1);
        expectedStats.put(StatisticName.ROWS_WITH_ERRORS.name(), 0);
        expectedStats.put(StatisticName.ROWS_INSERTED.name(), 3);

        String expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table2.csv";

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.of(EVENT_ID_1));
        executePlansAndVerifyResults(ingestor, datasets, schema, expectedDataPath, expectedStats, false);
        Map<String, Object> appendMetadata = h2Sink.executeQuery("select * from batch_metadata").get(0);
        verifyBulkLoadMetadata(appendMetadata, filePath, 1, Optional.of(EVENT_ID_1), Optional.empty());


        // Verify execution using ingestor (second batch)
        expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table5.csv";

        ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.of(EVENT_ID_2));
        executePlansAndVerifyResults(ingestor, datasets, schema, expectedDataPath, expectedStats, false);
        appendMetadata = h2Sink.executeQuery("select * from batch_metadata").get(0);
        verifyBulkLoadMetadata(appendMetadata, filePath, 1, Optional.of(EVENT_ID_1), Optional.empty());
        appendMetadata = h2Sink.executeQuery("select * from batch_metadata").get(1);
        verifyBulkLoadMetadata(appendMetadata, filePath, 2, Optional.of(EVENT_ID_2), Optional.empty());
    }

    @Test
    public void testBulkLoadDigestColumnNotProvided()
    {
        try
        {
            BulkLoad bulkLoad = BulkLoad.builder()
                    .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestUdfName(DIGEST_UDF).build())
                    .batchIdField(BATCH_ID)
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
                    .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestField(DIGEST).build())
                    .batchIdField(BATCH_ID)
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
                    .batchIdField(BATCH_ID)
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
                    .relationalSink(H2Sink.get())
                    .bulkLoadEventIdValue(EVENT_ID_1)
                    .collectStatistics(true)
                    .executionTimestampClock(fixedClock_2000_01_01)
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
    public void testBulkLoadStageHasPrimaryKey()
    {
        try
        {
            Field pkCol = Field.builder()
                .name("some_pk")
                .type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty()))
                .primaryKey(true)
                .build();

            BulkLoad bulkLoad = BulkLoad.builder()
                .batchIdField(BATCH_ID)
                .digestGenStrategy(NoDigestGenStrategy.builder().build())
                .auditing(NoAuditing.builder().build())
                .build();

            Dataset stagedFilesDataset = StagedFilesDataset.builder()
                .stagedFilesDatasetProperties(
                    H2StagedFilesDatasetProperties.builder()
                        .fileFormat(FileFormatType.CSV)
                        .addAllFilePaths(Collections.singletonList("src/test/resources/data/bulk-load/input/staged_file1.csv")).build())
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4, pkCol)).build())
                .build();

            Dataset mainDataset = DatasetDefinition.builder()
                .database("my_db").name("my_name").alias("my_alias")
                .schema(SchemaDefinition.builder().build())
                .build();

            RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(bulkLoad)
                .relationalSink(H2Sink.get())
                .bulkLoadEventIdValue(EVENT_ID_1)
                .collectStatistics(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();

            GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e.getMessage().contains("Primary key list must be empty"));
        }
    }

    @Test
    public void testBulkLoadMainHasPrimaryKey()
    {
        try
        {
            Field pkCol = Field.builder()
                .name("some_pk")
                .type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty()))
                .primaryKey(true)
                .build();

            BulkLoad bulkLoad = BulkLoad.builder()
                .batchIdField(BATCH_ID)
                .digestGenStrategy(NoDigestGenStrategy.builder().build())
                .auditing(NoAuditing.builder().build())
                .build();

            Dataset stagedFilesDataset = StagedFilesDataset.builder()
                .stagedFilesDatasetProperties(
                    H2StagedFilesDatasetProperties.builder()
                        .fileFormat(FileFormatType.CSV)
                        .addAllFilePaths(Collections.singletonList("src/test/resources/data/bulk-load/input/staged_file1.csv")).build())
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
                .build();

            Dataset mainDataset = DatasetDefinition.builder()
                .database("my_db").name("my_name").alias("my_alias")
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4, pkCol)).build())
                .build();

            RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(bulkLoad)
                .relationalSink(H2Sink.get())
                .bulkLoadEventIdValue(EVENT_ID_1)
                .collectStatistics(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();

            GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e.getMessage().contains("Primary key list must be empty"));
        }
    }

    @Test
    public void testBulkLoadMoreThanOneFile()
    {
        try
        {
            Dataset stagedFilesDataset = StagedFilesDataset.builder()
                .stagedFilesDatasetProperties(
                    H2StagedFilesDatasetProperties.builder()
                        .fileFormat(FileFormatType.CSV)
                        .addAllFilePaths(Arrays.asList("src/test/resources/data/bulk-load/input/staged_file1.csv", "src/test/resources/data/bulk-load/input/staged_file2.csv")).build())
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
                .build();
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e.getMessage().contains("Cannot build H2StagedFilesDatasetProperties, only 1 file per load supported"));
        }
    }

    @Test
    public void testBulkLoadNotCsvFile()
    {
        try
        {
            Dataset stagedFilesDataset = StagedFilesDataset.builder()
                .stagedFilesDatasetProperties(
                    H2StagedFilesDatasetProperties.builder()
                        .fileFormat(FileFormatType.JSON)
                        .addAllFilePaths(Arrays.asList("src/test/resources/data/bulk-load/input/staged_file1.json")).build())
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
                .build();
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e.getMessage().contains("Cannot build H2StagedFilesDatasetProperties, only CSV file loading supported"));
        }
    }

    @Test
    public void testBulkLoadDryRunSuccess()
    {
        String filePath = "src/test/resources/data/bulk-load/input/good_file_with_edge_case.csv";

        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(NoDigestGenStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList(filePath)).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database(testDatabaseName).group(testSchemaName).name(mainTableName).alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainDataset, stagedFilesDataset);

        // Verify SQLs using generator
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(H2Sink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .batchIdPattern("{NEXT_BATCH_ID_PATTERN}")
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"TEST_DB\".\"TEST\".\"main\"" +
            "(\"col_int\" INTEGER,\"col_string\" VARCHAR,\"col_decimal\" DECIMAL(5,2),\"col_datetime\" TIMESTAMP,\"batch_id\" INTEGER,\"append_time\" TIMESTAMP)";

        String expectedIngestSql = "INSERT INTO \"TEST_DB\".\"TEST\".\"main\" " +
            "(\"col_int\", \"col_string\", \"col_decimal\", \"col_datetime\", \"batch_id\", \"append_time\") " +
            "SELECT CONVERT(\"col_int\",INTEGER),CONVERT(\"col_string\",VARCHAR),CONVERT(\"col_decimal\",DECIMAL(5,2)),CONVERT(\"col_datetime\",TIMESTAMP)," +
            "{NEXT_BATCH_ID_PATTERN},'2000-01-01 00:00:00.000000' FROM CSVREAD('src/test/resources/data/bulk-load/input/good_file_with_edge_case.csv'," +
            "'col_int,col_string,col_decimal,col_datetime',NULL)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM \"TEST_DB\".\"TEST\".\"main\" as my_alias WHERE my_alias.\"batch_id\" = {NEXT_BATCH_ID_PATTERN}", statsSql.get(ROWS_INSERTED));

        // Checking dry run
        String expectedDryRunPreActionSql = "CREATE TABLE IF NOT EXISTS \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\"" +
            "(\"col_int\" VARCHAR,\"col_string\" VARCHAR,\"col_decimal\" VARCHAR,\"col_datetime\" VARCHAR,\"legend_persistence_file\" VARCHAR,\"legend_persistence_row_number\" BIGINT)";

        String expectedDryRunDeleteSql = "DELETE FROM \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" as main_validation_lp_yosulf";

        String expectedDryRunLoadSQl = "INSERT INTO \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" " +
            "(\"col_int\", \"col_string\", \"col_decimal\", \"col_datetime\", \"legend_persistence_file\", \"legend_persistence_row_number\") " +
            "SELECT CONVERT(\"col_int\",VARCHAR),CONVERT(\"col_string\",VARCHAR),CONVERT(\"col_decimal\",VARCHAR),CONVERT(\"col_datetime\",VARCHAR)," +
            "'src/test/resources/data/bulk-load/input/good_file_with_edge_case.csv',ROW_NUMBER() OVER () " +
            "FROM CSVREAD('src/test/resources/data/bulk-load/input/good_file_with_edge_case.csv','col_int,col_string,col_decimal,col_datetime',NULL)";

        String expectedDryRunDatatypeValidationSql1 = "SELECT main_validation_lp_yosulf.\"col_int\",main_validation_lp_yosulf.\"col_string\",main_validation_lp_yosulf.\"col_decimal\",main_validation_lp_yosulf.\"col_datetime\",main_validation_lp_yosulf.\"legend_persistence_file\",main_validation_lp_yosulf.\"legend_persistence_row_number\" " +
            "FROM \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" as main_validation_lp_yosulf " +
            "WHERE (NOT (main_validation_lp_yosulf.\"col_int\" IS NULL)) AND (CAST(main_validation_lp_yosulf.\"col_int\" AS INTEGER) IS NULL) LIMIT 20";

        String expectedDryRunDatatypeValidationSql2 = "SELECT main_validation_lp_yosulf.\"col_int\",main_validation_lp_yosulf.\"col_string\",main_validation_lp_yosulf.\"col_decimal\",main_validation_lp_yosulf.\"col_datetime\",main_validation_lp_yosulf.\"legend_persistence_file\",main_validation_lp_yosulf.\"legend_persistence_row_number\" " +
            "FROM \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" as main_validation_lp_yosulf " +
            "WHERE (NOT (main_validation_lp_yosulf.\"col_decimal\" IS NULL)) AND (CAST(main_validation_lp_yosulf.\"col_decimal\" AS DECIMAL(5,2)) IS NULL) LIMIT 20";

        String expectedDryRunDatatypeValidationSql3 = "SELECT main_validation_lp_yosulf.\"col_int\",main_validation_lp_yosulf.\"col_string\",main_validation_lp_yosulf.\"col_decimal\",main_validation_lp_yosulf.\"col_datetime\",main_validation_lp_yosulf.\"legend_persistence_file\",main_validation_lp_yosulf.\"legend_persistence_row_number\" " +
            "FROM \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" as main_validation_lp_yosulf " +
            "WHERE (NOT (main_validation_lp_yosulf.\"col_datetime\" IS NULL)) AND (CAST(main_validation_lp_yosulf.\"col_datetime\" AS TIMESTAMP) IS NULL) LIMIT 20";

        String expectedDryRunPostCleanupSql = "DROP TABLE IF EXISTS \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\"";

        Assertions.assertEquals(expectedDryRunPreActionSql, operations.dryRunPreActionsSql().get(0));
        Assertions.assertEquals(expectedDryRunDeleteSql, operations.dryRunSql().get(0));
        Assertions.assertEquals(expectedDryRunLoadSQl, operations.dryRunSql().get(1));
        Assertions.assertNull(operations.dryRunValidationSql().get(ValidationCategory.NULL_VALUE));
        Assertions.assertEquals(expectedDryRunDatatypeValidationSql1, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).get(0).getTwo());
        Assertions.assertEquals(expectedDryRunDatatypeValidationSql2, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).get(1).getTwo());
        Assertions.assertEquals(expectedDryRunDatatypeValidationSql3, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).get(2).getTwo());
        Assertions.assertEquals(3, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).size());
        Assertions.assertEquals(expectedDryRunPostCleanupSql, operations.dryRunPostCleanupSql().get(0));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.empty());
        ingestor.initExecutor(JdbcConnection.of(h2Sink.connection()));
        ingestor.initDatasets(datasets);
        DryRunResult dryRunResult = ingestor.dryRun();

        Assertions.assertEquals(dryRunResult.status(), IngestStatus.SUCCEEDED);
        Assertions.assertTrue(dryRunResult.errorRecords().isEmpty());
    }

    @Test
    public void testBulkLoadDryRunFailure()
    {
        String filePath = "src/test/resources/data/bulk-load/input/bad_file.csv";

        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(NoDigestGenStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList(filePath)).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2NonNullable, col3NonNullable, col4)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database(testDatabaseName).group(testSchemaName).name(mainTableName).alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainDataset, stagedFilesDataset);

        // Verify SQLs using generator
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(H2Sink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .batchIdPattern("{NEXT_BATCH_ID_PATTERN}")
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"TEST_DB\".\"TEST\".\"main\"" +
            "(\"col_int\" INTEGER,\"col_string\" VARCHAR NOT NULL,\"col_decimal\" DECIMAL(5,2) NOT NULL,\"col_datetime\" TIMESTAMP,\"batch_id\" INTEGER,\"append_time\" TIMESTAMP)";

        String expectedIngestSql = "INSERT INTO \"TEST_DB\".\"TEST\".\"main\" " +
            "(\"col_int\", \"col_string\", \"col_decimal\", \"col_datetime\", \"batch_id\", \"append_time\") " +
            "SELECT CONVERT(\"col_int\",INTEGER),CONVERT(\"col_string\",VARCHAR),CONVERT(\"col_decimal\",DECIMAL(5,2)),CONVERT(\"col_datetime\",TIMESTAMP)," +
            "{NEXT_BATCH_ID_PATTERN},'2000-01-01 00:00:00.000000' FROM CSVREAD('src/test/resources/data/bulk-load/input/bad_file.csv'," +
            "'col_int,col_string,col_decimal,col_datetime',NULL)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM \"TEST_DB\".\"TEST\".\"main\" as my_alias WHERE my_alias.\"batch_id\" = {NEXT_BATCH_ID_PATTERN}", statsSql.get(ROWS_INSERTED));

        // Checking dry run
        String expectedDryRunPreActionSql = "CREATE TABLE IF NOT EXISTS \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\"" +
            "(\"col_int\" VARCHAR,\"col_string\" VARCHAR,\"col_decimal\" VARCHAR,\"col_datetime\" VARCHAR,\"legend_persistence_file\" VARCHAR,\"legend_persistence_row_number\" BIGINT)";

        String expectedDryRunDeleteSql = "DELETE FROM \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" as main_validation_lp_yosulf";

        String expectedDryRunLoadSQl = "INSERT INTO \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" " +
            "(\"col_int\", \"col_string\", \"col_decimal\", \"col_datetime\", \"legend_persistence_file\", \"legend_persistence_row_number\") " +
            "SELECT CONVERT(\"col_int\",VARCHAR),CONVERT(\"col_string\",VARCHAR),CONVERT(\"col_decimal\",VARCHAR),CONVERT(\"col_datetime\",VARCHAR)," +
            "'src/test/resources/data/bulk-load/input/bad_file.csv',ROW_NUMBER() OVER () " +
            "FROM CSVREAD('src/test/resources/data/bulk-load/input/bad_file.csv','col_int,col_string,col_decimal,col_datetime',NULL)";

        String expectedDryRunNullValidationSql = "SELECT main_validation_lp_yosulf.\"col_int\",main_validation_lp_yosulf.\"col_string\",main_validation_lp_yosulf.\"col_decimal\",main_validation_lp_yosulf.\"col_datetime\",main_validation_lp_yosulf.\"legend_persistence_file\",main_validation_lp_yosulf.\"legend_persistence_row_number\" " +
            "FROM \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" as main_validation_lp_yosulf " +
            "WHERE (main_validation_lp_yosulf.\"col_string\" IS NULL) OR (main_validation_lp_yosulf.\"col_decimal\" IS NULL) LIMIT 20";

        String expectedDryRunDatatypeValidationSql1 = "SELECT main_validation_lp_yosulf.\"col_int\",main_validation_lp_yosulf.\"col_string\",main_validation_lp_yosulf.\"col_decimal\",main_validation_lp_yosulf.\"col_datetime\",main_validation_lp_yosulf.\"legend_persistence_file\",main_validation_lp_yosulf.\"legend_persistence_row_number\" " +
            "FROM \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" as main_validation_lp_yosulf " +
            "WHERE (NOT (main_validation_lp_yosulf.\"col_int\" IS NULL)) AND (CAST(main_validation_lp_yosulf.\"col_int\" AS INTEGER) IS NULL) LIMIT 20";

        String expectedDryRunDatatypeValidationSql2 = "SELECT main_validation_lp_yosulf.\"col_int\",main_validation_lp_yosulf.\"col_string\",main_validation_lp_yosulf.\"col_decimal\",main_validation_lp_yosulf.\"col_datetime\",main_validation_lp_yosulf.\"legend_persistence_file\",main_validation_lp_yosulf.\"legend_persistence_row_number\" " +
            "FROM \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" as main_validation_lp_yosulf " +
            "WHERE (NOT (main_validation_lp_yosulf.\"col_decimal\" IS NULL)) AND (CAST(main_validation_lp_yosulf.\"col_decimal\" AS DECIMAL(5,2)) IS NULL) LIMIT 20";

        String expectedDryRunDatatypeValidationSql3 = "SELECT main_validation_lp_yosulf.\"col_int\",main_validation_lp_yosulf.\"col_string\",main_validation_lp_yosulf.\"col_decimal\",main_validation_lp_yosulf.\"col_datetime\",main_validation_lp_yosulf.\"legend_persistence_file\",main_validation_lp_yosulf.\"legend_persistence_row_number\" " +
            "FROM \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" as main_validation_lp_yosulf " +
            "WHERE (NOT (main_validation_lp_yosulf.\"col_datetime\" IS NULL)) AND (CAST(main_validation_lp_yosulf.\"col_datetime\" AS TIMESTAMP) IS NULL) LIMIT 20";

        String expectedDryRunPostCleanupSql = "DROP TABLE IF EXISTS \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\"";

        Assertions.assertEquals(expectedDryRunPreActionSql, operations.dryRunPreActionsSql().get(0));
        Assertions.assertEquals(expectedDryRunDeleteSql, operations.dryRunSql().get(0));
        Assertions.assertEquals(expectedDryRunLoadSQl, operations.dryRunSql().get(1));
        Assertions.assertEquals(expectedDryRunNullValidationSql, operations.dryRunValidationSql().get(ValidationCategory.NULL_VALUE).get(0).getTwo());
        Assertions.assertEquals(1, operations.dryRunValidationSql().get(ValidationCategory.NULL_VALUE).size());
        Assertions.assertEquals(expectedDryRunDatatypeValidationSql1, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).get(0).getTwo());
        Assertions.assertEquals(expectedDryRunDatatypeValidationSql2, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).get(1).getTwo());
        Assertions.assertEquals(expectedDryRunDatatypeValidationSql3, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).get(2).getTwo());
        Assertions.assertEquals(3, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).size());
        Assertions.assertEquals(expectedDryRunPostCleanupSql, operations.dryRunPostCleanupSql().get(0));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.empty());
        ingestor.initExecutor(JdbcConnection.of(h2Sink.connection()));
        ingestor.initDatasets(datasets);
        DryRunResult dryRunResult = ingestor.dryRun();

        List<DataError> expectedErrorRecords = Arrays.asList(DataError.builder()
            .errorCategory(ErrorCategory.CHECK_NULL_CONSTRAINT)
            .errorRecord("{\"col_int\":\"??\",\"col_decimal\":null,\"col_string\":\"Andy\",\"col_datetime\":\"2022-01-99 00:00:00.0\"}")
            .errorMessage("Null values found in non-nullable column")
            .putAllErrorDetails(buildErrorDetails(filePath, col3NonNullable.name(), 1L))
            .build(), DataError.builder()
            .errorCategory(ErrorCategory.CHECK_NULL_CONSTRAINT)
            .errorRecord("{\"col_int\":\"2\",\"col_decimal\":\"NaN\",\"col_string\":null,\"col_datetime\":\"2022-01-12 00:00:00.0\"}")
            .errorMessage("Null values found in non-nullable column")
            .putAllErrorDetails(buildErrorDetails(filePath, col2NonNullable.name(), 2L))
            .build(), DataError.builder()
            .errorCategory(ErrorCategory.TYPE_CONVERSION)
            .errorRecord("{\"col_int\":\"??\",\"col_decimal\":null,\"col_string\":\"Andy\",\"col_datetime\":\"2022-01-99 00:00:00.0\"}")
            .errorMessage("Unable to type cast column")
            .putAllErrorDetails(buildErrorDetails(filePath, col1.name(), 1L))
            .build(), DataError.builder()
            .errorCategory(ErrorCategory.TYPE_CONVERSION)
            .errorRecord("{\"col_int\":\"??\",\"col_decimal\":null,\"col_string\":\"Andy\",\"col_datetime\":\"2022-01-99 00:00:00.0\"}")
            .errorMessage("Unable to type cast column")
            .putAllErrorDetails(buildErrorDetails(filePath, col4.name(), 1L))
            .build(), DataError.builder()
            .errorCategory(ErrorCategory.TYPE_CONVERSION)
            .errorRecord("{\"col_int\":\"2\",\"col_decimal\":\"NaN\",\"col_string\":null,\"col_datetime\":\"2022-01-12 00:00:00.0\"}")
            .errorMessage("Unable to type cast column")
            .putAllErrorDetails(buildErrorDetails(filePath, col3.name(), 2L))
            .build());

        Assertions.assertEquals(IngestStatus.FAILED, dryRunResult.status());
        Assertions.assertEquals(new HashSet<>(expectedErrorRecords), new HashSet<>(dryRunResult.errorRecords()));
    }

    @Test
    public void testBulkLoadDryRunFailureWithSampleRowCountWithUpperCase()
    {
        String filePath = "src/test/resources/data/bulk-load/input/bad_file.csv";

        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(NoDigestGenStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList(filePath)).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2NonNullable, col3NonNullable, col4)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database(testDatabaseName).group(testSchemaName).name(mainTableName).alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainDataset, stagedFilesDataset);

        // Verify SQLs using generator
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(H2Sink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .batchIdPattern("{NEXT_BATCH_ID_PATTERN}")
            .ingestRunId(ingestRunId)
            .sampleRowCount(3)
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"TEST_DB\".\"TEST\".\"MAIN\"" +
            "(\"COL_INT\" INTEGER,\"COL_STRING\" VARCHAR NOT NULL,\"COL_DECIMAL\" DECIMAL(5,2) NOT NULL,\"COL_DATETIME\" TIMESTAMP,\"BATCH_ID\" INTEGER,\"APPEND_TIME\" TIMESTAMP)";

        String expectedIngestSql = "INSERT INTO \"TEST_DB\".\"TEST\".\"MAIN\" " +
            "(\"COL_INT\", \"COL_STRING\", \"COL_DECIMAL\", \"COL_DATETIME\", \"BATCH_ID\", \"APPEND_TIME\") " +
            "SELECT CONVERT(\"COL_INT\",INTEGER),CONVERT(\"COL_STRING\",VARCHAR),CONVERT(\"COL_DECIMAL\",DECIMAL(5,2)),CONVERT(\"COL_DATETIME\",TIMESTAMP)," +
            "{NEXT_BATCH_ID_PATTERN},'2000-01-01 00:00:00.000000' " +
            "FROM CSVREAD('src/test/resources/data/bulk-load/input/bad_file.csv','COL_INT,COL_STRING,COL_DECIMAL,COL_DATETIME',NULL)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"ROWSINSERTED\" FROM \"TEST_DB\".\"TEST\".\"MAIN\" as my_alias WHERE my_alias.\"BATCH_ID\" = {NEXT_BATCH_ID_PATTERN}", statsSql.get(ROWS_INSERTED));

        // Checking dry run
        String expectedDryRunPreActionSql = "CREATE TABLE IF NOT EXISTS \"TEST_DB\".\"TEST\".\"MAIN_VALIDATION_LP_YOSULF\"" +
            "(\"COL_INT\" VARCHAR,\"COL_STRING\" VARCHAR,\"COL_DECIMAL\" VARCHAR,\"COL_DATETIME\" VARCHAR,\"LEGEND_PERSISTENCE_FILE\" VARCHAR,\"LEGEND_PERSISTENCE_ROW_NUMBER\" BIGINT)";

        String expectedDryRunDeleteSql = "DELETE FROM \"TEST_DB\".\"TEST\".\"MAIN_VALIDATION_LP_YOSULF\" as MAIN_validation_lp_yosulf";

        String expectedDryRunLoadSQl = "INSERT INTO \"TEST_DB\".\"TEST\".\"MAIN_VALIDATION_LP_YOSULF\" " +
            "(\"COL_INT\", \"COL_STRING\", \"COL_DECIMAL\", \"COL_DATETIME\", \"LEGEND_PERSISTENCE_FILE\", \"LEGEND_PERSISTENCE_ROW_NUMBER\") " +
            "SELECT CONVERT(\"COL_INT\",VARCHAR),CONVERT(\"COL_STRING\",VARCHAR),CONVERT(\"COL_DECIMAL\",VARCHAR),CONVERT(\"COL_DATETIME\",VARCHAR),'src/test/resources/data/bulk-load/input/bad_file.csv',ROW_NUMBER() OVER () " +
            "FROM CSVREAD('src/test/resources/data/bulk-load/input/bad_file.csv','COL_INT,COL_STRING,COL_DECIMAL,COL_DATETIME',NULL)";

        String expectedDryRunNullValidationSql = "SELECT MAIN_validation_lp_yosulf.\"COL_INT\",MAIN_validation_lp_yosulf.\"COL_STRING\",MAIN_validation_lp_yosulf.\"COL_DECIMAL\",MAIN_validation_lp_yosulf.\"COL_DATETIME\",MAIN_validation_lp_yosulf.\"LEGEND_PERSISTENCE_FILE\",MAIN_validation_lp_yosulf.\"LEGEND_PERSISTENCE_ROW_NUMBER\" " +
            "FROM \"TEST_DB\".\"TEST\".\"MAIN_VALIDATION_LP_YOSULF\" as MAIN_validation_lp_yosulf " +
            "WHERE (MAIN_validation_lp_yosulf.\"COL_STRING\" IS NULL) OR (MAIN_validation_lp_yosulf.\"COL_DECIMAL\" IS NULL) LIMIT 3";

        String expectedDryRunDatatypeValidationSql1 = "SELECT MAIN_validation_lp_yosulf.\"COL_INT\",MAIN_validation_lp_yosulf.\"COL_STRING\",MAIN_validation_lp_yosulf.\"COL_DECIMAL\",MAIN_validation_lp_yosulf.\"COL_DATETIME\",MAIN_validation_lp_yosulf.\"LEGEND_PERSISTENCE_FILE\",MAIN_validation_lp_yosulf.\"LEGEND_PERSISTENCE_ROW_NUMBER\" " +
            "FROM \"TEST_DB\".\"TEST\".\"MAIN_VALIDATION_LP_YOSULF\" as MAIN_validation_lp_yosulf " +
            "WHERE (NOT (MAIN_validation_lp_yosulf.\"COL_INT\" IS NULL)) AND (CAST(MAIN_validation_lp_yosulf.\"COL_INT\" AS INTEGER) IS NULL) LIMIT 3";

        String expectedDryRunDatatypeValidationSql2 = "SELECT MAIN_validation_lp_yosulf.\"COL_INT\",MAIN_validation_lp_yosulf.\"COL_STRING\",MAIN_validation_lp_yosulf.\"COL_DECIMAL\",MAIN_validation_lp_yosulf.\"COL_DATETIME\",MAIN_validation_lp_yosulf.\"LEGEND_PERSISTENCE_FILE\",MAIN_validation_lp_yosulf.\"LEGEND_PERSISTENCE_ROW_NUMBER\" " +
            "FROM \"TEST_DB\".\"TEST\".\"MAIN_VALIDATION_LP_YOSULF\" as MAIN_validation_lp_yosulf " +
            "WHERE (NOT (MAIN_validation_lp_yosulf.\"COL_DECIMAL\" IS NULL)) AND (CAST(MAIN_validation_lp_yosulf.\"COL_DECIMAL\" AS DECIMAL(5,2)) IS NULL) LIMIT 3";

        String expectedDryRunDatatypeValidationSql3 = "SELECT MAIN_validation_lp_yosulf.\"COL_INT\",MAIN_validation_lp_yosulf.\"COL_STRING\",MAIN_validation_lp_yosulf.\"COL_DECIMAL\",MAIN_validation_lp_yosulf.\"COL_DATETIME\",MAIN_validation_lp_yosulf.\"LEGEND_PERSISTENCE_FILE\",MAIN_validation_lp_yosulf.\"LEGEND_PERSISTENCE_ROW_NUMBER\" " +
            "FROM \"TEST_DB\".\"TEST\".\"MAIN_VALIDATION_LP_YOSULF\" as MAIN_validation_lp_yosulf " +
            "WHERE (NOT (MAIN_validation_lp_yosulf.\"COL_DATETIME\" IS NULL)) AND (CAST(MAIN_validation_lp_yosulf.\"COL_DATETIME\" AS TIMESTAMP) IS NULL) LIMIT 3";

        String expectedDryRunPostCleanupSql = "DROP TABLE IF EXISTS \"TEST_DB\".\"TEST\".\"MAIN_VALIDATION_LP_YOSULF\"";

        Assertions.assertEquals(expectedDryRunPreActionSql, operations.dryRunPreActionsSql().get(0));
        Assertions.assertEquals(expectedDryRunDeleteSql, operations.dryRunSql().get(0));
        Assertions.assertEquals(expectedDryRunLoadSQl, operations.dryRunSql().get(1));
        Assertions.assertEquals(expectedDryRunNullValidationSql, operations.dryRunValidationSql().get(ValidationCategory.NULL_VALUE).get(0).getTwo());
        Assertions.assertEquals(1, operations.dryRunValidationSql().get(ValidationCategory.NULL_VALUE).size());
        Assertions.assertEquals(expectedDryRunDatatypeValidationSql1, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).get(0).getTwo());
        Assertions.assertEquals(expectedDryRunDatatypeValidationSql2, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).get(1).getTwo());
        Assertions.assertEquals(expectedDryRunDatatypeValidationSql3, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).get(2).getTwo());
        Assertions.assertEquals(3, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).size());
        Assertions.assertEquals(expectedDryRunPostCleanupSql, operations.dryRunPostCleanupSql().get(0));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.TO_UPPER, 3);
        ingestor.initExecutor(JdbcConnection.of(h2Sink.connection()));
        ingestor.initDatasets(datasets);
        DryRunResult dryRunResult = ingestor.dryRun();

        List<DataError> expectedErrorRecords = Arrays.asList(DataError.builder()
            .errorCategory(ErrorCategory.CHECK_NULL_CONSTRAINT)
            .errorRecord("{\"COL_STRING\":\"Andy\",\"COL_DATETIME\":\"2022-01-99 00:00:00.0\",\"COL_INT\":\"??\",\"COL_DECIMAL\":null}")
            .errorMessage("Null values found in non-nullable column")
            .putAllErrorDetails(buildErrorDetails(filePath, col3NonNullable.name().toUpperCase(), 1L))
            .build(), DataError.builder()
            .errorCategory(ErrorCategory.CHECK_NULL_CONSTRAINT)
            .errorRecord("{\"COL_STRING\":null,\"COL_DATETIME\":\"2022-01-12 00:00:00.0\",\"COL_INT\":\"2\",\"COL_DECIMAL\":\"NaN\"}")
            .errorMessage("Null values found in non-nullable column")
            .putAllErrorDetails(buildErrorDetails(filePath, col2NonNullable.name().toUpperCase(), 2L))
            .build(), DataError.builder()
            .errorCategory(ErrorCategory.TYPE_CONVERSION)
            .errorRecord("{\"COL_STRING\":\"Andy\",\"COL_DATETIME\":\"2022-01-99 00:00:00.0\",\"COL_INT\":\"??\",\"COL_DECIMAL\":null}")
            .errorMessage("Unable to type cast column")
            .putAllErrorDetails(buildErrorDetails(filePath, col1.name().toUpperCase(), 1L))
            .build());

        Assertions.assertEquals(IngestStatus.FAILED, dryRunResult.status());
        Assertions.assertEquals(new HashSet<>(expectedErrorRecords), new HashSet<>(dryRunResult.errorRecords()));
    }

    @Test
    public void testBulkLoadDryRunFailureWithFileNotFound()
    {
        String filePath = "src/test/resources/data/bulk-load/input/non_existent_file.csv";

        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(NoDigestGenStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                H2StagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(Collections.singletonList(filePath)).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2NonNullable, col3NonNullable, col4)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database(testDatabaseName).group(testSchemaName).name(mainTableName).alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainDataset, stagedFilesDataset);

        // Verify SQLs using generator
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(H2Sink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .batchIdPattern("{NEXT_BATCH_ID_PATTERN}")
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"TEST_DB\".\"TEST\".\"main\"" +
            "(\"col_int\" INTEGER,\"col_string\" VARCHAR NOT NULL,\"col_decimal\" DECIMAL(5,2) NOT NULL,\"col_datetime\" TIMESTAMP,\"batch_id\" INTEGER,\"append_time\" TIMESTAMP)";

        String expectedIngestSql = "INSERT INTO \"TEST_DB\".\"TEST\".\"main\" " +
            "(\"col_int\", \"col_string\", \"col_decimal\", \"col_datetime\", \"batch_id\", \"append_time\") " +
            "SELECT CONVERT(\"col_int\",INTEGER),CONVERT(\"col_string\",VARCHAR),CONVERT(\"col_decimal\",DECIMAL(5,2)),CONVERT(\"col_datetime\",TIMESTAMP)," +
            "{NEXT_BATCH_ID_PATTERN},'2000-01-01 00:00:00.000000' FROM CSVREAD('src/test/resources/data/bulk-load/input/non_existent_file.csv'," +
            "'col_int,col_string,col_decimal,col_datetime',NULL)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM \"TEST_DB\".\"TEST\".\"main\" as my_alias WHERE my_alias.\"batch_id\" = {NEXT_BATCH_ID_PATTERN}", statsSql.get(ROWS_INSERTED));

        // Checking dry run
        String expectedDryRunPreActionSql = "CREATE TABLE IF NOT EXISTS \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\"" +
            "(\"col_int\" VARCHAR,\"col_string\" VARCHAR,\"col_decimal\" VARCHAR,\"col_datetime\" VARCHAR,\"legend_persistence_file\" VARCHAR,\"legend_persistence_row_number\" BIGINT)";

        String expectedDryRunDeleteSql = "DELETE FROM \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" as main_validation_lp_yosulf";

        String expectedDryRunLoadSQl = "INSERT INTO \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" " +
            "(\"col_int\", \"col_string\", \"col_decimal\", \"col_datetime\", \"legend_persistence_file\", \"legend_persistence_row_number\") " +
            "SELECT CONVERT(\"col_int\",VARCHAR),CONVERT(\"col_string\",VARCHAR),CONVERT(\"col_decimal\",VARCHAR),CONVERT(\"col_datetime\",VARCHAR)," +
            "'src/test/resources/data/bulk-load/input/non_existent_file.csv',ROW_NUMBER() OVER () " +
            "FROM CSVREAD('src/test/resources/data/bulk-load/input/non_existent_file.csv','col_int,col_string,col_decimal,col_datetime',NULL)";

        String expectedDryRunNullValidationSql = "SELECT main_validation_lp_yosulf.\"col_int\",main_validation_lp_yosulf.\"col_string\",main_validation_lp_yosulf.\"col_decimal\",main_validation_lp_yosulf.\"col_datetime\",main_validation_lp_yosulf.\"legend_persistence_file\",main_validation_lp_yosulf.\"legend_persistence_row_number\" " +
            "FROM \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" as main_validation_lp_yosulf " +
            "WHERE (main_validation_lp_yosulf.\"col_string\" IS NULL) OR (main_validation_lp_yosulf.\"col_decimal\" IS NULL) LIMIT 20";

        String expectedDryRunDatatypeValidationSql1 = "SELECT main_validation_lp_yosulf.\"col_int\",main_validation_lp_yosulf.\"col_string\",main_validation_lp_yosulf.\"col_decimal\",main_validation_lp_yosulf.\"col_datetime\",main_validation_lp_yosulf.\"legend_persistence_file\",main_validation_lp_yosulf.\"legend_persistence_row_number\" " +
            "FROM \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" as main_validation_lp_yosulf " +
            "WHERE (NOT (main_validation_lp_yosulf.\"col_int\" IS NULL)) AND (CAST(main_validation_lp_yosulf.\"col_int\" AS INTEGER) IS NULL) LIMIT 20";

        String expectedDryRunDatatypeValidationSql2 = "SELECT main_validation_lp_yosulf.\"col_int\",main_validation_lp_yosulf.\"col_string\",main_validation_lp_yosulf.\"col_decimal\",main_validation_lp_yosulf.\"col_datetime\",main_validation_lp_yosulf.\"legend_persistence_file\",main_validation_lp_yosulf.\"legend_persistence_row_number\" " +
            "FROM \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" as main_validation_lp_yosulf " +
            "WHERE (NOT (main_validation_lp_yosulf.\"col_decimal\" IS NULL)) AND (CAST(main_validation_lp_yosulf.\"col_decimal\" AS DECIMAL(5,2)) IS NULL) LIMIT 20";

        String expectedDryRunDatatypeValidationSql3 = "SELECT main_validation_lp_yosulf.\"col_int\",main_validation_lp_yosulf.\"col_string\",main_validation_lp_yosulf.\"col_decimal\",main_validation_lp_yosulf.\"col_datetime\",main_validation_lp_yosulf.\"legend_persistence_file\",main_validation_lp_yosulf.\"legend_persistence_row_number\" " +
            "FROM \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\" as main_validation_lp_yosulf " +
            "WHERE (NOT (main_validation_lp_yosulf.\"col_datetime\" IS NULL)) AND (CAST(main_validation_lp_yosulf.\"col_datetime\" AS TIMESTAMP) IS NULL) LIMIT 20";

        String expectedDryRunPostCleanupSql = "DROP TABLE IF EXISTS \"TEST_DB\".\"TEST\".\"main_validation_lp_yosulf\"";

        Assertions.assertEquals(expectedDryRunPreActionSql, operations.dryRunPreActionsSql().get(0));
        Assertions.assertEquals(expectedDryRunDeleteSql, operations.dryRunSql().get(0));
        Assertions.assertEquals(expectedDryRunLoadSQl, operations.dryRunSql().get(1));
        Assertions.assertEquals(expectedDryRunNullValidationSql, operations.dryRunValidationSql().get(ValidationCategory.NULL_VALUE).get(0).getTwo());
        Assertions.assertEquals(1, operations.dryRunValidationSql().get(ValidationCategory.NULL_VALUE).size());
        Assertions.assertEquals(expectedDryRunDatatypeValidationSql1, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).get(0).getTwo());
        Assertions.assertEquals(expectedDryRunDatatypeValidationSql2, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).get(1).getTwo());
        Assertions.assertEquals(expectedDryRunDatatypeValidationSql3, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).get(2).getTwo());
        Assertions.assertEquals(3, operations.dryRunValidationSql().get(ValidationCategory.TYPE_CONVERSION).size());
        Assertions.assertEquals(expectedDryRunPostCleanupSql, operations.dryRunPostCleanupSql().get(0));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.empty());
        ingestor.initExecutor(JdbcConnection.of(h2Sink.connection()));
        ingestor.initDatasets(datasets);
        DryRunResult dryRunResult = ingestor.dryRun();

        List<DataError> expectedErrorRecords = Arrays.asList(DataError.builder()
            .errorCategory(ErrorCategory.FILE_NOT_FOUND)
            .errorMessage("File not found in specified location")
            .putAllErrorDetails(buildErrorDetails(filePath))
            .build());

        Assertions.assertEquals(IngestStatus.FAILED, dryRunResult.status());
        Assertions.assertEquals(new HashSet<>(expectedErrorRecords), new HashSet<>(dryRunResult.errorRecords()));
    }

    RelationalIngestor getRelationalIngestor(IngestMode ingestMode, PlannerOptions options, Clock executionTimestampClock, CaseConversion caseConversion, int sampleRowCount)
    {
        return RelationalIngestor.builder()
            .ingestMode(ingestMode)
            .relationalSink(H2Sink.get())
            .executionTimestampClock(executionTimestampClock)
            .cleanupStagingData(options.cleanupStagingData())
            .collectStatistics(options.collectStatistics())
            .enableConcurrentSafety(true)
            .caseConversion(caseConversion)
            .sampleRowCount(sampleRowCount)
            .build();
    }

    RelationalIngestor getRelationalIngestor(IngestMode ingestMode, PlannerOptions options, Clock executionTimestampClock, CaseConversion caseConversion, Optional<String> eventId)
    {
        return RelationalIngestor.builder()
            .ingestMode(ingestMode)
            .relationalSink(H2Sink.get())
            .executionTimestampClock(executionTimestampClock)
            .cleanupStagingData(options.cleanupStagingData())
            .collectStatistics(options.collectStatistics())
            .bulkLoadEventIdValue(eventId)
            .enableConcurrentSafety(true)
            .caseConversion(caseConversion)
            .build();
    }

    RelationalIngestor getRelationalIngestor(IngestMode ingestMode, PlannerOptions options, Clock executionTimestampClock, CaseConversion caseConversion, Optional<String> eventId, Map<String, Object> additionalMetadata)
    {
        return RelationalIngestor.builder()
                .ingestMode(ingestMode)
                .relationalSink(H2Sink.get())
                .executionTimestampClock(executionTimestampClock)
                .cleanupStagingData(options.cleanupStagingData())
                .collectStatistics(options.collectStatistics())
                .bulkLoadEventIdValue(eventId)
                .putAllAdditionalMetadata(additionalMetadata)
                .enableConcurrentSafety(true)
                .caseConversion(caseConversion)
                .build();
    }

    private void verifyBulkLoadMetadata(Map<String, Object> appendMetadata, String fileName, int batchId, Optional<String> eventId, Optional<Map<String, Object>> additionalMetadata) throws JsonProcessingException
    {
        Assertions.assertEquals(batchId, appendMetadata.get("table_batch_id"));
        Assertions.assertEquals("SUCCEEDED", appendMetadata.get("batch_status"));
        Assertions.assertEquals("main", appendMetadata.get("table_name"));
        Assertions.assertEquals("2000-01-01 00:00:00.0", appendMetadata.get("batch_start_ts_utc").toString());
        Assertions.assertEquals("2000-01-01 00:00:00.0", appendMetadata.get("batch_end_ts_utc").toString());
        String batchSourceInfoStr = (String) appendMetadata.get("batch_source_info");
        HashMap<String,Object> batchSourceInfoMap = new ObjectMapper().readValue(batchSourceInfoStr, HashMap.class);
        Assertions.assertEquals(batchSourceInfoMap.get("file_paths").toString(), String.format("[%s]", fileName));

        if (eventId.isPresent())
        {
            Assertions.assertEquals(batchSourceInfoMap.get("event_id"), eventId.get());
        }
        else
        {
            Assertions.assertFalse(batchSourceInfoMap.containsKey("event_id"));
        }
        if (additionalMetadata.isPresent())
        {
            String additionalMetaStr = (String) appendMetadata.get("additional_metadata");
            Assertions.assertNotNull(additionalMetaStr);
            HashMap<String,Object> additionalMetaMap = new ObjectMapper().readValue(additionalMetaStr, HashMap.class);
            for (Map.Entry<String, Object> entry :additionalMetadata.get().entrySet())
            {
                Assertions.assertEquals(additionalMetaMap.get(entry.getKey()), entry.getValue());
            }
        }
        else
        {
            Assertions.assertNull(appendMetadata.get("additional_metadata"));
        }
    }

    private void verifyBulkLoadMetadataForUpperCase(Map<String, Object> appendMetadata, String fileName, int batchId, Optional<String> eventId, Optional<Map<String, Object>> additionalMetadata)
    {
        Assertions.assertEquals(batchId, appendMetadata.get("TABLE_BATCH_ID"));
        Assertions.assertEquals("SUCCEEDED", appendMetadata.get("BATCH_STATUS"));
        Assertions.assertEquals("MAIN", appendMetadata.get("TABLE_NAME"));
        Assertions.assertEquals("2000-01-01 00:00:00.0", appendMetadata.get("BATCH_START_TS_UTC").toString());
        Assertions.assertEquals("2000-01-01 00:00:00.0", appendMetadata.get("BATCH_END_TS_UTC").toString());
        Assertions.assertTrue(appendMetadata.get("BATCH_SOURCE_INFO").toString().contains(String.format("\"file_paths\":[\"%s\"]", fileName)));
        if (eventId.isPresent())
        {
            Assertions.assertTrue(appendMetadata.get("BATCH_SOURCE_INFO").toString().contains(String.format("\"event_id\":\"%s\"", eventId.get())));
        }
        else
        {
            Assertions.assertFalse(appendMetadata.get("BATCH_SOURCE_INFO").toString().contains("\"event_id\""));
        }
        if (additionalMetadata.isPresent())
        {
            Assertions.assertNotNull(appendMetadata.get("ADDITIONAL_METADATA"));
        }
        else
        {
            Assertions.assertNull(appendMetadata.get("ADDITIONAL_METADATA"));
        }
    }

    private Map<String, Object> buildErrorDetails(String fileName, String columnName, Long recordNumber)
    {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put(DataError.FILE_NAME, fileName);
        errorDetails.put(DataError.COLUMN_NAME, columnName);
        errorDetails.put(DataError.RECORD_NUMBER, recordNumber);
        return errorDetails;
    }

    private Map<String, Object> buildErrorDetails(String fileName)
    {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put(DataError.FILE_NAME, fileName);
        return errorDetails;
    }
}
