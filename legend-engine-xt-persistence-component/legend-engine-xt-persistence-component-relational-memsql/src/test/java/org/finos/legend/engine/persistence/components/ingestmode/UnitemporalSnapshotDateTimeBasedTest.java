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

import org.finos.legend.engine.persistence.components.AnsiTestArtifacts;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.memsql.MemSqlSink;
import org.finos.legend.engine.persistence.components.testcases.ingestmode.unitemporal.UnitmemporalSnapshotDateTimeBasedTestCases;
import org.junit.jupiter.api.Assertions;

import java.util.List;

public class UnitemporalSnapshotDateTimeBasedTest extends UnitmemporalSnapshotDateTimeBasedTestCases
{

    String incomingRecordCount = "SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`staging` as stage";
    String rowsUpdated = "SELECT COUNT(*) as `rowsUpdated` FROM `mydb`.`main` as sink WHERE (sink.`batch_time_out` = '2000-01-01 00:00:00') AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_time_in` = '2000-01-01 00:00:00')))";
    String rowsDeleted = "SELECT 0 as `rowsDeleted`";
    String rowsInserted = "SELECT (SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE sink.`batch_time_in` = '2000-01-01 00:00:00')-(SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE (sink.`batch_time_out` = '2000-01-01 00:00:00') AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_time_in` = '2000-01-01 00:00:00')))) as `rowsInserted`";
    String rowsTerminated = "SELECT (SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE sink.`batch_time_out` = '2000-01-01 00:00:00')-(SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE (sink.`batch_time_out` = '2000-01-01 00:00:00') AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_time_in` = '2000-01-01 00:00:00')))) as `rowsTerminated`";

    @Override
    public void verifyUnitemporalSnapshotWithoutPartitionNoDataSplits(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
                "SET sink.`batch_time_out` = '2000-01-01 00:00:00' " +
                "WHERE (sink.`batch_time_out` = '9999-12-31 23:59:59') " +
                "AND (NOT (EXISTS " +
                "(SELECT * FROM `mydb`.`staging` as stage " +
                "WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`))))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_time_in`, `batch_time_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
                "FROM `mydb`.`staging` as stage " +
                "WHERE NOT (stage.`digest` IN (SELECT sink.`digest` FROM `mydb`.`main` as sink WHERE sink.`batch_time_out` = '9999-12-31 23:59:59')))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedMainTableTimeBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalSnapshotWithoutPartitionForEmptyBatch(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink SET " +
                "sink.`batch_time_out` = '2000-01-01 00:00:00' " +
                "WHERE sink.`batch_time_out` = '9999-12-31 23:59:59'";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedMainTableTimeBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithoutPartitionWithUpperCaseOptimizer(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `MYDB`.`MAIN` as sink SET " +
                "sink.`BATCH_TIME_OUT` = '2000-01-01 00:00:00' " +
                "WHERE (sink.`BATCH_TIME_OUT` = '9999-12-31 23:59:59') AND " +
                "(NOT (EXISTS (SELECT * FROM `MYDB`.`STAGING` as stage WHERE ((sink.`ID` = stage.`ID`) " +
                "AND (sink.`NAME` = stage.`NAME`)) AND (sink.`DIGEST` = stage.`DIGEST`))))";

        String expectedUpsertQuery = "INSERT INTO `MYDB`.`MAIN` " +
                "(`ID`, `NAME`, `AMOUNT`, `BIZ_DATE`, `DIGEST`, `BATCH_TIME_IN`, `BATCH_TIME_OUT`) " +
                "(SELECT stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`BIZ_DATE`,stage.`DIGEST`," +
                "'2000-01-01 00:00:00','9999-12-31 23:59:59' FROM `MYDB`.`STAGING` as stage " +
                "WHERE NOT (stage.`DIGEST` IN (SELECT sink.`DIGEST` FROM `MYDB`.`MAIN` as sink " +
                "WHERE sink.`BATCH_TIME_OUT` = '9999-12-31 23:59:59')))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedMainTableTimeBasedCreateQueryWithUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQueryWithUpperCase, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithUpperCase(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionNoDataSplits(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
                "SET sink.`batch_time_out` = '2000-01-01 00:00:00' " +
                "WHERE (sink.`batch_time_out` = '9999-12-31 23:59:59') " +
                "AND (NOT (EXISTS " +
                "(SELECT * FROM `mydb`.`staging` as stage WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`)))) " +
                "AND (EXISTS (SELECT * FROM `mydb`.`staging` as stage WHERE sink.`biz_date` = stage.`biz_date`))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_time_in`, `batch_time_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
                "FROM `mydb`.`staging` as stage " +
                "WHERE NOT (stage.`digest` IN (SELECT sink.`digest` FROM `mydb`.`main` as sink WHERE (sink.`batch_time_out` = '9999-12-31 23:59:59') AND (sink.`biz_date` = stage.`biz_date`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedMainTableTimeBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionFiltersNoDataSplits(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink SET " +
                "sink.`batch_time_out` = '2000-01-01 00:00:00' " +
                "WHERE (sink.`batch_time_out` = '9999-12-31 23:59:59') AND " +
                "(NOT (EXISTS (SELECT * FROM `mydb`.`staging` as stage WHERE ((sink.`id` = stage.`id`) AND " +
                "(sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`)))) AND " +
                "(sink.`biz_date` IN ('2000-01-01 00:00:00','2000-01-02 00:00:00'))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_time_in`, `batch_time_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "'2000-01-01 00:00:00','9999-12-31 23:59:59' FROM `mydb`.`staging` as stage " +
                "WHERE NOT (stage.`digest` IN (SELECT sink.`digest` FROM `mydb`.`main` as sink " +
                "WHERE (sink.`batch_time_out` = '9999-12-31 23:59:59') AND " +
                "(sink.`biz_date` IN ('2000-01-01 00:00:00','2000-01-02 00:00:00')))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedMainTableTimeBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalSnapshotWithCleanStagingData(GeneratorResult operations)
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
