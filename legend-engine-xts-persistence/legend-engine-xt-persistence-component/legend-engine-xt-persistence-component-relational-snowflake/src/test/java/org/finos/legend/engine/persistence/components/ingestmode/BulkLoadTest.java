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
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
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
            .primaryKey(true)
            .build();
    private static Field col2 = Field.builder()
            .name("col_integer")
            .type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty()))
            .nullable(false)
            .unique(true)
            .build();
    private static Field col3 = Field.builder()
            .name("col_bigint")
            .type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty()))
            .columnNumber(4)
            .build();
    private static Field col4 = Field.builder()
            .name("col_tinyint")
            .type(FieldType.of(DataType.TINYINT, Optional.empty(), Optional.empty()))
            .columnNumber(5)
            .build();

    protected final ZonedDateTime fixedZonedDateTime_2000_01_01 = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    protected final Clock fixedClock_2000_01_01 = Clock.fixed(fixedZonedDateTime_2000_01_01.toInstant(), ZoneOffset.UTC);

    @Test
    public void testBulkLoadWithDigestNotGeneratedColumnNumbersDerived()
    {
        BulkLoad bulkLoad = BulkLoad.builder()
                .digestField("digest")
                .generateDigest(false)
                .auditing(DateTimeAuditing.builder().dateTimeField(APPEND_TIME).build())
                .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
                .location("my_location")
                .fileFormat("my_file_format")
                .filePattern("my_file_pattern")
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
                .build();

        GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"my_db\".\"my_name\"(\"col_int\" INTEGER NOT NULL PRIMARY KEY,\"col_integer\" INTEGER NOT NULL UNIQUE)";
        String expectedIngestSql = "COPY INTO \"my_db\".\"my_name\" " +
                "(\"col_int\", \"col_integer\", \"append_time\") " +
                "FROM " +
                "(SELECT legend_persistence_stage.$1 as \"col_int\",legend_persistence_stage.$2 as \"col_integer\",'2000-01-01 00:00:00' " +
                "FROM my_location (FILE_FORMAT => 'my_file_format', PATTERN => 'my_file_pattern') as legend_persistence_stage)" +
                " on_error = 'ABORT_STATEMENT'";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        System.out.println(statsSql);

        Assertions.assertEquals("SELECT 0 as \"rowsDeleted\"", statsSql.get(ROWS_DELETED));
        Assertions.assertEquals("SELECT 0 as \"rowsTerminated\"", statsSql.get(ROWS_TERMINATED));
        Assertions.assertEquals("SELECT 0 as \"rowsUpdated\"", statsSql.get(ROWS_UPDATED));
        Assertions.assertEquals("SELECT COUNT(*) as \"incomingRecordCount\" FROM my_location (FILE_FORMAT => 'my_file_format', PATTERN => 'my_file_pattern') as legend_persistence_stage", statsSql.get(INCOMING_RECORD_COUNT));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM my_location (FILE_FORMAT => 'my_file_format', PATTERN => 'my_file_pattern') as legend_persistence_stage", statsSql.get(ROWS_INSERTED));
    }

    @Test
    public void testBulkLoadWithDigestNotGeneratedColumnNumbersProvided()
    {
        BulkLoad bulkLoad = BulkLoad.builder()
                .digestField("digest")
                .generateDigest(false)
                .auditing(NoAuditing.builder().build())
                .build();

        Dataset stagedFilesDataset = StagedFilesDataset.builder()
                .location("my_location")
                .fileFormat("my_file_format")
                .filePattern("my_file_pattern")
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col3, col4)).build())
                .alias("t")
                .build();

        Dataset mainDataset = DatasetDefinition.builder()
                .database("my_db").name("my_name").alias("my_alias")
                .schema(SchemaDefinition.builder().addAllFields(Arrays.asList(col3, col4)).build())
                .build();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(bulkLoad)
                .relationalSink(SnowflakeSink.get())
                .collectStatistics(true)
                .build();

        GeneratorResult operations = generator.generateOperations(Datasets.of(mainDataset, stagedFilesDataset));

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();
        Map<StatisticName, String> statsSql = operations.postIngestStatisticsSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"my_db\".\"my_name\"(\"col_bigint\" BIGINT,\"col_tinyint\" TINYINT)";
        String expectedIngestSql = "COPY INTO \"my_db\".\"my_name\" " +
                "(\"col_bigint\", \"col_tinyint\") " +
                "FROM " +
                "(SELECT t.$4 as \"col_bigint\",t.$5 as \"col_tinyint\" " +
                "FROM my_location (FILE_FORMAT => 'my_file_format', PATTERN => 'my_file_pattern') as t) " +
                "on_error = 'ABORT_STATEMENT'";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));
        System.out.println(statsSql);

        Assertions.assertEquals("SELECT 0 as \"rowsDeleted\"", statsSql.get(ROWS_DELETED));
        Assertions.assertEquals("SELECT 0 as \"rowsTerminated\"", statsSql.get(ROWS_TERMINATED));
        Assertions.assertEquals("SELECT 0 as \"rowsUpdated\"", statsSql.get(ROWS_UPDATED));
        Assertions.assertEquals("SELECT COUNT(*) as \"incomingRecordCount\" FROM my_location (FILE_FORMAT => 'my_file_format', PATTERN => 'my_file_pattern') as t", statsSql.get(INCOMING_RECORD_COUNT));
        Assertions.assertEquals("SELECT COUNT(*) as \"rowsInserted\" FROM my_location (FILE_FORMAT => 'my_file_format', PATTERN => 'my_file_pattern') as t", statsSql.get(ROWS_INSERTED));
    }
}
