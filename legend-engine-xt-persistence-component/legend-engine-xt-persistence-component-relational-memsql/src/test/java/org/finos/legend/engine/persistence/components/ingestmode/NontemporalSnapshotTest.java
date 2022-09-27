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

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.Planner;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.planner.Planners;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.memsql.MemSqlSink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NontemporalSnapshotTest extends IngestModeTest
{
    String cleanupTableSql = "DELETE FROM `mydb`.`main` as sink";
    String cleanupTableSqlWithUpperCase = "DELETE FROM `MYDB`.`MAIN` as sink";

    @Test
    void testSnapshotMilestoning()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchema)
            .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` " +
            "(`id`, `name`, `amount`, `biz_date`) " +
            "(SELECT * FROM `mydb`.`staging` as stage)";

        Assertions.assertEquals(expectedBaseTableCreateQuery, preActionsSqlList.get(0));

        Assertions.assertEquals(cleanupTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Test
    void testSnapshotMilestoningWithUpperCaseOptimizer()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchema)
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `MYDB`.`MAIN` " +
            "(`ID`, `NAME`, `AMOUNT`, `BIZ_DATE`) " +
            "(SELECT * FROM `MYDB`.`STAGING` as stage)";

        Assertions.assertEquals(expectedBaseTableCreateQueryWithUpperCase, preActionsSqlList.get(0));

        Assertions.assertEquals(cleanupTableSqlWithUpperCase, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Test
    void testSnapshotMilestoningWithLessColumnsInStaging()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableShortenedSchema)
            .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` (" +
            "`id`, `name`, `amount`) " +
            "(SELECT * FROM `mydb`.`staging` as stage)";

        Assertions.assertEquals(expectedBaseTableCreateQuery, preActionsSqlList.get(0));

        Assertions.assertEquals(cleanupTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Test
    void testSnapshotMilestoningWithUpdateBatchTime() throws Exception
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchema)
            .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder()
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
            "(`id`, `name`, `amount`, `biz_date`, `batch_update_time`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,'2000-01-01 00:00:00' " +
            "FROM `mydb`.`staging` as stage)";

        Assertions.assertEquals(expectedBaseTableCreateQuery, preActionsSqlList.get(0));

        Assertions.assertEquals(cleanupTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Test
    void testSnapshotMilestoningValidation()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchema)
            .build();

        try
        {
            Datasets datasets = Datasets.of(mainTable, null);

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("stagingDataset", e.getMessage());
        }
    }

    @Test
    void testSnapshotMilestoningValidationWithUpdateBatchTime()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableShortenedSchema)
            .build();

        try
        {
            NontemporalSnapshot ingestMode = NontemporalSnapshot.builder()
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
    public void testPostRunStatisticsSql()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableShortenedSchema)
            .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM `mydb`.`staging` as stage";
        String rowsUpdated = "SELECT 0 as rowsUpdated";
        String rowsInserted = "SELECT COUNT(*) as rowsInserted FROM `mydb`.`main` as sink";
        String rowsTerminated = "SELECT 0 as rowsTerminated";

        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }

    @Test
    public void testPostRunStatisticsAndPostActionSqlWithCleanStagingData()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableShortenedSchema)
            .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

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
        String rowsInserted = "SELECT COUNT(*) as rowsInserted FROM `mydb`.`main` as sink";
        String rowsTerminated = "SELECT 0 as rowsTerminated";

        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));

        List<String> postActionsSql = operations.postActionsSql();
        expectedSQL = new ArrayList<>();
        expectedSQL.add(expectedStagingCleanupQuery);

        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);
    }

    @Test
    public void testPostRunStatisticsAndPostActionSqlWithDeleteStagingData() throws Exception
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableShortenedSchema)
            .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(true).collectStatistics(true).build();
        Resources resources = Resources.builder().externalDatasetImported(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        Planner planner = Planners.get(datasets, ingestMode, options);

        // post actions
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());
        LogicalPlan postActionsLogicalPlan = planner.buildLogicalPlanForPostActions(resources);
        SqlPlan physicalPlanForPostActions = transformer.generatePhysicalPlan(postActionsLogicalPlan);
        List<String> sqlsForPostActions = physicalPlanForPostActions.getSqlList();

        // post-run stats
        Map<StatisticName, LogicalPlan> postRunStatisticsLogicalPlan = planner.buildLogicalPlanForPostRunStatistics(resources);
        Map<StatisticName, SqlPlan> postRunStatisticsPhysicalPlan = new HashMap<>();
        for (StatisticName statistic : postRunStatisticsLogicalPlan.keySet())
        {
            postRunStatisticsPhysicalPlan.put(statistic, transformer.generatePhysicalPlan(postRunStatisticsLogicalPlan.get(statistic)));
        }
        Map<StatisticName, String> postMilestoneStatistics = new HashMap<>();
        for (StatisticName statistic : postRunStatisticsPhysicalPlan.keySet())
        {
            postMilestoneStatistics.put(statistic, postRunStatisticsPhysicalPlan.get(statistic).getSql());
        }

        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM `mydb`.`staging` as stage";
        String rowsUpdated = "SELECT 0 as rowsUpdated";
        String rowsInserted = "SELECT COUNT(*) as rowsInserted FROM `mydb`.`main` as sink";
        String rowsTerminated = "SELECT 0 as rowsTerminated";

        Assertions.assertEquals(incomingRecordCount, postMilestoneStatistics.get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, postMilestoneStatistics.get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsInserted, postMilestoneStatistics.get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, postMilestoneStatistics.get(StatisticName.ROWS_TERMINATED));

        List expectedSQL = new ArrayList<>();
        expectedSQL.add(expectedDropTableQuery);
        assertIfListsAreSameIgnoringOrder(expectedSQL, sqlsForPostActions);
    }

    @Test
    public void testPreRunStatisticMainTableExists()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableShortenedSchema)
            .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .cleanupStagingData(true)
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preRunStatisticsSql = new ArrayList<>(operations.preIngestStatisticsSql().values());
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add("SELECT COUNT(*) as rowsDeleted FROM `mydb`.`main` as sink");

        assertIfListsAreSameIgnoringOrder(expectedSQL, preRunStatisticsSql);
    }
}