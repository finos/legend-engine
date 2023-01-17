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

package org.finos.legend.engine.persistence.components.ingestmode.nontemporal;

import org.finos.legend.engine.persistence.components.AnsiTestArtifacts;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.testcases.ingestmode.nontemporal.NontemporalSnapshotTestCases;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

public class NontemporalSnapshotTest extends NontemporalSnapshotTestCases
{
    String cleanUpMainTableSql = "DELETE FROM \"mydb\".\"main\" as sink";
    String cleanupMainTableSqlUpperCase = "DELETE FROM \"MYDB\".\"MAIN\" as sink";
    String rowsDeleted = "SELECT COUNT(*) as \"rowsDeleted\" FROM \"mydb\".\"main\" as sink";
    String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage";
    String rowsUpdated = "SELECT 0 as \"rowsUpdated\"";
    String rowsInserted = "SELECT COUNT(*) as \"rowsInserted\" FROM \"mydb\".\"main\" as sink";
    String rowsTerminated = "SELECT 0 as \"rowsTerminated\"";

    @Override
    public void verifyNontemporalSnapshotNoAuditingNoDataSplit(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\") " +
                "(SELECT * FROM \"mydb\".\"staging\" as stage)";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTableCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(cleanUpMainTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));

        // Stats
        verifyStats(operations);
    }

    @Override
    public void verifyNontemporalSnapshotNoAuditingWithDataSplit(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\" FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage_right WHERE " +
                "(stage.\"data_split\" < stage_right.\"data_split\") AND ((stage.\"id\" = stage_right.\"id\") AND (stage.\"name\" = stage_right.\"name\")))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTableCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(cleanUpMainTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));

        // Stats
        verifyStats(operations);
    }

    @Override
    public void verifyNontemporalSnapshotWithAuditingNoDataSplit(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"batch_update_time\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",'2000-01-01 00:00:00' " +
            "FROM \"mydb\".\"staging\" as stage)";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTableCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(cleanUpMainTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));

        // Stats
        verifyStats(operations);
    }

    @Override
    public void verifyNontemporalSnapshotWithAuditingWithDataSplit(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\", \"batch_update_time\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",'2000-01-01 00:00:00' " +
                "FROM \"mydb\".\"staging\" as stage WHERE NOT (EXISTS " +
                "(SELECT * FROM \"mydb\".\"staging\" as stage_right " +
                "WHERE (stage.\"data_split\" < stage_right.\"data_split\") AND ((stage.\"id\" = stage_right.\"id\") AND " +
                "(stage.\"name\" = stage_right.\"name\")))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTableCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(cleanUpMainTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));

        // Stats
        verifyStats(operations);
    }

    @Override
    public void verifyNontemporalSnapshotWithUpperCaseOptimizer(GeneratorResult queries)
    {
        List<String> preActionsSqlList = queries.preActionsSql();
        List<String> milestoningSqlList = queries.ingestSql();

        String insertSql = "INSERT INTO \"MYDB\".\"MAIN\" (\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\") " +
                "(SELECT * FROM \"MYDB\".\"STAGING\" as stage)";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTableCreateQueryWithUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(cleanupMainTableSqlUpperCase, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Override
    public void verifyNontemporalSnapshotWithLessColumnsInStaging(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\") " +
                "(SELECT * FROM \"mydb\".\"staging\" as stage)";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTableCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(cleanUpMainTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Override
    public void verifyNontemporalSnapshotWithCleanStagingData(GeneratorResult operations)
    {
        List<String> postActionsSql = operations.postActionsSql();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(AnsiTestArtifacts.expectedStagingCleanupQuery);
        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);
    }

    @Override
    public void verifyNontemporalSnapshotWithDropStagingData(SqlPlan physicalPlanForPostActions)
    {
        List<String> sqlsForPostActions = physicalPlanForPostActions.getSqlList();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(AnsiTestArtifacts.expectedDropTableQuery);
        assertIfListsAreSameIgnoringOrder(expectedSQL, sqlsForPostActions);
    }

    @Override
    public RelationalSink getRelationalSink()
    {
        return AnsiSqlSink.get();
    }

    private void verifyStats(GeneratorResult operations)
    {
        // Pre stats:
        Assertions.assertEquals(rowsDeleted, operations.preIngestStatisticsSql().get(StatisticName.ROWS_DELETED));

        // Post Stats:
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }
}