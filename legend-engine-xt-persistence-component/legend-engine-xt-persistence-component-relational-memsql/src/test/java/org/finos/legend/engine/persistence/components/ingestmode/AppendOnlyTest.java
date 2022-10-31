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
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

public class AppendOnlyTest extends org.finos.legend.engine.persistence.components.ingestmode.nontemporal.AppendOnlyTest
{

    String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM `mydb`.`staging` as stage";
    String rowsUpdated = "SELECT 0 as rowsUpdated";
    String rowsTerminated = "SELECT 0 as rowsTerminated";
    String rowsDeleted = "SELECT 0 as rowsDeleted";
    String rowsInserted = "SELECT COUNT(*) as rowsInserted FROM `mydb`.`staging` as stage";

    @Override
    public RelationalSink getRelationalSink()
    {
        return MemSqlSink.get();
    }

    @Override
    public void verifyAppendOnlyAllowDuplicatesNoAuditing(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `biz_date`, `digest`) " +
                "(SELECT * FROM `mydb`.`staging` as stage)";
        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTableCreateQueryWithNoPKs, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));

        // Stats
        verifyStats(operations);
    }

    @Override
    public void verifyAppendOnlyAllowDuplicatesWithAuditing(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_update_time`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,'2000-01-01 00:00:00' " +
                "FROM `mydb`.`staging` as stage)";
        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTableCreateQueryWithAuditAndNoPKs, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));

        // Stats
        verifyStats(operations);
    }

    @Override
    public void verifyAppendOnlyFailOnDuplicatesNoAuditing(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `biz_date`, `digest`) " +
                "(SELECT * FROM `mydb`.`staging` as stage)";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));

        // Stats
        verifyStats(operations);
    }

    @Override
    public void verifyAppendOnlyFailOnDuplicatesWithAuditing(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_update_time`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,'2000-01-01 00:00:00' " +
                "FROM `mydb`.`staging` as stage)";
        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));

        // Stats
        verifyStats(operations);
    }

    @Override
    public void verifyAppendOnlyFilterDuplicatesNoAuditing(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`) " +
                "(SELECT * FROM `mydb`.`staging` as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
                "WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND " +
                "(sink.`digest` = stage.`digest`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyAppendOnlyFilterDuplicatesWithAuditing(GeneratorResult queries)
    {
        List<String> preActionsSqlList = queries.preActionsSql();
        List<String> milestoningSqlList = queries.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `biz_date`, `digest`, `batch_update_time`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,'2000-01-01 00:00:00' FROM `mydb`.`staging` as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink WHERE ((sink.`id` = stage.`id`) AND " +
                "(sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`))))";
        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));

        List<String> postActionsSql = queries.postActionsSql();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(MemsqlTestArtifacts.expectedStagingCleanupQuery);
        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);

        // Stats
        String rowsInserted = "SELECT COUNT(*) as rowsInserted FROM `mydb`.`main` as sink WHERE sink.`batch_update_time` = (SELECT MAX(sink.`batch_update_time`) FROM `mydb`.`main` as sink)";
        Assertions.assertEquals(incomingRecordCount, queries.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, queries.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, queries.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, queries.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, queries.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }

    @Override
    public void verifyAppendOnlyFilterDuplicatesWithAuditingWithDataSplit(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String insertSql = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_update_time`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,'2000-01-01 00:00:00' " +
                "FROM `mydb`.`staging` as stage " +
                "WHERE ((stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) AND " +
                "(NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
                "WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND " +
                "(sink.`digest` = stage.`digest`)))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, operations.get(0).preActionsSql().get(0));

        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(2, operations.size());

        // Stats
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM `mydb`.`staging` as stage " +
                "WHERE (stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
        String rowsInserted = "SELECT COUNT(*) as rowsInserted FROM `mydb`.`main` as sink WHERE sink.`batch_update_time` = (SELECT MAX(sink.`batch_update_time`) FROM `mydb`.`main` as sink)";

        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCount, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCount, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }

    @Override
    public void verifyAppendOnlyWithUpperCaseOptimizer(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `MYDB`.`MAIN` (`ID`, `NAME`, `AMOUNT`, `BIZ_DATE`, `DIGEST`) " +
                "(SELECT * FROM `MYDB`.`STAGING` as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM `MYDB`.`MAIN` as sink " +
                "WHERE ((sink.`ID` = stage.`ID`) " +
                "AND (sink.`NAME` = stage.`NAME`)) " +
                "AND (sink.`DIGEST` = stage.`DIGEST`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQueryWithUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));
    }

    @Override
    public void verifyAppendOnlyWithLessColumnsInStaging(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `digest`) " +
                "(SELECT * FROM `mydb`.`staging` as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
                "WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND " +
                "(sink.`digest` = stage.`digest`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));
    }

    private void verifyStats(GeneratorResult operations)
    {
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
    }
}
