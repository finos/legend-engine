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

    public static String expectedBitemporalMainTableWithBatchIdDatetimeCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"" +
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

    public static String expectedBitemporalMainTableWithDatetimeCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"digest\" VARCHAR," +
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

    public static String expectedBitemporalFromOnlyStageWithDataSplitWithoutDuplicatesTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"stagingWithoutDuplicates\"" +
            "(\"id\" INTEGER NOT NULL," +
            "\"name\" VARCHAR NOT NULL," +
            "\"amount\" DOUBLE," +
            "\"validity_from_reference\" DATETIME NOT NULL," +
            "\"digest\" VARCHAR," +
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

    public static String expectedDropTableQuery = "DROP TABLE IF EXISTS \"mydb\".\"staging\" CASCADE";

    public static String expectedMetadataTableIngestQuery = "INSERT INTO batch_metadata " +
            "(\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\")" +
            " (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata " +
            "WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'),'2000-01-01 00:00:00',CURRENT_TIMESTAMP(),'DONE')";

    public static String expectedMetadataTableIngestQueryWithUpperCase = "INSERT INTO BATCH_METADATA " +
            "(\"TABLE_NAME\", \"TABLE_BATCH_ID\", \"BATCH_START_TS_UTC\", \"BATCH_END_TS_UTC\", \"BATCH_STATUS\")" +
            " (SELECT 'MAIN',(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA " +
            "WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN'),'2000-01-01 00:00:00',CURRENT_TIMESTAMP(),'DONE')";

    public static String expectedMetadataTableIngestQueryWithPlaceHolders = "INSERT INTO batch_metadata " +
            "(\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\") " +
            "(SELECT 'main',{BATCH_ID_PATTERN},'{BATCH_START_TS_PATTERN}','{BATCH_END_TS_PATTERN}','DONE')";
}
