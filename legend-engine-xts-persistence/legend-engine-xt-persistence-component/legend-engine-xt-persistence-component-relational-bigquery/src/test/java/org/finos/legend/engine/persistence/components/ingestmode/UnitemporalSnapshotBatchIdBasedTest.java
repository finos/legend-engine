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

import org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.testcases.ingestmode.unitemporal.UnitmemporalSnapshotBatchIdBasedTestCases;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.DATA_ERROR_ROWS;
import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.DUPLICATE_ROWS;

public class UnitemporalSnapshotBatchIdBasedTest extends UnitmemporalSnapshotBatchIdBasedTestCases
{
    String incomingRecordCount = "SELECT COUNT(*) as `incomingRecordCount` FROM `mydb`.`staging` as stage";
    String rowsUpdated = "SELECT COUNT(*) as `rowsUpdated` FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1) AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'))))";
    String rowsDeleted = "SELECT 0 as `rowsDeleted`";
    String rowsInserted = "SELECT (SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE sink.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'))-(SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1) AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'))))) as `rowsInserted`";
    String rowsTerminated = "SELECT (SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1)-(SELECT COUNT(*) FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1) AND (EXISTS (SELECT * FROM `mydb`.`main` as sink2 WHERE ((sink2.`id` = sink.`id`) AND (sink2.`name` = sink.`name`)) AND (sink2.`batch_id_in` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'))))) as `rowsTerminated`";

