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

public class BigQueryTestArtifacts
{
    public static String expectedBaseTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`biz_date` DATE," +
            "PRIMARY KEY (`id`, `name`) NOT ENFORCED)";

    public static String expectedBaseTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
            "(`ID` INTEGER NOT NULL," +
            "`NAME` STRING NOT NULL," +
            "`AMOUNT` FLOAT," +
            "`BIZ_DATE` DATE," +
            "PRIMARY KEY (`ID`, `NAME`) NOT ENFORCED)";

    public static String expectedBaseTablePlusDigestCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`biz_date` DATE," +
            "`digest` STRING," +
            "PRIMARY KEY (`id`, `name`) NOT ENFORCED)";

    public static String expectedBaseTablePlusDigestPlusVersionCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
        "`id` INTEGER NOT NULL," +
        "`name` STRING NOT NULL," +
        "`amount` FLOAT," +
        "`biz_date` DATE," +
        "`digest` STRING," +
        "`version` INTEGER," +
        "PRIMARY KEY (`id`, `name`) NOT ENFORCED)";

    public static String expectedBaseTablePlusDigestPlusVersionCreateQueryUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`(" +
        "`ID` INTEGER NOT NULL," +
        "`NAME` STRING NOT NULL," +
        "`AMOUNT` FLOAT," +
        "`BIZ_DATE` DATE," +
        "`DIGEST` STRING," +
        "`VERSION` INTEGER," +
        "PRIMARY KEY (`ID`, `NAME`) NOT ENFORCED)";

    public static String expectedBaseTablePlusDigestCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`(" +
            "`ID` INTEGER NOT NULL," +
            "`NAME` STRING NOT NULL," +
            "`AMOUNT` FLOAT," +
            "`BIZ_DATE` DATE," +
            "`DIGEST` STRING," +
            "PRIMARY KEY (`ID`, `NAME`) NOT ENFORCED)";

    public static String expectedBaseTableCreateQueryWithNoPKs = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER," +
            "`name` STRING," +
            "`amount` FLOAT," +
            "`biz_date` DATE," +
            "`digest` STRING)";

    public static String expectedBaseTableCreateQueryWithAuditAndNoPKs = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER,`name` STRING,`amount` FLOAT,`biz_date` DATE,`digest` STRING,`batch_update_time` DATETIME)";

    public static String expectedMainTableBatchIdAndVersionBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL,`name` STRING NOT NULL,`amount` FLOAT,`biz_date` DATE,`digest` STRING,`version` INTEGER," +
            "`batch_id_in` INTEGER NOT NULL,`batch_id_out` INTEGER,PRIMARY KEY (`id`, `name`, `batch_id_in`) NOT ENFORCED)";

    public static String expectedMainTableBatchIdAndVersionBasedCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
            "(`ID` INTEGER NOT NULL,`NAME` STRING NOT NULL,`AMOUNT` FLOAT,`BIZ_DATE` DATE,`DIGEST` STRING,`VERSION` INTEGER,`BATCH_ID_IN` INTEGER NOT NULL," +
            "`BATCH_ID_OUT` INTEGER,PRIMARY KEY (`ID`, `NAME`, `BATCH_ID_IN`) NOT ENFORCED)";

    public static String expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`biz_date` DATE," +
            "`digest` STRING," +
            "`batch_update_time` DATETIME NOT NULL," +
            "PRIMARY KEY (`id`, `name`, `batch_update_time`) NOT ENFORCED)";

    public static String expectedBaseTableWithAuditNotPKCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`biz_date` DATE," +
            "`digest` STRING," +
            "`batch_update_time` DATETIME," +
            "PRIMARY KEY (`id`, `name`) NOT ENFORCED)";

    public static String expectedBaseTableWithAuditPKCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`biz_date` DATE," +
            "`batch_update_time` DATETIME NOT NULL," +
            "PRIMARY KEY (`id`, `name`, `batch_update_time`) NOT ENFORCED)";

    public static String expectedStagingCleanupQuery = "TRUNCATE TABLE `mydb`.`staging`";

    public static String expectedDropTableQuery = "DROP TABLE IF EXISTS `mydb`.`staging` CASCADE";

    public static String cleanUpMainTableSql = "TRUNCATE TABLE `mydb`.`main`";
    public static String cleanupMainTableSqlUpperCase = "TRUNCATE TABLE `MYDB`.`MAIN`";

    public static String expectedMainTableBatchIdBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL,`name` STRING NOT NULL,`amount` FLOAT,`biz_date` DATE,`digest` STRING," +
            "`batch_id_in` INTEGER NOT NULL,`batch_id_out` INTEGER,PRIMARY KEY (`id`, `name`, `batch_id_in`) NOT ENFORCED)";

    public static String expectedMetadataTableCreateQuery = "CREATE TABLE IF NOT EXISTS batch_metadata" +
            "(`table_name` STRING(255)," +
            "`batch_start_ts_utc` DATETIME," +
            "`batch_end_ts_utc` DATETIME," +
            "`batch_status` STRING(32)," +
            "`table_batch_id` INTEGER)";

    public static String expectedMetadataTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS BATCH_METADATA" +
            "(`TABLE_NAME` STRING(255)," +
            "`BATCH_START_TS_UTC` DATETIME," +
            "`BATCH_END_TS_UTC` DATETIME," +
            "`BATCH_STATUS` STRING(32)," +
            "`TABLE_BATCH_ID` INTEGER)";

    public static String expectedMainTableBatchIdBasedCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
            "(`ID` INTEGER NOT NULL,`NAME` STRING NOT NULL,`AMOUNT` FLOAT,`BIZ_DATE` DATE,`DIGEST` STRING," +
            "`BATCH_ID_IN` INTEGER NOT NULL,`BATCH_ID_OUT` INTEGER,PRIMARY KEY (`ID`, `NAME`, `BATCH_ID_IN`) NOT ENFORCED)";

    public static String expectedMetadataTableIngestQuery = "INSERT INTO batch_metadata (`table_name`, `table_batch_id`, `batch_start_ts_utc`, `batch_end_ts_utc`, `batch_status`)" +
            " (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.`table_name`) = 'MAIN'),'2000-01-01 00:00:00',CURRENT_DATETIME(),'DONE')";

    public static String expectedMetadataTableIngestQueryWithUpperCase = "INSERT INTO BATCH_METADATA (`TABLE_NAME`, `TABLE_BATCH_ID`, `BATCH_START_TS_UTC`, `BATCH_END_TS_UTC`, `BATCH_STATUS`)" +
            " (SELECT 'MAIN',(SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.`TABLE_NAME`) = 'MAIN'),'2000-01-01 00:00:00',CURRENT_DATETIME(),'DONE')";
    
    public static String expectedMetadataTableIngestQueryWithPlaceHolders = "INSERT INTO batch_metadata (`table_name`, `table_batch_id`, `batch_start_ts_utc`, `batch_end_ts_utc`, `batch_status`) " +
            "(SELECT 'main',{BATCH_ID_PATTERN},'{BATCH_START_TS_PATTERN}','{BATCH_END_TS_PATTERN}','DONE')";

    public static String expectedMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`biz_date` DATE," +
            "`digest` STRING," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`batch_time_in` DATETIME," +
            "`batch_time_out` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`) NOT ENFORCED)";

    public static String expectedMainTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
            "(`ID` INTEGER NOT NULL," +
            "`NAME` STRING NOT NULL," +
            "`AMOUNT` FLOAT," +
            "`BIZ_DATE` DATE," +
            "`DIGEST` STRING," +
            "`BATCH_ID_IN` INTEGER NOT NULL," +
            "`BATCH_ID_OUT` INTEGER," +
            "`BATCH_TIME_IN` DATETIME," +
            "`BATCH_TIME_OUT` DATETIME," +
            "PRIMARY KEY (`ID`, `NAME`, `BATCH_ID_IN`) NOT ENFORCED)";

    public static String expectedMainTableTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL,`name` STRING NOT NULL,`amount` FLOAT,`biz_date` DATE,`digest` STRING," +
            "`batch_time_in` DATETIME NOT NULL,`batch_time_out` DATETIME,PRIMARY KEY (`id`, `name`, `batch_time_in`) NOT ENFORCED)";

    public static String expectedMainTableTimeBasedCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
            "(`ID` INTEGER NOT NULL,`NAME` STRING NOT NULL,`AMOUNT` FLOAT,`BIZ_DATE` DATE,`DIGEST` STRING," +
            "`BATCH_TIME_IN` DATETIME NOT NULL,`BATCH_TIME_OUT` DATETIME,PRIMARY KEY (`ID`, `NAME`, `BATCH_TIME_IN`) NOT ENFORCED)";

