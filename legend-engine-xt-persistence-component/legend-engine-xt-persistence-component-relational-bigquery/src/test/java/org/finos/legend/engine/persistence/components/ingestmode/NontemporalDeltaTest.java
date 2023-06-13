// Copyright 2023 Goldman Sachs
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
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

public class NontemporalDeltaTest extends org.finos.legend.engine.persistence.components.ingestmode.nontemporal.NontemporalDeltaTest
{

    protected String incomingRecordCount = "SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`staging` as stage";
    protected String incomingRecordCountWithSplits = "SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`staging` as stage WHERE " +
            "(stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
    protected String rowsTerminated = "SELECT 0 as `rowsTerminated`";
    protected String rowsDeleted = "SELECT 0 as `rowsDeleted`";
    protected String rowsDeletedWithDeleteIndicator = "SELECT COUNT(*) as `rowsDeleted` FROM `mydb`.`main` as sink WHERE EXISTS (SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest` FROM `mydb`.`staging` as stage WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`) AND (stage.`delete_indicator` IN ('yes','1','true')))";


    @Override
    public RelationalSink getRelationalSink()
    {
        return BigQuerySink.get();
    }

    @Override
    public void verifyNontemporalDeltaNoAuditingNoDataSplit(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO `mydb`.`main` as sink " +
            "USING `mydb`.`staging` as stage " +
            "ON (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`) " +
            "WHEN MATCHED AND sink.`digest` <> stage.`digest` " +
            "THEN UPDATE SET " +
            "sink.`id` = stage.`id`," +
            "sink.`name` = stage.`name`," +
            "sink.`amount` = stage.`amount`," +
            "sink.`biz_date` = stage.`biz_date`," +
            "sink.`digest` = stage.`digest` " +
            "WHEN NOT MATCHED THEN " +
            "INSERT (`id`, `name`, `amount`, `biz_date`, `digest`) " +
            "VALUES (stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`)";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));

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

        String mergeSql = "MERGE INTO `mydb`.`main` as sink " +
                "USING `mydb`.`staging` as stage " +
                "ON (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`) " +
                "WHEN MATCHED AND sink.`digest` <> stage.`digest` " +
                "THEN UPDATE SET " +
                "sink.`id` = stage.`id`," +
                "sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`biz_date` = stage.`biz_date`," +
                "sink.`digest` = stage.`digest`," +
                "sink.`batch_update_time` = PARSE_DATETIME('%Y-%m-%d %H:%M:%S','2000-01-01 00:00:00') " +
                "WHEN NOT MATCHED THEN INSERT " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_update_time`) " +
                "VALUES (stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,PARSE_DATETIME('%Y-%m-%d %H:%M:%S','2000-01-01 00:00:00'))";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNonTemporalDeltaNoAuditingWithDataSplit(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String mergeSql = "MERGE INTO `mydb`.`main` as sink " +
                "USING (SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest` FROM `mydb`.`staging` as stage " +
                "WHERE (stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) " +
                "as stage ON (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`) " +
                "WHEN MATCHED AND sink.`digest` <> stage.`digest` " +
                "THEN UPDATE SET sink.`id` = stage.`id`,sink.`name` = stage.`name`,sink.`amount` = stage.`amount`,sink.`biz_date` = stage.`biz_date`,sink.`digest` = stage.`digest` " +
                "WHEN NOT MATCHED " +
                "THEN INSERT (`id`, `name`, `amount`, `biz_date`, `digest`) " +
                "VALUES (stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`)";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedBaseTablePlusDigestCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(mergeSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(mergeSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));

        // Stats
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplits, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplits, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNonTemporalDeltaWithWithAuditingWithDataSplit(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String mergeSql = "MERGE INTO `mydb`.`main` as sink " +
                "USING (SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest` FROM `mydb`.`staging` as stage " +
                "WHERE (stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) " +
                "as stage ON (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`) " +
                "WHEN MATCHED AND sink.`digest` <> stage.`digest` " +
                "THEN UPDATE SET sink.`id` = stage.`id`,sink.`name` = stage.`name`,sink.`amount` = stage.`amount`,sink.`biz_date` = stage.`biz_date`,sink.`digest` = stage.`digest`,sink.`batch_update_time` = PARSE_DATETIME('%Y-%m-%d %H:%M:%S','2000-01-01 00:00:00') " +
                "WHEN NOT MATCHED " +
                "THEN INSERT (`id`, `name`, `amount`, `biz_date`, `digest`, `batch_update_time`) " +
                "VALUES (stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,PARSE_DATETIME('%Y-%m-%d %H:%M:%S','2000-01-01 00:00:00'))";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(mergeSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(mergeSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));

        // Stats
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplits, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplits, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaNoAuditingNoDataSplitWithDeleteIndicator(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO `mydb`.`main` as sink " +
                "USING `mydb`.`staging` as stage " +
                "ON (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`) " +
                "WHEN MATCHED AND (sink.`digest` <> stage.`digest`) AND (stage.`delete_indicator` NOT IN ('yes','1','true')) " +
                "THEN UPDATE SET " +
                "sink.`id` = stage.`id`," +
                "sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`biz_date` = stage.`biz_date`," +
                "sink.`digest` = stage.`digest` " +
                "WHEN NOT MATCHED THEN " +
                "INSERT (`id`, `name`, `amount`, `biz_date`, `digest`) " +
                "VALUES (stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`)";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(null, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsDeletedWithDeleteIndicator, operations.preIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaWithUpperCaseOptimizer(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO `MYDB`.`MAIN` as sink USING `MYDB`.`STAGING` as stage " +
                "ON (sink.`ID` = stage.`ID`) AND (sink.`NAME` = stage.`NAME`) WHEN MATCHED " +
                "AND sink.`DIGEST` <> stage.`DIGEST` THEN UPDATE SET sink.`ID` = stage.`ID`," +
                "sink.`NAME` = stage.`NAME`,sink.`AMOUNT` = stage.`AMOUNT`," +
                "sink.`BIZ_DATE` = stage.`BIZ_DATE`,sink.`DIGEST` = stage.`DIGEST` " +
                "WHEN NOT MATCHED THEN INSERT (`ID`, `NAME`, `AMOUNT`, `BIZ_DATE`, `DIGEST`) " +
                "VALUES (stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`BIZ_DATE`,stage.`DIGEST`)";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedBaseTablePlusDigestCreateQueryWithUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));
    }

    @Override
    public void verifyNontemporalDeltaWithLessColumnsInStaging(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO `mydb`.`main` as sink " +
                "USING `mydb`.`staging` as stage " +
                "ON (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`) " +
                "WHEN MATCHED AND sink.`digest` <> stage.`digest` " +
                "THEN UPDATE SET " +
                "sink.`id` = stage.`id`,sink.`name` = stage.`name`,sink.`amount` = stage.`amount`,sink.`digest` = stage.`digest` " +
                "WHEN NOT MATCHED THEN INSERT " +
                "(`id`, `name`, `amount`, `digest`) " +
                "VALUES (stage.`id`,stage.`name`,stage.`amount`,stage.`digest`)";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));
    }

    @Override
    public void verifyNontemporalDeltaWithNoVersionAndStagingFilter(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO `mydb`.`main` as sink " +
                "USING " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest` FROM `mydb`.`staging` as stage WHERE (stage.`biz_date` > '2020-01-01') AND (stage.`biz_date` < '2020-01-03')) as stage " +
                "ON (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`) " +
                "WHEN MATCHED AND sink.`digest` <> stage.`digest` " +
                "THEN UPDATE SET " +
                "sink.`id` = stage.`id`," +
                "sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`biz_date` = stage.`biz_date`," +
                "sink.`digest` = stage.`digest` " +
                "WHEN NOT MATCHED THEN " +
                "INSERT (`id`, `name`, `amount`, `biz_date`, `digest`) " +
                "VALUES (stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`)";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`staging` as stage WHERE (stage.`biz_date` > '2020-01-01') AND (stage.`biz_date` < '2020-01-03')";
        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaWithMaxVersioningAndStagingFiltersWithDedup(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO `mydb`.`main` as sink " +
            "USING " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,stage.`version` FROM " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,stage.`version`,ROW_NUMBER() OVER (PARTITION BY stage.`id`,stage.`name` ORDER BY stage.`version` DESC) as `legend_persistence_row_num` FROM `mydb`.`staging` as stage WHERE stage.`snapshot_id` > 18972) as stage WHERE stage.`legend_persistence_row_num` = 1) as stage " +
            "ON (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`) " +
            "WHEN MATCHED AND stage.`version` > sink.`version` " +
            "THEN UPDATE SET sink.`id` = stage.`id`,sink.`name` = stage.`name`,sink.`amount` = stage.`amount`,sink.`biz_date` = stage.`biz_date`,sink.`digest` = stage.`digest`,sink.`version` = stage.`version` " +
            "WHEN NOT MATCHED THEN INSERT (`id`, `name`, `amount`, `biz_date`, `digest`, `version`) VALUES (stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,stage.`version`)";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`staging` as stage WHERE stage.`snapshot_id` > 18972";
        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaWithMaxVersioningNoDedupAndStagingFilters(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO `mydb`.`main` as sink " +
            "USING " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,stage.`version` FROM `mydb`.`staging` as stage WHERE stage.`snapshot_id` > 18972) as stage " +
            "ON (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`) " +
            "WHEN MATCHED AND stage.`version` > sink.`version` " +
            "THEN UPDATE SET sink.`id` = stage.`id`,sink.`name` = stage.`name`,sink.`amount` = stage.`amount`,sink.`biz_date` = stage.`biz_date`,sink.`digest` = stage.`digest`,sink.`version` = stage.`version` " +
            "WHEN NOT MATCHED THEN INSERT (`id`, `name`, `amount`, `biz_date`, `digest`, `version`) VALUES (stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,stage.`version`)";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`staging` as stage WHERE stage.`snapshot_id` > 18972";
        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaWithMaxVersioningNoDedupWithoutStagingFilters(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO `mydb`.`main` as sink " +
            "USING " +
            "`mydb`.`staging` as stage " +
            "ON (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`) " +
            "WHEN MATCHED AND stage.`version` > sink.`version` " +
            "THEN UPDATE SET sink.`id` = stage.`id`,sink.`name` = stage.`name`,sink.`amount` = stage.`amount`,sink.`biz_date` = stage.`biz_date`,sink.`digest` = stage.`digest`,sink.`version` = stage.`version` " +
            "WHEN NOT MATCHED THEN INSERT (`id`, `name`, `amount`, `biz_date`, `digest`, `version`) VALUES (stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,stage.`version`)";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaWithWithMaxVersioningDedupEnabledAndUpperCaseWithoutStagingFilters(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO `MYDB`.`MAIN` as sink " +
            "USING " +
            "(SELECT stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`BIZ_DATE`,stage.`DIGEST`,stage.`VERSION` FROM " +
            "(SELECT stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`BIZ_DATE`,stage.`DIGEST`,stage.`VERSION`,ROW_NUMBER() OVER (PARTITION BY stage.`ID`,stage.`NAME` ORDER BY stage.`VERSION` DESC) as `LEGEND_PERSISTENCE_ROW_NUM` FROM `MYDB`.`STAGING` as stage) as stage WHERE stage.`LEGEND_PERSISTENCE_ROW_NUM` = 1) as stage " +
            "ON (sink.`ID` = stage.`ID`) AND (sink.`NAME` = stage.`NAME`) " +
            "WHEN MATCHED AND stage.`VERSION` >= sink.`VERSION` " +
            "THEN UPDATE SET sink.`ID` = stage.`ID`,sink.`NAME` = stage.`NAME`,sink.`AMOUNT` = stage.`AMOUNT`,sink.`BIZ_DATE` = stage.`BIZ_DATE`,sink.`DIGEST` = stage.`DIGEST`,sink.`VERSION` = stage.`VERSION` " +
            "WHEN NOT MATCHED THEN INSERT (`ID`, `NAME`, `AMOUNT`, `BIZ_DATE`, `DIGEST`, `VERSION`) VALUES (stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`BIZ_DATE`,stage.`DIGEST`,stage.`VERSION`)";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQueryUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));
    }

    @Override
    public void verifyNontemporalDeltaPostActionSqlAndCleanStagingData(GeneratorResult operations)
    {
        List<String> postActionsSql = operations.postActionsSql();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(BigQueryTestArtifacts.expectedStagingCleanupQuery);
        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);
    }
}
