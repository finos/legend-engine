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

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.FileFormat;
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
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2DigestUtil;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.h2.logicalplan.datasets.H2StagedFilesDatasetProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
    private static final String TASK_ID_VALUE_1 = "xyz123";
    private static final String TASK_ID_VALUE_2 = "abc987";
    private static final String COL_INT = "col_int";
    private static final String COL_STRING = "col_string";
    private static final String COL_DECIMAL = "col_decimal";
    private static final String COL_DATETIME = "col_datetime";

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

    protected final ZonedDateTime fixedZonedDateTime_2000_01_01 = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    protected final Clock fixedClock_2000_01_01 = Clock.fixed(fixedZonedDateTime_2000_01_01.toInstant(), ZoneOffset.UTC);

    @Test
    public void testBulkLoadWithDigestNotGeneratedAuditEnabledNoTaskId() throws Exception
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
                            .fileFormat(FileFormat.CSV)
                            .addAllFiles(Collections.singletonList(filePath)).build())
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
                "{NEXT_BATCH_ID_PATTERN},'2000-01-01 00:00:00' FROM CSVREAD('src/test/resources/data/bulk-load/input/staged_file1.csv'," +
                "'col_int,col_string,col_decimal,col_datetime',NULL)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM \"TEST_DB\".\"TEST\".\"main\" as my_alias WHERE my_alias.\"append_time\" = '2000-01-01 00:00:00'", statsSql.get(ROWS_INSERTED));


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

        Map<String, Object> appendMetadata = h2Sink.executeQuery("select * from bulk_load_batch_metadata").get(0);
        verifyBulkLoadMetadata(appendMetadata, filePath, 1, Optional.empty());
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
                    .fileFormat(FileFormat.CSV)
                    .addAllFiles(Collections.singletonList(filePath)).build())
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
            .bulkLoadTaskIdValue(TASK_ID_VALUE_1)
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
                "(SELECT COALESCE(MAX(bulk_load_batch_metadata.\"batch_id\"),0)+1 FROM bulk_load_batch_metadata as bulk_load_batch_metadata WHERE UPPER(bulk_load_batch_metadata.\"table_name\") = 'MAIN') FROM CSVREAD('src/test/resources/data/bulk-load/input/staged_file2.csv','col_int,col_string,col_decimal,col_datetime',NULL)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertNull(statsSql.get(ROWS_INSERTED));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{COL_INT, COL_STRING, COL_DECIMAL, COL_DATETIME, BATCH_ID};

        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.FILES_LOADED.name(), 1);
        expectedStats.put(StatisticName.ROWS_WITH_ERRORS.name(), 0);

        String expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table2.csv";

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.of(TASK_ID_VALUE_1));
        executePlansAndVerifyResults(ingestor, datasets, schema, expectedDataPath, expectedStats, false);
        Map<String, Object> appendMetadata = h2Sink.executeQuery("select * from bulk_load_batch_metadata").get(0);
        verifyBulkLoadMetadata(appendMetadata, filePath, 1, Optional.of(TASK_ID_VALUE_1));
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
                    .fileFormat(FileFormat.CSV)
                    .addAllFiles(Collections.singletonList(filePath)).build())
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
            .bulkLoadTaskIdValue(TASK_ID_VALUE_1)
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
                "(SELECT COALESCE(MAX(bulk_load_batch_metadata.\"batch_id\"),0)+1 FROM bulk_load_batch_metadata as bulk_load_batch_metadata WHERE UPPER(bulk_load_batch_metadata.\"table_name\") = 'MAIN'),'2000-01-01 00:00:00' FROM CSVREAD('src/test/resources/data/bulk-load/input/staged_file3.csv','col_int,col_string,col_decimal,col_datetime',NULL)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM \"TEST_DB\".\"TEST\".\"main\" as my_alias WHERE my_alias.\"append_time\" = '2000-01-01 00:00:00'", statsSql.get(ROWS_INSERTED));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{COL_INT, COL_STRING, COL_DECIMAL, COL_DATETIME, DIGEST, BATCH_ID, APPEND_TIME};

        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.ROWS_INSERTED.name(), 3);
        expectedStats.put(StatisticName.FILES_LOADED.name(), 1);
        expectedStats.put(StatisticName.ROWS_WITH_ERRORS.name(), 0);

        String expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table3.csv";

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.of(TASK_ID_VALUE_1));
        executePlansAndVerifyResults(ingestor, datasets, schema, expectedDataPath, expectedStats, false);
        Map<String, Object> appendMetadata = h2Sink.executeQuery("select * from bulk_load_batch_metadata").get(0);
        verifyBulkLoadMetadata(appendMetadata, filePath, 1, Optional.of(TASK_ID_VALUE_1));
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
                    .fileFormat(FileFormat.CSV)
                    .addAllFiles(Collections.singletonList(filePath)).build())
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
            .bulkLoadTaskIdValue(TASK_ID_VALUE_1)
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
                "(SELECT COALESCE(MAX(BULK_LOAD_BATCH_METADATA.\"BATCH_ID\"),0)+1 FROM BULK_LOAD_BATCH_METADATA as BULK_LOAD_BATCH_METADATA WHERE UPPER(BULK_LOAD_BATCH_METADATA.\"TABLE_NAME\") = 'MAIN'),'2000-01-01 00:00:00' " +
                "FROM CSVREAD('src/test/resources/data/bulk-load/input/staged_file4.csv','COL_INT,COL_STRING,COL_DECIMAL,COL_DATETIME',NULL)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"ROWSINSERTED\" FROM \"TEST_DB\".\"TEST\".\"MAIN\" as my_alias WHERE my_alias.\"APPEND_TIME\" = '2000-01-01 00:00:00'", statsSql.get(ROWS_INSERTED));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{COL_INT.toUpperCase(), COL_STRING.toUpperCase(), COL_DECIMAL.toUpperCase(), COL_DATETIME.toUpperCase(), DIGEST.toUpperCase(), BATCH_ID.toUpperCase(), APPEND_TIME.toUpperCase()};

        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.ROWS_INSERTED.name(), 3);
        expectedStats.put(StatisticName.FILES_LOADED.name(), 1);
        expectedStats.put(StatisticName.ROWS_WITH_ERRORS.name(), 0);

        String expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table4.csv";

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.TO_UPPER, Optional.of(TASK_ID_VALUE_1));
        executePlansAndVerifyForCaseConversion(ingestor, datasets, schema, expectedDataPath, expectedStats);
        Map<String, Object> appendMetadata = h2Sink.executeQuery("select * from BULK_LOAD_BATCH_METADATA").get(0);
        verifyBulkLoadMetadataForUpperCase(appendMetadata, filePath, 1, Optional.of(TASK_ID_VALUE_1));
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
                    .fileFormat(FileFormat.CSV)
                    .addAllFiles(Collections.singletonList(filePath)).build())
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

        String expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table2.csv";

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.of(TASK_ID_VALUE_1));
        executePlansAndVerifyResults(ingestor, datasets, schema, expectedDataPath, expectedStats, false);
        Map<String, Object> appendMetadata = h2Sink.executeQuery("select * from bulk_load_batch_metadata").get(0);
        verifyBulkLoadMetadata(appendMetadata, filePath, 1, Optional.of(TASK_ID_VALUE_1));


        // Verify execution using ingestor (second batch)
        expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table5.csv";

        ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.of(TASK_ID_VALUE_2));
        executePlansAndVerifyResults(ingestor, datasets, schema, expectedDataPath, expectedStats, false);
        appendMetadata = h2Sink.executeQuery("select * from bulk_load_batch_metadata").get(0);
        verifyBulkLoadMetadata(appendMetadata, filePath, 1, Optional.of(TASK_ID_VALUE_1));
        appendMetadata = h2Sink.executeQuery("select * from bulk_load_batch_metadata").get(1);
        verifyBulkLoadMetadata(appendMetadata, filePath, 2, Optional.of(TASK_ID_VALUE_2));
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
                    .bulkLoadTaskIdValue(TASK_ID_VALUE_1)
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
                        .fileFormat(FileFormat.CSV)
                        .addAllFiles(Collections.singletonList("src/test/resources/data/bulk-load/input/staged_file1.csv")).build())
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4, pkCol)).build())
                .build();

            Dataset mainDataset = DatasetDefinition.builder()
                .database("my_db").name("my_name").alias("my_alias")
                .schema(SchemaDefinition.builder().build())
                .build();

            RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(bulkLoad)
                .relationalSink(H2Sink.get())
                .bulkLoadTaskIdValue(TASK_ID_VALUE_1)
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
                        .fileFormat(FileFormat.CSV)
                        .addAllFiles(Collections.singletonList("src/test/resources/data/bulk-load/input/staged_file1.csv")).build())
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
                .build();

            Dataset mainDataset = DatasetDefinition.builder()
                .database("my_db").name("my_name").alias("my_alias")
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4, pkCol)).build())
                .build();

            RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(bulkLoad)
                .relationalSink(H2Sink.get())
                .bulkLoadTaskIdValue(TASK_ID_VALUE_1)
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
                        .fileFormat(FileFormat.CSV)
                        .addAllFiles(Arrays.asList("src/test/resources/data/bulk-load/input/staged_file1.csv", "src/test/resources/data/bulk-load/input/staged_file2.csv")).build())
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
                        .fileFormat(FileFormat.JSON)
                        .addAllFiles(Arrays.asList("src/test/resources/data/bulk-load/input/staged_file1.json")).build())
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4)).build())
                .build();
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e.getMessage().contains("Cannot build H2StagedFilesDatasetProperties, only CSV file loading supported"));
        }
    }

    RelationalIngestor getRelationalIngestor(IngestMode ingestMode, PlannerOptions options, Clock executionTimestampClock, CaseConversion caseConversion, Optional<String> taskId)
    {
        return RelationalIngestor.builder()
                .ingestMode(ingestMode)
                .relationalSink(H2Sink.get())
                .executionTimestampClock(executionTimestampClock)
                .cleanupStagingData(options.cleanupStagingData())
                .collectStatistics(options.collectStatistics())
                .bulkLoadTaskIdValue(taskId)
                .enableConcurrentSafety(true)
                .caseConversion(caseConversion)
                .build();
    }

    private void verifyBulkLoadMetadata(Map<String, Object> appendMetadata, String fileName, int batchId, Optional<String> taskId)
    {
        Assertions.assertEquals(batchId, appendMetadata.get("batch_id"));
        Assertions.assertEquals("SUCCEEDED", appendMetadata.get("batch_status"));
        Assertions.assertEquals("main", appendMetadata.get("table_name"));
        Assertions.assertEquals("2000-01-01 00:00:00.0", appendMetadata.get("batch_start_ts_utc").toString());
        Assertions.assertEquals("2000-01-01 00:00:00.0", appendMetadata.get("batch_end_ts_utc").toString());
        Assertions.assertTrue(appendMetadata.get("batch_source_info").toString().contains(String.format("\"files\":[\"%s\"]", fileName)));
        if (taskId.isPresent())
        {
            Assertions.assertTrue(appendMetadata.get("batch_source_info").toString().contains(String.format("\"task_id\":\"%s\"", taskId.get())));
        }
        else
        {
            Assertions.assertFalse(appendMetadata.get("batch_source_info").toString().contains("\"task_id\""));
        }
    }

    private void verifyBulkLoadMetadataForUpperCase(Map<String, Object> appendMetadata, String fileName, int batchId, Optional<String> taskId)
    {
        Assertions.assertEquals(batchId, appendMetadata.get("BATCH_ID"));
        Assertions.assertEquals("SUCCEEDED", appendMetadata.get("BATCH_STATUS"));
        Assertions.assertEquals("MAIN", appendMetadata.get("TABLE_NAME"));
        Assertions.assertEquals("2000-01-01 00:00:00.0", appendMetadata.get("BATCH_START_TS_UTC").toString());
        Assertions.assertEquals("2000-01-01 00:00:00.0", appendMetadata.get("BATCH_END_TS_UTC").toString());
        Assertions.assertTrue(appendMetadata.get("BATCH_SOURCE_INFO").toString().contains(String.format("\"files\":[\"%s\"]", fileName)));
        if (taskId.isPresent())
        {
            Assertions.assertTrue(appendMetadata.get("BATCH_SOURCE_INFO").toString().contains(String.format("\"task_id\":\"%s\"", taskId.get())));
        }
        else
        {
            Assertions.assertFalse(appendMetadata.get("BATCH_SOURCE_INFO").toString().contains("\"task_id\""));
        }
    }

}
