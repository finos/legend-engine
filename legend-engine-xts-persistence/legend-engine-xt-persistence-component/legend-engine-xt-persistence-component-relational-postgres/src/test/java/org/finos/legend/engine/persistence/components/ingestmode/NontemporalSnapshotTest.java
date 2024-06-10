// Copyright 2024 Goldman Sachs
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

import org.finos.legend.engine.persistence.components.PostgresTestArtifacts;
import org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.postgres.PostgresSink;
import org.finos.legend.engine.persistence.components.testcases.ingestmode.nontemporal.NontemporalSnapshotTestCases;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.PostgresTestArtifacts.*;
import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.DATA_ERROR_ROWS;
import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.DUPLICATE_ROWS;

public class NontemporalSnapshotTest extends NontemporalSnapshotTestCases
{
    String cleanUpMainTableSql = "DELETE FROM \"mydb\".\"main\" as sink";
    String cleanupMainTableSqlUpperCase = "DELETE FROM \"MYDB\".\"MAIN\" as sink";
    String rowsDeleted = "SELECT COUNT(*) as \"rowsDeleted\" FROM \"mydb\".\"main\" as sink";
    String rowsUpdated = "SELECT 0 as \"rowsUpdated\"";
    String rowsInserted = "SELECT COUNT(*) as \"rowsInserted\" FROM \"mydb\".\"main\" as sink";
    String rowsTerminated = "SELECT 0 as \"rowsTerminated\"";

    @Override
    public void verifyNontemporalSnapshotNoAuditingNoDedupNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metadataIngestSqlList = operations.metadataIngestSql();
        List<String> initializeLockSql = operations.initializeLockSql();
        List<String> acquireLockSql = operations.acquireLockSql();
        List<String> deduplicationAndVersioningSql = operations.deduplicationAndVersioningSql();
        Map<DedupAndVersionErrorSqlType, String> andVersioningErrorChecksSql = operations.deduplicationAndVersioningErrorChecksSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\", \"batch_id\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
                "FROM \"mydb\".\"staging\" as stage)";

        Assertions.assertEquals(expectedLockInfoTableCreateQuery, preActionsSqlList.get(0));

        Assertions.assertEquals(cleanUpMainTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(lockInitializedQuery, initializeLockSql.get(0));
        Assertions.assertEquals(lockAcquiredQuery, acquireLockSql.get(0));
        Assertions.assertTrue(deduplicationAndVersioningSql.isEmpty());
        Assertions.assertTrue(andVersioningErrorChecksSql.isEmpty());
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSqlList.get(0));

