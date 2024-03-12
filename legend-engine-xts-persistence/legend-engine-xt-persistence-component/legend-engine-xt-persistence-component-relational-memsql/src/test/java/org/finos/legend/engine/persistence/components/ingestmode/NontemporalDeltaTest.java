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
    protected String incomingRecordCount = "SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`staging` as stage";
    protected String incomingRecordCountWithSplits = "SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`staging` as stage WHERE " +
            "(stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";

    protected String incomingRecordCountWithSplitsAndDuplicates = "SELECT COALESCE(SUM(stage.`legend_persistence_count`),0) as `incomingRecordCount` " +
            "FROM `mydb`.`staging_legend_persistence_temp_staging` as stage WHERE "
            + "(stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";

    protected String incomingRecordCountWithSplitsTempTable = "SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`staging_legend_persistence_temp_staging` as stage WHERE " +
            "(stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
    protected String rowsTerminated = "SELECT 0 as `rowsTerminated`";
    protected String rowsDeleted = "SELECT 0 as `rowsDeleted`";

    @Override
    public void verifyNontemporalDeltaNoAuditingNoDedupNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metaIngestSqlList = operations.metadataIngestSql();

        String updateSql = "UPDATE `mydb`.`main` as sink " +
                "INNER JOIN `mydb`.`staging` as stage " +
                "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` <> stage.`digest`) " +
                "SET sink.`id` = stage.`id`," +
                "sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`biz_date` = stage.`biz_date`," +
                "sink.`digest` = stage.`digest`," +
                "sink.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')";

        String insertSql = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
                "FROM `mydb`.`staging` as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
                "WHERE (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedStagingTableWithDigestCreateQuery, preActionsSqlList.get(1));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSqlList.get(2));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableIngestQuery, metaIngestSqlList.get(0));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaWithAuditingFilterDupsNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metaIngestSqlList = operations.metadataIngestSql();

        String updateSql = "UPDATE `mydb`.`main` as sink " +
                "INNER JOIN `mydb`.`staging_legend_persistence_temp_staging` as stage " +
                "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` <> stage.`digest`) " +
                "SET sink.`id` = stage.`id`," +
                "sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`biz_date` = stage.`biz_date`," +
                "sink.`digest` = stage.`digest`," +
                "sink.`batch_update_time` = '2000-01-01 00:00:00.000000'," +
                "sink.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')";

        String insertSql = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_update_time`, `batch_id`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,'2000-01-01 00:00:00.000000'," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
                "FROM `mydb`.`staging_legend_persistence_temp_staging` as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
                "WHERE (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSqlList.get(1));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableIngestQuery, metaIngestSqlList.get(0));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNonTemporalDeltaNoAuditingNoDedupAllVersion(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String updateSql = "UPDATE `mydb`.`main` as sink " +
                "INNER JOIN " +
                "(SELECT * FROM `mydb`.`staging_legend_persistence_temp_staging` as stage WHERE (stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as stage " +
                "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` <> stage.`digest`) " +
                "SET sink.`id` = stage.`id`," +
                "sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`biz_date` = stage.`biz_date`," +
                "sink.`digest` = stage.`digest`," +
                "sink.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')";

        String insertSql = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
                "FROM `mydb`.`staging_legend_persistence_temp_staging` as stage " +
                "WHERE ((stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) " +
                "AND (NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink WHERE (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));

        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));

        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableIngestQuery, operations.get(0).metadataIngestSql().get(0));

        // Stats
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplitsTempTable, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplitsTempTable, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNonTemporalDeltaNoAuditingNoDedupAllVersionWithoutPerform(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String updateSql = "UPDATE `mydb`.`main` as sink " +
                "INNER JOIN " +
                "(SELECT * FROM `mydb`.`staging` as stage WHERE (stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as stage " +
                "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` <> stage.`digest`) " +
                "SET sink.`id` = stage.`id`," +
                "sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`biz_date` = stage.`biz_date`," +
                "sink.`digest` = stage.`digest`," +
                "sink.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')";

        String insertSql = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
                "FROM `mydb`.`staging` as stage " +
                "WHERE ((stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) " +
                "AND (NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink WHERE (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));

        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));

        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableIngestQuery, operations.get(0).metadataIngestSql().get(0));

        // Stats
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplits, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplits, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNonTemporalDeltaWithWithAuditingFailOnDupsAllVersion(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String updateSql = "UPDATE `mydb`.`main` as sink " +
                "INNER JOIN " +
                "(SELECT * FROM `mydb`.`staging_legend_persistence_temp_staging` as stage WHERE (stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as stage " +
                "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` <> stage.`digest`) SET " +
                "sink.`id` = stage.`id`," +
                "sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`biz_date` = stage.`biz_date`," +
                "sink.`digest` = stage.`digest`," +
                "sink.`batch_update_time` = '2000-01-01 00:00:00.000000'," +
                "sink.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')";

        String insertSql = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `biz_date`, `digest`, `batch_update_time`, `batch_id`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,'2000-01-01 00:00:00.000000'," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
                "FROM `mydb`.`staging_legend_persistence_temp_staging` as stage " +
                "WHERE ((stage.`data_split` >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.`data_split` <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) " +
                "AND (NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink WHERE (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));

        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));

        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableIngestQuery, operations.get(0).metadataIngestSql().get(0));

        // Stats
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplitsAndDuplicates, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplitsAndDuplicates, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaNoAuditingWithDeleteIndicatorNoDedupNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metaIngestSqlList = operations.metadataIngestSql();

        String updateSql = "UPDATE `mydb`.`main` as sink " +
                "INNER JOIN `mydb`.`staging` as stage " +
                "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` <> stage.`digest`) AND (stage.`delete_indicator` NOT IN ('yes','1','true')) " +
                "SET sink.`id` = stage.`id`" +
                ",sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`biz_date` = stage.`biz_date`," +
                "sink.`digest` = stage.`digest`," +
                "sink.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')";

        String insertSql = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
                "FROM `mydb`.`staging` as stage " +
                "WHERE (stage.`delete_indicator` NOT IN ('yes','1','true')) AND (NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
                "WHERE (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)))))";

        String deleteSql = "DELETE FROM `mydb`.`main` as sink " +
                "WHERE EXISTS (" +
                "SELECT * FROM `mydb`.`staging` as stage WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) " +
                "AND (sink.`digest` = stage.`digest`) AND (stage.`delete_indicator` IN ('yes','1','true')))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSqlList.get(1));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(deleteSql, milestoningSqlList.get(2));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableIngestQueryWithAdditionalMetadata, metaIngestSqlList.get(0));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(null, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaWithUpperCaseOptimizer(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metaIngestSqlList = operations.metadataIngestSql();

        String updateSql = "UPDATE `MYDB`.`MAIN` as sink " +
                "INNER JOIN `MYDB`.`STAGING` as stage " +
                "ON ((sink.`ID` = stage.`ID`) AND (sink.`NAME` = stage.`NAME`)) AND (sink.`DIGEST` <> stage.`DIGEST`) " +
                "SET sink.`ID` = stage.`ID`," +
                "sink.`NAME` = stage.`NAME`," +
                "sink.`AMOUNT` = stage.`AMOUNT`," +
                "sink.`BIZ_DATE` = stage.`BIZ_DATE`," +
                "sink.`DIGEST` = stage.`DIGEST`," +
                "sink.`BATCH_ID` = (SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MAIN')";

        String insertSql = "INSERT INTO `MYDB`.`MAIN` (`ID`, `NAME`, `AMOUNT`, `BIZ_DATE`, `DIGEST`, `BATCH_ID`) " +
                "(SELECT stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`BIZ_DATE`,stage.`DIGEST`," +
                "(SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MAIN') " +
                "FROM `MYDB`.`STAGING` as stage WHERE NOT (EXISTS (SELECT * FROM `MYDB`.`MAIN` as sink " +
                "WHERE (sink.`ID` = stage.`ID`) " +
                "AND (sink.`NAME` = stage.`NAME`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQueryWithUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQueryWithUpperCase, preActionsSqlList.get(1));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableIngestQueryWithUpperCase, metaIngestSqlList.get(0));
    }

    @Override
    public void verifyNontemporalDeltaWithLessColumnsInStaging(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metaIngestSqlList = operations.metadataIngestSql();

        String updateSql = "UPDATE `mydb`.`main` as sink " +
                "INNER JOIN `mydb`.`staging` as stage " +
                "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` <> stage.`digest`) " +
                "SET sink.`id` = stage.`id`," +
                "sink.`name` = stage.`name`," +
                "sink.`amount` = stage.`amount`," +
                "sink.`digest` = stage.`digest`," +
                "sink.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')";

        String insertSql = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `digest`, `batch_id`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
                "FROM `mydb`.`staging` as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink " +
                "WHERE (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSqlList.get(1));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableIngestQuery, metaIngestSqlList.get(0));
    }

    @Override
    public void verifyNontemporalDeltaPostActionSqlAndCleanStagingData(GeneratorResult operations)
    {
        List<String> postActionsSql = operations.postActionsSql();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(MemsqlTestArtifacts.expectedStagingCleanupQuery);
        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);
    }

    @Override
    public void verifyNontemporalDeltaWithNoVersionAndStagingFilter(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metaIngestSqlList = operations.metadataIngestSql();

        String updateSql = "UPDATE `mydb`.`main` as sink " +
            "INNER JOIN " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest` FROM `mydb`.`staging` as stage " +
            "WHERE (stage.`biz_date` > '2020-01-01') AND (stage.`biz_date` < '2020-01-03')) as stage " +
            "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` <> stage.`digest`) " +
            "SET sink.`id` = stage.`id`,sink.`name` = stage.`name`,sink.`amount` = stage.`amount`,sink.`biz_date` = stage.`biz_date`,sink.`digest` = stage.`digest`," +
            "sink.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')";

        String insertSql = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
                "FROM `mydb`.`staging` as stage WHERE (NOT (EXISTS " +
                "(SELECT * FROM `mydb`.`main` as sink WHERE (sink.`id` = stage.`id`) AND " +
                "(sink.`name` = stage.`name`)))) AND ((stage.`biz_date` > '2020-01-01') AND (stage.`biz_date` < '2020-01-03')))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSqlList.get(1));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithStagingFiltersAndAdditionalMetadata("{\"staging_filters\":{\"biz_date\":{\"LT\":\"2020-01-03\",\"GT\":\"2020-01-01\"}}}"), metaIngestSqlList.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`staging` as stage WHERE (stage.`biz_date` > '2020-01-01') AND (stage.`biz_date` < '2020-01-03')";
        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaWithNoVersionAndFilteredDataset(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metaIngestSqlList = operations.metadataIngestSql();

        String updateSql = "UPDATE `mydb`.`main` as sink " +
            "INNER JOIN " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest` FROM `mydb`.`staging` as stage " +
            "WHERE (stage.`biz_date` > '2020-01-10') OR ((stage.`biz_date` > '2020-01-01') AND (stage.`biz_date` < '2020-01-05'))) as stage " +
            "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` <> stage.`digest`) " +
            "SET sink.`id` = stage.`id`,sink.`name` = stage.`name`,sink.`amount` = stage.`amount`,sink.`biz_date` = stage.`biz_date`,sink.`digest` = stage.`digest`," +
            "sink.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')";

        String insertSql = "INSERT INTO `mydb`.`main` " +
            "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
            "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
            "FROM `mydb`.`staging` as stage WHERE (NOT (EXISTS " +
            "(SELECT * FROM `mydb`.`main` as sink WHERE (sink.`id` = stage.`id`) AND " +
            "(sink.`name` = stage.`name`)))) AND ((stage.`biz_date` > '2020-01-10') OR ((stage.`biz_date` > '2020-01-01') AND (stage.`biz_date` < '2020-01-05'))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSqlList.get(1));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableIngestQueryWithAdditionalMetadata, metaIngestSqlList.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`staging` as stage WHERE (stage.`biz_date` > '2020-01-10') OR ((stage.`biz_date` > '2020-01-01') AND (stage.`biz_date` < '2020-01-05'))";
        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaWithFilterDupsMaxVersionWithStagingFilters(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metaIngestSqlList = operations.metadataIngestSql();

        String updateSql = "UPDATE `mydb`.`main` as sink " +
            "INNER JOIN " +
            "`mydb`.`staging_legend_persistence_temp_staging` as stage " +
            "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (stage.`version` > sink.`version`) " +
            "SET sink.`id` = stage.`id`,sink.`name` = stage.`name`,sink.`amount` = stage.`amount`,sink.`biz_date` = stage.`biz_date`,sink.`digest` = stage.`digest`,sink.`version` = stage.`version`," +
            "sink.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')";

        String insertSql = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `biz_date`, `digest`, `version`, `batch_id`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,stage.`version`," +
            "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
            "FROM `mydb`.`staging_legend_persistence_temp_staging` as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink WHERE (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSqlList.get(1));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithStagingFilters("{\"staging_filters\":{\"snapshot_id\":{\"GT\":18972}}}"), metaIngestSqlList.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`staging` as stage WHERE stage.`snapshot_id` > 18972";
        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaWithNoDedupMaxVersioningWithoutPerformWithStagingFilters(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metaIngestSqlList = operations.metadataIngestSql();

        String updateSql = "UPDATE `mydb`.`main` as sink " +
            "INNER JOIN " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,stage.`version` FROM `mydb`.`staging` as stage WHERE stage.`snapshot_id` > 18972) as stage " +
            "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (stage.`version` > sink.`version`) " +
            "SET sink.`id` = stage.`id`,sink.`name` = stage.`name`,sink.`amount` = stage.`amount`,sink.`biz_date` = stage.`biz_date`,sink.`digest` = stage.`digest`,sink.`version` = stage.`version`," +
            "sink.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')";

        String insertSql = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `version`, `batch_id`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,stage.`version`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
                "FROM `mydb`.`staging` as stage WHERE (NOT (EXISTS " +
                "(SELECT * FROM `mydb`.`main` as sink WHERE (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)))) " +
                "AND (stage.`snapshot_id` > 18972))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSqlList.get(1));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithStagingFilters("{\"staging_filters\":{\"snapshot_id\":{\"GT\":18972}}}"), metaIngestSqlList.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`staging` as stage WHERE stage.`snapshot_id` > 18972";
        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaNoDedupMaxVersionWithoutPerform(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metaIngestSqlList = operations.metadataIngestSql();

        String updateSql = "UPDATE `mydb`.`main` as sink " +
            "INNER JOIN `mydb`.`staging` as stage " +
            "ON ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (stage.`version` > sink.`version`) " +
            "SET sink.`id` = stage.`id`,sink.`name` = stage.`name`,sink.`amount` = stage.`amount`,sink.`biz_date` = stage.`biz_date`,sink.`digest` = stage.`digest`,sink.`version` = stage.`version`," +
            "sink.`batch_id` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')";

        String insertSql = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `biz_date`, `digest`, `version`, `batch_id`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,stage.`version`," +
            "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN') " +
            "FROM `mydb`.`staging` as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM `mydb`.`main` as sink WHERE (sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQuery, preActionsSqlList.get(1));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableIngestQuery, metaIngestSqlList.get(0));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaAllowDuplicatesMaxVersionWithUpperCase(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metaIngestSqlList = operations.metadataIngestSql();

        String updateSql = "UPDATE `MYDB`.`MAIN` as sink " +
            "INNER JOIN " +
            "`MYDB`.`STAGING_LEGEND_PERSISTENCE_TEMP_STAGING` as stage " +
            "ON ((sink.`ID` = stage.`ID`) AND (sink.`NAME` = stage.`NAME`)) AND (stage.`VERSION` >= sink.`VERSION`) " +
            "SET sink.`ID` = stage.`ID`,sink.`NAME` = stage.`NAME`,sink.`AMOUNT` = stage.`AMOUNT`,sink.`BIZ_DATE` = stage.`BIZ_DATE`,sink.`DIGEST` = stage.`DIGEST`,sink.`VERSION` = stage.`VERSION`," +
            "sink.`BATCH_ID` = (SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MAIN')";

        String insertSql = "INSERT INTO `MYDB`.`MAIN` (`ID`, `NAME`, `AMOUNT`, `BIZ_DATE`, `DIGEST`, `VERSION`, `BATCH_ID`) " +
            "(SELECT stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`BIZ_DATE`,stage.`DIGEST`,stage.`VERSION`," +
            "(SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MAIN') " +
            "FROM `MYDB`.`STAGING_LEGEND_PERSISTENCE_TEMP_STAGING` as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM `MYDB`.`MAIN` as sink WHERE (sink.`ID` = stage.`ID`) AND (sink.`NAME` = stage.`NAME`))))";

        Assertions.assertEquals(MemsqlTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQueryUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableCreateQueryWithUpperCase, preActionsSqlList.get(1));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(MemsqlTestArtifacts.expectedMetadataTableIngestQueryWithUpperCase, metaIngestSqlList.get(0));
    }

    public RelationalSink getRelationalSink()
    {
        return MemSqlSink.get();
    }

    protected String getExpectedMetadataTableIngestQueryWithStagingFilters(String stagingFilters)
    {
        return "INSERT INTO batch_metadata " +
            "(`table_name`, `table_batch_id`, `batch_start_ts_utc`, `batch_end_ts_utc`, `batch_status`, `batch_source_info`) " +
            "(SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata " +
            "WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')," +
            "'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE'," +
            String.format("PARSE_JSON('%s'))", stagingFilters);
    }

    protected String getExpectedMetadataTableIngestQueryWithStagingFiltersAndAdditionalMetadata(String stagingFilters)
    {
        return "INSERT INTO batch_metadata " +
            "(`table_name`, `table_batch_id`, `batch_start_ts_utc`, `batch_end_ts_utc`, `batch_status`, `batch_source_info`, `additional_metadata`) " +
            "(SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata " +
            "WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')," +
            "'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE'," +
            String.format("PARSE_JSON('%s'),", stagingFilters) +
            "PARSE_JSON('{\"watermark\":\"my_watermark_value\"}'))";
    }
}
