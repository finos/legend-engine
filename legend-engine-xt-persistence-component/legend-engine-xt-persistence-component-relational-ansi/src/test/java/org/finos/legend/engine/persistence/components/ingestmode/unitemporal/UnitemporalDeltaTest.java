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
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
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

public class UnitemporalDeltaTest extends IngestModeTest
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
    void testUnitemporalMilestoningWithoutDeleteIndicator()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
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
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1," +
            "sink.\"batch_time_out\" = '2000-01-01 00:00:00' " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "(sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")))))";

        Assertions.assertEquals(expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testUnitemporalMilestoningWithoutDeleteIndicatorWithDataSplit()
    {
        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigestAndDataSplit)
            .build();

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestField)
            .dataSplitField(Optional.of(dataSplitField))
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
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1," +
            "sink.\"batch_time_out\" = '2000-01-01 00:00:00' " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "(sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) AND " +
            "(NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\"))))))";

        Assertions.assertEquals(expectedMainTableCreateQuery, operations.get(0).preActionsSql().get(0));
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
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
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

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"MYDB\".\"MAIN\" as sink SET sink.\"BATCH_ID_OUT\" = (SELECT COALESCE(MAX(batch_metadata.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as batch_metadata WHERE batch_metadata.\"TABLE_NAME\" = 'main')-1,sink.\"BATCH_TIME_OUT\" = '2000-01-01 00:00:00' WHERE (sink.\"BATCH_ID_OUT\" = 999999999) AND (EXISTS (SELECT * FROM \"MYDB\".\"STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (sink.\"DIGEST\" <> stage.\"DIGEST\")))";

        String expectedUpsertQuery = "INSERT INTO \"MYDB\".\"MAIN\" (\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"DIGEST\", \"BATCH_ID_IN\", \"BATCH_ID_OUT\", \"BATCH_TIME_IN\", \"BATCH_TIME_OUT\") (SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"BIZ_DATE\",stage.\"DIGEST\",(SELECT COALESCE(MAX(batch_metadata.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as batch_metadata WHERE batch_metadata.\"TABLE_NAME\" = 'main'),999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' FROM \"MYDB\".\"STAGING\" as stage WHERE NOT (EXISTS (SELECT * FROM \"MYDB\".\"MAIN\" as sink WHERE (sink.\"BATCH_ID_OUT\" = 999999999) AND (sink.\"DIGEST\" = stage.\"DIGEST\") AND ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")))))";

        Assertions.assertEquals(expectedMainTableCreateQueryWithUpperCase, preActionsSql.get(0));
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
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
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
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1," +
            "sink.\"batch_time_out\" = '2000-01-01 00:00:00' " +
            "WHERE " +
            "(sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) " +
            "AND ((sink.\"digest\" <> stage.\"digest\") OR (stage.\"delete_indicator\" IN ('yes','1','true')))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", " +
            "\"batch_time_in\", \"batch_time_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' FROM \"mydb\".\"staging\" as stage " +
            "WHERE (NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND (sink.\"digest\" = stage.\"digest\") " +
            "AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\"))))) AND " +
            "(stage.\"delete_indicator\" NOT IN ('yes','1','true')))";

        Assertions.assertEquals(expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testUnitemporalMilestoningWithBooleanDeleteIndicator()
    {
        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(stagingTableSchemaWithBooleanDeleteIndicator)
            .build();

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .dateTimeInName(batchTimeInField)
                .dateTimeOutName(batchTimeOutField)
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorField)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorBooleanValues))
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
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1," +
            "sink.\"batch_time_out\" = '2000-01-01 00:00:00' " +
            "WHERE " +
            "(sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) " +
            "AND ((sink.\"digest\" <> stage.\"digest\") OR (stage.\"delete_indicator\" = true))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", " +
            "\"batch_time_in\", \"batch_time_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' FROM \"mydb\".\"staging\" as stage " +
            "WHERE (NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND (sink.\"digest\" = stage.\"digest\") " +
            "AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\"))))) AND " +
            "(stage.\"delete_indicator\" <> true))";

        Assertions.assertEquals(expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }


    @Test
    void testUnitemporalIncrementalWithLessColumnsInStaging()
    {
        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(stagingTableSchemaWithLimitedColumns)
            .build();

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
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
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1," +
            "sink.\"batch_time_out\" = '2000-01-01 00:00:00' " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE " +
            "((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND (sink.\"digest\" = stage.\"digest\") " +
            "AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")))))";

        Assertions.assertEquals(expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testUnitemporalIncrementalMilestoningValidationDigestMissing()
    {
        try
        {
            UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .transactionMilestoning(BatchIdAndDateTime.builder()
                    .batchIdInName(batchIdInField)
                    .batchIdOutName(batchIdOutField)
                    .dateTimeInName(batchTimeInField)
                    .dateTimeOutName(batchTimeOutField)
                    .build())
                .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build UnitemporalDelta, some of required attributes are not set [digestField]", e.getMessage());
        }
    }

    @Test
    void testUnitemporalIncrementalMilestoningValidationBatchTimeOutMissing()
    {
        try
        {
            UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                    .batchIdInName(batchIdInField)
                    .batchIdOutName(batchIdOutField)
                    .dateTimeInName(batchTimeInField)
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
            Assertions.assertEquals("Cannot build BatchIdAndDateTime, some of required attributes are not set [dateTimeOutName]", e.getMessage());
        }
    }

    @Test
    void testUnitemporalIncrementalMilestoningValidationDeleteIndicatorFieldMissing()
    {
        try
        {
            UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                    .batchIdInName(batchIdInField)
                    .batchIdOutName(batchIdOutField)
                    .dateTimeInName(batchTimeInField)
                    .dateTimeOutName(batchTimeOutField)
                    .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                    .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                    .build())
                .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build DeleteIndicatorMergeStrategy, some of required attributes are not set [deleteField]", e.getMessage());
        }
    }

    @Test
    void testUnitemporalIncrementalMilestoningValidationDeleteIndicatorValuesMissing()
    {
        try
        {
            UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                    .batchIdInName(batchIdInField)
                    .batchIdOutName(batchIdOutField)
                    .dateTimeInName(batchTimeInField)
                    .dateTimeOutName(batchTimeOutField)
                    .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                    .deleteField(deleteIndicatorField)
                    .build())
                .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build DeleteIndicatorMergeStrategy, [deleteValues] must contain at least one element", e.getMessage());
        }
    }

    @Test
    void testUnitemporalIncrementalMilestoningValidationBatchIdPrimaryKey()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName)
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(mainTableSchemaWithBatchIdInNotPrimary)
            .build();

        try
        {
            UnitemporalDelta ingestMode = UnitemporalDelta.builder()
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
    void testUnitemporalMilestoningWithBothDbAndSchemaNotSet()
    {
        mainTable = DatasetDefinition.builder()
            .name(mainTableName).alias(mainTableAlias)
            .schema(mainTableSchema)
            .build();

        stagingTable = DatasetDefinition.builder()
            .name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
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
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedCreateMainTableQuery = "CREATE TABLE IF NOT EXISTS main" +
            "(\"id\" INTEGER," +
            "\"name\" VARCHAR," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER," +
            "\"batch_id_out\" INTEGER," +
            "\"batch_time_in\" DATETIME," +
            "\"batch_time_out\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"batch_time_in\"))";

        String expectedMilestoneQuery = "UPDATE main as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1," +
            "sink.\"batch_time_out\" = '2000-01-01 00:00:00' " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM staging as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "(sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO main " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
            "FROM staging as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM main as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")))))";

        Assertions.assertEquals(expectedCreateMainTableQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testUnitemporalMilestoningWithDbAndSchemaBothSet()
    {
        mainTable = DatasetDefinition.builder()
            .database(mainDbName)
            .group("my_schema")
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(mainTableSchema)
            .build();

        stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .group("my_schema")
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
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
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedCreateMainTableQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"my_schema\".\"main\"" +
            "(\"id\" INTEGER," +
            "\"name\" VARCHAR," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER," +
            "\"batch_id_out\" INTEGER," +
            "\"batch_time_in\" DATETIME," +
            "\"batch_time_out\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"batch_time_in\"))";

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"my_schema\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1," +
            "sink.\"batch_time_out\" = '2000-01-01 00:00:00' " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"mydb\".\"my_schema\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "(sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"my_schema\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
            "FROM \"mydb\".\"my_schema\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"my_schema\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")))))";

        Assertions.assertEquals(expectedCreateMainTableQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testUnitemporalMilestoningWithOnlySchemaSet()
    {
        mainTable = DatasetDefinition.builder()
            .group("my_schema")
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(mainTableSchema)
            .build();

        stagingTable = DatasetDefinition.builder()
            .group("my_schema")
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
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
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedCreateMainTableQuery = "CREATE TABLE IF NOT EXISTS \"my_schema\".\"main\"" +
            "(\"id\" INTEGER," +
            "\"name\" VARCHAR," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER," +
            "\"batch_id_out\" INTEGER," +
            "\"batch_time_in\" DATETIME," +
            "\"batch_time_out\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"batch_time_in\"))";

        String expectedMilestoneQuery = "UPDATE \"my_schema\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1," +
            "sink.\"batch_time_out\" = '2000-01-01 00:00:00' " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"my_schema\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "(sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"my_schema\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
            "FROM \"my_schema\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"my_schema\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")))))";

        Assertions.assertEquals(expectedCreateMainTableQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    public void testPostRunStatisticsAndPostActionSqlWithCleanStagingDataWithoutStatCollection()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
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
            .relationalSink(AnsiSqlSink.get())
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        Assertions.assertEquals(0, new ArrayList<>(operations.postIngestStatisticsSql().values()).size());

        List<String> postActionsSql = operations.postActionsSql();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(expectedStagingCleanupQuery);

        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);
    }

    @Test
    public void testPostRunStatisticsAndPostActionSqlWithoutCleanStagingDataWithStatCollection()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
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
            .relationalSink(AnsiSqlSink.get())
            .cleanupStagingData(false)
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM \"mydb\".\"staging\" as stage";
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1";
        String rowsDeleted = "SELECT 0 as rowsDeleted";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) as rowsInserted";
        String rowsTerminated = "SELECT 0 as rowsTerminated";

        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(0, operations.postActionsSql().size());
    }

    @Test
    void testUnitemporalIncrementalMilestoningMetadataOperationsDisabled()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
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
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult queries = generator.generateOperations(datasets);
        List<String> preActionsSql = queries.preActionsSql();
        List<String> milestoningSql = queries.ingestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1," +
            "sink.\"batch_time_out\" = '2000-01-01 00:00:00' " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "(sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")))))";

        Assertions.assertEquals(2, queries.ingestSql().size());
        Assertions.assertEquals(2, queries.preActionsSql().size());
        Assertions.assertEquals(1, queries.postActionsSql().size());
        Assertions.assertEquals(expectedMilestoneQuery, queries.ingestSql().get(0));
        Assertions.assertEquals(expectedUpsertQuery, queries.ingestSql().get(1));
    }

    @Test
    void testUnitemporalMilestoningWithoutDeleteIndicatorWithPlaceholders()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
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
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .batchIdPattern("{BATCH_ID_PATTERN}")
            .batchStartTimestampPattern("{BATCH_START_TS_PATTERN}")
            .batchEndTimestampPattern("{BATCH_END_TS_PATTERN}")
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = {BATCH_ID_PATTERN}-1," +
            "sink.\"batch_time_out\" = '{BATCH_START_TS_PATTERN}' " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "(sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "{BATCH_ID_PATTERN},999999999,'{BATCH_START_TS_PATTERN}','9999-12-31 23:59:59' " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")))))";

        Assertions.assertEquals(expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQueryWithPlaceHolders, metadataIngestSql.get(0));
    }

}