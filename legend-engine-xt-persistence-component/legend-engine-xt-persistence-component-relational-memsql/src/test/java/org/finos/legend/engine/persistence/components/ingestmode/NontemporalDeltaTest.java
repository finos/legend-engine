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

import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.memsql.MemSqlSink;
import org.finos.legend.engine.persistence.components.testcases.ingestmode.nontemporal.NontemporalDeltaTestCases;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

public class NontemporalDeltaTest extends NontemporalDeltaTestCases
{
    protected String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM `mydb`.`staging` as stage";
    protected String incomingRecordCountWithSplits = "SELECT COUNT(*) as incomingRecordCount FROM `mydb`.`staging` as stage WHERE " +
            "(stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
    protected String rowsTerminated = "SELECT 0 as rowsTerminated";
    protected String rowsDeleted = "SELECT 0 as rowsDeleted";

    @Override
    public void verifyNontemporalDeltaNoAuditingNoDataSplit(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String updateSql = "UPDATE `mydb`.`main` as sink " +
                "INNER JOIN `mydb`.`staging` as stage " +
                "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` <> stage.`digest`) " +
                "SET sink.`id` = stage.`id`," +
                "sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`biz_date` = stage.`biz_date`," +
                "sink.`digest` = stage.`digest`";

        String insertSql = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`) " +
                "(SELECT * FROM `mydb`.`staging` as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
                "WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND " +
                "(sink.`digest` = stage.`digest`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaWithAuditingNoDataSplit(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String updateSql = "UPDATE `mydb`.`main` as sink " +
                "INNER JOIN `mydb`.`staging` as stage " +
                "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` <> stage.`digest`) " +
                "SET sink.`id` = stage.`id`," +
                "sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`biz_date` = stage.`biz_date`," +
                "sink.`digest` = stage.`digest`," +
                "sink.`batch_update_time` = '2000-01-01 00:00:00'";

        String insertSql = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_update_time`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,'2000-01-01 00:00:00' " +
                "FROM `mydb`.`staging` as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
                "WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND " +
                "(sink.`digest` = stage.`digest`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNonTemporalDeltaNoAuditingWithDataSplit(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String updateSql = "UPDATE `mydb`.`main` as sink " +
                "INNER JOIN " +
                "(SELECT * FROM `mydb`.`staging` as stage WHERE (stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as stage " +
                "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` <> stage.`digest`) " +
                "SET sink.`id` = stage.`id`," +
                "sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`biz_date` = stage.`biz_date`," +
                "sink.`digest` = stage.`digest`";

        String insertSql = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `biz_date`, `digest`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest` FROM `mydb`.`staging` as stage " +
                "WHERE ((stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) " +
                "AND (NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`)))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));

        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));
        
        // Stats
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplits, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplits, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNonTemporalDeltaWithWithAuditingWithDataSplit(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String updateSql = "UPDATE `mydb`.`main` as sink " +
                "INNER JOIN " +
                "(SELECT * FROM `mydb`.`staging` as stage WHERE (stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as stage " +
                "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` <> stage.`digest`) SET " +
                "sink.`id` = stage.`id`," +
                "sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`biz_date` = stage.`biz_date`," +
                "sink.`digest` = stage.`digest`," +
                "sink.`batch_update_time` = '2000-01-01 00:00:00'";

        String insertSql = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `biz_date`, `digest`, `batch_update_time`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,'2000-01-01 00:00:00' FROM `mydb`.`staging` as stage " +
                "WHERE ((stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) " +
                "AND (NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`)))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));

        // Stats
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplits, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplits, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaWithUpperCaseOptimizer(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String updateSql = "UPDATE `MYDB`.`MAIN` as sink " +
                "INNER JOIN `MYDB`.`STAGING` as stage " +
                "ON ((sink.`ID` = stage.`ID`) AND (sink.`NAME` = stage.`NAME`)) AND (sink.`DIGEST` <> stage.`DIGEST`) " +
                "SET sink.`ID` = stage.`ID`," +
                "sink.`NAME` = stage.`NAME`," +
                "sink.`AMOUNT` = stage.`AMOUNT`," +
                "sink.`BIZ_DATE` = stage.`BIZ_DATE`," +
                "sink.`DIGEST` = stage.`DIGEST`";

        String insertSql = "INSERT INTO `MYDB`.`MAIN` (`ID`, `NAME`, `AMOUNT`, `BIZ_DATE`, `DIGEST`) " +
                "(SELECT * FROM `MYDB`.`STAGING` as stage WHERE NOT (EXISTS (SELECT * FROM `MYDB`.`MAIN` as sink " +
                "WHERE ((sink.`ID` = stage.`ID`) " +
                "AND (sink.`NAME` = stage.`NAME`)) " +
                "AND (sink.`DIGEST` = stage.`DIGEST`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQueryWithUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Override
    public void verifyNontemporalDeltaWithLessColumnsInStaging(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String updateSql = "UPDATE `mydb`.`main` as sink " +
                "INNER JOIN `mydb`.`staging` as stage " +
                "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` <> stage.`digest`) " +
                "SET sink.`id` = stage.`id`," +
                "sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`digest` = stage.`digest`";

        String insertSql = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `digest`) " +
                "(SELECT * FROM `mydb`.`staging` as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
                "WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND " +
                "(sink.`digest` = stage.`digest`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Override
    public void verifyNontemporalDeltaPostActionSqlAndCleanStagingData(GeneratorResult operations)
    {
        List<String> postActionsSql = operations.postActionsSql();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(MemsqlTestArtifacts.expectedStagingCleanupQuery);
        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);
    }

    public RelationalSink getRelationalSink()
    {
        return MemSqlSink.get();
    }
}
