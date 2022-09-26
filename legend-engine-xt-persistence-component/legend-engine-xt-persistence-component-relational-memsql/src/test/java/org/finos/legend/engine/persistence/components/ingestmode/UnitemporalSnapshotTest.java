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
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.Planners;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.memsql.MemSqlSink;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnitemporalSnapshotTest extends IngestModeTest
{
    private static DatasetDefinition mainTable;
    private static DatasetDefinition stagingTable;

    @BeforeEach
    void initializeTables()
    {
        mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(mainTableSchema)
            .build();

        stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();
    }

    @Test
    void testGeneratePhysicalPlanForEmptyBatch()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
                .build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperationsForEmptyBatch(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
            "SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1,sink.`batch_time_out` = '2000-01-01 00:00:00' " +
            "WHERE sink.`batch_id_out` = 999999999";

        Assertions.assertEquals(expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testGeneratePhysicalPlanWithoutPartition()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
                .build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
            "SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1,sink.`batch_time_out` = '2000-01-01 00:00:00' " +
            "WHERE (sink.`batch_id_out` = 999999999) " +
            "AND (NOT (EXISTS " +
            "(SELECT * FROM `mydb`.`staging` as stage WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`))))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
            "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id_in`, `batch_id_out`, `batch_time_in`, `batch_time_out`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
            "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'),999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
            "FROM `mydb`.`staging` as stage " +
            "WHERE NOT (stage.`digest` IN (SELECT sink.`digest` FROM `mydb`.`main` as sink WHERE sink.`batch_id_out` = 999999999)))";

        Assertions.assertEquals(expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testGeneratePhysicalPlanWithoutPartitionWithUpperCaseOptimizer()
    {
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
                .build())
            .build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `MYDB`.`MAIN` as sink SET sink.`BATCH_ID_OUT` = (SELECT COALESCE(MAX(batch_metadata.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as batch_metadata WHERE batch_metadata.`TABLE_NAME` = 'main')-1,sink.`BATCH_TIME_OUT` = '2000-01-01 00:00:00' WHERE (sink.`BATCH_ID_OUT` = 999999999) AND (NOT (EXISTS (SELECT * FROM `MYDB`.`STAGING` as stage WHERE ((sink.`ID` = stage.`ID`) AND (sink.`NAME` = stage.`NAME`)) AND (sink.`DIGEST` = stage.`DIGEST`))))";
        String expectedUpsertQuery = "INSERT INTO `MYDB`.`MAIN` (`ID`, `NAME`, `AMOUNT`, `BIZ_DATE`, `DIGEST`, `BATCH_ID_IN`, `BATCH_ID_OUT`, `BATCH_TIME_IN`, `BATCH_TIME_OUT`) (SELECT stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`BIZ_DATE`,stage.`DIGEST`,(SELECT COALESCE(MAX(batch_metadata.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as batch_metadata WHERE batch_metadata.`TABLE_NAME` = 'main'),999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' FROM `MYDB`.`STAGING` as stage WHERE NOT (stage.`DIGEST` IN (SELECT sink.`DIGEST` FROM `MYDB`.`MAIN` as sink WHERE sink.`BATCH_ID_OUT` = 999999999)))";
        Assertions.assertEquals(expectedMainTableCreateQueryWithUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQueryWithUpperCase, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQueryWithUpperCase, metadataIngestSql.get(0));
    }

    @Test
    void testGeneratePhysicalPlanWithPartitionFilter()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
                .build())
            .addAllPartitionFields(Arrays.asList(partitionKeys))
            .putAllPartitionValuesByField(partitionFilter)
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
            "SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1,sink.`batch_time_out` = '2000-01-01 00:00:00' " +
            "WHERE (sink.`batch_id_out` = 999999999) " +
            "AND (NOT (EXISTS " +
            "(SELECT * FROM `mydb`.`staging` as stage WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`)))) " +
            "AND (sink.`biz_date` IN ('2000-01-01 00:00:00','2000-01-02 00:00:00'))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
            "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id_in`, `batch_id_out`, `batch_time_in`, `batch_time_out`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
            "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'),999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
            "FROM `mydb`.`staging` as stage " +
            "WHERE NOT (stage.`digest` IN (SELECT sink.`digest` FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = 999999999) AND (sink.`biz_date` IN ('2000-01-01 00:00:00','2000-01-02 00:00:00')))))";

        Assertions.assertEquals(expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testGeneratePhysicalPlanWithPartition()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
                .build())
            .addAllPartitionFields(Arrays.asList(partitionKeys))
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
            "SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1,sink.`batch_time_out` = '2000-01-01 00:00:00' " +
            "WHERE (sink.`batch_id_out` = 999999999) " +
            "AND (NOT (EXISTS " +
            "(SELECT * FROM `mydb`.`staging` as stage WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`)))) " +
            "AND (EXISTS (SELECT * FROM `mydb`.`staging` as stage WHERE sink.`biz_date` = stage.`biz_date`))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
            "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id_in`, `batch_id_out`, `batch_time_in`, `batch_time_out`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
            "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'),999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
            "FROM `mydb`.`staging` as stage " +
            "WHERE NOT (stage.`digest` IN (SELECT sink.`digest` FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = 999999999) AND (sink.`biz_date` = stage.`biz_date`))))";

        Assertions.assertEquals(expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testGeneratePhysicalPlanWithLessColumnsInStaging()
    {
        Dataset stagingTableWithLessColumns = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(stagingTableSchemaWithLimitedColumns)
            .build();

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
                .build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTableWithLessColumns);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
            "SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1,sink.`batch_time_out` = '2000-01-01 00:00:00' " +
            "WHERE (sink.`batch_id_out` = 999999999) " +
            "AND (NOT (EXISTS " +
            "(SELECT * FROM `mydb`.`staging` as stage WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`))))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
            "(`id`, `name`, `amount`, `digest`, `batch_id_in`, `batch_id_out`, `batch_time_in`, `batch_time_out`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`digest`," +
            "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'),999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
            "FROM `mydb`.`staging` as stage " +
            "WHERE NOT (stage.`digest` IN (SELECT sink.`digest` FROM `mydb`.`main` as sink WHERE sink.`batch_id_out` = 999999999)))";

        Assertions.assertEquals(expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testUnitemporalSnapshotMilestoningValidationMainDatasetMissing()
    {
        try
        {
            UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                    .batchIdInName(batchIdInField)
                    .batchIdOutName(batchIdOutField)
                    .dateTimeInName(batchTimeInField)
                    .dateTimeOutName(batchTimeOutField)
                    .build())
                .build();

            Datasets datasets = Datasets.of(null, stagingTable);

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("mainDataset", e.getMessage());
        }
    }

    @Test
    void testUnitemporalSnapshotMilestoningValidationBatchIdInMissing()
    {
        try
        {
            UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                    .batchIdOutName(batchIdOutField)
                    .dateTimeInName(batchTimeInField)
                    .dateTimeOutName(batchTimeOutField)
                    .build())
                .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build BatchIdAndDateTime, some of required attributes are not set [batchIdInName]", e.getMessage());
        }
    }

    @Test
    void testUnitemporalSnapshotMilestoningValidationBatchIdPrimaryKey()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(mainTableSchemaWithBatchIdInNotPrimary)
            .build();

        try
        {
            UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                    .batchIdInName(batchIdInField)
                    .batchIdOutName(batchIdOutField)
                    .dateTimeInName(batchTimeInField)
                    .dateTimeOutName(batchTimeOutField)
                    .build())
                .build();

            Planners.get(Datasets.of(mainTable, stagingTable), ingestMode);

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Field \"batch_id_in\" must be a primary key", e.getMessage());
        }
    }

    @Test
    public void testPostRunStatisticsAndPostActionsSqlForBatchIdModelWithCleanStagingDataWithoutStatCollection()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
                .build())
            .addAllPartitionFields(Arrays.asList(partitionKeys))
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        Assertions.assertEquals(0, new ArrayList<>(operations.postIngestStatisticsSql().values()).size());

        List<String> postActionsSql = operations.postActionsSql();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(expectedStagingCleanupQuery);

        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);
    }

    @Test
    public void testPostRunStatisticsAndPostActionsSqlForBatchIdModelWithoutCleanStagingDataWithStatCollection()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
                .build())
            .addAllPartitionFields(Arrays.asList(partitionKeys))
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(MemSqlSink.get())
            .cleanupStagingData(false)
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM `mydb`.`staging` as stage";
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1) AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'))))";
        String rowsDeleted = "SELECT 0 as rowsDeleted";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE sink.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'))-(SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1) AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'))))) as rowsInserted";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1)-(SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1) AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'))))) as rowsTerminated";

        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));

        Assertions.assertEquals(0, operations.postActionsSql().size());
    }
}
