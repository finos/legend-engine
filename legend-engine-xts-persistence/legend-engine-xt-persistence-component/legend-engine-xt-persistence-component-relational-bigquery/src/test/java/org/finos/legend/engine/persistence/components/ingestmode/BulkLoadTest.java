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
import org.finos.legend.engine.persistence.components.common.FileFormatType;
import org.finos.legend.engine.persistence.components.common.StatisticName;
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
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.relational.bigquery.logicalplan.datasets.BigQueryStagedFilesDatasetProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

import static org.finos.legend.engine.persistence.components.common.StatisticName.INCOMING_RECORD_COUNT;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_DELETED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_TERMINATED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_UPDATED;

public class BulkLoadTest
{
    private static final String APPEND_TIME = "append_time";
    private static final String DIGEST = "digest";
    private static final String DIGEST_UDF = "LAKEHOUSE_MD5";
    private static final String BATCH_ID = "batch_id";
    private static final String EVENT_ID = "xyz123";
    private static final Map<String, Object> ADDITIONAL_METADATA = Collections.singletonMap("watermark", "my_watermark_value");
    private static final String COL_INT = "col_int";
    private static final String COL_STRING = "col_string";
    private static final String COL_DECIMAL = "col_decimal";
    private static final String COL_DATETIME = "col_datetime";
    private static final String COL_VARIANT = "col_variant";
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

    private static Field col5 = Field.builder()
        .name(COL_VARIANT)
        .type(FieldType.of(DataType.VARIANT, Optional.empty(), Optional.empty()))
        .build();

    private List<String> filesList = Arrays.asList("/path/xyz/file1.csv", "/path/xyz/file2.csv");

    protected final ZonedDateTime fixedZonedDateTime_2000_01_01 = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    protected final Clock fixedClock_2000_01_01 = Clock.fixed(fixedZonedDateTime_2000_01_01.toInstant(), ZoneOffset.UTC);

    @Test
    public void testBulkLoadWithDigestNotGeneratedAuditEnabledNoExtraOptions()
    {
        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(NoDigestGenStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                BigQueryStagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(filesList).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4, col5)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database("my_db").name("my_name").alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(BigQuerySink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .bulkLoadEventIdValue(EVENT_ID)
            .batchIdPattern("{NEXT_BATCH_ID}")
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS `my_db`.`my_name`" +
            "(`col_int` INT64,`col_string` STRING,`col_decimal` NUMERIC(5,2),`col_datetime` DATETIME,`col_variant` JSON,`batch_id` INT64,`append_time` DATETIME)";

        String expectedCopySql = "CREATE OR REPLACE EXTERNAL TABLE `my_db`.`my_name_temp_lp_yosulf` " +
            "(`col_int` INT64,`col_string` STRING,`col_decimal` NUMERIC,`col_datetime` DATETIME,`col_variant` JSON) " +
            "OPTIONS (uris=['/path/xyz/file1.csv','/path/xyz/file2.csv'], format='CSV')";

        String expectedInsertSql = "INSERT INTO `my_db`.`my_name` " +
            "(`col_int`, `col_string`, `col_decimal`, `col_datetime`, `col_variant`, `batch_id`, `append_time`) " +
            "(SELECT legend_persistence_temp.`col_int`,legend_persistence_temp.`col_string`,legend_persistence_temp.`col_decimal`,legend_persistence_temp.`col_datetime`,legend_persistence_temp.`col_variant`,{NEXT_BATCH_ID},PARSE_DATETIME('%Y-%m-%d %H:%M:%E6S','2000-01-01 00:00:00.000000') " +
            "FROM `my_db`.`my_name_temp_lp_yosulf` as legend_persistence_temp)";

        String expectedMetadataIngestSql = "INSERT INTO `batch_metadata` (`table_name`, `table_batch_id`, `batch_start_ts_utc`, `batch_end_ts_utc`, `batch_status`, `batch_source_info`) " +
            "(SELECT 'my_name',{NEXT_BATCH_ID},PARSE_DATETIME('%Y-%m-%d %H:%M:%E6S','2000-01-01 00:00:00.000000'),CURRENT_DATETIME(),'{BULK_LOAD_BATCH_STATUS_PLACEHOLDER}',PARSE_JSON('{\"event_id\":\"xyz123\",\"file_paths\":[\"/path/xyz/file1.csv\",\"/path/xyz/file2.csv\"]}'))";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedCopySql, preActionsSql.get(2));
        Assertions.assertEquals(expectedInsertSql, ingestSql.get(0));
        Assertions.assertEquals(expectedMetadataIngestSql, metadataIngestSql.get(0));

        Assertions.assertNull(statsSql.get(INCOMING_RECORD_COUNT));
        Assertions.assertNull(statsSql.get(ROWS_DELETED));
        Assertions.assertNull(statsSql.get(ROWS_TERMINATED));
        Assertions.assertNull(statsSql.get(ROWS_UPDATED));
        Assertions.assertEquals("SELECT COUNT(*) as `rowsInserted` FROM `my_db`.`my_name` as my_alias WHERE my_alias.`batch_id` = {NEXT_BATCH_ID}", statsSql.get(ROWS_INSERTED));
    }

