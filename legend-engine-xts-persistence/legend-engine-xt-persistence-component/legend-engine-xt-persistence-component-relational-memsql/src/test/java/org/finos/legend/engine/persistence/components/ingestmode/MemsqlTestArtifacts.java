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

public class MemsqlTestArtifacts
{
    public static String expectedBaseTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`batch_id` INTEGER," +
            "PRIMARY KEY (`id`, `name`))";

    public static String expectedStagingTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`staging`(" +
            "`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "PRIMARY KEY (`id`, `name`))";

    public static String expectedBaseTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
            "(`ID` INTEGER NOT NULL," +
            "`NAME` VARCHAR(256) NOT NULL," +
            "`AMOUNT` DOUBLE," +
            "`BIZ_DATE` DATE," +
            "`BATCH_ID` INTEGER," +
            "PRIMARY KEY (`ID`, `NAME`))";

    public static String expectedBaseTablePlusDigestCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`digest` VARCHAR(256)," +
            "`batch_id` INTEGER," +
            "PRIMARY KEY (`id`, `name`))";

    public static String expectedStagingTableWithDigestCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`staging`(" +
            "`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`digest` VARCHAR(256)," +
            "PRIMARY KEY (`id`, `name`))";

    public static String expectedBaseTablePlusDigestPlusVersionCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`digest` VARCHAR(256)," +
            "`version` INTEGER," +
            "`batch_id` INTEGER," +
            "PRIMARY KEY (`id`, `name`))";

    public static String expectedBaseTempStagingTableWithCount = "CREATE TABLE IF NOT EXISTS `mydb`.`staging_temp_staging_lp_yosulf`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`legend_persistence_count` INTEGER)";

    public static String expectedBaseTempStagingTableWithVersionAndCount = "CREATE TABLE IF NOT EXISTS `mydb`.`staging_temp_staging_lp_yosulf`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`digest` VARCHAR(256)," +
            "`version` INTEGER," +
            "`legend_persistence_count` INTEGER)";

    public static String expectedBaseTempStagingTablePlusDigestWithCount = "CREATE TABLE IF NOT EXISTS `mydb`.`staging_temp_staging_lp_yosulf`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`digest` VARCHAR(256)," +
            "`legend_persistence_count` INTEGER)";

    public static String expectedBaseTempStagingTablePlusDigestWithCountAndDataSplit = "CREATE TABLE IF NOT EXISTS `mydb`.`staging_temp_staging_lp_yosulf`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`digest` VARCHAR(256)," +
            "`legend_persistence_count` INTEGER," +
            "`data_split` INTEGER NOT NULL)";

    public static String expectedBaseTablePlusDigestPlusVersionCreateQueryUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`(" +
            "`ID` INTEGER NOT NULL," +
            "`NAME` VARCHAR(256) NOT NULL," +
            "`AMOUNT` DOUBLE," +
            "`BIZ_DATE` DATE," +
            "`DIGEST` VARCHAR(256)," +
            "`VERSION` INTEGER," +
            "`BATCH_ID` INTEGER," +
            "PRIMARY KEY (`ID`, `NAME`))";

    public static String expectedBaseTablePlusDigestCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`(" +
            "`ID` INTEGER NOT NULL," +
            "`NAME` VARCHAR(256) NOT NULL," +
            "`AMOUNT` DOUBLE," +
            "`BIZ_DATE` DATE," +
            "`DIGEST` VARCHAR(256)," +
            "`BATCH_ID` INTEGER," +
            "PRIMARY KEY (`ID`, `NAME`))";

    public static String expectedBaseTableCreateQueryWithNoPKs = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER," +
            "`name` VARCHAR(256)," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`digest` VARCHAR(256)," +
            "`batch_id` INTEGER)";

    public static String expectedStagingTableCreateQueryWithNoPKs = "CREATE TABLE IF NOT EXISTS `mydb`.`staging`(" +
            "`id` INTEGER," +
            "`name` VARCHAR(256)," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`digest` VARCHAR(256))";

    public static String expectedMainTableBatchIdAndVersionBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL,`name` VARCHAR(256) NOT NULL,`amount` DOUBLE,`biz_date` DATE,`digest` VARCHAR(256),`version` INTEGER," +
            "`batch_id_in` INTEGER NOT NULL,`batch_id_out` INTEGER,PRIMARY KEY (`id`, `name`, `batch_id_in`))";

    public static String expectedMainTableBatchIdAndVersionBasedCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
            "(`ID` INTEGER NOT NULL,`NAME` VARCHAR(256) NOT NULL,`AMOUNT` DOUBLE,`BIZ_DATE` DATE,`DIGEST` VARCHAR(256),`VERSION` INTEGER,`BATCH_ID_IN` INTEGER NOT NULL," +
            "`BATCH_ID_OUT` INTEGER,PRIMARY KEY (`ID`, `NAME`, `BATCH_ID_IN`))";

    public static String expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`digest` VARCHAR(256)," +
            "`batch_update_time` DATETIME NOT NULL," +
            "`batch_id` INTEGER," +
            "PRIMARY KEY (`id`, `name`, `batch_update_time`))";

    public static String expectedBaseTablePlusDigestPlusUpdateTimestampCreateQueryUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`(" +
            "`ID` INTEGER NOT NULL," +
            "`NAME` VARCHAR(256) NOT NULL," +
            "`AMOUNT` DOUBLE," +
            "`BIZ_DATE` DATE," +
            "`DIGEST` VARCHAR(256)," +
            "`BATCH_UPDATE_TIME` DATETIME NOT NULL," +
            "`BATCH_ID` INTEGER," +
            "PRIMARY KEY (`ID`, `NAME`, `BATCH_UPDATE_TIME`))";

    public static String expectedBaseTablePlusDigestPlusUpdateTimestampAndBatchNumberCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
        "`id` INTEGER NOT NULL," +
        "`name` VARCHAR(256) NOT NULL," +
        "`amount` DOUBLE," +
        "`biz_date` DATE," +
        "`digest` VARCHAR(256)," +
        "`batch_update_time` DATETIME NOT NULL," +
        "`batch_number` INTEGER," +
        "PRIMARY KEY (`id`, `name`, `batch_update_time`))";

    public static String expectedBaseTableWithAuditPKCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`batch_update_time` DATETIME NOT NULL," +
            "`batch_id` INTEGER," +
            "PRIMARY KEY (`id`, `name`, `batch_update_time`))";

    public static String expectedStagingCleanupQuery = "DELETE FROM `mydb`.`staging` as stage";
    public static String expectedTempStagingCleanupQuery = "DELETE FROM `mydb`.`staging_temp_staging_lp_yosulf` as stage";

    public static String expectedDropTableQuery = "DROP TABLE IF EXISTS `mydb`.`staging` CASCADE";

    public static String cleanUpMainTableSql = "DELETE FROM `mydb`.`main` as sink";
    public static String cleanupMainTableSqlUpperCase = "DELETE FROM `MYDB`.`MAIN` as sink";

    public static String expectedMainTableBatchIdBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL,`name` VARCHAR(256) NOT NULL,`amount` DOUBLE,`biz_date` DATE,`digest` VARCHAR(256)," +
            "`batch_id_in` INTEGER NOT NULL,`batch_id_out` INTEGER,PRIMARY KEY (`id`, `name`, `batch_id_in`))";

    public static String expectedMainTableMultiPartitionKeysCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL,`name` VARCHAR(256) NOT NULL,`amount` DOUBLE,`account_type` INTEGER,`biz_date` DATE,`digest` VARCHAR(256)," +
            "`batch_id_in` INTEGER NOT NULL,`batch_id_out` INTEGER,PRIMARY KEY (`id`, `name`, `batch_id_in`))";

    public static String expectedMetadataTableCreateQuery = "CREATE TABLE IF NOT EXISTS batch_metadata" +
            "(`table_name` VARCHAR(255)," +
            "`batch_start_ts_utc` DATETIME," +
            "`batch_end_ts_utc` DATETIME," +
            "`batch_status` VARCHAR(32)," +
            "`table_batch_id` INTEGER," +
            "`ingest_request_id` VARCHAR(64)," +
            "`batch_source_info` JSON," +
            "`additional_metadata` JSON," +
            "`batch_statistics` JSON)";

    public static String expectedMetadataTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS BATCH_METADATA" +
            "(`TABLE_NAME` VARCHAR(255)," +
            "`BATCH_START_TS_UTC` DATETIME," +
            "`BATCH_END_TS_UTC` DATETIME," +
            "`BATCH_STATUS` VARCHAR(32)," +
            "`TABLE_BATCH_ID` INTEGER," +
            "`INGEST_REQUEST_ID` VARCHAR(64)," +
            "`BATCH_SOURCE_INFO` JSON," +
            "`ADDITIONAL_METADATA` JSON," +
            "`BATCH_STATISTICS` JSON)";

    public static String expectedMainTableBatchIdBasedCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
            "(`ID` INTEGER NOT NULL,`NAME` VARCHAR(256) NOT NULL,`AMOUNT` DOUBLE,`BIZ_DATE` DATE,`DIGEST` VARCHAR(256)," +
            "`BATCH_ID_IN` INTEGER NOT NULL,`BATCH_ID_OUT` INTEGER,PRIMARY KEY (`ID`, `NAME`, `BATCH_ID_IN`))";

    public static String expectedMainTableMultiPartitionsCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
            "(`ID` INTEGER NOT NULL,`NAME` VARCHAR(256) NOT NULL,`AMOUNT` DOUBLE,`ACCOUNT_TYPE` INTEGER,`BIZ_DATE` DATE,`DIGEST` VARCHAR(256)," +
            "`BATCH_ID_IN` INTEGER NOT NULL,`BATCH_ID_OUT` INTEGER,PRIMARY KEY (`ID`, `NAME`, `BATCH_ID_IN`))";

    public static String expectedMetadataTableIngestQuery = "INSERT INTO batch_metadata (`table_name`, `table_batch_id`, `batch_start_ts_utc`, `batch_end_ts_utc`, `batch_status`)" +
            " (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'),'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE')";

    public static String expectedMetadataTableIngestQueryWithUpperCase = "INSERT INTO BATCH_METADATA (`TABLE_NAME`, `TABLE_BATCH_ID`, `BATCH_START_TS_UTC`, `BATCH_END_TS_UTC`, `BATCH_STATUS`)" +
            " (SELECT 'MAIN',(SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MAIN'),'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE')";

    public static String expectedMetadataTableIngestQueryWithPlaceHolders = "INSERT INTO batch_metadata (`table_name`, `table_batch_id`, `batch_start_ts_utc`, `batch_end_ts_utc`, `batch_status`) " +
            "(SELECT 'main',{BATCH_ID_PATTERN},'{BATCH_START_TS_PATTERN}','{BATCH_END_TS_PATTERN}','DONE')";

    public static String expectedMetadataTableIngestQueryWithBatchSuccessValue = "INSERT INTO batch_metadata (`table_name`, `table_batch_id`, `batch_start_ts_utc`, `batch_end_ts_utc`, `batch_status`)" +
        " (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'),'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'SUCCEEDED')";

    public static String expectedMetadataTableIngestQueryWithAdditionalMetadata = "INSERT INTO batch_metadata " +
        "(`table_name`, `table_batch_id`, `batch_start_ts_utc`, `batch_end_ts_utc`, `batch_status`, `additional_metadata`)" +
        " (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata " +
        "WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'),'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE'," +
        "PARSE_JSON('{\"watermark\":\"my_watermark_value\"}'))";

    public static String expectedMetadataTableIngestQueryWithAdditionalMetadataWithUpperCase = "INSERT INTO BATCH_METADATA " +
        "(`TABLE_NAME`, `TABLE_BATCH_ID`, `BATCH_START_TS_UTC`, `BATCH_END_TS_UTC`, `BATCH_STATUS`, `ADDITIONAL_METADATA`)" +
        " (SELECT 'MAIN',(SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA " +
        "WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MAIN'),'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE'," +
        "PARSE_JSON('{\"watermark\":\"my_watermark_value\"}'))";

    public static String expectedMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`biz_date` DATE," +
            "`digest` VARCHAR(256)," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`batch_time_in` DATETIME," +
            "`batch_time_out` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`))";

    public static String expectedMainTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
            "(`ID` INTEGER NOT NULL," +
            "`NAME` VARCHAR(256) NOT NULL," +
            "`AMOUNT` DOUBLE," +
            "`BIZ_DATE` DATE," +
            "`DIGEST` VARCHAR(256)," +
            "`BATCH_ID_IN` INTEGER NOT NULL," +
            "`BATCH_ID_OUT` INTEGER," +
            "`BATCH_TIME_IN` DATETIME," +
            "`BATCH_TIME_OUT` DATETIME," +
            "PRIMARY KEY (`ID`, `NAME`, `BATCH_ID_IN`))";

    public static String expectedMainTableTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL,`name` VARCHAR(256) NOT NULL,`amount` DOUBLE,`biz_date` DATE,`digest` VARCHAR(256)," +
            "`batch_time_in` DATETIME NOT NULL,`batch_time_out` DATETIME,PRIMARY KEY (`id`, `name`, `batch_time_in`))";

    public static String expectedMainTableTimeBasedCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
            "(`ID` INTEGER NOT NULL,`NAME` VARCHAR(256) NOT NULL,`AMOUNT` DOUBLE,`BIZ_DATE` DATE,`DIGEST` VARCHAR(256)," +
            "`BATCH_TIME_IN` DATETIME NOT NULL,`BATCH_TIME_OUT` DATETIME,PRIMARY KEY (`ID`, `NAME`, `BATCH_TIME_IN`))";

    public static String expectedBitemporalMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`digest` VARCHAR(256)," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`))";

    public static String expectedBitemporalStagingTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`staging`(" +
            "`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`validity_from_reference` DATETIME NOT NULL," +
            "`validity_through_reference` DATETIME," +
            "`digest` VARCHAR(256)," +
            "PRIMARY KEY (`id`, `name`, `validity_from_reference`))";

    public static String expectedBitemporalMainTableWithVersionWithBatchIdDatetimeCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`digest` VARCHAR(256)," +
            "`version` INTEGER," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`batch_time_in` DATETIME," +
            "`batch_time_out` DATETIME," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`))";

    public static String expectedBitemporalMainTableWithVersionBatchDateTimeCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`digest` VARCHAR(256)," +
            "`version` INTEGER," +
            "`batch_time_in` DATETIME NOT NULL," +
            "`batch_time_out` DATETIME," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_time_in`, `validity_from_target`))";

    public static String expectedBitemporalFromOnlyMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`digest` VARCHAR(256)," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`))";

    public static String expectedBitemporalFromOnlyMainTableWithVersionCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`digest` VARCHAR(256)," +
            "`version` INTEGER," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`))";

    public static String expectedBitemporalFromOnlyStagingTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`staging`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE,`validity_from_reference` DATETIME NOT NULL," +
            "`digest` VARCHAR(256)," +
            "PRIMARY KEY (`id`, `name`, `validity_from_reference`))";

    public static String expectedBitemporalFromOnlyMainTableBatchIdAndTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`digest` VARCHAR(256)," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`batch_time_in` DATETIME," +
            "`batch_time_out` DATETIME," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`))";

    public static String expectedBitemporalFromOnlyMainTableDateTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`digest` VARCHAR(256)," +
            "`batch_time_in` DATETIME NOT NULL," +
            "`batch_time_out` DATETIME," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_time_in`, `validity_from_target`))";

    public static String expectedBitemporalMainTableCreateQueryUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
            "(`ID` INTEGER NOT NULL," +
            "`NAME` VARCHAR(256) NOT NULL," +
            "`AMOUNT` DOUBLE," +
            "`DIGEST` VARCHAR(256)," +
            "`BATCH_ID_IN` INTEGER NOT NULL," +
            "`BATCH_ID_OUT` INTEGER," +
            "`VALIDITY_FROM_TARGET` DATETIME NOT NULL," +
            "`VALIDITY_THROUGH_TARGET` DATETIME," +
            "PRIMARY KEY (`ID`, `NAME`, `BATCH_ID_IN`, `VALIDITY_FROM_TARGET`))";

    public static String expectedBitemporalFromOnlyTempTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`temp`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`digest` VARCHAR(256)," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`))";

    public static String expectedBitemporalFromOnlyTempTableWithVersionCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`temp`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`digest` VARCHAR(256)," +
            "`version` INTEGER," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`))";

    public static String expectedBitemporalFromOnlyTempTableBatchIdAndTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`temp`(" +
            "`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`digest` VARCHAR(256)," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`batch_time_in` DATETIME," +
            "`batch_time_out` DATETIME," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`))";

    public static String expectedBitemporalFromOnlyTempTableDateTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`temp`(" +
            "`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`digest` VARCHAR(256)," +
            "`batch_time_in` DATETIME NOT NULL," +
            "`batch_time_out` DATETIME," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_time_in`, `validity_from_target`))";

    public static String expectedBitemporalFromOnlyStageWithoutDuplicatesTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`stagingWithoutDuplicates`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`validity_from_reference` DATETIME NOT NULL," +
            "`digest` VARCHAR(256)," +
            "PRIMARY KEY (`id`, `name`, `validity_from_reference`))";

    public static String expectedBitemporalFromOnlyTempTableWithDeleteIndicatorCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`tempWithDeleteIndicator`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`digest` VARCHAR(256)," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "`delete_indicator` VARCHAR(256)," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`))";

    public static String expectedBitemporalFromOnlyStageWithVersionWithDataSplitWithoutDuplicatesTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`stagingWithoutDuplicates`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`validity_from_reference` DATETIME NOT NULL," +
            "`digest` VARCHAR(256)," +
            "`version` INTEGER," +
            "`data_split` BIGINT NOT NULL," +
            "PRIMARY KEY (`id`, `name`, `validity_from_reference`, `data_split`))";

    public static String expectedBitemporalFromOnlyStageWithDeleteIndicatorWithoutDuplicatesTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`stagingWithoutDuplicates`" +
            "(`id` INTEGER NOT NULL," +
            "`name` VARCHAR(256) NOT NULL," +
            "`amount` DOUBLE," +
            "`validity_from_reference` DATETIME NOT NULL," +
            "`digest` VARCHAR(256)," +
            "`delete_indicator` VARCHAR(256)," +
            "PRIMARY KEY (`id`, `name`, `validity_from_reference`))";

    public static String expectedInsertIntoBaseTempStagingWithMaxVersionAndFilterDuplicates = "INSERT INTO `mydb`.`staging_temp_staging_lp_yosulf` " +
            "(`id`, `name`, `amount`, `biz_date`, `legend_persistence_count`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`legend_persistence_count` as `legend_persistence_count` FROM " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`legend_persistence_count` as `legend_persistence_count`," +
            "DENSE_RANK() OVER (PARTITION BY stage.`id`,stage.`name` ORDER BY stage.`biz_date` DESC) as `legend_persistence_rank` FROM " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,COUNT(*) as `legend_persistence_count` FROM " +
            "`mydb`.`staging` as stage GROUP BY stage.`id`, stage.`name`, stage.`amount`, stage.`biz_date`) as stage) " +
            "as stage WHERE stage.`legend_persistence_rank` = 1)";

    public static String expectedInsertIntoBaseTempStagingPlusDigestWithFilterDuplicates = "INSERT INTO `mydb`.`staging_temp_staging_lp_yosulf` " +
            "(`id`, `name`, `amount`, `biz_date`, `digest`, `legend_persistence_count`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`," +
            "COUNT(*) as `legend_persistence_count` FROM `mydb`.`staging` as stage " +
            "GROUP BY stage.`id`, stage.`name`, stage.`amount`, stage.`biz_date`, stage.`digest`)";

    public static String expectedInsertIntoBaseTempStagingPlusDigestWithMaxVersionAndFilterDuplicates = "INSERT INTO `mydb`.`staging_temp_staging_lp_yosulf` " +
            "(`id`, `name`, `amount`, `biz_date`, `digest`, `legend_persistence_count`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,stage.`legend_persistence_count` as `legend_persistence_count` FROM " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,stage.`legend_persistence_count` as `legend_persistence_count`,DENSE_RANK() OVER " +
            "(PARTITION BY stage.`id`,stage.`name` ORDER BY stage.`biz_date` DESC) as `legend_persistence_rank` FROM " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,COUNT(*) as `legend_persistence_count` FROM " +
            "`mydb`.`staging` as stage GROUP BY stage.`id`, stage.`name`, stage.`amount`, stage.`biz_date`, stage.`digest`) as stage) as stage " +
            "WHERE stage.`legend_persistence_rank` = 1)";

    public static String expectedInsertIntoBaseTempStagingPlusDigestWithAllVersionAndFilterDuplicates = "INSERT INTO `mydb`.`staging_temp_staging_lp_yosulf` " +
            "(`id`, `name`, `amount`, `biz_date`, `digest`, `legend_persistence_count`, `data_split`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,stage.`legend_persistence_count` as `legend_persistence_count`,DENSE_RANK() OVER (PARTITION BY stage.`id`,stage.`name` ORDER BY stage.`biz_date` ASC) as `data_split` " +
            "FROM (SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,COUNT(*) as `legend_persistence_count` FROM `mydb`.`staging` as stage " +
            "GROUP BY stage.`id`, stage.`name`, stage.`amount`, stage.`biz_date`, stage.`digest`) as stage)";

    public static String maxDupsErrorCheckSql = "SELECT MAX(stage.`legend_persistence_count`) as `MAX_DUPLICATES` FROM " +
            "`mydb`.`staging_temp_staging_lp_yosulf` as stage";

    public static String maxPkDupsErrorCheckSql = "SELECT MAX(`legend_persistence_pk_count`) as `MAX_PK_DUPLICATES` FROM " +
        "(SELECT COUNT(*) as `legend_persistence_pk_count` FROM `mydb`.`staging_temp_staging_lp_yosulf` as stage GROUP BY `id`, `name`) as stage";

    public static String dupPkRowsSql = "SELECT `id`,`name`,COUNT(*) as `legend_persistence_pk_count` FROM " +
        "`mydb`.`staging_temp_staging_lp_yosulf` as stage GROUP BY `id`, `name` HAVING `legend_persistence_pk_count` > 1 LIMIT 20";

    public static String dataErrorCheckSqlForBizDateAsVersion = "SELECT MAX(`legend_persistence_distinct_rows`) as `MAX_DATA_ERRORS` FROM " +
            "(SELECT COUNT(DISTINCT(`digest`)) as `legend_persistence_distinct_rows` FROM " +
            "`mydb`.`staging_temp_staging_lp_yosulf` as stage GROUP BY `id`, `name`, `biz_date`) as stage";

    public static String dataErrorCheckSqlForVersionAsVersion = "SELECT MAX(`legend_persistence_distinct_rows`) as `MAX_DATA_ERRORS` FROM " +
        "(SELECT COUNT(DISTINCT(`digest`)) as `legend_persistence_distinct_rows` FROM " +
        "`mydb`.`staging_temp_staging_lp_yosulf` as stage GROUP BY `id`, `name`, `version`) as stage";

    public static String expectedInsertIntoBaseTempStagingPlusDigestWithMaxVersionAndAllowDuplicates = "INSERT INTO `mydb`.`staging_temp_staging_lp_yosulf` " +
            "(`id`, `name`, `amount`, `biz_date`, `digest`) " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest` FROM " +
            "(SELECT stage.`id`,stage.`name`,stage.`amount`,stage.`biz_date`,stage.`digest`,DENSE_RANK() " +
            "OVER (PARTITION BY stage.`id`,stage.`name` ORDER BY stage.`biz_date` DESC) as `legend_persistence_rank` " +
            "FROM `mydb`.`staging` as stage) as stage WHERE stage.`legend_persistence_rank` = 1)";

    public static String expectedTempStagingCleanupQueryInUpperCase = "DELETE FROM `MYDB`.`STAGING_TEMP_STAGING_LP_YOSULF` as stage";
    public static String expectedInsertIntoBaseTempStagingPlusDigestWithMaxVersionAndAllowDuplicatesUpperCase = "INSERT INTO `MYDB`.`STAGING_TEMP_STAGING_LP_YOSULF` " +
            "(`ID`, `NAME`, `AMOUNT`, `BIZ_DATE`, `DIGEST`, `LEGEND_PERSISTENCE_COUNT`) " +
            "(SELECT stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`BIZ_DATE`,stage.`DIGEST`,stage.`LEGEND_PERSISTENCE_COUNT` as `LEGEND_PERSISTENCE_COUNT` " +
            "FROM (SELECT stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`BIZ_DATE`,stage.`DIGEST`," +
            "stage.`LEGEND_PERSISTENCE_COUNT` as `LEGEND_PERSISTENCE_COUNT`," +
            "DENSE_RANK() OVER (PARTITION BY stage.`ID`,stage.`NAME` ORDER BY stage.`BIZ_DATE` DESC) as `LEGEND_PERSISTENCE_RANK` " +
            "FROM (SELECT stage.`ID`,stage.`NAME`,stage.`AMOUNT`,stage.`BIZ_DATE`,stage.`DIGEST`,COUNT(*) as `LEGEND_PERSISTENCE_COUNT` " +
            "FROM `MYDB`.`STAGING` as stage GROUP BY stage.`ID`, stage.`NAME`, stage.`AMOUNT`, stage.`BIZ_DATE`, stage.`DIGEST`) as stage) as stage WHERE stage.`LEGEND_PERSISTENCE_RANK` = 1)";
    public static String dataErrorCheckSqlUpperCase = "SELECT MAX(`LEGEND_PERSISTENCE_DISTINCT_ROWS`) as `MAX_DATA_ERRORS` " +
            "FROM (SELECT COUNT(DISTINCT(`DIGEST`)) as `LEGEND_PERSISTENCE_DISTINCT_ROWS` " +
            "FROM `MYDB`.`STAGING_TEMP_STAGING_LP_YOSULF` as stage GROUP BY `ID`, `NAME`, `BIZ_DATE`) as stage";

    public static String dupRowsSql = "SELECT `id`,`name`,`legend_persistence_count` FROM `mydb`.`staging_temp_staging_lp_yosulf` as stage " +
            "WHERE stage.`legend_persistence_count` > 1 LIMIT 20";

    public static String dataErrorsSqlWithBizDateVersion = "SELECT `id`,`name`,`biz_date`,COUNT(DISTINCT(`digest`)) as `legend_persistence_error_count` FROM " +
            "`mydb`.`staging_temp_staging_lp_yosulf` as stage GROUP BY `id`, `name`, `biz_date` HAVING `legend_persistence_error_count` > 1 LIMIT 20";

    public static String dataErrorsSqlWithBizDateVersionUpperCase = "SELECT `ID`,`NAME`,`BIZ_DATE`,COUNT(DISTINCT(`DIGEST`)) as `LEGEND_PERSISTENCE_ERROR_COUNT` FROM `MYDB`.`STAGING_TEMP_STAGING_LP_YOSULF` " +
            "as stage GROUP BY `ID`, `NAME`, `BIZ_DATE` HAVING `LEGEND_PERSISTENCE_ERROR_COUNT` > 1 LIMIT 20";

    public static String dataErrorsSql = "SELECT `id`,`name`,`version`,COUNT(DISTINCT(`digest`)) as `legend_persistence_error_count` FROM " +
            "`mydb`.`staging_temp_staging_lp_yosulf` as stage GROUP BY `id`, `name`, `version` HAVING `legend_persistence_error_count` > 1 LIMIT 20";
}