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

import org.finos.legend.engine.persistence.components.common.CsvFileFormat;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoad;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.digest.NoDigestGenStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.relational.bigquery.logicalplan.datasets.BigQueryStagedFilesDatasetProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private static final String BATCH_ID_VALUE = "xyz123";
    private static final String col_int = "col_int";
    private static final String col_string = "col_string";
    private static final String col_decimal = "col_decimal";
    private static final String col_datetime = "col_datetime";
    private static final String col_variant = "col_variant";

    private static Field col1 = Field.builder()
        .name(col_int)
        .type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty()))
        .primaryKey(true)
        .build();
    private static Field col2 = Field.builder()
        .name(col_string)
        .type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty()))
        .build();
    private static Field col3 = Field.builder()
        .name(col_decimal)
        .type(FieldType.of(DataType.DECIMAL, 5, 2))
        .build();
    private static Field col4 = Field.builder()
        .name(col_datetime)
        .type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty()))
        .build();

    private static Field col5 = Field.builder()
        .name(col_variant)
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
                    .fileFormat(CsvFileFormat.builder().build())
                    .addAllFiles(filesList).build())
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
            .bulkLoadBatchIdValue(BATCH_ID_VALUE)
            .build();

        GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS `my_db`.`my_name`" +
            "(`col_int` INT64 NOT NULL PRIMARY KEY NOT ENFORCED,`col_string` STRING,`col_decimal` NUMERIC(5,2),`col_datetime` DATETIME,`col_variant` JSON,`batch_id` STRING,`append_time` DATETIME)";

        String expectedCopySql = "LOAD DATA OVERWRITE `my_db`.`my_name_legend_persistence_temp` " +
            "(`col_int` INT64,`col_string` STRING,`col_decimal` NUMERIC(5,2),`col_datetime` DATETIME,`col_variant` JSON) " +
            "FROM FILES (uris=['/path/xyz/file1.csv','/path/xyz/file2.csv'], format='CSV')";

        String expectedInsertSql = "INSERT INTO `my_db`.`my_name` " +
            "(`col_int`, `col_string`, `col_decimal`, `col_datetime`, `col_variant`, `batch_id`, `append_time`) " +
            "(SELECT legend_persistence_temp.`col_int`,legend_persistence_temp.`col_string`,legend_persistence_temp.`col_decimal`,legend_persistence_temp.`col_datetime`,legend_persistence_temp.`col_variant`,'xyz123',PARSE_DATETIME('%Y-%m-%d %H:%M:%S','2000-01-01 00:00:00') " +
            "FROM `my_db`.`my_name_legend_persistence_temp` as legend_persistence_temp)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedCopySql, ingestSql.get(0));
        Assertions.assertEquals(expectedInsertSql, ingestSql.get(1));

        Assertions.assertEquals("SELECT 0 as `rowsDeleted`", statsSql.get(ROWS_DELETED));
        Assertions.assertEquals("SELECT 0 as `rowsTerminated`", statsSql.get(ROWS_TERMINATED));
        Assertions.assertEquals("SELECT 0 as `rowsUpdated`", statsSql.get(ROWS_UPDATED));
        Assertions.assertEquals("SELECT COUNT(*) as `rowsInserted` FROM `my_db`.`my_name` as my_alias WHERE my_alias.`append_time` = PARSE_DATETIME('%Y-%m-%d %H:%M:%S','2000-01-01 00:00:00')", statsSql.get(ROWS_INSERTED));
    }

    @Test
    public void testBulkLoadWithDigestNotGeneratedAuditEnabledAllOptions()
    {
        BulkLoad bulkLoad = BulkLoad.builder()
            .batchIdField(BATCH_ID)
            .digestGenStrategy(NoDigestGenStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
            .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
            .stagedFilesDatasetProperties(
                BigQueryStagedFilesDatasetProperties.builder()
                    .fileFormat(CsvFileFormat.builder()
                        .encoding("UTF8")
                        .maxBadRecords(100L)
                        .nullMarker("NULL")
                        .quote("'")
                        .compression("GZIP")
                        .fieldDelimiter(",")
                        .skipLeadingRows(1L)
                        .build())
                    .addAllFiles(filesList).build())
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
            .bulkLoadBatchIdValue(BATCH_ID_VALUE)
            .build();

        GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS `my_db`.`my_name`" +
            "(`col_int` INT64 NOT NULL PRIMARY KEY NOT ENFORCED,`col_string` STRING,`col_decimal` NUMERIC(5,2),`col_datetime` DATETIME,`col_variant` JSON,`batch_id` STRING,`append_time` DATETIME)";

        String expectedCopySql = "LOAD DATA OVERWRITE `my_db`.`my_name_legend_persistence_temp` " +
            "(`col_int` INT64,`col_string` STRING,`col_decimal` NUMERIC(5,2),`col_datetime` DATETIME,`col_variant` JSON) " +
            "FROM FILES " +
            "(uris=['/path/xyz/file1.csv','/path/xyz/file2.csv'], max_bad_records=100, quote=''', skip_leading_rows=1, format='CSV', encoding='UTF8', compression='GZIP', field_delimiter=',', null_marker='NULL')";

        String expectedInsertSql = "INSERT INTO `my_db`.`my_name` " +
            "(`col_int`, `col_string`, `col_decimal`, `col_datetime`, `col_variant`, `batch_id`, `append_time`) " +
            "(SELECT legend_persistence_temp.`col_int`,legend_persistence_temp.`col_string`,legend_persistence_temp.`col_decimal`,legend_persistence_temp.`col_datetime`,legend_persistence_temp.`col_variant`,'xyz123',PARSE_DATETIME('%Y-%m-%d %H:%M:%S','2000-01-01 00:00:00') " +
            "FROM `my_db`.`my_name_legend_persistence_temp` as legend_persistence_temp)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedCopySql, ingestSql.get(0));
        Assertions.assertEquals(expectedInsertSql, ingestSql.get(1));

        Assertions.assertEquals("SELECT 0 as `rowsDeleted`", statsSql.get(ROWS_DELETED));
        Assertions.assertEquals("SELECT 0 as `rowsTerminated`", statsSql.get(ROWS_TERMINATED));
        Assertions.assertEquals("SELECT 0 as `rowsUpdated`", statsSql.get(ROWS_UPDATED));
        Assertions.assertEquals("SELECT COUNT(*) as `rowsInserted` FROM `my_db`.`my_name` as my_alias WHERE my_alias.`append_time` = PARSE_DATETIME('%Y-%m-%d %H:%M:%S','2000-01-01 00:00:00')", statsSql.get(ROWS_INSERTED));
    }
}
