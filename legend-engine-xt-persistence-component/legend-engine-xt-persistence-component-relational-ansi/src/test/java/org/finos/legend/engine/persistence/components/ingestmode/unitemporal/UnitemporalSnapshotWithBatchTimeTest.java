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

package org.finos.legend.engine.persistence.components.ingestmode.unitemporal;

import org.finos.legend.engine.persistence.components.IngestModeTest;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.Planners;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnitemporalSnapshotWithBatchTimeTest extends IngestModeTest
{
    private static DatasetDefinition mainTable;
    private static DatasetDefinition stagingTable;

    @BeforeEach
    void initializeTables()
    {
        mainTable = DatasetDefinition.builder()
            .database(mainDbName)
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(mainTableTimeBasedSchema)
            .build();

        stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();
    }

    @Test
    void testGeneratePhysicalPlanForEmptyBatch()
    {
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestField)
            .transactionMilestoning(TransactionDateTime.builder()
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
                .build())
            .build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        GeneratorResult operations = generator.generateOperationsForEmptyBatch(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"MYDB\".\"MAIN\" as sink " +
            "SET sink.\"BATCH_TIME_OUT\" = '2000-01-01 00:00:00' " +
            "WHERE sink.\"BATCH_TIME_OUT\" = '9999-12-31 23:59:59'";

        Assertions.assertEquals(expectedMainTableTimeBasedCreateQueryWithUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQueryWithUpperCase, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedMetadataTableIngestQueryWithUpperCase, metadataIngestSql.get(0));
    }

    @Test
    void testGeneratePhysicalPlanWithoutPartition()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestField)
            .transactionMilestoning(TransactionDateTime.builder()
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
                .build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_time_out\" = '2000-01-01 00:00:00' " +
            "WHERE (sink.\"batch_time_out\" = '9999-12-31 23:59:59') " +
            "AND (NOT (EXISTS " +
            "(SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" = stage.\"digest\"))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_time_in\", \"batch_time_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (stage.\"digest\" IN (SELECT sink.\"digest\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '9999-12-31 23:59:59')))";

        Assertions.assertEquals(expectedMainTableTimeBasedCreateQuery, preActionsSql.get(0));
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
            .transactionMilestoning(TransactionDateTime.builder()
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
                .build())
            .addAllPartitionFields(Arrays.asList(partitionKeys))
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_time_out\" = '2000-01-01 00:00:00' " +
            "WHERE (sink.\"batch_time_out\" = '9999-12-31 23:59:59') " +
            "AND (NOT (EXISTS " +
            "(SELECT * FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" = stage.\"digest\")))) " +
            "AND (EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE sink.\"biz_date\" = stage.\"biz_date\"))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_time_in\", \"batch_time_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (stage.\"digest\" IN (SELECT sink.\"digest\" FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '9999-12-31 23:59:59') AND (sink.\"biz_date\" = stage.\"biz_date\"))))";

        Assertions.assertEquals(expectedMainTableTimeBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testUnitemporalSnapshotMilestoningValidationTimeInMissing()
    {
        try
        {
            UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(TransactionDateTime.builder()
                    .dateTimeOutName(batchTimeOutField)
                    .build())
                .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build TransactionDateTime, some of required attributes are not set [dateTimeInName]", e.getMessage());
        }
    }

    @Test
    void testUnitemporalSnapshotMilestoningValidationBatchIdPrimaryKey()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(mainTableTimeBasedSchemaWithBatchTimeInNotPrimary)
            .build();

        try
        {
            UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(TransactionDateTime.builder()
                    .dateTimeInName(batchTimeInField)
                    .dateTimeOutName(batchTimeOutField)
                    .build())
                .build();

            Planners.get(Datasets.of(mainTable, stagingTable), ingestMode);

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Field \"batch_time_in\" must be a primary key", e.getMessage());
        }
    }

    @Test
    public void testPostRunStatisticsSqlForTimeMode()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestField)
            .transactionMilestoning(TransactionDateTime.builder()
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
                .build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> expectedSQL = new ArrayList<>();
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM \"mydb\".\"staging\" as stage";
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00')))";
        String rowsDeleted = "SELECT 0 as rowsDeleted";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_in\" = '2000-01-01 00:00:00')-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00')))) as rowsInserted";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '2000-01-01 00:00:00')-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00')))) as rowsTerminated";

        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }

    @Test
    public void testPostRunStatisticsAndPostActionSqlForTimeModeWithCleanStagingData()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestField)
            .transactionMilestoning(TransactionDateTime.builder()
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
                .build())
            .addAllPartitionFields(Arrays.asList(partitionKeys))
            .putAllPartitionValuesByField(partitionFilter)
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .cleanupStagingData(true)
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM \"mydb\".\"staging\" as stage";
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00')))";
        String rowsDeleted = "SELECT 0 as rowsDeleted";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_in\" = '2000-01-01 00:00:00')-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00')))) as rowsInserted";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '2000-01-01 00:00:00')-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00')))) as rowsTerminated";

        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));

        List<String> postActionsSql = operations.postActionsSql();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(expectedStagingCleanupQuery);

        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);
    }
}
