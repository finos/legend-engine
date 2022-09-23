// Copyright 2022 Goldman Sachs
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

import java.util.ArrayList;
import java.util.List;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.memsql.MemSqlSink;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AppendOnlyTest extends IngestModeTest
{

    @Test
    void testIncrementalAppendMilestoning()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestField)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` " +
            "(`id`, `name`, `amount`, `biz_date`, `digest`) " +
            "(SELECT * FROM `mydb`.`staging` as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
            "WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND " +
            "(sink.`digest` = stage.`digest`))))";

        Assertions.assertEquals(expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));
    }

    @Test
    void testIncrementalAppendMilestoningWithUpperCaseOptimizer()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestField)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `MYDB`.`MAIN` (`ID`, `NAME`, `AMOUNT`, `BIZ_DATE`, `DIGEST`) " +
            "(SELECT * FROM `MYDB`.`STAGING` as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM `MYDB`.`MAIN` as sink " +
            "WHERE ((sink.`ID` = stage.`ID`) " +
            "AND (sink.`NAME` = stage.`NAME`)) " +
            "AND (sink.`DIGEST` = stage.`DIGEST`))))";

        Assertions.assertEquals(expectedBaseTablePlusDigestCreateQueryWithUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));
    }

    @Test
    void testIncrementalAppendMilestoningWithLessColumnsInStaging()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(stagingTableSchemaWithLimitedColumns)
            .build();

        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestField)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` " +
            "(`id`, `name`, `amount`, `digest`) " +
            "(SELECT * FROM `mydb`.`staging` as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
            "WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND " +
            "(sink.`digest` = stage.`digest`))))";

        Assertions.assertEquals(expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));
    }

    @Test
    void testIncrementalAppendMilestoningWithUpdateBatchTimeField()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigestAndUpdateBatchTimeField)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestField)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` " +
            "(" +
            "`id`, " +
            "`name`, " +
            "`amount`, " +
            "`biz_date`, " +
            "`digest`, " +
            "`batch_update_time`" +
            ") " +
            "(" +
            "SELECT " +
            "stage.`id`," +
            "stage.`name`," +
            "stage.`amount`," +
            "stage.`biz_date`," +
            "stage.`digest`," +
            "'2000-01-01 00:00:00' " +
            "FROM `mydb`.`staging` as stage " +
            "WHERE NOT " +
            "(" +
            "EXISTS " +
            "(" +
            "SELECT * " +
            "FROM `mydb`.`main` as sink " +
            "WHERE " +
            "((sink.`id` = stage.`id`) " +
            "AND " +
            "(sink.`name` = stage.`name`)) " +
            "AND " +
            "(sink.`digest` = stage.`digest`)" +
            ")" +
            ")" +
            ")";

        Assertions.assertEquals(expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));
    }

    @Test
    void testIncrementalAppendMilestoningValidationPkFieldsMissing()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithNoPrimaryKeys)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestField)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        try
        {
            RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(ingestMode)
                .relationalSink(MemSqlSink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();

            GeneratorResult operations = generator.generateOperations(datasets);

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Primary key list must not be empty", e.getMessage());
        }
    }

    @Test
    void testIncrementalAppendMilestoningValidationupdateBatchTimeFieldMissing()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        try
        {
            AppendOnly ingestMode = AppendOnly.builder()
                .digestField(digestField)
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .auditing(DateTimeAuditing.builder().build())
                .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build DateTimeAuditing, some of required attributes are not set [dateTimeField]", e.getMessage());
        }
    }

    @Test
    public void testPostRunStatsSqlWithoutUpdateBatchColumn()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestField)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM `mydb`.`staging` as stage";

        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
    }

    @Test
    public void testPostRunStatisticsSqlWithUpdateBatchColumn()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigestAndUpdateBatchTimeField)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestField)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> expectedSQL = new ArrayList<>();
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM `mydb`.`staging` as stage";
        String rowsUpdated = "SELECT 0 as rowsUpdated";
        String rowsDeleted = "SELECT 0 as rowsDeleted";
        String rowsInserted = "SELECT COUNT(*) as rowsInserted FROM `mydb`.`main` as sink WHERE sink.`batch_update_time` = (SELECT MAX(sink.`batch_update_time`) FROM `mydb`.`main` as sink)";
        String rowsTerminated = "SELECT 0 as rowsTerminated";

        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));

    }

    @Test
    public void testPostRunStatisticsAndPostActionSqlWithUpdateBatchColumnAndCleanStagingData()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigestAndUpdateBatchTimeField)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestField)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
            .build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .cleanupStagingData(true)
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> expectedSQL = new ArrayList<>();
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM `mydb`.`staging` as stage";
        String rowsUpdated = "SELECT 0 as rowsUpdated";
        String rowsDeleted = "SELECT 0 as rowsDeleted";
        String rowsInserted = "SELECT COUNT(*) as rowsInserted FROM `mydb`.`main` as sink WHERE sink.`batch_update_time` = (SELECT MAX(sink.`batch_update_time`) FROM `mydb`.`main` as sink)";
        String rowsTerminated = "SELECT 0 as rowsTerminated";

        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));

        List<String> postActionsSql = operations.postActionsSql();
        expectedSQL = new ArrayList<>();
        expectedSQL.add(expectedStagingCleanupQuery);

        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);
    }
}