    public static String expectedBitemporalMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`digest` STRING," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`) NOT ENFORCED)";

    public static String expectedBitemporalMainTableWithBatchIdDatetimeCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`digest` STRING," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`batch_time_in` DATETIME," +
            "`batch_time_out` DATETIME," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`) NOT ENFORCED)";

    public static String expectedBitemporalMainTableWithDatetimeCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`digest` STRING," +
            "`batch_time_in` DATETIME NOT NULL," +
            "`batch_time_out` DATETIME," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_time_in`, `validity_from_target`) NOT ENFORCED)";

    public static String expectedBitemporalFromOnlyMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`digest` STRING," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`) NOT ENFORCED)";

    public static String expectedBitemporalFromOnlyMainTableBatchIdAndTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
            "(`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`digest` STRING," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`batch_time_in` DATETIME," +
            "`batch_time_out` DATETIME," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`) NOT ENFORCED)";

    public static String expectedBitemporalFromOnlyMainTableDateTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
            "`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`digest` STRING," +
            "`batch_time_in` DATETIME NOT NULL," +
            "`batch_time_out` DATETIME," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_time_in`, `validity_from_target`) NOT ENFORCED)";

    public static String expectedBitemporalMainTableCreateQueryUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
            "(`ID` INTEGER NOT NULL," +
            "`NAME` STRING NOT NULL," +
            "`AMOUNT` FLOAT," +
            "`DIGEST` STRING," +
            "`BATCH_ID_IN` INTEGER NOT NULL," +
            "`BATCH_ID_OUT` INTEGER," +
            "`VALIDITY_FROM_TARGET` DATETIME NOT NULL," +
            "`VALIDITY_THROUGH_TARGET` DATETIME," +
            "PRIMARY KEY (`ID`, `NAME`, `BATCH_ID_IN`, `VALIDITY_FROM_TARGET`) NOT ENFORCED)";

    public static String expectedBitemporalFromOnlyTempTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`temp`" +
            "(`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`digest` STRING," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`) NOT ENFORCED)";

    public static String expectedBitemporalFromOnlyTempTableBatchIdAndTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`temp`(" +
            "`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`digest` STRING," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`batch_time_in` DATETIME," +
            "`batch_time_out` DATETIME," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`) NOT ENFORCED)";

    public static String expectedBitemporalFromOnlyTempTableDateTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`temp`(" +
            "`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`digest` STRING," +
            "`batch_time_in` DATETIME NOT NULL," +
            "`batch_time_out` DATETIME," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "PRIMARY KEY (`id`, `name`, `batch_time_in`, `validity_from_target`) NOT ENFORCED)";

    public static String expectedBitemporalFromOnlyStageWithoutDuplicatesTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`stagingWithoutDuplicates`" +
            "(`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`validity_from_reference` DATETIME NOT NULL," +
            "`digest` STRING," +
            "PRIMARY KEY (`id`, `name`, `validity_from_reference`) NOT ENFORCED)";

    public static String expectedBitemporalFromOnlyTempTableWithDeleteIndicatorCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`tempWithDeleteIndicator`" +
            "(`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`digest` STRING," +
            "`batch_id_in` INTEGER NOT NULL," +
            "`batch_id_out` INTEGER," +
            "`validity_from_target` DATETIME NOT NULL," +
            "`validity_through_target` DATETIME," +
            "`delete_indicator` STRING," +
            "PRIMARY KEY (`id`, `name`, `batch_id_in`, `validity_from_target`) NOT ENFORCED)";

    public static String expectedBitemporalFromOnlyStageWithDataSplitWithoutDuplicatesTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`stagingWithoutDuplicates`" +
            "(`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`validity_from_reference` DATETIME NOT NULL," +
            "`digest` STRING," +
            "`data_split` INTEGER NOT NULL," +
            "PRIMARY KEY (`id`, `name`, `validity_from_reference`, `data_split`) NOT ENFORCED)";

    public static String expectedBitemporalFromOnlyStageWithDeleteIndicatorWithoutDuplicatesTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`stagingWithoutDuplicates`" +
            "(`id` INTEGER NOT NULL," +
            "`name` STRING NOT NULL," +
            "`amount` FLOAT," +
            "`validity_from_reference` DATETIME NOT NULL," +
            "`digest` STRING," +
            "`delete_indicator` STRING," +
            "PRIMARY KEY (`id`, `name`, `validity_from_reference`) NOT ENFORCED)";

}
