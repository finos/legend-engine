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
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.snowflake.SnowflakeSink;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class UnitemporalDeltaTest extends IngestModeTest
{

    @Test
    void testUnitemporalMilestoningWithoutDeleteIndicator()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(mainTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        MetadataDataset metadataDataset = MetadataDataset.builder()
            .metadataDatasetDatabaseName(mainDbName)
            .metadataDatasetName("custom_metadata")
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

        Datasets datasets = Datasets.of(mainTable, stagingTable).withMetadataDataset(metadataDataset);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(SnowflakeSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> postActionsSql = operations.postActionsSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(custom_metadata.\"table_batch_id\"),0)+1 " +
            "FROM \"mydb\".\"custom_metadata\" as custom_metadata WHERE custom_metadata.\"table_name\" = 'main')-1," +
            "sink.\"batch_time_out\" = '2000-01-01 00:00:00' " +
            "WHERE (sink.\"batch_id_out\" = 999999999) AND " +
            "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "(sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(custom_metadata.\"table_batch_id\"),0)+1 FROM \"mydb\".\"custom_metadata\" as custom_metadata WHERE custom_metadata.\"table_name\" = 'main')," +
            "999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"batch_id_out\" = 999999999) " +
            "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")))))";
        String expectedPostActionsSql = "DELETE FROM \"mydb\".\"staging\" as stage";

        Assertions.assertEquals(expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedCustomMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedCustomMetadataTableIngestQuery, metadataIngestSql.get(0));
        Assertions.assertEquals(expectedPostActionsSql, postActionsSql.get(0));
    }

    @Test
    void testUnitemporalMilestoningWithDeleteIndicator()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(mainTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
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
            .relationalSink(SnowflakeSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> postActionsSql = operations.postActionsSql();

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

        String expectedPostActionsSql = "DELETE FROM \"mydb\".\"staging\" as stage";

        Assertions.assertEquals(expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
        Assertions.assertEquals(expectedPostActionsSql, postActionsSql.get(0));
    }

    @Test
    void testUnitemporalMilestoningWithDeleteIndicatorWithUpperCase()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(mainTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(stagingTableSchemaWithDeleteIndicator)
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

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

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(SnowflakeSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> postActionsSql = operations.postActionsSql();


        String expectedPostActionsSql = "DELETE FROM \"MYDB\".\"STAGING\" as stage";

        String expectedMilestoneQuery = "UPDATE \"MYDB\".\"MAIN\" as sink " +
            "SET sink.\"BATCH_ID_OUT\" = (SELECT COALESCE(MAX(batch_metadata.\"TABLE_BATCH_ID\"),0)+1 " +
            "FROM BATCH_METADATA as batch_metadata WHERE batch_metadata.\"TABLE_NAME\" = 'main')-1,sink.\"BATCH_TIME_OUT\" = '2000-01-01 00:00:00' " +
            "WHERE (sink.\"BATCH_ID_OUT\" = 999999999) AND (EXISTS (SELECT * FROM \"MYDB\".\"STAGING\" as stage " +
            "WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) " +
            "AND ((sink.\"DIGEST\" <> stage.\"DIGEST\") OR (stage.\"DELETE_INDICATOR\" IN ('yes','1','true')))))";

        String expectedUpsertQuery = "INSERT INTO \"MYDB\".\"MAIN\" " +
            "(\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"DIGEST\", \"BATCH_ID_IN\", \"BATCH_ID_OUT\", " +
            "\"BATCH_TIME_IN\", \"BATCH_TIME_OUT\") (SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\"," +
            "stage.\"BIZ_DATE\",stage.\"DIGEST\",(SELECT COALESCE(MAX(batch_metadata.\"TABLE_BATCH_ID\"),0)+1 " +
            "FROM BATCH_METADATA as batch_metadata WHERE batch_metadata.\"TABLE_NAME\" = 'main'),999999999,'2000-01-01 00:00:00'," +
            "'9999-12-31 23:59:59' FROM \"MYDB\".\"STAGING\" as stage WHERE (NOT (EXISTS " +
            "(SELECT * FROM \"MYDB\".\"MAIN\" as sink WHERE (sink.\"BATCH_ID_OUT\" = 999999999) AND " +
            "(sink.\"DIGEST\" = stage.\"DIGEST\") AND ((sink.\"ID\" = stage.\"ID\") AND " +
            "(sink.\"NAME\" = stage.\"NAME\"))))) AND (stage.\"DELETE_INDICATOR\" NOT IN ('yes','1','true')))";
        Assertions.assertEquals(expectedMainTableCreateQueryWithUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(expectedMetadataTableCreateQueryWithUpperCase, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(expectedMetadataTableIngestQueryWithUpperCase, metadataIngestSql.get(0));
        Assertions.assertEquals(expectedPostActionsSql, postActionsSql.get(0));
    }
}
