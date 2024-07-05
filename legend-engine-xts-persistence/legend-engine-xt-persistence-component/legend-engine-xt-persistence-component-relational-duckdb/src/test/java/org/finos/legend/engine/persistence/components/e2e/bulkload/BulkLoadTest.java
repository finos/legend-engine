// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.e2e.bulkload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.FileFormatType;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.e2e.BaseTest;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoad;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.digest.NoDigestGenStrategy;
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
import org.finos.legend.engine.persistence.components.relational.duckdb.DuckDBSink;
import org.finos.legend.engine.persistence.components.relational.duckdb.logicalplan.datasets.DuckDBStagedFilesDatasetProperties;
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

import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.digest;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.expiryDate;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.idNonPk;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.income;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.mainTableName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.name;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.startTimeNonPk;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.testDatabaseName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.testSchemaName;

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
    public void testBulkLoadWithDigestNotGeneratedAuditEnabled() throws Exception
    {
        String filePath = "src/test/resources/data/bulk-load/input/staged_file1.csv";

        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(NoDigestGenStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                DuckDBStagedFilesDatasetProperties.builder()
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
            .relationalSink(DuckDBSink.get())
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
            "SELECT \"col_int\",\"col_string\",\"col_decimal\",\"col_datetime\",{NEXT_BATCH_ID_PATTERN},'2000-01-01 00:00:00.000000' " +
            "FROM READ_CSV(['src/test/resources/data/bulk-load/input/staged_file1.csv'], " +
            "COLUMNS = {'col_int':'INTEGER', 'col_string':'VARCHAR', 'col_decimal':'DECIMAL(5,2)', 'col_datetime':'TIMESTAMP'}, " +
            "AUTO_DETECT = FALSE)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM \"TEST_DB\".\"TEST\".\"main\" as my_alias WHERE my_alias.\"batch_id\" = {NEXT_BATCH_ID_PATTERN}", statsSql.get(ROWS_INSERTED));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{COL_INT, COL_STRING, COL_DECIMAL, COL_DATETIME, BATCH_ID, APPEND_TIME};

        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.ROWS_INSERTED.name(), 3);

        String expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table1.csv";

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.of(EVENT_ID_1), new HashMap<>());
        executePlansAndVerifyResults(ingestor, datasets, schema, expectedDataPath, expectedStats, false, "");

        Map<String, Object> appendMetadata = duckDBSink.executeQuery("select * from batch_metadata").get(0);
        verifyBulkLoadMetadata(appendMetadata, Collections.singletonList(filePath), 1, Optional.of(EVENT_ID_1), Optional.empty());
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
                DuckDBStagedFilesDatasetProperties.builder()
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
            .relationalSink(DuckDBSink.get())
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
            "SELECT \"col_int\",\"col_string\",\"col_decimal\",\"col_datetime\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
            "FROM READ_CSV(['src/test/resources/data/bulk-load/input/staged_file2.csv'], COLUMNS = {'col_int':'INTEGER', 'col_string':'VARCHAR', 'col_decimal':'DECIMAL(5,2)', 'col_datetime':'TIMESTAMP'}, AUTO_DETECT = FALSE)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM \"TEST_DB\".\"TEST\".\"main\" as my_alias WHERE my_alias.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')", statsSql.get(ROWS_INSERTED));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{COL_INT, COL_STRING, COL_DECIMAL, COL_DATETIME, BATCH_ID};

        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.ROWS_INSERTED.name(), 3);

        String expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table2.csv";

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.NONE, Optional.of(EVENT_ID_1), ADDITIONAL_METADATA);
        executePlansAndVerifyResults(ingestor, datasets, schema, expectedDataPath, expectedStats, false, "");
        Map<String, Object> appendMetadata = duckDBSink.executeQuery("select * from batch_metadata").get(0);
        verifyBulkLoadMetadata(appendMetadata, Collections.singletonList(filePath), 1, Optional.of(EVENT_ID_1), Optional.of(ADDITIONAL_METADATA));
    }

    @Test
    public void testBulkLoadJsonUpperCase() throws Exception
    {
        String filePath1 = "src/test/resources/data/bulk-load/input/staged_file1.json";
        String filePath2 = "src/test/resources/data/bulk-load/input/staged_file2.json";

        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(NoDigestGenStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                DuckDBStagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.JSON)
                    .putLoadOptions("maximum_depth", 5)
                    .putLoadOptions("ignore_errors", false)
                    .addAllFilePaths(Arrays.asList(filePath1, filePath2)).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(idNonPk, name, income, startTimeNonPk, expiryDate, digest)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database(testDatabaseName).group(testSchemaName).name(mainTableName).alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainDataset, stagedFilesDataset);

        // Verify SQLs using generator
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(DuckDBSink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .batchIdPattern("{NEXT_BATCH_ID_PATTERN}")
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"TEST_DB\".\"TEST\".\"MAIN\"" +
            "(\"ID\" INTEGER,\"NAME\" VARCHAR NOT NULL,\"INCOME\" BIGINT,\"START_TIME\" TIMESTAMP,\"EXPIRY_DATE\" DATE,\"DIGEST\" VARCHAR,\"BATCH_ID\" INTEGER,\"APPEND_TIME\" TIMESTAMP)";

        String expectedIngestSql = "INSERT INTO \"TEST_DB\".\"TEST\".\"MAIN\" " +
            "(\"ID\", \"NAME\", \"INCOME\", \"START_TIME\", \"EXPIRY_DATE\", \"DIGEST\", \"BATCH_ID\", \"APPEND_TIME\") " +
            "SELECT \"ID\",\"NAME\",\"INCOME\",\"START_TIME\",\"EXPIRY_DATE\",\"DIGEST\",{NEXT_BATCH_ID_PATTERN},'2000-01-01 00:00:00.000000' " +
            "FROM READ_JSON(['src/test/resources/data/bulk-load/input/staged_file1.json','src/test/resources/data/bulk-load/input/staged_file2.json'], ignore_errors=false, maximum_depth=5)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        Assertions.assertEquals("SELECT COUNT(*) as \"ROWSINSERTED\" FROM \"TEST_DB\".\"TEST\".\"MAIN\" as my_alias WHERE my_alias.\"BATCH_ID\" = {NEXT_BATCH_ID_PATTERN}", statsSql.get(ROWS_INSERTED));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{idName.toUpperCase(), nameName.toUpperCase(), incomeName.toUpperCase(), startTimeName.toUpperCase(), expiryDateName.toUpperCase(), digestName.toUpperCase(), BATCH_ID.toUpperCase(), APPEND_TIME.toUpperCase()};

        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.ROWS_INSERTED.name(), 7);

        String expectedDataPath = "src/test/resources/data/bulk-load/expected/expected_table_json.csv";

        RelationalIngestor ingestor = getRelationalIngestor(bulkLoad, options, fixedClock_2000_01_01, CaseConversion.TO_UPPER, Optional.of(EVENT_ID_1), new HashMap<>());
        executePlansAndVerifyResults(ingestor, datasets, schema, expectedDataPath, expectedStats, false, "");

        Map<String, Object> appendMetadata = duckDBSink.executeQuery("select * from batch_metadata").get(0);
        verifyBulkLoadMetadataForUpperCase(appendMetadata, Arrays.asList(filePath1, filePath2), 1, Optional.of(EVENT_ID_1), Optional.empty());
    }

    RelationalIngestor getRelationalIngestor(IngestMode ingestMode, PlannerOptions options, Clock executionTimestampClock, CaseConversion caseConversion, Optional<String> eventId, Map<String, Object> additionalMetadata)
    {
        return RelationalIngestor.builder()
            .ingestMode(ingestMode)
            .relationalSink(DuckDBSink.get())
            .executionTimestampClock(executionTimestampClock)
            .cleanupStagingData(options.cleanupStagingData())
            .collectStatistics(options.collectStatistics())
            .ingestRequestId(eventId)
            .putAllAdditionalMetadata(additionalMetadata)
            .enableConcurrentSafety(true)
            .enableIdempotencyCheck(true)
            .writeStatistics(true)
            .caseConversion(caseConversion)
            .build();
    }

    private void verifyBulkLoadMetadata(Map<String, Object> appendMetadata, List<String> fileNames, int batchId, Optional<String> eventId, Optional<Map<String, Object>> additionalMetadata) throws JsonProcessingException
    {
        Assertions.assertEquals(batchId, appendMetadata.get("table_batch_id"));
        Assertions.assertEquals("SUCCEEDED", appendMetadata.get("batch_status"));
        Assertions.assertEquals("main", appendMetadata.get("table_name"));
        Assertions.assertEquals("2000-01-01 00:00:00.0", appendMetadata.get("batch_start_ts_utc").toString());
        Assertions.assertEquals("2000-01-01 00:00:00.0", appendMetadata.get("batch_end_ts_utc").toString());
        String batchSourceInfoStr = String.valueOf(appendMetadata.get("batch_source_info"));
        HashMap<String,Object> batchSourceInfoMap = new ObjectMapper().readValue(batchSourceInfoStr, HashMap.class);
        Assertions.assertEquals(batchSourceInfoMap.get("file_paths").toString(), String.format("[%s]", String.join(", ", fileNames)));

        if (eventId.isPresent())
        {
            Assertions.assertEquals(appendMetadata.get("ingest_request_id").toString(), eventId.get());
        }
        else
        {
            Assertions.assertNull(appendMetadata.get("ingest_request_id"));
        }

        if (additionalMetadata.isPresent())
        {
            String additionalMetaStr = String.valueOf(appendMetadata.get("additional_metadata"));
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

    private void verifyBulkLoadMetadataForUpperCase(Map<String, Object> appendMetadata, List<String> fileNames, int batchId, Optional<String> eventId, Optional<Map<String, Object>> additionalMetadata) throws JsonProcessingException
    {
        Assertions.assertEquals(batchId, appendMetadata.get("TABLE_BATCH_ID"));
        Assertions.assertEquals("SUCCEEDED", appendMetadata.get("BATCH_STATUS"));
        Assertions.assertEquals("MAIN", appendMetadata.get("TABLE_NAME"));
        Assertions.assertEquals("2000-01-01 00:00:00.0", appendMetadata.get("BATCH_START_TS_UTC").toString());
        Assertions.assertEquals("2000-01-01 00:00:00.0", appendMetadata.get("BATCH_END_TS_UTC").toString());
        String batchSourceInfoStr = String.valueOf(appendMetadata.get("BATCH_SOURCE_INFO"));
        HashMap<String,Object> batchSourceInfoMap = new ObjectMapper().readValue(batchSourceInfoStr, HashMap.class);
        Assertions.assertEquals(batchSourceInfoMap.get("file_paths").toString(), String.format("[%s]", String.join(", ", fileNames)));

        if (eventId.isPresent())
        {
            Assertions.assertEquals(appendMetadata.get("INGEST_REQUEST_ID").toString(), eventId.get());
        }
        else
        {
            Assertions.assertNull(appendMetadata.get("INGEST_REQUEST_ID"));
        }

        if (additionalMetadata.isPresent())
        {
            String additionalMetaStr = String.valueOf(appendMetadata.get("ADDITIONAL_METADATA"));
            Assertions.assertNotNull(additionalMetaStr);
            HashMap<String,Object> additionalMetaMap = new ObjectMapper().readValue(additionalMetaStr, HashMap.class);
            for (Map.Entry<String, Object> entry :additionalMetadata.get().entrySet())
            {
                Assertions.assertEquals(additionalMetaMap.get(entry.getKey()), entry.getValue());
            }
        }
        else
        {
            Assertions.assertNull(appendMetadata.get("ADDITIONAL_METADATA"));
        }
    }
}