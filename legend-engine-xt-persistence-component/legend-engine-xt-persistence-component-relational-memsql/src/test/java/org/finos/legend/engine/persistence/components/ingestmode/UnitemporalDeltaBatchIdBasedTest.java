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

import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.memsql.MemSqlSink;
import org.finos.legend.engine.persistence.components.testcases.ingestmode.unitemporal.UnitmemporalDeltaBatchIdBasedTestCases;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

public class UnitemporalDeltaBatchIdBasedTest extends UnitmemporalDeltaBatchIdBasedTestCases
{
    @Override
    public void verifyUnitemporalDeltaNoDeleteIndNoAuditing(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
                "SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1 " +
                "WHERE (sink.`batch_id_out` = 999999999) AND " +
                "(EXISTS (SELECT * FROM `mydb`.`staging` as stage " +
                "WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND " +
                "(sink.`digest` <> stage.`digest`)))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id_in`, `batch_id_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')," +
                "999999999 " +
                "FROM `mydb`.`staging` as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
                "WHERE (sink.`batch_id_out` = 999999999) " +
                "AND (sink.`digest` = stage.`digest`) AND ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedMainTableBatchIdBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM `mydb`.`staging` as stage";
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM `mydb`.`main` as sink WHERE sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1";
        String rowsDeleted = "SELECT 0 as rowsDeleted";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE sink.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'))-(SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1) as rowsInserted";
        String rowsTerminated = "SELECT 0 as rowsTerminated";

        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalDeltaNoDeleteIndWithDataSplits(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
                "SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1 " +
                "WHERE (sink.`batch_id_out` = 999999999) AND " +
                "(EXISTS (SELECT * FROM `mydb`.`staging` as stage " +
                "WHERE ((stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) AND ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND " +
                "(sink.`digest` <> stage.`digest`)))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id_in`, `batch_id_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')," +
                "999999999 " +
                "FROM `mydb`.`staging` as stage " +
                "WHERE ((stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) AND " +
                "(NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
                "WHERE (sink.`batch_id_out` = 999999999) " +
                "AND (sink.`digest` = stage.`digest`) AND ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`))))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedMainTableBatchIdBasedCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));

        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMilestoneQuery, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedUpsertQuery, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMilestoneQuery, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedUpsertQuery, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(0).metadataIngestSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(1).metadataIngestSql().get(0));
        Assertions.assertEquals(2, operations.size());

        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM `mydb`.`staging` as stage WHERE (stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM `mydb`.`main` as sink WHERE sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1";
        String rowsDeleted = "SELECT 0 as rowsDeleted";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE sink.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'))-(SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1) as rowsInserted";
        String rowsTerminated = "SELECT 0 as rowsTerminated";

        verifyStats(operations.get(0), enrichSqlWithDataSplits(incomingRecordCount, dataSplitRanges.get(0)), rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalDeltaWithDeleteIndNoDataSplits(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink SET sink.`batch_id_out` = " +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1 " +
                "WHERE " +
                "(sink.`batch_id_out` = 999999999) AND " +
                "(EXISTS (SELECT * FROM `mydb`.`staging` as stage " +
                "WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) " +
                "AND ((sink.`digest` <> stage.`digest`) OR (stage.`delete_indicator` IN ('yes','1','true')))))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id_in`, `batch_id_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')," +
                "999999999 FROM `mydb`.`staging` as stage " +
                "WHERE (NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
                "WHERE (sink.`batch_id_out` = 999999999) AND (sink.`digest` = stage.`digest`) " +
                "AND ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`))))) AND " +
                "(stage.`delete_indicator` NOT IN ('yes','1','true')))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedMainTableBatchIdBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM `mydb`.`staging` as stage";
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1) AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'))))";
        String rowsDeleted = "SELECT 0 as rowsDeleted";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE sink.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'))-(SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1) AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'))))) as rowsInserted";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1)-(SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1) AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'))))) as rowsTerminated";
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalDeltaWithDeleteIndWithDataSplits(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink SET sink.`batch_id_out` = " +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1 " +
                "WHERE " +
                "(sink.`batch_id_out` = 999999999) AND " +
                "(EXISTS (SELECT * FROM `mydb`.`staging` as stage " +
                "WHERE ((stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) AND ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) " +
                "AND ((sink.`digest` <> stage.`digest`) OR (stage.`delete_indicator` IN ('yes','1','true')))))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id_in`, `batch_id_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')," +
                "999999999 FROM `mydb`.`staging` as stage " +
                "WHERE ((stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) AND " +
                "(NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
                "WHERE (sink.`batch_id_out` = 999999999) AND (sink.`digest` = stage.`digest`) " +
                "AND ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`))))) AND " +
                "(stage.`delete_indicator` NOT IN ('yes','1','true')))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedMainTableBatchIdBasedCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));

        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMilestoneQuery, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedUpsertQuery, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMilestoneQuery, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedUpsertQuery, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(0).metadataIngestSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(1).metadataIngestSql().get(0));
        Assertions.assertEquals(2, operations.size());

        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM `mydb`.`staging` as stage WHERE (stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1) AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'))))";
        String rowsDeleted = "SELECT 0 as rowsDeleted";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE sink.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'))-(SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1) AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'))))) as rowsInserted";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1)-(SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main')-1) AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'))))) as rowsTerminated";
        verifyStats(operations.get(0), enrichSqlWithDataSplits(incomingRecordCount, dataSplitRanges.get(0)), rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalDeltaWithUpperCaseOptimizer(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `MYDB`.`MAIN` as sink " +
                "SET sink.`BATCH_ID_OUT` = (SELECT COALESCE(MAX(batch_metadata.`TABLE_BATCH_ID`),0)+1 " +
                "FROM BATCH_METADATA as batch_metadata WHERE batch_metadata.`TABLE_NAME` = 'main')-1 " +
                "WHERE (sink.`BATCH_ID_OUT` = 999999999) " +
                "AND (EXISTS (SELECT * FROM `MYDB`.`STAGING` as stage " +
                "WHERE ((sink.`ID` = stage.`ID`) AND (sink.`NAME` = stage.`NAME`)) " +
                "AND (sink.`DIGEST` <> stage.`DIGEST`)))";

        String expectedUpsertQuery = "INSERT INTO `MYDB`.`MAIN` " +
                "(`ID`, `NAME`, `AMOUNT`, `BIZ_DATE`, `DIGEST`, `BATCH_ID_IN`, `BATCH_ID_OUT`) " +
                "(SELECT stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`BIZ_DATE`," +
                "stage.`DIGEST`,(SELECT COALESCE(MAX(batch_metadata.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as batch_metadata WHERE batch_metadata.`TABLE_NAME` = 'main')," +
                "999999999 FROM `MYDB`.`STAGING` as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM `MYDB`.`MAIN` as sink " +
                "WHERE (sink.`BATCH_ID_OUT` = 999999999) AND (sink.`DIGEST` = stage.`DIGEST`) " +
                "AND ((sink.`ID` = stage.`ID`) AND (sink.`NAME` = stage.`NAME`)))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedMainTableBatchIdBasedCreateQueryWithUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQueryWithUpperCase, preActionsSql.get(1));
        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithUpperCase(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalDeltaWithCleanStagingData(GeneratorResult operations)
    {
        List<String> postActionsSql = operations.postActionsSql();
        Assertions.assertEquals(MemsqlTestArtifacts.expectedStagingCleanupQuery, postActionsSql.get(0));
    }

    @Override
    public RelationalSink getRelationalSink()
    {
        return MemSqlSink.get();
    }

    protected String getExpectedMetadataTableIngestQuery()
    {
        return MemsqlTestArtifacts.expectedMetadataTableIngestQuery;
    }

    protected String getExpectedMetadataTableIngestQueryWithUpperCase()
    {
        return MemsqlTestArtifacts.expectedMetadataTableIngestQueryWithUpperCase;
    }
}
