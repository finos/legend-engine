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

package org.finos.legend.engine.persistence.components;

public class AnsiTestArtifacts
{

    public static String expectedMetadataTableCreateQuery = "CREATE TABLE IF NOT EXISTS batch_metadata" +
            "(\"table_name\" VARCHAR(255)," +
            "\"batch_start_ts_utc\" DATETIME," +
            "\"batch_end_ts_utc\" DATETIME," +
            "\"batch_status\" VARCHAR(32)," +
            "\"table_batch_id\" INTEGER," +
            "\"staging_filters\" JSON)";

    public static String expectedMetadataTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS BATCH_METADATA" +
            "(\"TABLE_NAME\" VARCHAR(255)," +
            "\"BATCH_START_TS_UTC\" DATETIME," +
            "\"BATCH_END_TS_UTC\" DATETIME," +
            "\"BATCH_STATUS\" VARCHAR(32)," +
            "\"TABLE_BATCH_ID\" INTEGER," +
            "\"STAGING_FILTERS\" JSON)";

    public static String expectedMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER NOT NULL," +
            "\"batch_id_out\" INTEGER," +
            "\"batch_time_in\" DATETIME," +
            "\"batch_time_out\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\"))";

    public static String expectedMainTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"" +
            "(\"ID\" INTEGER NOT NULL," +
            "\"NAME\" VARCHAR NOT NULL," +
            "\"AMOUNT\" DOUBLE," +
            "\"BIZ_DATE\" DATE," +
            "\"DIGEST\" VARCHAR," +
            "\"BATCH_ID_IN\" INTEGER NOT NULL," +
            "\"BATCH_ID_OUT\" INTEGER," +
            "\"BATCH_TIME_IN\" DATETIME," +
            "\"BATCH_TIME_OUT\" DATETIME," +
            "PRIMARY KEY (\"ID\", \"NAME\", \"BATCH_ID_IN\"))";

    public static String expectedMainTableBatchIdBasedCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
            "\"id\" INTEGER NOT NULL,\"name\" VARCHAR NOT NULL,\"amount\" DOUBLE,\"biz_date\" DATE,\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER NOT NULL,\"batch_id_out\" INTEGER,PRIMARY KEY (\"id\", \"name\", \"batch_id_in\"))";

    public static String expectedMainTableBatchIdAndVersionBasedCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
            "\"id\" INTEGER NOT NULL,\"name\" VARCHAR NOT NULL,\"amount\" DOUBLE,\"biz_date\" DATE,\"digest\" VARCHAR,\"version\" INTEGER," +
            "\"batch_id_in\" INTEGER NOT NULL,\"batch_id_out\" INTEGER,PRIMARY KEY (\"id\", \"name\", \"batch_id_in\"))";

    public static String expectedMainTableBatchIdAndVersionBasedCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"" +
            "(\"ID\" INTEGER NOT NULL,\"NAME\" VARCHAR NOT NULL,\"AMOUNT\" DOUBLE,\"BIZ_DATE\" DATE,\"DIGEST\" VARCHAR,\"VERSION\" INTEGER," +
            "\"BATCH_ID_IN\" INTEGER NOT NULL,\"BATCH_ID_OUT\" INTEGER,PRIMARY KEY (\"ID\", \"NAME\", \"BATCH_ID_IN\"))";

    public static String expectedMainTableBatchIdBasedCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"" +
            "(\"ID\" INTEGER NOT NULL,\"NAME\" VARCHAR NOT NULL,\"AMOUNT\" DOUBLE,\"BIZ_DATE\" DATE,\"DIGEST\" VARCHAR," +
            "\"BATCH_ID_IN\" INTEGER NOT NULL,\"BATCH_ID_OUT\" INTEGER,PRIMARY KEY (\"ID\", \"NAME\", \"BATCH_ID_IN\"))";

    public static String expectedMainTableTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
            "\"id\" INTEGER NOT NULL,\"name\" VARCHAR NOT NULL,\"amount\" DOUBLE,\"biz_date\" DATE,\"digest\" VARCHAR," +
            "\"batch_time_in\" DATETIME NOT NULL,\"batch_time_out\" DATETIME,PRIMARY KEY (\"id\", \"name\", \"batch_time_in\"))";

    public static String expectedMainTableTimeBasedCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"" +
            "(\"ID\" INTEGER NOT NULL,\"NAME\" VARCHAR NOT NULL,\"AMOUNT\" DOUBLE,\"BIZ_DATE\" DATE,\"DIGEST\" VARCHAR," +
            "\"BATCH_TIME_IN\" DATETIME NOT NULL,\"BATCH_TIME_OUT\" DATETIME,PRIMARY KEY (\"ID\", \"NAME\", \"BATCH_TIME_IN\"))";

    public static String expectedBaseTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
            "\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "PRIMARY KEY (\"id\", \"name\"))";

    public static String expectedBaseStagingTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"staging\"(" +
            "\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "PRIMARY KEY (\"id\", \"name\"))";

    public static String expectedBaseTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"" +
            "(\"ID\" INTEGER NOT NULL," +
            "\"NAME\" VARCHAR NOT NULL," +
            "\"AMOUNT\" DOUBLE," +
            "\"BIZ_DATE\" DATE," +
            "PRIMARY KEY (\"ID\", \"NAME\"))";

    public static String expectedBaseTablePlusDigestCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
            "\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "\"digest\" VARCHAR," +
            "PRIMARY KEY (\"id\", \"name\"))";

    public static String expectedStagingTableWithDigestCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"staging\"(" +
            "\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "\"digest\" VARCHAR," +
            "PRIMARY KEY (\"id\", \"name\"))";

    public static String expectedBaseTablePlusDigestPlusVersionCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
        "\"id\" INTEGER NOT NULL," +
        "\"name\" VARCHAR NOT NULL," +
        "\"amount\" DOUBLE," +
        "\"biz_date\" DATE," +
        "\"digest\" VARCHAR," +
        "\"version\" INTEGER," +
        "PRIMARY KEY (\"id\", \"name\"))";

    public static String expectedBaseTablePlusDigestPlusVersionCreateQueryUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"(" +
        "\"ID\" INTEGER NOT NULL," +
        "\"NAME\" VARCHAR NOT NULL," +
        "\"AMOUNT\" DOUBLE," +
        "\"BIZ_DATE\" DATE," +
        "\"DIGEST\" VARCHAR," +
        "\"VERSION\" INTEGER," +
        "PRIMARY KEY (\"ID\", \"NAME\"))";
    public static String expectedBaseTableCreateQueryWithNoPKs = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
            "\"id\" INTEGER,\"name\" VARCHAR,\"amount\" DOUBLE,\"biz_date\" DATE,\"digest\" VARCHAR)";

    public static String expectedBaseStagingTableCreateQueryWithNoPKs = "CREATE TABLE IF NOT EXISTS \"mydb\".\"staging\"(" +
            "\"id\" INTEGER,\"name\" VARCHAR,\"amount\" DOUBLE,\"biz_date\" DATE,\"digest\" VARCHAR)";

    public static String expectedLockInfoTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main_legend_persistence_lock\"" +
            "(\"insert_ts_utc\" DATETIME,\"last_used_ts_utc\" DATETIME,\"table_name\" VARCHAR UNIQUE)";

    public static String expectedLockInfoTableUpperCaseCreateQuery = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN_LEGEND_PERSISTENCE_LOCK\"" +
            "(\"INSERT_TS_UTC\" DATETIME,\"LAST_USED_TS_UTC\" DATETIME,\"TABLE_NAME\" VARCHAR UNIQUE)";

    public static String lockInitializedQuery = "INSERT INTO \"mydb\".\"main_legend_persistence_lock\" " +
            "(\"insert_ts_utc\", \"table_name\") " +
            "(SELECT '2000-01-01 00:00:00.000000','main' " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main_legend_persistence_lock\" as main_legend_persistence_lock)))";

    public static String lockInitializedUpperCaseQuery = "INSERT INTO \"MYDB\".\"MAIN_LEGEND_PERSISTENCE_LOCK\" (\"INSERT_TS_UTC\", \"TABLE_NAME\")" +
            " (SELECT '2000-01-01 00:00:00.000000','MAIN' WHERE NOT (EXISTS (SELECT * FROM \"MYDB\".\"MAIN_LEGEND_PERSISTENCE_LOCK\" as MAIN_LEGEND_PERSISTENCE_LOCK)))";

    public static String lockAcquiredQuery = "UPDATE \"mydb\".\"main_legend_persistence_lock\" as main_legend_persistence_lock " +
            "SET main_legend_persistence_lock.\"last_used_ts_utc\" = '2000-01-01 00:00:00.000000'";

    public static String lockAcquiredUpperCaseQuery = "UPDATE \"MYDB\".\"MAIN_LEGEND_PERSISTENCE_LOCK\" as MAIN_LEGEND_PERSISTENCE_LOCK " +
            "SET MAIN_LEGEND_PERSISTENCE_LOCK.\"LAST_USED_TS_UTC\" = '2000-01-01 00:00:00.000000'";

    public static String getDropTempTableQuery(String tableName)
    {
        return String.format("DROP TABLE IF EXISTS %s CASCADE", tableName);
    }

    public static String expectedBaseTableCreateQueryWithAuditAndNoPKs = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"" +
            "(\"id\" INTEGER,\"name\" VARCHAR,\"amount\" DOUBLE,\"biz_date\" DATE,\"digest\" VARCHAR,\"batch_update_time\" DATETIME)";

    public static String expectedBaseTablePlusDigestCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"(" +
            "\"ID\" INTEGER NOT NULL," +
            "\"NAME\" VARCHAR NOT NULL," +
            "\"AMOUNT\" DOUBLE," +
            "\"BIZ_DATE\" DATE," +
            "\"DIGEST\" VARCHAR," +
            "PRIMARY KEY (\"ID\", \"NAME\"))";

    public static String expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
            "\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "\"digest\" VARCHAR," +
            "\"batch_update_time\" DATETIME NOT NULL," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_update_time\"))";

    public static String expectedBaseTablePlusDigestPlusUpdateTimestampCreateQueryUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"(" +
        "\"ID\" INTEGER NOT NULL," +
        "\"NAME\" VARCHAR NOT NULL," +
        "\"AMOUNT\" DOUBLE," +
        "\"BIZ_DATE\" DATE," +
        "\"DIGEST\" VARCHAR," +
        "\"BATCH_UPDATE_TIME\" DATETIME NOT NULL," +
        "PRIMARY KEY (\"ID\", \"NAME\", \"BATCH_UPDATE_TIME\"))";

    public static String expectedBaseTableWithAuditNotPkCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
            "\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "\"digest\" VARCHAR," +
            "\"batch_update_time\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\"))";

    public static String expectedBaseTableWithAuditPkCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
            "\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "\"batch_update_time\" DATETIME NOT NULL," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_update_time\"))";

    public static String expectedBaseTempStagingTablePlusDigest = "CREATE TABLE IF NOT EXISTS \"mydb\".\"staging_legend_persistence_temp_staging\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "\"digest\" VARCHAR)";

    public static String expectedBaseTempStagingTableWithCount = "CREATE TABLE IF NOT EXISTS \"mydb\".\"staging_legend_persistence_temp_staging\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "\"legend_persistence_count\" INTEGER)";

    public static String expectedBaseTempStagingTableWithVersionAndCount = "CREATE TABLE IF NOT EXISTS \"mydb\".\"staging_legend_persistence_temp_staging\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "\"digest\" VARCHAR," +
            "\"version\" INTEGER," +
            "\"legend_persistence_count\" INTEGER)";

    public static String expectedBaseTempStagingTablePlusDigestWithCount = "CREATE TABLE IF NOT EXISTS \"mydb\".\"staging_legend_persistence_temp_staging\"" +
        "(\"id\" INTEGER NOT NULL," +
        "\"name\" VARCHAR NOT NULL," +
        "\"amount\" DOUBLE," +
        "\"biz_date\" DATE," +
        "\"digest\" VARCHAR," +
        "\"legend_persistence_count\" INTEGER)";

    public static String expectedBaseTempStagingTablePlusDigestWithCountUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\"" +
            "(\"ID\" INTEGER NOT NULL," +
            "\"NAME\" VARCHAR NOT NULL," +
            "\"AMOUNT\" DOUBLE," +
            "\"BIZ_DATE\" DATE," +
            "\"DIGEST\" VARCHAR," +
            "\"LEGEND_PERSISTENCE_COUNT\" INTEGER)";

    public static String expectedBaseTempStagingTablePlusDigestWithVersionUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\"" +
            "(\"ID\" INTEGER NOT NULL," +
            "\"NAME\" VARCHAR NOT NULL," +
            "\"AMOUNT\" DOUBLE," +
            "\"BIZ_DATE\" DATE," +
            "\"DIGEST\" VARCHAR," +
            "\"VERSION\" INTEGER)";

    public static String expectedBaseTempStagingTablePlusDigestWithDataSplit = "CREATE TABLE IF NOT EXISTS \"mydb\".\"staging_legend_persistence_temp_staging\"(" +
            "\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "\"digest\" VARCHAR," +
            "\"data_split\" INTEGER NOT NULL)";

    public static String expectedBaseTempStagingTablePlusDigestWithCountAndDataSplit = "CREATE TABLE IF NOT EXISTS \"mydb\".\"staging_legend_persistence_temp_staging\"" +
        "(\"id\" INTEGER NOT NULL," +
        "\"name\" VARCHAR NOT NULL," +
        "\"amount\" DOUBLE," +
        "\"biz_date\" DATE," +
        "\"digest\" VARCHAR," +
        "\"legend_persistence_count\" INTEGER," +
        "\"data_split\" INTEGER NOT NULL)";

    public static String expectedBaseTempStagingTablePlusDigestWithDataSplitAndCount = "CREATE TABLE IF NOT EXISTS \"mydb\".\"staging_legend_persistence_temp_staging\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"biz_date\" DATE," +
            "\"digest\" VARCHAR," +
            "\"data_split\" BIGINT NOT NULL," +
            "\"legend_persistence_count\" INTEGER)";


    public static String expectedBitemporalMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER NOT NULL," +
            "\"batch_id_out\" INTEGER," +
            "\"validity_from_target\" DATETIME NOT NULL," +
            "\"validity_through_target\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

    public static String expectedBitemporalStagingTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"staging\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"validity_from_reference\" DATETIME NOT NULL," +
            "\"validity_through_reference\" DATETIME," +
            "\"digest\" VARCHAR," +
            "PRIMARY KEY (\"id\", \"name\", \"validity_from_reference\"))";

    public static String expectedBitemporalMainTableWithVersionWithBatchIdDatetimeCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
            "\"version\" INTEGER," +
            "\"batch_id_in\" INTEGER NOT NULL," +
            "\"batch_id_out\" INTEGER," +
            "\"batch_time_in\" DATETIME," +
            "\"batch_time_out\" DATETIME," +
            "\"validity_from_target\" DATETIME NOT NULL," +
            "\"validity_through_target\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

    public static String expectedBitemporalMainTableWithVersionBatchDateTimeCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
            "\"version\" INTEGER," +
            "\"batch_time_in\" DATETIME NOT NULL," +
            "\"batch_time_out\" DATETIME," +
            "\"validity_from_target\" DATETIME NOT NULL," +
            "\"validity_through_target\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_time_in\", \"validity_from_target\"))";

    public static String expectedBitemporalFromOnlyMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER NOT NULL," +
            "\"batch_id_out\" INTEGER," +
            "\"validity_from_target\" DATETIME NOT NULL," +
            "\"validity_through_target\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

    public static String expectedBitemporalFromOnlyMainTableWithVersionCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"" +
        "(\"id\" INTEGER NOT NULL," +
        "\"name\" VARCHAR NOT NULL," +
        "\"amount\" DOUBLE," +
        "\"digest\" VARCHAR," +
        "\"version\" INTEGER," +
        "\"batch_id_in\" INTEGER NOT NULL," +
        "\"batch_id_out\" INTEGER," +
        "\"validity_from_target\" DATETIME NOT NULL," +
        "\"validity_through_target\" DATETIME," +
        "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

    public static String expectedBitemporalFromOnlyStagingTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"staging\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"validity_from_reference\" DATETIME NOT NULL," +
            "\"digest\" VARCHAR," +
            "PRIMARY KEY (\"id\", \"name\", \"validity_from_reference\"))";

    public static String expectedBitemporalFromOnlyMainTableBatchIdAndTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER NOT NULL," +
            "\"batch_id_out\" INTEGER," +
            "\"batch_time_in\" DATETIME," +
            "\"batch_time_out\" DATETIME," +
            "\"validity_from_target\" DATETIME NOT NULL," +
            "\"validity_through_target\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

    public static String expectedBitemporalFromOnlyMainTableDateTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
            "\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
            "\"batch_time_in\" DATETIME NOT NULL," +
            "\"batch_time_out\" DATETIME," +
            "\"validity_from_target\" DATETIME NOT NULL," +
            "\"validity_through_target\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_time_in\", \"validity_from_target\"))";

    public static String expectedBitemporalMainTableCreateQueryUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"" +
            "(\"ID\" INTEGER NOT NULL," +
            "\"NAME\" VARCHAR NOT NULL," +
            "\"AMOUNT\" DOUBLE," +
            "\"DIGEST\" VARCHAR," +
            "\"BATCH_ID_IN\" INTEGER NOT NULL," +
            "\"BATCH_ID_OUT\" INTEGER," +
            "\"VALIDITY_FROM_TARGET\" DATETIME NOT NULL," +
            "\"VALIDITY_THROUGH_TARGET\" DATETIME," +
            "PRIMARY KEY (\"ID\", \"NAME\", \"BATCH_ID_IN\", \"VALIDITY_FROM_TARGET\"))";

    public static String expectedBitemporalFromOnlyMainTableCreateQueryUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"" +
            "(\"ID\" INTEGER NOT NULL," +
            "\"NAME\" VARCHAR NOT NULL," +
            "\"AMOUNT\" DOUBLE," +
            "\"VALIDITY_FROM_REFERENCE\" DATETIME NOT NULL," +
            "\"DIGEST\" VARCHAR," +
            "\"BATCH_ID_IN\" INTEGER NOT NULL," +
            "\"BATCH_ID_OUT\" INTEGER," +
            "\"VALIDITY_FROM_TARGET\" DATETIME NOT NULL," +
            "\"VALIDITY_THROUGH_TARGET\" DATETIME," +
            "PRIMARY KEY (\"ID\", \"NAME\", \"VALIDITY_FROM_REFERENCE\", \"BATCH_ID_IN\", \"VALIDITY_FROM_TARGET\"))";

    public static String expectedBitemporalFromOnlyTempTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"temp\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER NOT NULL," +
            "\"batch_id_out\" INTEGER," +
            "\"validity_from_target\" DATETIME NOT NULL," +
            "\"validity_through_target\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

    public static String expectedBitemporalFromOnlyTempTableWithVersionCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"temp\"" +
        "(\"id\" INTEGER NOT NULL," +
        "\"name\" VARCHAR NOT NULL," +
        "\"amount\" DOUBLE," +
        "\"digest\" VARCHAR," +
        "\"version\" INTEGER," +
        "\"batch_id_in\" INTEGER NOT NULL," +
        "\"batch_id_out\" INTEGER," +
        "\"validity_from_target\" DATETIME NOT NULL," +
        "\"validity_through_target\" DATETIME," +
        "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

    public static String expectedBitemporalFromOnlyTempTableBatchIdAndTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"temp\"(" +
            "\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER NOT NULL," +
            "\"batch_id_out\" INTEGER," +
            "\"batch_time_in\" DATETIME," +
            "\"batch_time_out\" DATETIME," +
            "\"validity_from_target\" DATETIME NOT NULL," +
            "\"validity_through_target\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

    public static String expectedBitemporalFromOnlyTempTableDateTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"temp\"(" +
            "\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
            "\"batch_time_in\" DATETIME NOT NULL," +
            "\"batch_time_out\" DATETIME," +
            "\"validity_from_target\" DATETIME NOT NULL," +
            "\"validity_through_target\" DATETIME," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_time_in\", \"validity_from_target\"))";

    public static String expectedBitemporalFromOnlyStageWithoutDuplicatesTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"stagingWithoutDuplicates\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"validity_from_reference\" DATETIME NOT NULL," +
            "\"digest\" VARCHAR," +
            "PRIMARY KEY (\"id\", \"name\", \"validity_from_reference\"))";

    public static String expectedBitemporalFromOnlyTempTableWithDeleteIndicatorCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"tempWithDeleteIndicator\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
            "\"batch_id_in\" INTEGER NOT NULL," +
            "\"batch_id_out\" INTEGER," +
            "\"validity_from_target\" DATETIME NOT NULL," +
            "\"validity_through_target\" DATETIME," +
            "\"delete_indicator\" VARCHAR," +
            "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

    public static String expectedBitemporalFromOnlyStageWithVersionWithDataSplitWithoutDuplicatesTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"stagingWithoutDuplicates\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"validity_from_reference\" DATETIME NOT NULL," +
            "\"digest\" VARCHAR," +
            "\"version\" INTEGER," +
            "\"data_split\" BIGINT NOT NULL," +
            "PRIMARY KEY (\"id\", \"name\", \"validity_from_reference\", \"data_split\"))";

    public static String expectedBitemporalFromOnlyStageWithDeleteIndicatorWithoutDuplicatesTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"stagingWithoutDuplicates\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"validity_from_reference\" DATETIME NOT NULL," +
            "\"digest\" VARCHAR," +
            "\"delete_indicator\" VARCHAR," +
            "PRIMARY KEY (\"id\", \"name\", \"validity_from_reference\"))";

    public static String expectedStagingCleanupQuery = "DELETE FROM \"mydb\".\"staging\" as stage";
    public static String expectedTempStagingCleanupQuery = "DELETE FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage";
    public static String expectedTempStagingCleanupQueryInUpperCase = "DELETE FROM \"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" as stage";
    public static String expectedDropTableQuery = "DROP TABLE IF EXISTS \"mydb\".\"staging\" CASCADE";

    public static String expectedMetadataTableIngestQuery = "INSERT INTO batch_metadata " +
            "(\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\")" +
            " (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata " +
            "WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'),'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE')";

    public static String expectedMetadataTableIngestQueryWithUpperCase = "INSERT INTO BATCH_METADATA " +
            "(\"TABLE_NAME\", \"TABLE_BATCH_ID\", \"BATCH_START_TS_UTC\", \"BATCH_END_TS_UTC\", \"BATCH_STATUS\")" +
            " (SELECT 'MAIN',(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA " +
            "WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN'),'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE')";

    public static String expectedMetadataTableIngestQueryWithPlaceHolders = "INSERT INTO batch_metadata " +
            "(\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\") " +
            "(SELECT 'main',{BATCH_ID_PATTERN},'{BATCH_START_TS_PATTERN}','{BATCH_END_TS_PATTERN}','DONE')";

    public static String expectedInsertIntoBaseTempStagingWithMaxVersionAndFilterDuplicates = "INSERT INTO \"mydb\".\"staging_legend_persistence_temp_staging\" " +
        "(\"id\", \"name\", \"amount\", \"biz_date\", \"legend_persistence_count\") " +
        "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"legend_persistence_count\" as \"legend_persistence_count\" FROM " +
        "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"legend_persistence_count\" as \"legend_persistence_count\",DENSE_RANK() OVER " +
        "(PARTITION BY stage.\"id\",stage.\"name\" ORDER BY stage.\"biz_date\" DESC) as \"legend_persistence_rank\" FROM " +
        "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",COUNT(*) as \"legend_persistence_count\" FROM " +
        "\"mydb\".\"staging\" as stage GROUP BY stage.\"id\", stage.\"name\", stage.\"amount\", stage.\"biz_date\") as stage) as stage " +
        "WHERE stage.\"legend_persistence_rank\" = 1)";

    public static String expectedInsertIntoBaseTempStagingWithFilterDuplicates = "INSERT INTO \"mydb\".\"staging_legend_persistence_temp_staging\" " +
        "(\"id\", \"name\", \"amount\", \"biz_date\", \"legend_persistence_count\") " +
        "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\"," +
        "COUNT(*) as \"legend_persistence_count\" FROM \"mydb\".\"staging\" as stage " +
        "GROUP BY stage.\"id\", stage.\"name\", stage.\"amount\", stage.\"biz_date\")";

    public static String expectedInsertIntoBaseTempStagingWithFilterDupsAndMaxVersion = "INSERT INTO \"mydb\".\"staging_legend_persistence_temp_staging\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"legend_persistence_count\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"legend_persistence_count\" as " +
            "\"legend_persistence_count\" FROM (SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "stage.\"legend_persistence_count\" as \"legend_persistence_count\",DENSE_RANK() OVER " +
            "(PARTITION BY stage.\"id\",stage.\"name\" ORDER BY stage.\"biz_date\" DESC) as \"legend_persistence_rank\" " +
            "FROM (SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",COUNT(*) as \"legend_persistence_count\" FROM \"mydb\".\"staging\" as stage " +
            "GROUP BY stage.\"id\", stage.\"name\", stage.\"amount\", stage.\"biz_date\", stage.\"digest\") as stage) as stage WHERE stage.\"legend_persistence_rank\" = 1)";

    public static String expectedInsertIntoBaseTempStagingPlusDigestWithFilterDuplicates = "INSERT INTO \"mydb\".\"staging_legend_persistence_temp_staging\" " +
        "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"legend_persistence_count\") " +
        "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
        "COUNT(*) as \"legend_persistence_count\" FROM \"mydb\".\"staging\" as stage " +
        "GROUP BY stage.\"id\", stage.\"name\", stage.\"amount\", stage.\"biz_date\", stage.\"digest\")";

    public static String expectedInsertIntoBaseTempStagingPlusDigestWithMaxVersionAndFilterDuplicates = "INSERT INTO \"mydb\".\"staging_legend_persistence_temp_staging\" " +
        "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"legend_persistence_count\") " +
        "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"legend_persistence_count\" as \"legend_persistence_count\" FROM " +
        "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"legend_persistence_count\" as \"legend_persistence_count\",DENSE_RANK() OVER " +
        "(PARTITION BY stage.\"id\",stage.\"name\" ORDER BY stage.\"biz_date\" DESC) as \"legend_persistence_rank\" FROM " +
        "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",COUNT(*) as \"legend_persistence_count\" FROM " +
        "\"mydb\".\"staging\" as stage GROUP BY stage.\"id\", stage.\"name\", stage.\"amount\", stage.\"biz_date\", stage.\"digest\") as stage) as stage " +
        "WHERE stage.\"legend_persistence_rank\" = 1)";

    public static String expectedInsertIntoBaseTempStagingPlusDigestWithMaxVersionAndAllowDuplicates = "INSERT INTO \"mydb\".\"staging_legend_persistence_temp_staging\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\" FROM " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",DENSE_RANK() " +
            "OVER (PARTITION BY stage.\"id\",stage.\"name\" ORDER BY stage.\"biz_date\" DESC) as \"legend_persistence_rank\" " +
            "FROM \"mydb\".\"staging\" as stage) as stage WHERE stage.\"legend_persistence_rank\" = 1)";

    public static String expectedInsertIntoBaseTempStagingPlusDigestWithMaxVersionAndFilterDuplicatesUpperCase = "INSERT INTO " +
            "\"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" " +
            "(\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"DIGEST\", \"LEGEND_PERSISTENCE_COUNT\") " +
            "(SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"BIZ_DATE\",stage.\"DIGEST\",stage.\"LEGEND_PERSISTENCE_COUNT\" as \"LEGEND_PERSISTENCE_COUNT\" FROM " +
            "(SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"BIZ_DATE\",stage.\"DIGEST\",stage.\"LEGEND_PERSISTENCE_COUNT\" as \"LEGEND_PERSISTENCE_COUNT\",DENSE_RANK() " +
            "OVER (PARTITION BY stage.\"ID\",stage.\"NAME\" ORDER BY stage.\"BIZ_DATE\" DESC) as \"LEGEND_PERSISTENCE_RANK\" " +
            "FROM (SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"BIZ_DATE\",stage.\"DIGEST\",COUNT(*) as \"LEGEND_PERSISTENCE_COUNT\" FROM \"MYDB\".\"STAGING\" as stage " +
            "GROUP BY stage.\"ID\", stage.\"NAME\", stage.\"AMOUNT\", stage.\"BIZ_DATE\", stage.\"DIGEST\") as stage) as stage WHERE stage.\"LEGEND_PERSISTENCE_RANK\" = 1)";

    public static String expectedInsertIntoBaseTempStagingPlusDigestWithAllVersionAndFilterDuplicates = "INSERT INTO \"mydb\".\"staging_legend_persistence_temp_staging\" " +
        "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"legend_persistence_count\", \"data_split\") " +
        "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"legend_persistence_count\" as \"legend_persistence_count\",DENSE_RANK() OVER (PARTITION BY stage.\"id\",stage.\"name\" ORDER BY stage.\"biz_date\" ASC) as \"data_split\" " +
        "FROM (SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",COUNT(*) as \"legend_persistence_count\" FROM \"mydb\".\"staging\" as stage " +
        "GROUP BY stage.\"id\", stage.\"name\", stage.\"amount\", stage.\"biz_date\", stage.\"digest\") as stage)";

    public static String expectedInsertIntoBaseTempStagingPlusDigestWithAllVersionAndAllowDups = "INSERT INTO \"mydb\".\"staging_legend_persistence_temp_staging\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"data_split\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "DENSE_RANK() OVER (PARTITION BY stage.\"id\",stage.\"name\" ORDER BY stage.\"biz_date\" ASC) as \"data_split\" " +
            "FROM \"mydb\".\"staging\" as stage)";

    public static String maxDupsErrorCheckSql = "SELECT MAX(stage.\"legend_persistence_count\") as \"MAX_DUPLICATES\" FROM " +
            "\"mydb\".\"staging_legend_persistence_temp_staging\" as stage";

    public static String dataErrorCheckSqlWithBizDateVersion = "SELECT MAX(\"legend_persistence_distinct_rows\") as \"MAX_DATA_ERRORS\" FROM " +
        "(SELECT COUNT(DISTINCT(\"digest\")) as \"legend_persistence_distinct_rows\" FROM " +
        "\"mydb\".\"staging_legend_persistence_temp_staging\" as stage GROUP BY \"id\", \"name\", \"biz_date\") as stage";

    public static String dataErrorCheckSql = "SELECT MAX(\"legend_persistence_distinct_rows\") as \"MAX_DATA_ERRORS\" FROM " +
            "(SELECT COUNT(DISTINCT(\"digest\")) as \"legend_persistence_distinct_rows\" FROM " +
            "\"mydb\".\"staging_legend_persistence_temp_staging\" as stage GROUP BY \"id\", \"name\", \"version\") as stage";

    public static String dataErrorCheckSqlUpperCase = "SELECT MAX(\"LEGEND_PERSISTENCE_DISTINCT_ROWS\") as \"MAX_DATA_ERRORS\" FROM" +
            " (SELECT COUNT(DISTINCT(\"DIGEST\")) as \"LEGEND_PERSISTENCE_DISTINCT_ROWS\" FROM " +
            "\"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" as stage GROUP BY \"ID\", \"NAME\", \"VERSION\") as stage";

    public static String dataErrorCheckSqlWithBizDateAsVersionUpperCase = "SELECT MAX(\"LEGEND_PERSISTENCE_DISTINCT_ROWS\") as \"MAX_DATA_ERRORS\" " +
            "FROM (SELECT COUNT(DISTINCT(\"DIGEST\")) as \"LEGEND_PERSISTENCE_DISTINCT_ROWS\" FROM " +
            "\"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" as stage GROUP BY \"ID\", \"NAME\", \"BIZ_DATE\") as stage";


}