        // Stats
        verifyStats(operations, "staging");
    }

    @Override
    public void verifyNontemporalSnapshotWithAuditingFilterDupsNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metadataIngestSqlList = operations.metadataIngestSql();
        List<String> deduplicationAndVersioningSql = operations.deduplicationAndVersioningSql();
        Map<DedupAndVersionErrorSqlType, String> deduplicationAndVersioningErrorChecksSql = operations.deduplicationAndVersioningErrorChecksSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"batch_update_time\", \"batch_id\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",'2000-01-01 00:00:00.000000'," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
            "FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" as stage)";

        Assertions.assertEquals(expectedBaseTableWithAuditPkCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSqlList.get(1));
        Assertions.assertEquals(expectedBaseTempStagingTableWithCount, preActionsSqlList.get(2));
        Assertions.assertEquals(cleanUpMainTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSqlList.get(0));
        Assertions.assertEquals(expectedTempStagingCleanupQuery, deduplicationAndVersioningSql.get(0));
        Assertions.assertTrue(deduplicationAndVersioningErrorChecksSql.isEmpty());
        Assertions.assertEquals(expectedInsertIntoBaseTempStagingWithFilterDuplicates, deduplicationAndVersioningSql.get(1));

        // Stats
        verifyStats(operations, "staging");
    }

    @Override
    public void verifyNontemporalSnapshotWithAuditingFailOnDupMaxVersion(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metadataIngestSqlList = operations.metadataIngestSql();
        List<String> deduplicationAndVersioningSql = operations.deduplicationAndVersioningSql();
        Map<DedupAndVersionErrorSqlType, String> deduplicationAndVersioningErrorChecksSql = operations.deduplicationAndVersioningErrorChecksSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"batch_update_time\", \"batch_id\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",'2000-01-01 00:00:00.000000'," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
                "FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" as stage)";

        String maxDataErrorCheckSql = "SELECT MAX(\"legend_persistence_distinct_rows\") as \"MAX_DATA_ERRORS\" FROM " +
                "(SELECT COUNT(DISTINCT(\"amount\")) as \"legend_persistence_distinct_rows\" FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" " +
                "as stage GROUP BY \"id\", \"name\", \"biz_date\") as stage";

        String dataErrorsSql = "SELECT \"id\",\"name\",\"biz_date\",COUNT(DISTINCT(\"amount\")) as \"legend_persistence_error_count\" FROM " +
                "\"mydb\".\"staging_temp_staging_lp_yosulf\" as stage GROUP BY \"id\", \"name\", \"biz_date\" HAVING COUNT(DISTINCT(\"amount\")) > 1 LIMIT 20";

        Assertions.assertEquals(expectedBaseTableWithAuditPkCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSqlList.get(1));
        Assertions.assertEquals(expectedBaseTempStagingTableWithCount, preActionsSqlList.get(2));
        Assertions.assertEquals(cleanUpMainTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSqlList.get(0));

        Assertions.assertEquals(expectedTempStagingCleanupQuery, deduplicationAndVersioningSql.get(0));
        Assertions.assertEquals(expectedInsertIntoBaseTempStagingWithMaxVersionAndFilterDuplicates, deduplicationAndVersioningSql.get(1));
        Assertions.assertEquals(maxDupsErrorCheckSql, deduplicationAndVersioningErrorChecksSql.get(DedupAndVersionErrorSqlType.MAX_DUPLICATES));
        Assertions.assertEquals(maxDataErrorCheckSql, deduplicationAndVersioningErrorChecksSql.get(DedupAndVersionErrorSqlType.MAX_DATA_ERRORS));
        Assertions.assertEquals(dupRowsSql, deduplicationAndVersioningErrorChecksSql.get(DUPLICATE_ROWS));
        Assertions.assertEquals(dataErrorsSql, deduplicationAndVersioningErrorChecksSql.get(DATA_ERROR_ROWS));

        // Stats
        verifyStats(operations, "staging");
    }

    @Override
    public void verifyNontemporalSnapshotWithUpperCaseOptimizer(GeneratorResult queries)
    {
        List<String> preActionsSqlList = queries.preActionsSql();
        List<String> milestoningSqlList = queries.ingestSql();

        String insertSql = "INSERT INTO \"MYDB\".\"MAIN\" (\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"BATCH_ID\") " +
                "(SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"BIZ_DATE\"," +
                "(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN') " +
                "FROM \"MYDB\".\"STAGING\" as stage)";

        Assertions.assertEquals(expectedBaseTableCreateQueryWithUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(cleanupMainTableSqlUpperCase, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Override
    public void verifyNontemporalSnapshotWithLessColumnsInStaging(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"batch_id\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
                "FROM \"mydb\".\"staging\" as stage)";

        Assertions.assertEquals(expectedBaseTableCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(cleanUpMainTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Override
    public void verifyNontemporalSnapshotWithCleanStagingData(GeneratorResult operations)
    {
        List<String> postActionsSql = operations.postActionsSql();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(expectedStagingCleanupQuery);
        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);
    }

    @Override
    public void verifyNontemporalSnapshotWithDropStagingData(SqlPlan physicalPlanForPostCleanup)
    {
        List<String> sqlsForPostActions = physicalPlanForPostCleanup.getSqlList();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(expectedDropTableQuery);
        assertIfListsAreSameIgnoringOrder(expectedSQL, sqlsForPostActions);
    }

    @Override
    public RelationalSink getRelationalSink()
    {
        return PostgresSink.get();
    }

    protected String getExpectedMetadataTableIngestQuery()
    {
        return expectedMetadataTableIngestQuery;
    }

    protected String getExpectedMetadataTableIngestQueryWithUpperCase()
    {
        return expectedMetadataTableIngestQueryWithUpperCase;
    }

    protected String getExpectedMetadataTableCreateQuery()
    {
        return expectedMetadataTableCreateQuery;
    }

    protected String getExpectedMetadataTableCreateQueryWithUpperCase()
    {
        return expectedMetadataTableCreateQueryWithUpperCase;
    }

    private void verifyStats(GeneratorResult operations, String stageTableName)
    {
        // Pre stats:
        Assertions.assertEquals(rowsDeleted, operations.preIngestStatisticsSql().get(StatisticName.ROWS_DELETED));

        // Post Stats:
        String incomingRecordCount = String.format("SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"%s\" as stage", stageTableName);
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }
}