    @Test
    public void testBulkLoadWithDigestNotGeneratedAuditEnabledAllOptionsNoEventId()
    {
        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(NoDigestGenStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Map<String, Object> loadOptions = new HashMap<>();
        loadOptions.put("encoding", "UTF8");
        loadOptions.put("max_bad_records", 100L);
        loadOptions.put("null_marker", "NULL");
        loadOptions.put("quote", "'");
        loadOptions.put("compression", "GZIP");
        loadOptions.put("field_delimiter", ",");
        loadOptions.put("skip_leading_rows", 1L);

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                BigQueryStagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .putAllLoadOptions(loadOptions)
                    .addAllFilePaths(filesList).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4, col5)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database("my_db").name("my_name").alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(BigQuerySink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .putAllAdditionalMetadata(ADDITIONAL_METADATA)
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS `my_db`.`my_name`" +
            "(`col_int` INT64,`col_string` STRING,`col_decimal` NUMERIC(5,2),`col_datetime` DATETIME,`col_variant` JSON,`batch_id` INT64,`append_time` DATETIME)";

        String expectedCopySql = "CREATE OR REPLACE EXTERNAL TABLE `my_db`.`my_name_temp_lp_yosulf` " +
            "(`col_int` INT64,`col_string` STRING,`col_decimal` NUMERIC,`col_datetime` DATETIME,`col_variant` JSON) " +
            "OPTIONS (uris=['/path/xyz/file1.csv','/path/xyz/file2.csv'], compression='GZIP', encoding='UTF8', field_delimiter=',', format='CSV', max_bad_records=100, null_marker='NULL', quote=''', skip_leading_rows=1)";

        String expectedInsertSql = "INSERT INTO `my_db`.`my_name` " +
            "(`col_int`, `col_string`, `col_decimal`, `col_datetime`, `col_variant`, `batch_id`, `append_time`) " +
            "(SELECT legend_persistence_temp.`col_int`,legend_persistence_temp.`col_string`,legend_persistence_temp.`col_decimal`,legend_persistence_temp.`col_datetime`,legend_persistence_temp.`col_variant`,(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM `batch_metadata` as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MY_NAME'),PARSE_DATETIME('%Y-%m-%d %H:%M:%E6S','2000-01-01 00:00:00.000000') " +
            "FROM `my_db`.`my_name_temp_lp_yosulf` as legend_persistence_temp)";

        String expectedMetadataIngestSql = "INSERT INTO `batch_metadata` (`table_name`, `table_batch_id`, `batch_start_ts_utc`, `batch_end_ts_utc`, `batch_status`, `batch_source_info`, `additional_metadata`) " +
            "(SELECT 'my_name',(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM `batch_metadata` as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MY_NAME'),PARSE_DATETIME('%Y-%m-%d %H:%M:%E6S','2000-01-01 00:00:00.000000'),CURRENT_DATETIME(),'{BULK_LOAD_BATCH_STATUS_PLACEHOLDER}'," +
            "PARSE_JSON('{\"file_paths\":[\"/path/xyz/file1.csv\",\"/path/xyz/file2.csv\"]}')," +
            "PARSE_JSON('{\"watermark\":\"my_watermark_value\"}'))";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedCopySql, preActionsSql.get(2));
        Assertions.assertEquals(expectedInsertSql, ingestSql.get(0));
        Assertions.assertEquals(expectedMetadataIngestSql, metadataIngestSql.get(0));

        Assertions.assertNull(statsSql.get(INCOMING_RECORD_COUNT));
        Assertions.assertNull(statsSql.get(ROWS_DELETED));
        Assertions.assertNull(statsSql.get(ROWS_TERMINATED));
        Assertions.assertNull(statsSql.get(ROWS_UPDATED));
        Assertions.assertEquals("SELECT COUNT(*) as `rowsInserted` FROM `my_db`.`my_name` as my_alias WHERE my_alias.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM `batch_metadata` as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MY_NAME')", statsSql.get(ROWS_INSERTED));
    }