    @Override
    public void verifyUnitemporalSnapshotWithoutPartitionNoDedupNoVersion(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
                "SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1 " +
                "WHERE (sink.`batch_id_out` = 999999999) " +
                "AND (NOT (EXISTS " +
                "(SELECT * FROM `mydb`.`staging` as stage WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`))))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id_in`, `batch_id_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'),999999999 " +
                "FROM `mydb`.`staging` as stage " +
                "WHERE NOT (stage.`digest` IN (SELECT sink.`digest` FROM `mydb`.`main` as sink WHERE sink.`batch_id_out` = 999999999)))";

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalSnapshotWithoutPartitionFailOnDupsNoVersion(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> deduplicationAndVersioningSql = operations.deduplicationAndVersioningSql();
        Map<DedupAndVersionErrorSqlType, String> deduplicationAndVersioningErrorChecksSql = operations.deduplicationAndVersioningErrorChecksSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
                "SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1 " +
                "WHERE (sink.`batch_id_out` = 999999999) " +
                "AND (NOT (EXISTS " +
                "(SELECT * FROM `mydb`.`staging_temp_staging_lp_yosulf` as stage WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`))))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id_in`, `batch_id_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'),999999999 " +
                "FROM `mydb`.`staging_temp_staging_lp_yosulf` as stage " +
                "WHERE NOT (stage.`digest` IN (SELECT sink.`digest` FROM `mydb`.`main` as sink WHERE sink.`batch_id_out` = 999999999)))";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedTempStagingCleanupQuery, deduplicationAndVersioningSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedInsertIntoBaseTempStagingPlusDigestWithFilterDuplicates, deduplicationAndVersioningSql.get(1));
        Assertions.assertEquals(BigQueryTestArtifacts.maxDupsErrorCheckSql, deduplicationAndVersioningErrorChecksSql.get(DedupAndVersionErrorSqlType.MAX_DUPLICATES));
        Assertions.assertEquals(BigQueryTestArtifacts.dupRowsSql, deduplicationAndVersioningErrorChecksSql.get(DUPLICATE_ROWS));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalSnapshotWithoutPartitionWithNoOpEmptyBatchHandling(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        Assertions.assertEquals(BigQueryTestArtifacts.expectedMainTableBatchIdBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(0, milestoningSql.size());
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithoutPartitionWithUpperCaseOptimizer(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `MYDB`.`MAIN` as sink SET sink.`BATCH_ID_OUT` = (SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MAIN')-1 WHERE (sink.`BATCH_ID_OUT` = 999999999) AND (NOT (EXISTS (SELECT * FROM `MYDB`.`STAGING` as stage WHERE ((sink.`ID` = stage.`ID`) AND (sink.`NAME` = stage.`NAME`)) AND (sink.`DIGEST` = stage.`DIGEST`))))";
        String expectedUpsertQuery = "INSERT INTO `MYDB`.`MAIN` (`ID`, `NAME`, `AMOUNT`, `BIZ_DATE`, `DIGEST`, `BATCH_ID_IN`, `BATCH_ID_OUT`) (SELECT stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`BIZ_DATE`,stage.`DIGEST`,(SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MAIN'),999999999 FROM `MYDB`.`STAGING` as stage WHERE NOT (stage.`DIGEST` IN (SELECT sink.`DIGEST` FROM `MYDB`.`MAIN` as sink WHERE sink.`BATCH_ID_OUT` = 999999999)))";
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMainTableBatchIdBasedCreateQueryWithUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMetadataTableCreateQueryWithUpperCase, preActionsSql.get(1));
        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithUpperCase(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionNoDedupNoVersion(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
                "SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1 " +
                "WHERE (sink.`batch_id_out` = 999999999) " +
                "AND (NOT (EXISTS " +
                "(SELECT * FROM `mydb`.`staging` as stage WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`)))) " +
                "AND (EXISTS (SELECT * FROM `mydb`.`staging` as stage WHERE sink.`biz_date` = stage.`biz_date`))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id_in`, `batch_id_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'),999999999 " +
                "FROM `mydb`.`staging` as stage " +
                "WHERE NOT (stage.`digest` IN (SELECT sink.`digest` FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = 999999999) AND (sink.`biz_date` = stage.`biz_date`))))";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedMainTableBatchIdBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionFiltersNoDedupNoVersion(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
                "SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1 " +
                "WHERE (sink.`batch_id_out` = 999999999) " +
                "AND (NOT (EXISTS " +
                "(SELECT * FROM `mydb`.`staging` as stage WHERE ((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`)))) " +
                "AND (sink.`biz_date` IN ('2000-01-01 00:00:00','2000-01-02 00:00:00'))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `digest`, `batch_id_in`, `batch_id_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'),999999999 " +
                "FROM `mydb`.`staging` as stage " +
                "WHERE NOT (stage.`digest` IN (SELECT sink.`digest` FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = 999999999) AND " +
                "(sink.`biz_date` IN ('2000-01-01 00:00:00','2000-01-02 00:00:00')))))";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedMainTableBatchIdBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersion(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink SET " +
                "sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1 " +
                "WHERE (sink.`batch_id_out` = 999999999) AND " +
                "(NOT (EXISTS (SELECT * FROM `mydb`.`staging` as stage WHERE " +
                "((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`)))) AND " +
                "(((sink.`biz_date` = '2024-01-01') AND (sink.`account_type` = 1)) " +
                "OR ((sink.`biz_date` = '2024-01-02') AND (sink.`account_type` = 1)) " +
                "OR ((sink.`biz_date` = '2024-01-02') AND (sink.`account_type` = 2)))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `account_type`, `biz_date`, `digest`, `batch_id_in`, `batch_id_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`account_type`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata " +
                "WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'),999999999 " +
                "FROM `mydb`.`staging` as stage WHERE " +
                "NOT (stage.`digest` IN (SELECT sink.`digest` FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = 999999999) AND " +
                "(((sink.`biz_date` = '2024-01-01') AND (sink.`account_type` = 1)) " +
                "OR ((sink.`biz_date` = '2024-01-02') AND (sink.`account_type` = 1)) " +
                "OR ((sink.`biz_date` = '2024-01-02') AND (sink.`account_type` = 2))))))";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedMainTableWithMultiPartitionCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersionWithDeletePartition(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink SET " +
                "sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1 " +
                "WHERE (sink.`batch_id_out` = 999999999) AND " +
                "(NOT (EXISTS (SELECT * FROM `mydb`.`staging` as stage WHERE " +
                "((sink.`id` = stage.`id`) AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`)))) AND " +
                "(((sink.`biz_date` = '2024-01-01') AND (sink.`account_type` = 1)) " +
                "OR ((sink.`biz_date` = '2024-01-02') AND (sink.`account_type` = 1)) " +
                "OR ((sink.`biz_date` = '2024-01-02') AND (sink.`account_type` = 2)))";

        String expectedDeletePartitionQuery = getExpectedDeletePartitionQuery();

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `account_type`, `biz_date`, `digest`, `batch_id_in`, `batch_id_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`account_type`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata " +
                "WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'),999999999 " +
                "FROM `mydb`.`staging` as stage WHERE " +
                "NOT (stage.`digest` IN (SELECT sink.`digest` FROM `mydb`.`main` as sink WHERE (sink.`batch_id_out` = 999999999) AND " +
                "(((sink.`biz_date` = '2024-01-01') AND (sink.`account_type` = 1)) " +
                "OR ((sink.`biz_date` = '2024-01-02') AND (sink.`account_type` = 1)) " +
                "OR ((sink.`biz_date` = '2024-01-02') AND (sink.`account_type` = 2))))))";

        Assertions.assertEquals(Arrays.asList(BigQueryTestArtifacts.expectedMainTableWithMultiPartitionCreateQuery, BigQueryTestArtifacts.expectedMetadataTableCreateQuery), preActionsSql);
        Assertions.assertEquals(Arrays.asList(expectedMilestoneQuery, expectedDeletePartitionQuery, expectedUpsertQuery), milestoningSql);
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersionWithDeletePartitionWithNoPartitionList(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 " +
                "FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1 " +
                "WHERE (sink.`batch_id_out` = 999999999) AND (NOT (EXISTS (SELECT * FROM `mydb`.`staging` as stage WHERE ((sink.`id` = stage.`id`) " +
                "AND (sink.`name` = stage.`name`)) AND (sink.`digest` = stage.`digest`)))) AND (EXISTS (SELECT * FROM `mydb`.`staging` as stage " +
                "WHERE (sink.`biz_date` = stage.`biz_date`) AND (sink.`account_type` = stage.`account_type`)))";

        String expectedDeletePartitionQuery = getExpectedDeletePartitionQuery();

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `account_type`, `biz_date`, `digest`, `batch_id_in`, `batch_id_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`account_type`,stage.`biz_date`,stage.`digest`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')," +
                "999999999 FROM `mydb`.`staging` as stage WHERE NOT (stage.`digest` IN (SELECT sink.`digest` FROM `mydb`.`main` as sink " +
                "WHERE (sink.`batch_id_out` = 999999999) AND ((sink.`biz_date` = stage.`biz_date`) AND (sink.`account_type` = stage.`account_type`)))))";

        Assertions.assertEquals(Arrays.asList(BigQueryTestArtifacts.expectedMainTableWithMultiPartitionCreateQuery, BigQueryTestArtifacts.expectedMetadataTableCreateQuery), preActionsSql);
        Assertions.assertEquals(Arrays.asList(expectedMilestoneQuery, expectedDeletePartitionQuery, expectedUpsertQuery), milestoningSql);
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersionWithDeletePartitionWithEmptyStaging(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedDeletePartitionQuery = getExpectedDeletePartitionQuery();

        Assertions.assertEquals(Arrays.asList(BigQueryTestArtifacts.expectedMainTableWithMultiPartitionCreateQuery, BigQueryTestArtifacts.expectedMetadataTableCreateQuery), preActionsSql);
        Assertions.assertEquals(Collections.singletonList(expectedDeletePartitionQuery), milestoningSql);
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersionInUpperCase(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `MYDB`.`MAIN` as sink SET " +
                "sink.`BATCH_ID_OUT` = (SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MAIN')-1 " +
                "WHERE (sink.`BATCH_ID_OUT` = 999999999) AND (NOT (EXISTS (SELECT * FROM `MYDB`.`STAGING` as stage " +
                "WHERE ((sink.`ID` = stage.`ID`) AND (sink.`NAME` = stage.`NAME`)) AND (sink.`DIGEST` = stage.`DIGEST`)))) " +
                "AND (((sink.`ACCOUNT_TYPE` = 1) AND (sink.`BIZ_DATE` = '2024-01-01')) " +
                "OR ((sink.`ACCOUNT_TYPE` = 1) AND (sink.`BIZ_DATE` = '2024-01-02')) " +
                "OR ((sink.`ACCOUNT_TYPE` = 2) AND (sink.`BIZ_DATE` = '2024-01-02')))";

        String expectedUpsertQuery = "INSERT INTO `MYDB`.`MAIN` " +
                "(`ID`, `NAME`, `AMOUNT`, `ACCOUNT_TYPE`, `BIZ_DATE`, `DIGEST`, `BATCH_ID_IN`, `BATCH_ID_OUT`) " +
                "(SELECT stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`ACCOUNT_TYPE`,stage.`BIZ_DATE`,stage.`DIGEST`," +
                "(SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MAIN')," +
                "999999999 FROM `MYDB`.`STAGING` as stage WHERE " +
                "NOT (stage.`DIGEST` IN (SELECT sink.`DIGEST` FROM `MYDB`.`MAIN` as sink WHERE (sink.`BATCH_ID_OUT` = 999999999) AND " +
                "(((sink.`ACCOUNT_TYPE` = 1) AND (sink.`BIZ_DATE` = '2024-01-01')) " +
                "OR ((sink.`ACCOUNT_TYPE` = 1) AND (sink.`BIZ_DATE` = '2024-01-02')) " +
                "OR ((sink.`ACCOUNT_TYPE` = 2) AND (sink.`BIZ_DATE` = '2024-01-02'))))))";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedMainTableWithMultiPartitionCreateQueryUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMetadataTableCreateQueryWithUpperCase, preActionsSql.get(1));
        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithUpperCase(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionSpecListWithEmptyBatchHandling(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1 WHERE (sink.`batch_id_out` = 999999999) AND (((sink.`biz_date` = '2024-01-01') AND (sink.`account_type` = 1)) OR ((sink.`biz_date` = '2024-01-02') AND (sink.`account_type` = 1)) OR ((sink.`biz_date` = '2024-01-02') AND (sink.`account_type` = 2)))";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedMainTableWithMultiPartitionCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithCleanStagingData(GeneratorResult operations)
    {
        List<String> postActionsSql = operations.postActionsSql();
        Assertions.assertEquals(BigQueryTestArtifacts.expectedStagingCleanupQuery, postActionsSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionDeleteAllNoDedupNoVersion(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
                "SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1 " +
                "WHERE (sink.`batch_id_out` = 999999999) " +
                "AND (EXISTS (SELECT * FROM `mydb`.`staging` as stage WHERE sink.`biz_date` = stage.`biz_date`))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `batch_id_in`, `batch_id_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'),999999999 " +
                "FROM `mydb`.`staging` as stage)";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedMainTableBatchIdWithoutDigestBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionDeleteAllFilterDuplicatesMaxVersion(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> deduplicationAndVersioningSql = operations.deduplicationAndVersioningSql();
        Map<DedupAndVersionErrorSqlType, String> deduplicationAndVersioningErrorChecksSql = operations.deduplicationAndVersioningErrorChecksSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink SET sink.`batch_id_out` = " +
            "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE " +
            "UPPER(batch_metadata.`table_name`) = 'MAIN')-1 WHERE (sink.`batch_id_out` = 999999999) AND " +
            "(EXISTS (SELECT * FROM `mydb`.`staging_temp_staging_lp_yosulf` as stage WHERE sink.`biz_date` = stage.`biz_date`))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` (`id`, `name`, `amount`, `biz_date`, `batch_id_in`, `batch_id_out`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`," +
            "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE " +
            "UPPER(batch_metadata.`table_name`) = 'MAIN'),999999999 FROM `mydb`.`staging_temp_staging_lp_yosulf` as stage)";

        String maxDataErrorCheckSql = "SELECT MAX(`legend_persistence_distinct_rows`) as `MAX_DATA_ERRORS` FROM " +
            "(SELECT COUNT(DISTINCT(`amount`)) as `legend_persistence_distinct_rows` FROM `mydb`.`staging_temp_staging_lp_yosulf` " +
            "as stage GROUP BY `id`, `name`, `biz_date`) as stage";

        String dataErrorsSql = "SELECT `id`,`name`,`biz_date`,COUNT(DISTINCT(`amount`)) as `legend_persistence_error_count` FROM " +
            "`mydb`.`staging_temp_staging_lp_yosulf` as stage GROUP BY `id`, `name`, `biz_date` HAVING `legend_persistence_error_count` > 1 LIMIT 20";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedMainTableBatchIdWithoutDigestBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(BigQueryTestArtifacts.expectedTempStagingCleanupQuery, deduplicationAndVersioningSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedInsertIntoBaseTempStagingWithMaxVersionAndFilterDuplicates, deduplicationAndVersioningSql.get(1));
        Assertions.assertEquals(maxDataErrorCheckSql, deduplicationAndVersioningErrorChecksSql.get(DedupAndVersionErrorSqlType.MAX_DATA_ERRORS));
        Assertions.assertEquals(dataErrorsSql, deduplicationAndVersioningErrorChecksSql.get(DATA_ERROR_ROWS));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionFiltersDeleteAllNoDedupNoVersion(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink " +
                "SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1 " +
                "WHERE (sink.`batch_id_out` = 999999999) " +
                "AND (sink.`biz_date` IN ('2000-01-01 00:00:00','2000-01-02 00:00:00'))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `biz_date`, `batch_id_in`, `batch_id_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'),999999999 " +
                "FROM `mydb`.`staging` as stage)";
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMainTableBatchIdWithoutDigestBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionSpecListDeleteAllNoDedupNoVersion(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink SET " +
                "sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1 " +
                "WHERE (sink.`batch_id_out` = 999999999) AND " +
                "(((sink.`biz_date` = '2024-01-01') AND (sink.`account_type` = 1)) " +
                "OR ((sink.`biz_date` = '2024-01-02') AND (sink.`account_type` = 1)) " +
                "OR ((sink.`biz_date` = '2024-01-02') AND (sink.`account_type` = 2)))";

        String expectedUpsertQuery = "INSERT INTO `mydb`.`main` " +
                "(`id`, `name`, `amount`, `account_type`, `biz_date`, `batch_id_in`, `batch_id_out`) " +
                "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`account_type`,stage.`biz_date`," +
                "(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata " +
                "WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'),999999999 " +
                "FROM `mydb`.`staging` as stage)";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedMainTableWithMultiPartitionWithoutDigestCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionSpecListDeleteAllNoDedupNoVersionInUpperCase(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `MYDB`.`MAIN` as sink SET " +
                "sink.`BATCH_ID_OUT` = (SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MAIN')-1 " +
                "WHERE (sink.`BATCH_ID_OUT` = 999999999) " +
                "AND (((sink.`ACCOUNT_TYPE` = 1) AND (sink.`BIZ_DATE` = '2024-01-01')) " +
                "OR ((sink.`ACCOUNT_TYPE` = 1) AND (sink.`BIZ_DATE` = '2024-01-02')) " +
                "OR ((sink.`ACCOUNT_TYPE` = 2) AND (sink.`BIZ_DATE` = '2024-01-02')))";

        String expectedUpsertQuery = "INSERT INTO `MYDB`.`MAIN` " +
                "(`ID`, `NAME`, `AMOUNT`, `ACCOUNT_TYPE`, `BIZ_DATE`, `BATCH_ID_IN`, `BATCH_ID_OUT`) " +
                "(SELECT stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`ACCOUNT_TYPE`,stage.`BIZ_DATE`," +
                "(SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MAIN')," +
                "999999999 FROM `MYDB`.`STAGING` as stage)";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedMainTableWithMultiPartitionWithoutDigestCreateQueryUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMetadataTableCreateQueryWithUpperCase, preActionsSql.get(1));
        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithUpperCase(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionSpecListDeleteAllWithEmptyBatchHandling(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE `mydb`.`main` as sink SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1 WHERE (sink.`batch_id_out` = 999999999) AND (((sink.`biz_date` = '2024-01-01') AND (sink.`account_type` = 1)) OR ((sink.`biz_date` = '2024-01-02') AND (sink.`account_type` = 1)) OR ((sink.`biz_date` = '2024-01-02') AND (sink.`account_type` = 2)))";

        Assertions.assertEquals(BigQueryTestArtifacts.expectedMainTableWithMultiPartitionWithoutDigestCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(BigQueryTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public RelationalSink getRelationalSink()
    {
        return BigQuerySink.get();
    }

    protected String getExpectedMetadataTableIngestQuery()
    {
        return BigQueryTestArtifacts.expectedMetadataTableIngestQuery;
    }

    protected String getExpectedMetadataTableIngestQueryWithUpperCase()
    {
        return BigQueryTestArtifacts.expectedMetadataTableIngestQueryWithUpperCase;
    }

    protected String getExpectedDeletePartitionQuery()
    {
        return "UPDATE `mydb`.`main` as sink SET sink.`batch_id_out` = (SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1" +
                " FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN')-1 WHERE (sink.`batch_id_out` = 999999999)" +
                " AND (EXISTS (SELECT * FROM `mydb`.`delete_partition` as delete_partition_alias WHERE (sink.`biz_date` = delete_partition_alias.`biz_date`)" +
                " AND (sink.`account_type` = delete_partition_alias.`account_type`)))";
    }
}
