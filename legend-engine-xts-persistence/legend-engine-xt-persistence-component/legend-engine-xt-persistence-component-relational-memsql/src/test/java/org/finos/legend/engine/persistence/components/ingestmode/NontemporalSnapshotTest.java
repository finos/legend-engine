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

import org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.memsql.MemSqlSink;
import org.finos.legend.engine.persistence.components.testcases.ingestmode.nontemporal.NontemporalSnapshotTestCases;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.DATA_ERROR_ROWS;
import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.DUPLICATE_ROWS;

public class NontemporalSnapshotTest extends NontemporalSnapshotTestCases
{
    String rowsDeleted = "SELECT COUNT(*) as `rowsDeleted` FROM `mydb`.`main` as sink";
    String rowsUpdated = "SELECT 0 as `rowsUpdated`";
    String rowsInserted = "SELECT COUNT(*) as `rowsInserted` FROM `mydb`.`main` as sink";
    String rowsTerminated = "SELECT 0 as `rowsTerminated`";

    @Override
    public void verifyNontemporalSnapshotNoAuditingNoDedupNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `biz_date`, `batch_id`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
                "FROM `mydb`.`staging` as stage)";

        Assertions.assertEquals(MemsqlTestArtifacts.cleanUpMainTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));

        // Stats
        verifyStats(operations, "staging");
    }

    @Override
    public void verifyNontemporalSnapshotWithAuditingFilterDupsNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` " +
            "(`id`, `name`, `amount`, `biz_date`, `batch_update_time`, `batch_id`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,'2000-01-01 00:00:00.000000'," +
            "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
            "FROM `mydb`.`staging_temp_staging_lp_yosulf` as stage)";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTableWithAuditPKCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.cleanUpMainTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));

        // Stats
        verifyStats(operations, "staging");
    }

    @Override
    public void verifyNontemporalSnapshotWithAuditingFailOnDupMaxVersion(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metaIngestSqlList = operations.metadataIngestSql();
        List<String> deduplicationAndVersioningSql = operations.deduplicationAndVersioningSql();
        Map<DedupAndVersionErrorSqlType, String> deduplicationAndVersioningErrorChecksSql = operations.deduplicationAndVersioningErrorChecksSql();

        String insertSql = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `batch_update_time`, `batch_id`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,'2000-01-01 00:00:00.000000'," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
                "FROM `mydb`.`staging_temp_staging_lp_yosulf` as stage)";

        String maxDataErrorCheckSql = "SELECT MAX(`legend_persistence_distinct_rows`) as `MAX_DATA_ERRORS` FROM " +
                "(SELECT COUNT(DISTINCT(`amount`)) as `legend_persistence_distinct_rows` " +
                "FROM `mydb`.`staging_temp_staging_lp_yosulf` as stage GROUP BY `id`, `name`, `biz_date`) as stage";

        String dataErrorsSqlWithBizDateVersion = "SELECT `id`,`name`,`biz_date`,COUNT(DISTINCT(`amount`)) as `legend_persistence_error_count` " +
                "FROM `mydb`.`staging_temp_staging_lp_yosulf` as stage " +
                "GROUP BY `id`, `name`, `biz_date` HAVING `legend_persistence_error_count` > 1 LIMIT 20";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTableWithAuditPKCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSqlList.get(1));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTempStagingTableWithCount, preActionsSqlList.get(2));
        Assertions.assertEquals(MemsqlTestArtifacts.cleanUpMainTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableIngestQuery, metaIngestSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));

        Assertions.assertEquals(MemsqlTestArtifacts.expectedTempStagingCleanupQuery, deduplicationAndVersioningSql.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedInsertIntoBaseTempStagingWithMaxVersionAndFilterDuplicates, deduplicationAndVersioningSql.get(1));

        Assertions.assertEquals(MemsqlTestArtifacts.maxDupsErrorCheckSql, deduplicationAndVersioningErrorChecksSql.get(DedupAndVersionErrorSqlType.MAX_DUPLICATES));
        Assertions.assertEquals(maxDataErrorCheckSql, deduplicationAndVersioningErrorChecksSql.get(DedupAndVersionErrorSqlType.MAX_DATA_ERRORS));
        Assertions.assertEquals(MemsqlTestArtifacts.dupRowsSql, deduplicationAndVersioningErrorChecksSql.get(DUPLICATE_ROWS));
        Assertions.assertEquals(dataErrorsSqlWithBizDateVersion, deduplicationAndVersioningErrorChecksSql.get(DATA_ERROR_ROWS));

        // Stats
        verifyStats(operations, "staging");
    }

    @Override
    public void verifyNontemporalSnapshotWithUpperCaseOptimizer(GeneratorResult queries)
    {
        List<String> preActionsSqlList = queries.preActionsSql();
        List<String> milestoningSqlList = queries.ingestSql();

        String insertSql = "INSERT INTO `MYDB`.`MAIN` (`ID`, `NAME`, `AMOUNT`, `BIZ_DATE`, `BATCH_ID`) " +
                "(SELECT stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`BIZ_DATE`," +
                "(SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MAIN') " +
                "FROM `MYDB`.`STAGING` as stage)";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTableCreateQueryWithUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.cleanupMainTableSqlUpperCase, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Override
    public void verifyNontemporalSnapshotWithLessColumnsInStaging(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `batch_id`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
                "FROM `mydb`.`staging` as stage)";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTableCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.cleanUpMainTableSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Override
    public void verifyNontemporalSnapshotWithCleanStagingData(GeneratorResult operations)
    {
        List<String> postActionsSql = operations.postActionsSql();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(MemsqlTestArtifacts.expectedStagingCleanupQuery);
        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);
    }

    @Override
    public void verifyNontemporalSnapshotWithDropStagingData(SqlPlan physicalPlanForPostCleanup)
    {
        List<String> sqlsForPostActions = physicalPlanForPostCleanup.getSqlList();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(MemsqlTestArtifacts.expectedDropTableQuery);
        assertIfListsAreSameIgnoringOrder(expectedSQL, sqlsForPostActions);
    }

    @Override
    public RelationalSink getRelationalSink()
    {
        return MemSqlSink.get();
    }

    private void verifyStats(GeneratorResult operations, String stageTableName)
    {
        // Pre stats:
        Assertions.assertEquals(rowsDeleted, operations.preIngestStatisticsSql().get(StatisticName.ROWS_DELETED));

        // Post Stats:
        String incomingRecordCount = String.format("SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`%s` as stage", stageTableName);
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }
}