    @Test
    public void testBulkLoadWithDigestNotGeneratedAuditDisabledNoExtraOptions()
    {
        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(NoDigestGenStrategy.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                BigQueryStagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(filesList).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4, col5)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database("my_db").name("my_name").alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(BigQuerySink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .bulkLoadEventIdValue(EVENT_ID)
            .putAllAdditionalMetadata(ADDITIONAL_METADATA)
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        List<String> metaIngestSql = operations.metadataIngestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS `my_db`.`my_name`" +
            "(`col_int` INT64,`col_string` STRING,`col_decimal` NUMERIC(5,2),`col_datetime` DATETIME,`col_variant` JSON,`batch_id` INT64)";

        String expectedCopySql = "CREATE OR REPLACE EXTERNAL TABLE `my_db`.`my_name_temp_lp_yosulf` " +
            "(`col_int` INT64,`col_string` STRING,`col_decimal` NUMERIC,`col_datetime` DATETIME,`col_variant` JSON) " +
            "OPTIONS (uris=['/path/xyz/file1.csv','/path/xyz/file2.csv'], format='CSV')";

        String expectedInsertSql = "INSERT INTO `my_db`.`my_name` " +
            "(`col_int`, `col_string`, `col_decimal`, `col_datetime`, `col_variant`, `batch_id`) " +
            "(SELECT legend_persistence_temp.`col_int`,legend_persistence_temp.`col_string`,legend_persistence_temp.`col_decimal`,legend_persistence_temp.`col_datetime`,legend_persistence_temp.`col_variant`,(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM `batch_metadata` as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MY_NAME') " +
            "FROM `my_db`.`my_name_temp_lp_yosulf` as legend_persistence_temp)";

        String expectedMetaIngestSql = "INSERT INTO `batch_metadata` (`table_name`, `table_batch_id`, `batch_start_ts_utc`, `batch_end_ts_utc`, `batch_status`, `batch_source_info`, `additional_metadata`) " +
            "(SELECT 'my_name',(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM `batch_metadata` as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MY_NAME')," +
            "PARSE_DATETIME('%Y-%m-%d %H:%M:%E6S','2000-01-01 00:00:00.000000'),CURRENT_DATETIME(),'{BULK_LOAD_BATCH_STATUS_PLACEHOLDER}'," +
            "PARSE_JSON('{\"event_id\":\"xyz123\",\"file_paths\":[\"/path/xyz/file1.csv\",\"/path/xyz/file2.csv\"]}')," +
            "PARSE_JSON('{\"watermark\":\"my_watermark_value\"}'))";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedCopySql, preActionsSql.get(2));
        Assertions.assertEquals(expectedInsertSql, ingestSql.get(0));
        Assertions.assertEquals(expectedMetaIngestSql, metaIngestSql.get(0));

        Assertions.assertNull(statsSql.get(INCOMING_RECORD_COUNT));
        Assertions.assertNull(statsSql.get(ROWS_DELETED));
        Assertions.assertNull(statsSql.get(ROWS_TERMINATED));
        Assertions.assertNull(statsSql.get(ROWS_UPDATED));
        Assertions.assertEquals("SELECT COUNT(*) as `rowsInserted` FROM `my_db`.`my_name` as my_alias WHERE my_alias.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM `batch_metadata` as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MY_NAME')", statsSql.get(ROWS_INSERTED));
    }

    @Test
    public void testBulkLoadWithDigestGeneratedAuditEnabledNoExtraOptions()
    {
        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestField(DIGEST).digestUdfName(DIGEST_UDF).build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                BigQueryStagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(filesList).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4, col5)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database("my_db").name("my_name").alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(BigQuerySink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .bulkLoadEventIdValue(EVENT_ID)
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS `my_db`.`my_name`" +
            "(`col_int` INT64,`col_string` STRING,`col_decimal` NUMERIC(5,2),`col_datetime` DATETIME,`col_variant` JSON,`digest` STRING,`batch_id` INT64,`append_time` DATETIME)";

        String expectedCopySql = "CREATE OR REPLACE EXTERNAL TABLE `my_db`.`my_name_temp_lp_yosulf` " +
            "(`col_int` INT64,`col_string` STRING,`col_decimal` NUMERIC,`col_datetime` DATETIME,`col_variant` JSON) " +
            "OPTIONS (uris=['/path/xyz/file1.csv','/path/xyz/file2.csv'], format='CSV')";

        String expectedInsertSql = "INSERT INTO `my_db`.`my_name` " +
            "(`col_int`, `col_string`, `col_decimal`, `col_datetime`, `col_variant`, `digest`, `batch_id`, `append_time`) " +
            "(SELECT legend_persistence_temp.`col_int`,legend_persistence_temp.`col_string`,legend_persistence_temp.`col_decimal`,legend_persistence_temp.`col_datetime`,legend_persistence_temp.`col_variant`," +
            "LAKEHOUSE_MD5(TO_JSON(STRUCT(legend_persistence_temp.`col_int`,legend_persistence_temp.`col_string`,legend_persistence_temp.`col_decimal`,legend_persistence_temp.`col_datetime`,legend_persistence_temp.`col_variant`)))," +
            "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM `batch_metadata` as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MY_NAME'),PARSE_DATETIME('%Y-%m-%d %H:%M:%E6S','2000-01-01 00:00:00.000000') " +
            "FROM `my_db`.`my_name_temp_lp_yosulf` as legend_persistence_temp)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedCopySql, preActionsSql.get(2));
        Assertions.assertEquals(expectedInsertSql, ingestSql.get(0));

        Assertions.assertNull(statsSql.get(INCOMING_RECORD_COUNT));
        Assertions.assertNull(statsSql.get(ROWS_DELETED));
        Assertions.assertNull(statsSql.get(ROWS_TERMINATED));
        Assertions.assertNull(statsSql.get(ROWS_UPDATED));
        Assertions.assertEquals("SELECT COUNT(*) as `rowsInserted` FROM `my_db`.`my_name` as my_alias WHERE my_alias.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM `batch_metadata` as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MY_NAME')", statsSql.get(ROWS_INSERTED));
    }

