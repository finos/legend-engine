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
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.Planners;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class UnitemporalDeltaWithBatchIdTest extends IngestModeTest
{
    private static DatasetDefinition mainTable;
    private static DatasetDefinition stagingTable;

    @BeforeEach
    void initializeTables()
    {
        mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(mainTableBatchIdBasedSchema)
            .build();

        stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();
    }

    @Test
    void testUnitemporalMilestoningWithoutDeleteIndicator()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestField)
            .addAllKeyFields(primaryKeysList)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
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
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "(sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999 " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")))))";

        Assertions.assertEquals(expectedMainTableBatchIdBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testUnitemporalMilestoningWithoutDeleteIndicatorWithDataSplit()
    {
        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(baseTableSchemaWithDataSplit)
            .build();

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestField)
            .addAllKeyFields(primaryKeysList)
            .dataSplitField(Optional.of(dataSplitField))
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(datasets, dataSplitRanges);

        for (int i = 0; i < dataSplitRanges.size(); i++)
        {
            List<String> preActionsSql = operations.get(i).preActionsSql();
            List<String> milestoningSql = operations.get(i).ingestSql();
            DataSplitRange dataSplitRange = dataSplitRanges.get(i);
        }

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "(sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999 " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) AND " +
            "(NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\"))))))";

        Assertions.assertEquals(expectedMainTableBatchIdBasedCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));

        Assertions.assertEquals(String.format(expectedMilestoneQuery, 2, 5), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(String.format(expectedUpsertQuery, 2, 5), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(String.format(expectedMilestoneQuery, 6, 7), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(String.format(expectedUpsertQuery, 6, 7), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, operations.get(0).metadataIngestSql().get(0));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, operations.get(1).metadataIngestSql().get(0));

        Assertions.assertEquals(4, operations.size());
    }

    @Test
    void testUnitemporalMilestoningWithoutDeleteIndicatorWithUpperCaseOptimizer()
    {
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestField)
            .addAllKeyFields(primaryKeysList)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"MYDB\".\"MAIN\" as SINK " +
            "SET SINK.\"BATCH_ID_OUT\" = (SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 " +
            "FROM BATCH_METADATA as BATCH_METADATA WHERE BATCH_METADATA.\"TABLE_NAME\" = 'main')-1 " +
            "WHERE (SINK.\"BATCH_ID_OUT\" = 999999999) " +
            "AND (EXISTS (SELECT * FROM \"MYDB\".\"STAGING\" as STAGE " +
            "WHERE ((SINK.\"ID\" = STAGE.\"ID\") AND (SINK.\"NAME\" = STAGE.\"NAME\")) " +
            "AND (SINK.\"DIGEST\" <> STAGE.\"DIGEST\")))";

        String expectedUpsertQuery = "INSERT INTO \"MYDB\".\"MAIN\" " +
            "(\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"DIGEST\", \"BATCH_ID_IN\", \"BATCH_ID_OUT\") " +
            "(SELECT STAGE.\"ID\",STAGE.\"NAME\",STAGE.\"AMOUNT\",STAGE.\"BIZ_DATE\"," +
            "STAGE.\"DIGEST\",(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE BATCH_METADATA.\"TABLE_NAME\" = 'main')," +
            "999999999 FROM \"MYDB\".\"STAGING\" as STAGE " +
            "WHERE NOT (EXISTS (SELECT * FROM \"MYDB\".\"MAIN\" as SINK " +
            "WHERE (SINK.\"BATCH_ID_OUT\" = 999999999) AND (SINK.\"DIGEST\" = STAGE.\"DIGEST\") " +
            "AND ((SINK.\"ID\" = STAGE.\"ID\") AND (SINK.\"NAME\" = STAGE.\"NAME\")))))";

        Assertions.assertEquals(expectedMainTableBatchIdBasedCreateQueryWithUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQueryWithUpperCase, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQueryWithUpperCase, metadataIngestSql.get(0));
    }

    @Test
    void testUnitemporalMilestoningWithDeleteIndicator()
    {
        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(stagingTableSchemaWithDeleteIndicator)
            .build();

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestField)
            .addAllKeyFields(primaryKeysList)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorField)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
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

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink SET sink.\"batch_id_out\" = " +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE " +
            "(sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) " +
            "AND ((sink.\"digest\" <> stage.\"digest\") OR (stage.\"delete_indicator\" IN ('yes','1','true')))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999 FROM \"mydb\".\"staging\" as stage " +
            "WHERE (NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND (sink.\"digest\" = stage.\"digest\") " +
            "AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\"))))) AND " +
            "(stage.\"delete_indicator\" NOT IN ('yes','1','true')))";

        Assertions.assertEquals(expectedMainTableBatchIdBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testUnitemporalMilestoningWithDeleteIndicatorWithDataSplit()
    {
        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(stagingTableSchemaWithDeleteIndicatorWithDataSplit)
            .build();

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestField)
            .addAllKeyFields(primaryKeysList)
            .dataSplitField(Optional.of(dataSplitField))
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorField)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(datasets, dataSplitRanges);

        for (int i = 0; i < dataSplitRanges.size(); i++)
        {
            List<String> preActionsSql = operations.get(i).preActionsSql();
            List<String> milestoningSql = operations.get(i).ingestSql();
            DataSplitRange dataSplitRange = dataSplitRanges.get(i);
        }

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink SET sink.\"batch_id_out\" = " +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE " +
            "(sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) " +
            "AND ((sink.\"digest\" <> stage.\"digest\") OR (stage.\"delete_indicator\" IN ('yes','1','true')))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999 FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) AND " +
            "(NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND (sink.\"digest\" = stage.\"digest\") " +
            "AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\"))))) AND " +
            "(stage.\"delete_indicator\" NOT IN ('yes','1','true')))";

        Assertions.assertEquals(expectedMainTableBatchIdBasedCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));

        Assertions.assertEquals(String.format(expectedMilestoneQuery, 2, 5), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(String.format(expectedUpsertQuery, 2, 5), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(String.format(expectedMilestoneQuery, 11, 14), operations.get(3).ingestSql().get(0));
        Assertions.assertEquals(String.format(expectedUpsertQuery, 11, 14), operations.get(3).ingestSql().get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, operations.get(0).metadataIngestSql().get(0));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, operations.get(3).metadataIngestSql().get(0));
    }

    @Test
    void testUnitemporalIncrementalMilestoningValidationBatchIdOutMissing()
    {
        try
        {
            UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .addAllKeyFields(primaryKeysList)
                .transactionMilestoning(BatchId.builder()
                    .batchIdInName(batchIdInField)
                    .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                    .deleteField(deleteIndicatorField)
                    .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                    .build())
                .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build BatchId, some of required attributes are not set [batchIdOutName]", e.getMessage());
        }
    }

    @Test
    void testUnitemporalIncrementalMilestoningValidationBatchIdPrimaryKey()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName)
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(mainTableBatchIdBasedSchemaWithBatchIdInNotPrimary)
            .build();

        try
        {
            UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .addAllKeyFields(primaryKeysList)
                .transactionMilestoning(BatchId.builder()
                    .batchIdInName(batchIdInField)
                    .batchIdOutName(batchIdOutField)
                    .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                    .deleteField(deleteIndicatorField)
                    .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
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
    public void testPostRunStatisticsSqlForBatchIdMode()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestField)
            .addAllKeyFields(primaryKeysList)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM \"mydb\".\"staging\" as stage";
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))";
        String rowsDeleted = "SELECT 0 as rowsDeleted";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))) as rowsInserted";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1)-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))) as rowsTerminated";

        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }

    @Test
    public void testPostRunStatisticsAndPostActionSqlForBatchIdModeWithCleanStagingData()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestField)
            .addAllKeyFields(primaryKeysList)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .cleanupStagingData(true)
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM \"mydb\".\"staging\" as stage";
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))";
        String rowsDeleted = "SELECT 0 as rowsDeleted";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))) as rowsInserted";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1)-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))) as rowsTerminated";

        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));

        List<String> postActionsSql = operations.postActionsSql();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(expectedTruncateTableQuery);

        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);
    }
}