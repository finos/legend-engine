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
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshot;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

@Disabled
public class BitemporalSnapshotWithBatchIdTest extends IngestModeTest
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
    void testGeneratePhysicalPlanForEmptyBatch()
    {
        BitemporalSnapshot ingestMode = BitemporalSnapshot.builder()
            .digestField(digestField)
            .addAllKeyFields(bitemporalFromAndThroughPrimaryKeysList)
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
            .build();

        GeneratorResult operations = generator.generateOperationsForEmptyBatch(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE sink.\"batch_id_out\" = 999999999";

        Assertions.assertEquals(expectedBitemporalMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testGeneratePhysicalPlanWithoutPartition()
    {
        BitemporalSnapshot ingestMode = BitemporalSnapshot.builder()
            .digestField(digestField)
            .addAllKeyFields(bitemporalFromAndThroughPrimaryKeysList)
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
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (NOT (EXISTS " +
            "(SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") AND (sink.\"validity_from_reference\" = stage.\"validity_from_reference\") AND (sink.\"validity_through_reference\" = stage.\"validity_through_reference\")) AND (sink.\"digest\" = stage.\"digest\"))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"validity_from_reference\", \"validity_through_reference\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"validity_through_reference\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999,stage.\"validity_from_reference\",stage.\"validity_through_reference\" " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (stage.\"digest\" IN (SELECT sink.\"digest\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999)))";

        Assertions.assertEquals(expectedBitemporalMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testGeneratePhysicalPlanWithPartition()
    {
        BitemporalSnapshot ingestMode = BitemporalSnapshot.builder()
            .digestField(digestField)
            .addAllKeyFields(bitemporalFromAndThroughPrimaryKeysList)
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
            .addAllPartitionFields(Arrays.asList(partitionKeys))
            .putAllPartitionValuesByField(partitionFilter)
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (NOT (EXISTS " +
            "(SELECT * FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") AND (sink.\"validity_from_reference\" = stage.\"validity_from_reference\") AND (sink.\"validity_through_reference\" = stage.\"validity_through_reference\")) AND (sink.\"digest\" = stage.\"digest\")))) " +
            "AND (EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE (sink.\"validity_from_reference\" = stage.\"validity_from_reference\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"validity_from_reference\", \"validity_through_reference\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"validity_through_reference\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999,stage.\"validity_from_reference\",stage.\"validity_through_reference\" " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (stage.\"digest\" IN (SELECT sink.\"digest\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999)))";

        Assertions.assertEquals(expectedBitemporalMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testGeneratePhysicalPlanWithoutPartitionHasFromTimeOnly()
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

        BitemporalSnapshot ingestMode = BitemporalSnapshot.builder()
            .digestField(digestField)
            .addAllKeyFields(bitemporalFromOnlyPrimaryKeysList)
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

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (NOT (EXISTS " +
            "(SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") AND (sink.\"validity_from_reference\" = stage.\"validity_from_reference\")) AND (sink.\"digest\" = stage.\"digest\"))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"validity_from_reference\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999,stage.\"validity_from_reference\",'9999-12-31 23:59:59' " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (stage.\"digest\" IN (SELECT sink.\"digest\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999)))";

        Assertions.assertEquals(expectedBitemporalFromOnlyMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
    }

    @Test
    void testGeneratePhysicalPlanWithoutPartitionHasFromTimeOnlyWithUpperCase()
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

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        BitemporalSnapshot ingestMode = BitemporalSnapshot.builder()
            .digestField(digestField)
            .addAllKeyFields(bitemporalFromOnlyPrimaryKeysList)
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

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"MYDB\".\"MAIN\" as SINK " +
            "SET SINK.\"BATCH_ID_OUT\" = (SELECT MAX(\"TABLE_BATCH_ID\")-1 " +
            "FROM BATCH_METADATA as BATCH_METADATA WHERE BATCH_METADATA.\"TABLE_NAME\" = 'main') " +
            "WHERE (SINK.\"BATCH_ID_OUT\" = 999999999) " +
            "AND (NOT (EXISTS (SELECT * FROM \"MYDB\".\"STAGING\" as STAGE " +
            "WHERE ((SINK.\"ID\" = STAGE.\"ID\") AND (SINK.\"NAME\" = STAGE.\"NAME\") " +
            "AND (SINK.\"VALIDITY_FROM_REFERENCE\" = STAGE.\"VALIDITY_FROM_REFERENCE\")) " +
            "AND (SINK.\"DIGEST\" = STAGE.\"DIGEST\"))))";

        String expectedUpsertQuery = "INSERT INTO \"MYDB\".\"MAIN\" " +
            "(\"ID\", \"NAME\", \"AMOUNT\", \"VALIDITY_FROM_REFERENCE\", \"DIGEST\", " +
            "\"BATCH_ID_IN\", \"BATCH_ID_OUT\", \"VALIDITY_FROM_TARGET\", \"VALIDITY_THROUGH_TARGET\") " +
            "(SELECT STAGE.\"ID\",STAGE.\"NAME\",STAGE.\"AMOUNT\",STAGE.\"VALIDITY_FROM_REFERENCE\"," +
            "STAGE.\"DIGEST\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')" +
            ",999999999,STAGE.\"VALIDITY_FROM_REFERENCE\",'9999-12-31 23:59:59' FROM \"MYDB\".\"STAGING\" as STAGE " +
            "WHERE NOT (STAGE.\"DIGEST\" IN (SELECT SINK.\"DIGEST\" FROM \"MYDB\".\"MAIN\" as SINK WHERE SINK.\"BATCH_ID_OUT\" = 999999999)))";

        Assertions.assertEquals(expectedBitemporalFromOnlyMainTableCreateQueryUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQueryWithUpperCase, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQueryWithUpperCase, metadataIngestSql.get(0));
    }

    @Test
    void testBitemporalSnapshotMilestoningWithMetadataOperationsDisabled()
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

        BitemporalSnapshot ingestMode = BitemporalSnapshot.builder()
            .digestField(digestField)
            .addAllKeyFields(bitemporalFromOnlyPrimaryKeysList)
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

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (NOT (EXISTS " +
            "(SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") AND (sink.\"validity_from_reference\" = stage.\"validity_from_reference\")) AND (sink.\"digest\" = stage.\"digest\"))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"validity_from_reference\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999,stage.\"validity_from_reference\",'9999-12-31 23:59:59' " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (stage.\"digest\" IN (SELECT sink.\"digest\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999)))";

        Assertions.assertEquals(2, operations.ingestSql().size());
        Assertions.assertEquals(2, operations.preActionsSql().size());
        Assertions.assertEquals(0, operations.postActionsSql().size());
        Assertions.assertEquals(expectedMilestoneQuery, operations.ingestSql().get(0));
        Assertions.assertEquals(expectedUpsertQuery, operations.ingestSql().get(1));
    }

    // todo: exception, post action, stats... (refer to unitemporal equivalent for what's missing)
}