    @Test
    public void testBulkLoadWithDigestGeneratedAuditEnabledNoExtraOptionsUpperCase()
    {
        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestField(DIGEST).digestUdfName(DIGEST_UDF).addAllFieldsToExcludeFromDigest(Arrays.asList(col1.name(), col2.name(), col3.name(), col4.name())).build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                BigQueryStagedFilesDatasetProperties.builder()
                    .fileFormat(FileFormatType.CSV)
                    .addAllFilePaths(filesList).build())
            .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col1, col2, col3, col4, col5)).build())
            .build();

        Dataset mainDataset = DatasetDefinition.builder()
            .database("my_db").name("my_name").alias("my_alias")
            .schema(SchemaDefinition.builder().build())
            .build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(bulkLoad)
            .relationalSink(BigQuerySink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .bulkLoadEventIdValue(EVENT_ID)
            .caseConversion(CaseConversion.TO_UPPER)
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS `MY_DB`.`MY_NAME`" +
            "(`COL_INT` INT64,`COL_STRING` STRING,`COL_DECIMAL` NUMERIC(5,2),`COL_DATETIME` DATETIME,`COL_VARIANT` JSON,`DIGEST` STRING,`BATCH_ID` INT64,`APPEND_TIME` DATETIME)";

        String expectedCopySql = "CREATE OR REPLACE EXTERNAL TABLE `MY_DB`.`MY_NAME_TEMP_LP_YOSULF` " +
            "(`COL_INT` INT64,`COL_STRING` STRING,`COL_DECIMAL` NUMERIC,`COL_DATETIME` DATETIME,`COL_VARIANT` JSON) " +
            "OPTIONS (uris=['/path/xyz/file1.csv','/path/xyz/file2.csv'], format='CSV')";

        String expectedInsertSql = "INSERT INTO `MY_DB`.`MY_NAME` " +
            "(`COL_INT`, `COL_STRING`, `COL_DECIMAL`, `COL_DATETIME`, `COL_VARIANT`, `DIGEST`, `BATCH_ID`, `APPEND_TIME`) " +
            "(SELECT legend_persistence_temp.`COL_INT`,legend_persistence_temp.`COL_STRING`,legend_persistence_temp.`COL_DECIMAL`,legend_persistence_temp.`COL_DATETIME`,legend_persistence_temp.`COL_VARIANT`,LAKEHOUSE_MD5(TO_JSON(STRUCT(legend_persistence_temp.`COL_VARIANT`))),(SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM `BATCH_METADATA` as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MY_NAME'),PARSE_DATETIME('%Y-%m-%d %H:%M:%E6S','2000-01-01 00:00:00.000000') " +
            "FROM `MY_DB`.`MY_NAME_TEMP_LP_YOSULF` as legend_persistence_temp)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedCopySql, preActionsSql.get(2));
        Assertions.assertEquals(expectedInsertSql, ingestSql.get(0));

        Assertions.assertNull(statsSql.get(INCOMING_RECORD_COUNT));
        Assertions.assertNull(statsSql.get(ROWS_DELETED));
        Assertions.assertNull(statsSql.get(ROWS_TERMINATED));
        Assertions.assertNull(statsSql.get(ROWS_UPDATED));
        Assertions.assertEquals("SELECT COUNT(*) as `ROWSINSERTED` FROM `MY_DB`.`MY_NAME` as my_alias WHERE my_alias.`BATCH_ID` = (SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM `BATCH_METADATA` as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MY_NAME')", statsSql.get(ROWS_INSERTED));
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
                .relationalSink(BigQuerySink.get())
                .bulkLoadEventIdValue(EVENT_ID)
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
}
