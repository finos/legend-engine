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

package org.finos.legend.engine.persistence.components.ingestmode.bitemporal;

import org.finos.legend.engine.persistence.components.IngestModeTest;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BitemporalDeltaWithBatchIdTest extends IngestModeTest
{
    private static DatasetDefinition mainTable;
    private static DatasetDefinition stagingTable;

    @BeforeEach
    void initializeTables()
    {
        mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(bitemporalMainTableSchema)
            .build();

        stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(bitemporalStagingTableSchema)
            .build();
    }

    @Test
    void testMilestoningSourceSpecifiesFromAndThrough()
    {
        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(validityFromTargetField)
                .dateTimeThruName(validityThroughTargetField)
                .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(validityFromReferenceField)
                    .sourceDateTimeThruField(validityThroughReferenceField)
                    .build())
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
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") AND (sink.\"validity_from_target\" = stage.\"validity_from_reference\")) " +
            "AND (sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"validity_from_target\", \"validity_through_target\", \"digest\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"validity_through_reference\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999 " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") AND (sink.\"validity_from_target\" = stage.\"validity_from_reference\")))))";

        Assertions.assertEquals(expectedBitemporalMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testMilestoningSourceSpecifiesFromAndThroughWithDeleteIndicator()
    {
        DatasetDefinition stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(bitemporalStagingTableSchemaWithDeleteIndicator)
            .build();

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(validityFromTargetField)
                .dateTimeThruName(validityThroughTargetField)
                .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(validityFromReferenceField)
                    .sourceDateTimeThruField(validityThroughReferenceField)
                    .build())
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

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") AND (sink.\"validity_from_target\" = stage.\"validity_from_reference\")) " +
            "AND ((sink.\"digest\" <> stage.\"digest\") OR (stage.\"delete_indicator\" IN ('yes','1','true')))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"validity_from_target\", \"validity_through_target\", \"digest\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"validity_through_reference\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')," +
            "999999999 " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE (NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") AND (sink.\"validity_from_target\" = stage.\"validity_from_reference\"))))) " +
            "AND (stage.\"delete_indicator\" NOT IN ('yes','1','true')))";

        Assertions.assertEquals(expectedBitemporalMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testMilestoningSourceSpecifiesFromAndThroughWithUpperCase()
    {
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(validityFromTargetField)
                .dateTimeThruName(validityThroughTargetField)
                .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(validityFromReferenceField)
                    .sourceDateTimeThruField(validityThroughReferenceField)
                    .build())
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

        String expectedMilestoneQuery = "UPDATE \"MYDB\".\"MAIN\" as sink " +
            "SET sink.\"BATCH_ID_OUT\" = (SELECT COALESCE(MAX(batch_metadata.\"TABLE_BATCH_ID\"),0)+1 " +
            "FROM BATCH_METADATA as batch_metadata WHERE batch_metadata.\"TABLE_NAME\" = 'main')-1 " +
            "WHERE (sink.\"BATCH_ID_OUT\" = 999999999) AND (EXISTS (SELECT * FROM \"MYDB\".\"STAGING\" as stage " +
            "WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) " +
            "AND (sink.\"VALIDITY_FROM_TARGET\" = stage.\"VALIDITY_FROM_REFERENCE\") " +
            "AND (sink.\"DIGEST\" <> stage.\"DIGEST\")))";

        String expectedUpsertQuery = "INSERT INTO \"MYDB\".\"MAIN\" " +
                "(\"ID\", \"NAME\", \"AMOUNT\", \"VALIDITY_FROM_TARGET\", \"VALIDITY_THROUGH_TARGET\", \"DIGEST\", \"BATCH_ID_IN\", " +
                "\"BATCH_ID_OUT\") " +
                "(SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"VALIDITY_FROM_REFERENCE\",stage.\"VALIDITY_THROUGH_REFERENCE\"," +
                "stage.\"DIGEST\",(SELECT COALESCE(MAX(batch_metadata.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as batch_metadata WHERE batch_metadata.\"TABLE_NAME\" = 'main')" +
                ",999999999 " +
                "FROM \"MYDB\".\"STAGING\" as stage WHERE NOT (EXISTS " +
                "(SELECT * FROM \"MYDB\".\"MAIN\" as sink " +
                "WHERE (sink.\"BATCH_ID_OUT\" = 999999999) " +
                "AND (sink.\"DIGEST\" = stage.\"DIGEST\") " +
                "AND ((sink.\"ID\" = stage.\"ID\") " +
                "AND (sink.\"NAME\" = stage.\"NAME\")) " +
                "AND (sink.\"VALIDITY_FROM_TARGET\" = stage.\"VALIDITY_FROM_REFERENCE\"))))";

        Assertions.assertEquals(expectedBitemporalMainTableCreateQueryUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQueryWithUpperCase, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQueryWithUpperCase, metadataIngestSql.get(0));
    }

    @Test
    void testMilestoningSourceSpeciesFrom()
    {
        DatasetDefinition mainTable = DatasetDefinition.builder()
            .database(mainDbName)
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(bitemporalFromOnlyMainTableSchema)
            .build();

        DatasetDefinition stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(bitemporalFromOnlyStagingTableSchema)
            .build();

        DatasetDefinition tempTable = DatasetDefinition.builder()
            .database(tempDbName)
            .name(tempTableName)
            .alias(tempTableAlias)
            .schema(bitemporalFromOnlyTempTableSchema)
            .build();

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(validityFromTargetField)
                .dateTimeThruName(validityThroughTargetField)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(validityFromReferenceField)
                    .build())
                .build())
            .build();

        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_reference\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\" FROM \"mydb\".\"staging\" as stage) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),MIN(x.\"end_date\")) as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),'9999-12-31 23:59:59') as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"start_date\" < y.\"start_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_reference\" = y.\"start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",x.\"end_date\" as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",MIN(y.\"start_date\") as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date,\"validity_through_target\" as end_date " +
            "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "WHERE NOT (EXISTS " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((x.\"id\" = stage.\"id\") AND (x.\"name\" = stage.\"name\")) AND (x.\"start_date\" = stage.\"validity_from_reference\")))) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_target\" = y.\"start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (EXISTS " +
            "(SELECT * FROM \"mydb\".\"temp\" as temp " +
            "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
            "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
            "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        Assertions.assertEquals(expectedBitemporalFromOnlyMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(expectedBitemporalFromOnlyTempTableCreateQuery, preActionsSql.get(2));

        Assertions.assertEquals(expectedStageToTemp, milestoningSql.get(0));
        Assertions.assertEquals(expectedMainToTemp, milestoningSql.get(1));
        Assertions.assertEquals(expectedUpdateMain, milestoningSql.get(2));
        Assertions.assertEquals(expectedTempToMain, milestoningSql.get(3));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), milestoningSql.get(4));

        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testMilestoningSourceSpeciesFromWithDeleteIndicator()
    {
        DatasetDefinition mainTable = DatasetDefinition.builder()
            .database(mainDbName)
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(bitemporalFromOnlyMainTableSchema)
            .build();

        DatasetDefinition stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(bitemporalFromOnlyStagingTableSchemaWithDeleteIndicator)
            .build();

        DatasetDefinition tempTable = DatasetDefinition.builder()
            .database(tempDbName)
            .name(tempTableName)
            .alias(tempTableAlias)
            .schema(bitemporalFromOnlyTempTableSchema)
            .build();

        DatasetDefinition tempTableWithDeleteIndicator = DatasetDefinition.builder()
            .database(tempWithDeleteIndicatorDbName)
            .name(tempWithDeleteIndicatorTableName)
            .alias(tempWithDeleteIndicatorTableAlias)
            .schema(bitemporalFromOnlyTempTableWithDeleteIndicatorSchema)
            .build();

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(validityFromTargetField)
                .dateTimeThruName(validityThroughTargetField)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(validityFromReferenceField)
                    .build())
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorField)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .build();

        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).tempDatasetWithDeleteIndicator(tempTableWithDeleteIndicator).build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_reference\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"delete_indicator\" FROM \"mydb\".\"staging\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),MIN(x.\"end_date\")) as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),'9999-12-31 23:59:59') as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"start_date\" < y.\"start_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_reference\" = y.\"start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",x.\"end_date\" as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",MIN(y.\"start_date\") as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date,\"validity_through_target\" as end_date " +
            "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "WHERE NOT (EXISTS " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage " +
            "WHERE (((x.\"id\" = stage.\"id\") AND (x.\"name\" = stage.\"name\")) AND (x.\"start_date\" = stage.\"validity_from_reference\")) AND (stage.\"delete_indicator\" NOT IN ('yes','1','true'))))) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_target\" = y.\"start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (EXISTS " +
            "(SELECT * FROM \"mydb\".\"temp\" as temp " +
            "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
            "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
            "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        String expectedMainToTempForDeletion = "INSERT INTO \"mydb\".\"tempWithDeleteIndicator\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\", \"delete_indicator\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,x.\"validity_through_target\" as end_date,(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999,(CASE WHEN y.\"delete_indicator\" IS NULL THEN 0 ELSE 1 END) " +
            "FROM " +
            "(SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (EXISTS " +
            "(SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "((sink.\"validity_from_target\" = stage.\"validity_from_reference\") OR (sink.\"validity_through_target\" = stage.\"validity_from_reference\")) " +
            "AND (stage.\"delete_indicator\" IN ('yes','1','true'))))) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT * FROM \"mydb\".\"staging\" as stage) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_target\" = y.\"validity_from_reference\"))";

        String expectedUpdateMainForDeletion = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (EXISTS " +
            "(SELECT * FROM \"mydb\".\"tempWithDeleteIndicator\" as tempWithDeleteIndicator " +
            "WHERE ((sink.\"id\" = tempWithDeleteIndicator.\"id\") AND (sink.\"name\" = tempWithDeleteIndicator.\"name\")) AND (sink.\"validity_from_target\" = tempWithDeleteIndicator.\"validity_from_target\"))) " +
            "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMainForDeletion = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"start_date\" as start_date,MAX(y.\"validity_through_target\") as end_date,x.\"batch_id_in\",x.\"batch_id_out\" FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,COALESCE(MIN(y.\"validity_from_target\"),'9999-12-31 23:59:59') as end_date,x.\"batch_id_in\",x.\"batch_id_out\" " +
            "FROM \"mydb\".\"tempWithDeleteIndicator\" as x " +
            "LEFT OUTER JOIN \"mydb\".\"tempWithDeleteIndicator\" as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"validity_from_target\" > x.\"validity_from_target\") AND (y.\"delete_indicator\" = 0) " +
            "WHERE x.\"delete_indicator\" = 0 " +
            "GROUP BY x.\"id\", x.\"name\", x.\"amount\", x.\"digest\", x.\"validity_from_target\", x.\"batch_id_in\", x.\"batch_id_out\") as x " +
            "LEFT OUTER JOIN \"mydb\".\"tempWithDeleteIndicator\" as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"validity_through_target\" > x.\"start_date\") AND (y.\"validity_through_target\" <= x.\"end_date\") AND (y.\"delete_indicator\" <> 0) " +
            "GROUP BY x.\"id\", x.\"name\", x.\"amount\", x.\"digest\", x.\"start_date\", x.\"batch_id_in\", x.\"batch_id_out\")";

        Assertions.assertEquals(expectedBitemporalFromOnlyMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(expectedBitemporalFromOnlyTempTableCreateQuery, preActionsSql.get(2));
        Assertions.assertEquals(expectedBitemporalFromOnlyTempTableWithDeleteIndicatorCreateQuery, preActionsSql.get(3));

        Assertions.assertEquals(expectedStageToTemp, milestoningSql.get(0));
        Assertions.assertEquals(expectedMainToTemp, milestoningSql.get(1));
        Assertions.assertEquals(expectedUpdateMain, milestoningSql.get(2));
        Assertions.assertEquals(expectedTempToMain, milestoningSql.get(3));
        Assertions.assertEquals(expectedMainToTempForDeletion, milestoningSql.get(4));
        Assertions.assertEquals(expectedUpdateMainForDeletion, milestoningSql.get(5));
        Assertions.assertEquals(expectedTempToMainForDeletion, milestoningSql.get(6));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), milestoningSql.get(7));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"tempWithDeleteIndicator\"", "tempWithDeleteIndicator"), milestoningSql.get(8));

        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testMilestoningSourceSpeciesFromWithDataSplit()
    {
        DatasetDefinition mainTable = DatasetDefinition.builder()
            .database(mainDbName)
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(bitemporalFromOnlyMainTableSchema)
            .build();

        DatasetDefinition stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(bitemporalFromOnlyStagingTableSchemaWithDataSplit)
            .build();

        DatasetDefinition tempTable = DatasetDefinition.builder()
            .database(tempDbName)
            .name(tempTableName)
            .alias(tempTableAlias)
            .schema(bitemporalFromOnlyTempTableSchema)
            .build();

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestField)
            .dataSplitField(Optional.of(dataSplitField))
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(validityFromTargetField)
                .dateTimeThruName(validityThroughTargetField)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(validityFromReferenceField)
                    .build())
                .build())
            .build();

        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(datasets, dataSplitRanges);

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_reference\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"data_split\" FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),MIN(x.\"end_date\")) as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),'9999-12-31 23:59:59') as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"start_date\" < y.\"start_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_reference\" = y.\"start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",x.\"end_date\" as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",MIN(y.\"start_date\") as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date,\"validity_through_target\" as end_date " +
            "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "WHERE NOT (EXISTS " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage " +
            "WHERE (((x.\"id\" = stage.\"id\") AND (x.\"name\" = stage.\"name\")) AND (x.\"start_date\" = stage.\"validity_from_reference\")) AND ((stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s))))) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_target\" = y.\"start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (EXISTS " +
            "(SELECT * FROM \"mydb\".\"temp\" as temp " +
            "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
            "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
            "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        Assertions.assertEquals(expectedBitemporalFromOnlyMainTableCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));
        Assertions.assertEquals(expectedBitemporalFromOnlyTempTableCreateQuery, operations.get(0).preActionsSql().get(2));

        Assertions.assertEquals(String.format(expectedStageToTemp, 2, 5, 2, 5, 2, 5), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(String.format(expectedMainToTemp, 2, 5, 2, 5), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(expectedUpdateMain, operations.get(0).ingestSql().get(2));
        Assertions.assertEquals(expectedTempToMain, operations.get(0).ingestSql().get(3));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), operations.get(0).ingestSql().get(4));

        Assertions.assertEquals(String.format(expectedStageToTemp, 6, 7, 6, 7, 6, 7), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(String.format(expectedMainToTemp, 6, 7, 6, 7), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(expectedUpdateMain, operations.get(1).ingestSql().get(2));
        Assertions.assertEquals(expectedTempToMain, operations.get(1).ingestSql().get(3));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), operations.get(1).ingestSql().get(4));

        Assertions.assertEquals(expectedMetadataTableIngestQuery, operations.get(0).metadataIngestSql().get(0));

        Assertions.assertEquals(4, operations.size());
    }

    @Test
    void testMilestoningSourceSpeciesFromWithDeleteIndicatorWithDataSplit()
    {
        DatasetDefinition mainTable = DatasetDefinition.builder()
            .database(mainDbName)
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(bitemporalFromOnlyMainTableSchema)
            .build();

        DatasetDefinition stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(bitemporalFromOnlyStagingTableSchemaWithDeleteIndicatorWithDataSplit)
            .build();

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestField)
            .dataSplitField(Optional.of(dataSplitField))
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(validityFromTargetField)
                .dateTimeThruName(validityThroughTargetField)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(validityFromReferenceField)
                    .build())
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorField)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .build();

        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(datasets, dataSplitRanges);

        String tempName = operations.get(0).preActionsSql().get(2).split("CREATE TABLE IF NOT EXISTS ")[1].split("\\(")[0];
        String tempWithDeleteIndicatorName = operations.get(0).preActionsSql().get(3).split("CREATE TABLE IF NOT EXISTS ")[1].split("\\(")[0];

        String expectedBitemporalFromOnlyDefaultTempTableCreateQuery = "CREATE TABLE IF NOT EXISTS " + tempName +
            "(\"id\" INTEGER," +
            "\"name\" VARCHAR," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER," +
            "\"batch_id_out\" INTEGER," +
            "\"validity_from_target\" DATETIME," +
            "\"validity_through_target\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

        String expectedBitemporalFromOnlyDefaultTempTableWithDeleteIndicatorCreateQuery = "CREATE TABLE IF NOT EXISTS " + tempWithDeleteIndicatorName +
            "(\"id\" INTEGER," +
            "\"name\" VARCHAR," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER," +
            "\"batch_id_out\" INTEGER," +
            "\"validity_from_target\" DATETIME," +
            "\"validity_through_target\" DATETIME," +
            "\"delete_indicator\" BOOLEAN," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

        String expectedStageToTemp = "INSERT INTO " + tempName + " " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_reference\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"delete_indicator\",stage.\"data_split\" FROM \"mydb\".\"staging\" as stage WHERE (stage.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s))) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),MIN(x.\"end_date\")) as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),'9999-12-31 23:59:59') as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage WHERE (stage.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s))) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"start_date\" < y.\"start_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage WHERE (stage.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s))) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_reference\" = y.\"start_date\"))";

        String expectedMainToTemp = "INSERT INTO " + tempName + " " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",x.\"end_date\" as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",MIN(y.\"start_date\") as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date,\"validity_through_target\" as end_date " +
            "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage WHERE (stage.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s))) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "WHERE NOT (EXISTS " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((((x.\"id\" = stage.\"id\") AND (x.\"name\" = stage.\"name\")) AND (x.\"start_date\" = stage.\"validity_from_reference\")) AND (stage.\"delete_indicator\" NOT IN ('yes','1','true'))) AND ((stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s))))) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_target\" = y.\"start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (EXISTS " +
            "(SELECT * FROM " + tempName + " as temp " +
            "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
            "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
            "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM " + tempName + " as temp)";

        String expectedMainToTempForDeletion = "INSERT INTO " + tempWithDeleteIndicatorName + " " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\", \"delete_indicator\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,x.\"validity_through_target\" as end_date,(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999,(CASE WHEN y.\"delete_indicator\" IS NULL THEN 0 ELSE 1 END) " +
            "FROM " +
            "(SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (EXISTS " +
            "(SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "((sink.\"validity_from_target\" = stage.\"validity_from_reference\") OR (sink.\"validity_through_target\" = stage.\"validity_from_reference\")) " +
            "AND (stage.\"delete_indicator\" IN ('yes','1','true'))) AND ((stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s))))) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT * FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_target\" = y.\"validity_from_reference\"))";

        String expectedUpdateMainForDeletion = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (EXISTS " +
            "(SELECT * FROM " + tempWithDeleteIndicatorName + " as tempWithDeleteIndicator " +
            "WHERE ((sink.\"id\" = tempWithDeleteIndicator.\"id\") AND (sink.\"name\" = tempWithDeleteIndicator.\"name\")) AND (sink.\"validity_from_target\" = tempWithDeleteIndicator.\"validity_from_target\"))) " +
            "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMainForDeletion = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"start_date\" as start_date,MAX(y.\"validity_through_target\") as end_date,x.\"batch_id_in\",x.\"batch_id_out\" FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,COALESCE(MIN(y.\"validity_from_target\"),'9999-12-31 23:59:59') as end_date,x.\"batch_id_in\",x.\"batch_id_out\" " +
            "FROM " + tempWithDeleteIndicatorName + " as x " +
            "LEFT OUTER JOIN " + tempWithDeleteIndicatorName + " as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"validity_from_target\" > x.\"validity_from_target\") AND (y.\"delete_indicator\" = 0) " +
            "WHERE x.\"delete_indicator\" = 0 " +
            "GROUP BY x.\"id\", x.\"name\", x.\"amount\", x.\"digest\", x.\"validity_from_target\", x.\"batch_id_in\", x.\"batch_id_out\") as x " +
            "LEFT OUTER JOIN " + tempWithDeleteIndicatorName + " as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"validity_through_target\" > x.\"start_date\") AND (y.\"validity_through_target\" <= x.\"end_date\") AND (y.\"delete_indicator\" <> 0) " +
            "GROUP BY x.\"id\", x.\"name\", x.\"amount\", x.\"digest\", x.\"start_date\", x.\"batch_id_in\", x.\"batch_id_out\")";

        Assertions.assertEquals(expectedBitemporalFromOnlyMainTableCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));
        Assertions.assertEquals(expectedBitemporalFromOnlyDefaultTempTableCreateQuery, operations.get(0).preActionsSql().get(2));
        Assertions.assertEquals(expectedBitemporalFromOnlyDefaultTempTableWithDeleteIndicatorCreateQuery, operations.get(0).preActionsSql().get(3));

        Assertions.assertEquals(String.format(expectedStageToTemp, 2, 5, 2, 5, 2, 5), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(String.format(expectedMainToTemp, 2, 5, 2, 5), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(expectedUpdateMain, operations.get(0).ingestSql().get(2));
        Assertions.assertEquals(expectedTempToMain, operations.get(0).ingestSql().get(3));
        Assertions.assertEquals(String.format(expectedMainToTempForDeletion, 2, 5, 2, 5), operations.get(0).ingestSql().get(4));
        Assertions.assertEquals(expectedUpdateMainForDeletion, operations.get(0).ingestSql().get(5));
        Assertions.assertEquals(expectedTempToMainForDeletion, operations.get(0).ingestSql().get(6));
        Assertions.assertEquals(getExpectedCleanupSql(tempName, "temp"), operations.get(0).ingestSql().get(7));
        Assertions.assertEquals(getExpectedCleanupSql(tempWithDeleteIndicatorName, "tempWithDeleteIndicator"), operations.get(0).ingestSql().get(8));

        Assertions.assertEquals(String.format(expectedStageToTemp, 6, 7, 6, 7, 6, 7), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(String.format(expectedMainToTemp, 6, 7, 6, 7), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(expectedUpdateMain, operations.get(1).ingestSql().get(2));
        Assertions.assertEquals(expectedTempToMain, operations.get(1).ingestSql().get(3));
        Assertions.assertEquals(String.format(expectedMainToTempForDeletion, 6, 7, 6, 7), operations.get(1).ingestSql().get(4));
        Assertions.assertEquals(expectedUpdateMainForDeletion, operations.get(1).ingestSql().get(5));
        Assertions.assertEquals(expectedTempToMainForDeletion, operations.get(1).ingestSql().get(6));
        Assertions.assertEquals(getExpectedCleanupSql(tempName, "temp"), operations.get(1).ingestSql().get(7));
        Assertions.assertEquals(getExpectedCleanupSql(tempWithDeleteIndicatorName, "tempWithDeleteIndicator"), operations.get(1).ingestSql().get(8));

        Assertions.assertEquals(expectedMetadataTableIngestQuery, operations.get(0).metadataIngestSql().get(0));

        Assertions.assertEquals(4, operations.size());
    }

    @Test
    void testMilestoningSourceSpeciesFromFilterDuplicates()
    {
        DatasetDefinition mainTable = DatasetDefinition.builder()
            .database(mainDbName)
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(bitemporalFromOnlyMainTableSchema)
            .build();

        DatasetDefinition stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(bitemporalFromOnlyStagingTableSchema)
            .build();

        DatasetDefinition stagingTableWithoutDuplicates = DatasetDefinition.builder()
            .database(stagingWithoutDuplicatesDbName)
            .name(stagingTableWithoutDuplicatesName)
            .alias(stagingTableWithoutDuplicatesAlias)
            .schema(bitemporalFromOnlyStagingTableSchema)
            .build();

        DatasetDefinition tempTable = DatasetDefinition.builder()
            .database(tempDbName)
            .name(tempTableName)
            .alias(tempTableAlias)
            .schema(bitemporalFromOnlyTempTableSchema)
            .build();

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(validityFromTargetField)
                .dateTimeThruName(validityThroughTargetField)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(validityFromReferenceField)
                    .build())
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).stagingDatasetWithoutDuplicates(stagingTableWithoutDuplicates).build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedStageToStageWithoutDuplicates = "INSERT INTO \"mydb\".\"stagingWithoutDuplicates\" " +
            "(\"id\", \"name\", \"amount\", \"validity_from_reference\", \"digest\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\" FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"digest\" = stage.\"digest\") AND (sink.\"batch_id_out\" = 999999999))))";

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_reference\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\" FROM \"mydb\".\"stagingWithoutDuplicates\" as stage) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),MIN(x.\"end_date\")) as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),'9999-12-31 23:59:59') as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"start_date\" < y.\"start_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_reference\" = y.\"start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",x.\"end_date\" as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",MIN(y.\"start_date\") as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date,\"validity_through_target\" as end_date " +
            "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "WHERE NOT (EXISTS " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage " +
            "WHERE ((x.\"id\" = stage.\"id\") AND (x.\"name\" = stage.\"name\")) AND (x.\"start_date\" = stage.\"validity_from_reference\")))) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_target\" = y.\"start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (EXISTS " +
            "(SELECT * FROM \"mydb\".\"temp\" as temp " +
            "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
            "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
            "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        Assertions.assertEquals(expectedBitemporalFromOnlyMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(expectedBitemporalFromOnlyTempTableCreateQuery, preActionsSql.get(2));
        Assertions.assertEquals(expectedBitemporalFromOnlyStageWithoutDuplicatesTableCreateQuery, preActionsSql.get(3));

        Assertions.assertEquals(expectedStageToStageWithoutDuplicates, milestoningSql.get(0));
        Assertions.assertEquals(expectedStageToTemp, milestoningSql.get(1));
        Assertions.assertEquals(expectedMainToTemp, milestoningSql.get(2));
        Assertions.assertEquals(expectedUpdateMain, milestoningSql.get(3));
        Assertions.assertEquals(expectedTempToMain, milestoningSql.get(4));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), milestoningSql.get(5));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"stagingWithoutDuplicates\"", "stage"), milestoningSql.get(6));

        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testMilestoningSourceSpeciesFromWithDeleteIndicatorFilterDuplicates()
    {
        DatasetDefinition mainTable = DatasetDefinition.builder()
            .database(mainDbName)
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(bitemporalFromOnlyMainTableSchema)
            .build();

        DatasetDefinition stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(bitemporalFromOnlyStagingTableSchemaWithDeleteIndicator)
            .build();

        DatasetDefinition stagingTableWithoutDuplicates = DatasetDefinition.builder()
            .database(stagingWithoutDuplicatesDbName)
            .name(stagingTableWithoutDuplicatesName)
            .alias(stagingTableWithoutDuplicatesAlias)
            .schema(bitemporalFromOnlyStagingTableSchemaWithDeleteIndicator)
            .build();

        DatasetDefinition tempTable = DatasetDefinition.builder()
            .database(tempDbName)
            .name(tempTableName)
            .alias(tempTableAlias)
            .schema(bitemporalFromOnlyTempTableSchema)
            .build();

        DatasetDefinition tempTableWithDeleteIndicator = DatasetDefinition.builder()
            .database(tempWithDeleteIndicatorDbName)
            .name(tempWithDeleteIndicatorTableName)
            .alias(tempWithDeleteIndicatorTableAlias)
            .schema(bitemporalFromOnlyTempTableWithDeleteIndicatorSchema)
            .build();

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(validityFromTargetField)
                .dateTimeThruName(validityThroughTargetField)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(validityFromReferenceField)
                    .build())
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorField)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).tempDatasetWithDeleteIndicator(tempTableWithDeleteIndicator).stagingDatasetWithoutDuplicates(stagingTableWithoutDuplicates).build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedStageToStageWithoutDuplicates = "INSERT INTO \"mydb\".\"stagingWithoutDuplicates\" " +
            "(\"id\", \"name\", \"amount\", \"validity_from_reference\", \"digest\", \"delete_indicator\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"delete_indicator\" FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"digest\" = stage.\"digest\") AND (sink.\"batch_id_out\" = 999999999))))";

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_reference\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"delete_indicator\" FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),MIN(x.\"end_date\")) as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),'9999-12-31 23:59:59') as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"start_date\" < y.\"start_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_reference\" = y.\"start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",x.\"end_date\" as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",MIN(y.\"start_date\") as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date,\"validity_through_target\" as end_date " +
            "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "WHERE NOT (EXISTS " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage " +
            "WHERE (((x.\"id\" = stage.\"id\") AND (x.\"name\" = stage.\"name\")) AND (x.\"start_date\" = stage.\"validity_from_reference\")) AND (stage.\"delete_indicator\" NOT IN ('yes','1','true'))))) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_target\" = y.\"start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (EXISTS " +
            "(SELECT * FROM \"mydb\".\"temp\" as temp " +
            "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
            "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
            "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        String expectedMainToTempForDeletion = "INSERT INTO \"mydb\".\"tempWithDeleteIndicator\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\", \"delete_indicator\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,x.\"validity_through_target\" as end_date,(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999,(CASE WHEN y.\"delete_indicator\" IS NULL THEN 0 ELSE 1 END) " +
            "FROM " +
            "(SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (EXISTS " +
            "(SELECT * FROM \"mydb\".\"stagingWithoutDuplicates\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "((sink.\"validity_from_target\" = stage.\"validity_from_reference\") OR (sink.\"validity_through_target\" = stage.\"validity_from_reference\")) " +
            "AND (stage.\"delete_indicator\" IN ('yes','1','true'))))) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT * FROM \"mydb\".\"stagingWithoutDuplicates\" as stage) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_target\" = y.\"validity_from_reference\"))";

        String expectedUpdateMainForDeletion = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (EXISTS " +
            "(SELECT * FROM \"mydb\".\"tempWithDeleteIndicator\" as tempWithDeleteIndicator " +
            "WHERE ((sink.\"id\" = tempWithDeleteIndicator.\"id\") AND (sink.\"name\" = tempWithDeleteIndicator.\"name\")) AND (sink.\"validity_from_target\" = tempWithDeleteIndicator.\"validity_from_target\"))) " +
            "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMainForDeletion = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"start_date\" as start_date,MAX(y.\"validity_through_target\") as end_date,x.\"batch_id_in\",x.\"batch_id_out\" FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,COALESCE(MIN(y.\"validity_from_target\"),'9999-12-31 23:59:59') as end_date,x.\"batch_id_in\",x.\"batch_id_out\" " +
            "FROM \"mydb\".\"tempWithDeleteIndicator\" as x " +
            "LEFT OUTER JOIN \"mydb\".\"tempWithDeleteIndicator\" as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"validity_from_target\" > x.\"validity_from_target\") AND (y.\"delete_indicator\" = 0) " +
            "WHERE x.\"delete_indicator\" = 0 " +
            "GROUP BY x.\"id\", x.\"name\", x.\"amount\", x.\"digest\", x.\"validity_from_target\", x.\"batch_id_in\", x.\"batch_id_out\") as x " +
            "LEFT OUTER JOIN \"mydb\".\"tempWithDeleteIndicator\" as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"validity_through_target\" > x.\"start_date\") AND (y.\"validity_through_target\" <= x.\"end_date\") AND (y.\"delete_indicator\" <> 0) " +
            "GROUP BY x.\"id\", x.\"name\", x.\"amount\", x.\"digest\", x.\"start_date\", x.\"batch_id_in\", x.\"batch_id_out\")";

        Assertions.assertEquals(expectedBitemporalFromOnlyMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(expectedBitemporalFromOnlyTempTableCreateQuery, preActionsSql.get(2));
        Assertions.assertEquals(expectedBitemporalFromOnlyTempTableWithDeleteIndicatorCreateQuery, preActionsSql.get(3));
        Assertions.assertEquals(expectedBitemporalFromOnlyStageWithDeleteIndicatorWithoutDuplicatesTableCreateQuery, preActionsSql.get(4));

        Assertions.assertEquals(expectedStageToStageWithoutDuplicates, milestoningSql.get(0));
        Assertions.assertEquals(expectedStageToTemp, milestoningSql.get(1));
        Assertions.assertEquals(expectedMainToTemp, milestoningSql.get(2));
        Assertions.assertEquals(expectedUpdateMain, milestoningSql.get(3));
        Assertions.assertEquals(expectedTempToMain, milestoningSql.get(4));
        Assertions.assertEquals(expectedMainToTempForDeletion, milestoningSql.get(5));
        Assertions.assertEquals(expectedUpdateMainForDeletion, milestoningSql.get(6));
        Assertions.assertEquals(expectedTempToMainForDeletion, milestoningSql.get(7));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), milestoningSql.get(8));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"tempWithDeleteIndicator\"", "tempWithDeleteIndicator"), milestoningSql.get(9));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"stagingWithoutDuplicates\"", "stage"), milestoningSql.get(10));

        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testMilestoningSourceSpeciesFromWithDataSplitFilterDuplicates()
    {
        DatasetDefinition mainTable = DatasetDefinition.builder()
            .database(mainDbName)
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(bitemporalFromOnlyMainTableSchema)
            .build();

        DatasetDefinition stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(bitemporalFromOnlyStagingTableSchemaWithDataSplit)
            .build();

        DatasetDefinition stagingTableWithoutDuplicates = DatasetDefinition.builder()
            .database(stagingWithoutDuplicatesDbName)
            .name(stagingTableWithoutDuplicatesName)
            .alias(stagingTableWithoutDuplicatesAlias)
            .schema(bitemporalFromOnlyStagingTableSchemaWithDataSplit)
            .build();

        DatasetDefinition tempTable = DatasetDefinition.builder()
            .database(tempDbName)
            .name(tempTableName)
            .alias(tempTableAlias)
            .schema(bitemporalFromOnlyTempTableSchema)
            .build();

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestField)
            .dataSplitField(Optional.of(dataSplitField))
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(validityFromTargetField)
                .dateTimeThruName(validityThroughTargetField)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(validityFromReferenceField)
                    .build())
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).stagingDatasetWithoutDuplicates(stagingTableWithoutDuplicates).build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(datasets, dataSplitRanges);

        String expectedStageToStageWithoutDuplicates = "INSERT INTO \"mydb\".\"stagingWithoutDuplicates\" " +
            "(\"id\", \"name\", \"amount\", \"validity_from_reference\", \"digest\", \"data_split\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"data_split\" FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"digest\" = stage.\"digest\") AND (sink.\"batch_id_out\" = 999999999))))";

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_reference\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"data_split\" FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE (stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),MIN(x.\"end_date\")) as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),'9999-12-31 23:59:59') as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE (stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"start_date\" < y.\"start_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE (stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_reference\" = y.\"start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",x.\"end_date\" as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",MIN(y.\"start_date\") as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date,\"validity_through_target\" as end_date " +
            "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE (stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s)) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "WHERE NOT (EXISTS " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage " +
            "WHERE (((x.\"id\" = stage.\"id\") AND (x.\"name\" = stage.\"name\")) AND (x.\"start_date\" = stage.\"validity_from_reference\")) AND ((stage.\"data_split\" >= %s) AND (stage.\"data_split\" <= %s))))) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_target\" = y.\"start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (EXISTS " +
            "(SELECT * FROM \"mydb\".\"temp\" as temp " +
            "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
            "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
            "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        Assertions.assertEquals(expectedBitemporalFromOnlyMainTableCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));
        Assertions.assertEquals(expectedBitemporalFromOnlyTempTableCreateQuery, operations.get(0).preActionsSql().get(2));
        Assertions.assertEquals(expectedBitemporalFromOnlyStageWithDataSplitWithoutDuplicatesTableCreateQuery, operations.get(0).preActionsSql().get(3));

        Assertions.assertEquals(expectedStageToStageWithoutDuplicates, operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(String.format(expectedStageToTemp, 2, 5, 2, 5, 2, 5), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(String.format(expectedMainToTemp, 2, 5, 2, 5), operations.get(0).ingestSql().get(2));
        Assertions.assertEquals(expectedUpdateMain, operations.get(0).ingestSql().get(3));
        Assertions.assertEquals(expectedTempToMain, operations.get(0).ingestSql().get(4));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), operations.get(0).ingestSql().get(5));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"stagingWithoutDuplicates\"", "stage"), operations.get(0).ingestSql().get(6));

        Assertions.assertEquals(expectedStageToStageWithoutDuplicates, operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(String.format(expectedStageToTemp, 6, 7, 6, 7, 6, 7), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(String.format(expectedMainToTemp, 6, 7, 6, 7), operations.get(1).ingestSql().get(2));
        Assertions.assertEquals(expectedUpdateMain, operations.get(1).ingestSql().get(3));
        Assertions.assertEquals(expectedTempToMain, operations.get(1).ingestSql().get(4));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), operations.get(1).ingestSql().get(5));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"stagingWithoutDuplicates\"", "stage"), operations.get(1).ingestSql().get(6));

        Assertions.assertEquals(expectedMetadataTableIngestQuery, operations.get(0).metadataIngestSql().get(0));

        Assertions.assertEquals(4, operations.size());
    }

    @Test
    void testMilestoningSourceSpeciesFromWithDeleteIndicatorWithDataSplitFilterDuplicates()
    {
        DatasetDefinition mainTable = DatasetDefinition.builder()
            .database(mainDbName)
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(bitemporalFromOnlyMainTableSchema)
            .build();

        DatasetDefinition stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(bitemporalFromOnlyStagingTableSchemaWithDeleteIndicatorWithDataSplit)
            .build();

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestField)
            .dataSplitField(Optional.of(dataSplitField))
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(validityFromTargetField)
                .dateTimeThruName(validityThroughTargetField)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(validityFromReferenceField)
                    .build())
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorField)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(datasets, dataSplitRanges);

        String tempName = operations.get(0).preActionsSql().get(2).split("CREATE TABLE IF NOT EXISTS ")[1].split("\\(")[0];
        String tempWithDeleteIndicatorName = operations.get(0).preActionsSql().get(3).split("CREATE TABLE IF NOT EXISTS ")[1].split("\\(")[0];
        String stageWithoutDuplicatesName = operations.get(0).preActionsSql().get(4).split("CREATE TABLE IF NOT EXISTS ")[1].split("\\(")[0];

        String expectedBitemporalFromOnlyDefaultTempTableCreateQuery = "CREATE TABLE IF NOT EXISTS " + tempName +
            "(\"id\" INTEGER," +
            "\"name\" VARCHAR," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER," +
            "\"batch_id_out\" INTEGER," +
            "\"validity_from_target\" DATETIME," +
            "\"validity_through_target\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

        String expectedBitemporalFromOnlyDefaultTempTableWithDeleteIndicatorCreateQuery = "CREATE TABLE IF NOT EXISTS " + tempWithDeleteIndicatorName +
            "(\"id\" INTEGER," +
            "\"name\" VARCHAR," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER," +
            "\"batch_id_out\" INTEGER," +
            "\"validity_from_target\" DATETIME," +
            "\"validity_through_target\" DATETIME," +
            "\"delete_indicator\" BOOLEAN," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

        String expectedBitemporalFromOnlyStageWithDeleteIndicatorWithDataSplitWithoutDuplicatesTableCreateQuery = "CREATE TABLE IF NOT EXISTS " + stageWithoutDuplicatesName +
            "(\"id\" INTEGER," +
            "\"name\" VARCHAR," +
            "\"amount\" DOUBLE," +
            "\"validity_from_reference\" DATETIME," +
            "\"digest\" VARCHAR," +
            "\"delete_indicator\" VARCHAR," +
            "\"data_split\" BIGINT," +
            "PRIMARY KEY (\"id\", \"name\", \"validity_from_reference\", \"data_split\"))";

        String expectedStageToStageWithoutDuplicates = "INSERT INTO " + stageWithoutDuplicatesName + " " +
            "(\"id\", \"name\", \"amount\", \"validity_from_reference\", \"digest\", \"delete_indicator\", \"data_split\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"delete_indicator\",stage.\"data_split\" FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"digest\" = stage.\"digest\") AND (sink.\"batch_id_out\" = 999999999))))";

        String expectedStageToTemp = "INSERT INTO " + tempName + " " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_reference\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT stageWithoutDuplicates.\"id\",stageWithoutDuplicates.\"name\",stageWithoutDuplicates.\"amount\",stageWithoutDuplicates.\"validity_from_reference\",stageWithoutDuplicates.\"digest\",stageWithoutDuplicates.\"delete_indicator\",stageWithoutDuplicates.\"data_split\" FROM " + stageWithoutDuplicatesName + " as stageWithoutDuplicates WHERE (stageWithoutDuplicates.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((stageWithoutDuplicates.\"data_split\" >= %s) AND (stageWithoutDuplicates.\"data_split\" <= %s))) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),MIN(x.\"end_date\")) as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),'9999-12-31 23:59:59') as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM " + stageWithoutDuplicatesName + " as stageWithoutDuplicates WHERE (stageWithoutDuplicates.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((stageWithoutDuplicates.\"data_split\" >= %s) AND (stageWithoutDuplicates.\"data_split\" <= %s))) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"start_date\" < y.\"start_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM " + stageWithoutDuplicatesName + " as stageWithoutDuplicates WHERE (stageWithoutDuplicates.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((stageWithoutDuplicates.\"data_split\" >= %s) AND (stageWithoutDuplicates.\"data_split\" <= %s))) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_reference\" = y.\"start_date\"))";

        String expectedMainToTemp = "INSERT INTO " + tempName + " " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",x.\"end_date\" as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",MIN(y.\"start_date\") as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date,\"validity_through_target\" as end_date " +
            "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM " + stageWithoutDuplicatesName + " as stageWithoutDuplicates WHERE (stageWithoutDuplicates.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((stageWithoutDuplicates.\"data_split\" >= %s) AND (stageWithoutDuplicates.\"data_split\" <= %s))) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "WHERE NOT (EXISTS " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM " + stageWithoutDuplicatesName + " as stageWithoutDuplicates " +
            "WHERE ((((x.\"id\" = stageWithoutDuplicates.\"id\") AND (x.\"name\" = stageWithoutDuplicates.\"name\")) AND (x.\"start_date\" = stageWithoutDuplicates.\"validity_from_reference\")) AND (stageWithoutDuplicates.\"delete_indicator\" NOT IN ('yes','1','true'))) AND ((stageWithoutDuplicates.\"data_split\" >= %s) AND (stageWithoutDuplicates.\"data_split\" <= %s))))) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_target\" = y.\"start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (EXISTS " +
            "(SELECT * FROM " + tempName + " as temp " +
            "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
            "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
            "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM " + tempName + " as temp)";

        String expectedMainToTempForDeletion = "INSERT INTO " + tempWithDeleteIndicatorName + " " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\", \"delete_indicator\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,x.\"validity_through_target\" as end_date,(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999,(CASE WHEN y.\"delete_indicator\" IS NULL THEN 0 ELSE 1 END) " +
            "FROM " +
            "(SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (EXISTS " +
            "(SELECT * FROM " + stageWithoutDuplicatesName + " as stageWithoutDuplicates " +
            "WHERE (((sink.\"id\" = stageWithoutDuplicates.\"id\") AND (sink.\"name\" = stageWithoutDuplicates.\"name\")) AND " +
            "((sink.\"validity_from_target\" = stageWithoutDuplicates.\"validity_from_reference\") OR (sink.\"validity_through_target\" = stageWithoutDuplicates.\"validity_from_reference\")) " +
            "AND (stageWithoutDuplicates.\"delete_indicator\" IN ('yes','1','true'))) AND ((stageWithoutDuplicates.\"data_split\" >= %s) AND (stageWithoutDuplicates.\"data_split\" <= %s))))) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT * FROM " + stageWithoutDuplicatesName + " as stageWithoutDuplicates WHERE (stageWithoutDuplicates.\"data_split\" >= %s) AND (stageWithoutDuplicates.\"data_split\" <= %s)) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_target\" = y.\"validity_from_reference\"))";

        String expectedUpdateMainForDeletion = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (EXISTS " +
            "(SELECT * FROM " + tempWithDeleteIndicatorName + " as tempWithDeleteIndicator " +
            "WHERE ((sink.\"id\" = tempWithDeleteIndicator.\"id\") AND (sink.\"name\" = tempWithDeleteIndicator.\"name\")) AND (sink.\"validity_from_target\" = tempWithDeleteIndicator.\"validity_from_target\"))) " +
            "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMainForDeletion = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"start_date\" as start_date,MAX(y.\"validity_through_target\") as end_date,x.\"batch_id_in\",x.\"batch_id_out\" FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,COALESCE(MIN(y.\"validity_from_target\"),'9999-12-31 23:59:59') as end_date,x.\"batch_id_in\",x.\"batch_id_out\" " +
            "FROM " + tempWithDeleteIndicatorName + " as x " +
            "LEFT OUTER JOIN " + tempWithDeleteIndicatorName + " as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"validity_from_target\" > x.\"validity_from_target\") AND (y.\"delete_indicator\" = 0) " +
            "WHERE x.\"delete_indicator\" = 0 " +
            "GROUP BY x.\"id\", x.\"name\", x.\"amount\", x.\"digest\", x.\"validity_from_target\", x.\"batch_id_in\", x.\"batch_id_out\") as x " +
            "LEFT OUTER JOIN " + tempWithDeleteIndicatorName + " as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"validity_through_target\" > x.\"start_date\") AND (y.\"validity_through_target\" <= x.\"end_date\") AND (y.\"delete_indicator\" <> 0) " +
            "GROUP BY x.\"id\", x.\"name\", x.\"amount\", x.\"digest\", x.\"start_date\", x.\"batch_id_in\", x.\"batch_id_out\")";

        Assertions.assertEquals(expectedBitemporalFromOnlyMainTableCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));
        Assertions.assertEquals(expectedBitemporalFromOnlyDefaultTempTableCreateQuery, operations.get(0).preActionsSql().get(2));
        Assertions.assertEquals(expectedBitemporalFromOnlyDefaultTempTableWithDeleteIndicatorCreateQuery, operations.get(0).preActionsSql().get(3));
        Assertions.assertEquals(expectedBitemporalFromOnlyStageWithDeleteIndicatorWithDataSplitWithoutDuplicatesTableCreateQuery, operations.get(0).preActionsSql().get(4));

        Assertions.assertEquals(expectedStageToStageWithoutDuplicates, operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(String.format(expectedStageToTemp, 2, 5, 2, 5, 2, 5), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(String.format(expectedMainToTemp, 2, 5, 2, 5), operations.get(0).ingestSql().get(2));
        Assertions.assertEquals(expectedUpdateMain, operations.get(0).ingestSql().get(3));
        Assertions.assertEquals(expectedTempToMain, operations.get(0).ingestSql().get(4));
        Assertions.assertEquals(String.format(expectedMainToTempForDeletion, 2, 5, 2, 5), operations.get(0).ingestSql().get(5));
        Assertions.assertEquals(expectedUpdateMainForDeletion, operations.get(0).ingestSql().get(6));
        Assertions.assertEquals(expectedTempToMainForDeletion, operations.get(0).ingestSql().get(7));
        Assertions.assertEquals(getExpectedCleanupSql(tempName, "temp"), operations.get(0).ingestSql().get(8));
        Assertions.assertEquals(getExpectedCleanupSql(tempWithDeleteIndicatorName, "tempWithDeleteIndicator"), operations.get(0).ingestSql().get(9));
        Assertions.assertEquals(getExpectedCleanupSql(stageWithoutDuplicatesName, "stageWithoutDuplicates"), operations.get(0).ingestSql().get(10));

        Assertions.assertEquals(expectedStageToStageWithoutDuplicates, operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(String.format(expectedStageToTemp, 6, 7, 6, 7, 6, 7), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(String.format(expectedMainToTemp, 6, 7, 6, 7), operations.get(1).ingestSql().get(2));
        Assertions.assertEquals(expectedUpdateMain, operations.get(1).ingestSql().get(3));
        Assertions.assertEquals(expectedTempToMain, operations.get(1).ingestSql().get(4));
        Assertions.assertEquals(String.format(expectedMainToTempForDeletion, 6, 7, 6, 7), operations.get(1).ingestSql().get(5));
        Assertions.assertEquals(expectedUpdateMainForDeletion, operations.get(1).ingestSql().get(6));
        Assertions.assertEquals(expectedTempToMainForDeletion, operations.get(1).ingestSql().get(7));
        Assertions.assertEquals(getExpectedCleanupSql(tempName, "temp"), operations.get(1).ingestSql().get(8));
        Assertions.assertEquals(getExpectedCleanupSql(tempWithDeleteIndicatorName, "tempWithDeleteIndicator"), operations.get(1).ingestSql().get(9));
        Assertions.assertEquals(getExpectedCleanupSql(stageWithoutDuplicatesName, "stageWithoutDuplicates"), operations.get(1).ingestSql().get(10));

        Assertions.assertEquals(expectedMetadataTableIngestQuery, operations.get(0).metadataIngestSql().get(0));

        Assertions.assertEquals(4, operations.size());
    }

    @Test
    void testMilestoningSourceSpecifiesFromWithMetadataOperationsDisabled()
    {
        DatasetDefinition mainTable = DatasetDefinition.builder()
            .database(mainDbName)
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(bitemporalFromOnlyMainTableSchema)
            .build();

        DatasetDefinition stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(bitemporalFromOnlyStagingTableSchema)
            .build();

        DatasetDefinition tempTable = DatasetDefinition.builder()
            .database(tempDbName)
            .name(tempTableName)
            .alias(tempTableAlias)
            .schema(bitemporalFromOnlyTempTableSchema)
            .build();

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(validityFromTargetField)
                .dateTimeThruName(validityThroughTargetField)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(validityFromReferenceField)
                    .build())
                .build())
            .build();

        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_reference\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\" FROM \"mydb\".\"staging\" as stage) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),MIN(x.\"end_date\")) as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),'9999-12-31 23:59:59') as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"start_date\" < y.\"start_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_reference\" = y.\"start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,y.\"end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
            "FROM " +
            "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",x.\"end_date\" as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",MIN(y.\"start_date\") as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date,\"validity_through_target\" as end_date " +
            "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "WHERE NOT (EXISTS " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((x.\"id\" = stage.\"id\") AND (x.\"name\" = stage.\"name\")) AND (x.\"start_date\" = stage.\"validity_from_reference\")))) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_target\" = y.\"start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (EXISTS " +
            "(SELECT * FROM \"mydb\".\"temp\" as temp " +
            "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
            "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
            "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        Assertions.assertEquals(expectedBitemporalFromOnlyMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(expectedBitemporalFromOnlyTempTableCreateQuery, preActionsSql.get(2));

        Assertions.assertEquals(expectedStageToTemp, milestoningSql.get(0));
        Assertions.assertEquals(expectedMainToTemp, milestoningSql.get(1));
        Assertions.assertEquals(expectedUpdateMain, milestoningSql.get(2));
        Assertions.assertEquals(expectedTempToMain, milestoningSql.get(3));
    }

    @Test
    void testMilestoningSourceSpeciesFromWithPlaceHolders()
    {
        DatasetDefinition mainTable = DatasetDefinition.builder()
            .database(mainDbName)
            .name(mainTableName)
            .alias(mainTableAlias)
            .schema(bitemporalFromOnlyMainTableSchema)
            .build();

        DatasetDefinition stagingTable = DatasetDefinition.builder()
            .database(stagingDbName)
            .name(stagingTableName)
            .alias(stagingTableAlias)
            .schema(bitemporalFromOnlyStagingTableSchema)
            .build();

        DatasetDefinition tempTable = DatasetDefinition.builder()
            .database(tempDbName)
            .name(tempTableName)
            .alias(tempTableAlias)
            .schema(bitemporalFromOnlyTempTableSchema)
            .build();

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(validityFromTargetField)
                .dateTimeThruName(validityThroughTargetField)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(validityFromReferenceField)
                    .build())
                .build())
            .build();

        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).build();

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

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_reference\" as start_date,y.\"end_date\",{BATCH_ID_PATTERN},999999999 " +
            "FROM " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\" FROM \"mydb\".\"staging\" as stage) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),MIN(x.\"end_date\")) as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",COALESCE(MIN(y.\"start_date\"),'9999-12-31 23:59:59') as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage) as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"start_date\" < y.\"start_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "LEFT OUTER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_reference\" = y.\"start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
            "(SELECT x.\"id\",x.\"name\",x.\"amount\",x.\"digest\",x.\"validity_from_target\" as start_date,y.\"end_date\",{BATCH_ID_PATTERN},999999999 " +
            "FROM " +
            "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",x.\"end_date\" as end_date " +
            "FROM " +
            "(SELECT x.\"id\",x.\"name\",x.\"start_date\",MIN(y.\"start_date\") as end_date " +
            "FROM " +
            "(SELECT \"id\",\"name\",\"validity_from_target\" as start_date,\"validity_through_target\" as end_date " +
            "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as x " +
            "INNER JOIN " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (y.\"start_date\" > x.\"start_date\") AND (y.\"start_date\" < x.\"end_date\") " +
            "GROUP BY x.\"id\", x.\"name\", x.\"start_date\") as x " +
            "WHERE NOT (EXISTS " +
            "(SELECT \"id\",\"name\",\"validity_from_reference\" as start_date FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((x.\"id\" = stage.\"id\") AND (x.\"name\" = stage.\"name\")) AND (x.\"start_date\" = stage.\"validity_from_reference\")))) as y " +
            "ON ((x.\"id\" = y.\"id\") AND (x.\"name\" = y.\"name\")) AND (x.\"validity_from_target\" = y.\"start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = {BATCH_ID_PATTERN}-1 " +
            "WHERE (EXISTS " +
            "(SELECT * FROM \"mydb\".\"temp\" as temp " +
            "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
            "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
            "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        Assertions.assertEquals(expectedBitemporalFromOnlyMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(expectedBitemporalFromOnlyTempTableCreateQuery, preActionsSql.get(2));

        Assertions.assertEquals(expectedStageToTemp, milestoningSql.get(0));
        Assertions.assertEquals(expectedMainToTemp, milestoningSql.get(1));
        Assertions.assertEquals(expectedUpdateMain, milestoningSql.get(2));
        Assertions.assertEquals(expectedTempToMain, milestoningSql.get(3));
        Assertions.assertEquals(expectedMetadataTableIngestQueryWithPlaceHolders, metadataIngestSql.get(0));
    }

    // TODO: exception, post action, stats... (refer to unitemporal equivalent for what's missing)
}
