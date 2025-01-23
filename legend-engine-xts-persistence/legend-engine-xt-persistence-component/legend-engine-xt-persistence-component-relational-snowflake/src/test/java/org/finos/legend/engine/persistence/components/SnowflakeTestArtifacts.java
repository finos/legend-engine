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

public class SnowflakeTestArtifacts
{
    public static String expectedMetadataTableIngestQuery = "INSERT INTO batch_metadata (\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\")" +
            " (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'),'2000-01-01 00:00:00.000000',SYSDATE(),'DONE')";

    public static String expectedMetadataTableIngestQueryWithUpperCase = "INSERT INTO BATCH_METADATA (\"TABLE_NAME\", \"TABLE_BATCH_ID\", \"BATCH_START_TS_UTC\", \"BATCH_END_TS_UTC\", \"BATCH_STATUS\")" +
            " (SELECT 'MAIN',(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN'),'2000-01-01 00:00:00.000000',SYSDATE(),'DONE')";

    public static String expectedMetadataTableIngestQueryWithBatchSuccessValue = "INSERT INTO batch_metadata (\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\")" +
        " (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'),'2000-01-01 00:00:00.000000',SYSDATE(),'SUCCEEDED')";

    public static String expectedMetadataTableIngestQueryWithAdditionalMetadata = "INSERT INTO batch_metadata (\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\", \"additional_metadata\")" +
        " (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'),'2000-01-01 00:00:00.000000',SYSDATE(),'DONE',PARSE_JSON('{\"watermark\":\"my_watermark_value\"}'))";

    public static String expectedMetadataTableIngestQueryWithAdditionalMetadataWithUpperCase = "INSERT INTO BATCH_METADATA (\"TABLE_NAME\", \"TABLE_BATCH_ID\", \"BATCH_START_TS_UTC\", \"BATCH_END_TS_UTC\", \"BATCH_STATUS\", \"ADDITIONAL_METADATA\")" +
        " (SELECT 'MAIN',(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN'),'2000-01-01 00:00:00.000000',SYSDATE(),'DONE',PARSE_JSON('{\"watermark\":\"my_watermark_value\"}'))";

    public static String expectedMetadataTableIngestQueryWithAdditionalMetadataWithBatchSuccessValueWithUpperCase = "INSERT INTO BATCH_METADATA (\"TABLE_NAME\", \"TABLE_BATCH_ID\", \"BATCH_START_TS_UTC\", \"BATCH_END_TS_UTC\", \"BATCH_STATUS\", \"ADDITIONAL_METADATA\")" +
        " (SELECT 'MAIN',(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN'),'2000-01-01 00:00:00.000000',SYSDATE(),'SUCCEEDED',PARSE_JSON('{\"watermark\":\"my_watermark_value\"}'))";

    public static String expectedMetadataTableCreateQuery = "CREATE TABLE IF NOT EXISTS batch_metadata" +
            "(\"table_name\" VARCHAR(255)," +
            "\"batch_start_ts_utc\" DATETIME," +
            "\"batch_end_ts_utc\" DATETIME," +
            "\"batch_status\" VARCHAR(32)," +
            "\"table_batch_id\" INTEGER," +
            "\"ingest_request_id\" VARCHAR(64)," +
            "\"batch_source_info\" VARIANT," +
            "\"additional_metadata\" VARIANT," +
            "\"batch_statistics\" VARIANT)";

    public static String expectedMetadataTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS BATCH_METADATA" +
            "(\"TABLE_NAME\" VARCHAR(255)," +
            "\"BATCH_START_TS_UTC\" DATETIME," +
            "\"BATCH_END_TS_UTC\" DATETIME," +
            "\"BATCH_STATUS\" VARCHAR(32)," +
            "\"TABLE_BATCH_ID\" INTEGER," +
            "\"INGEST_REQUEST_ID\" VARCHAR(64)," +
            "\"BATCH_SOURCE_INFO\" VARIANT," +
            "\"ADDITIONAL_METADATA\" VARIANT," +
            "\"BATCH_STATISTICS\" VARIANT)";

    public static String expectedBaseTempStagingTableWithCountAndDataSplit = "CREATE TABLE IF NOT EXISTS \"mydb\".\"staging_temp_staging_lp_yosulf\"" +
        "(\"id\" INTEGER NOT NULL," +
        "\"name\" VARCHAR NOT NULL," +
        "\"amount\" DOUBLE," +
        "\"biz_date\" DATE," +
        "\"legend_persistence_count\" INTEGER," +
        "\"data_split\" INTEGER NOT NULL)";

    public static String expectedInsertIntoBaseTempStagingWithAllVersionAndFilterDuplicates = "INSERT INTO \"mydb\".\"staging_temp_staging_lp_yosulf\" " +
        "(\"id\", \"name\", \"amount\", \"biz_date\", \"legend_persistence_count\", \"data_split\") " +
        "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"legend_persistence_count\" as \"legend_persistence_count\",DENSE_RANK() OVER (PARTITION BY stage.\"id\",stage.\"name\" ORDER BY stage.\"biz_date\" ASC) as \"data_split\" " +
        "FROM (SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",COUNT(*) as \"legend_persistence_count\" FROM \"mydb\".\"staging\" as stage " +
        "GROUP BY stage.\"id\", stage.\"name\", stage.\"amount\", stage.\"biz_date\") as stage)";

    public static String dataErrorCheckSql = "SELECT MAX(\"legend_persistence_distinct_rows\") as \"MAX_DATA_ERRORS\" FROM " +
        "(SELECT COUNT(DISTINCT \"digest\") as \"legend_persistence_distinct_rows\" FROM " +
        "\"mydb\".\"staging_temp_staging_lp_yosulf\" as stage GROUP BY \"id\", \"name\", \"version\") as stage";

    public static String dataErrorsSql = "SELECT \"id\",\"name\",\"version\",COUNT(DISTINCT \"digest\") as \"legend_persistence_error_count\" FROM " +
        "\"mydb\".\"staging_temp_staging_lp_yosulf\" as stage GROUP BY \"id\", \"name\", \"version\" HAVING \"legend_persistence_error_count\" > 1 LIMIT 20";

    public static String dataErrorCheckSqlUpperCase = "SELECT MAX(\"LEGEND_PERSISTENCE_DISTINCT_ROWS\") as \"MAX_DATA_ERRORS\" FROM" +
        " (SELECT COUNT(DISTINCT \"DIGEST\") as \"LEGEND_PERSISTENCE_DISTINCT_ROWS\" FROM " +
        "\"MYDB\".\"STAGING_TEMP_STAGING_LP_YOSULF\" as stage GROUP BY \"ID\", \"NAME\", \"VERSION\") as stage";

    public static String dataErrorsSqlUpperCase = "SELECT \"ID\",\"NAME\",\"VERSION\",COUNT(DISTINCT \"DIGEST\") as \"LEGEND_PERSISTENCE_ERROR_COUNT\" FROM \"MYDB\".\"STAGING_TEMP_STAGING_LP_YOSULF\" " +
        "as stage GROUP BY \"ID\", \"NAME\", \"VERSION\" HAVING \"LEGEND_PERSISTENCE_ERROR_COUNT\" > 1 LIMIT 10";

    public static String dataErrorCheckSqlWithBizDateVersion = "SELECT MAX(\"legend_persistence_distinct_rows\") as \"MAX_DATA_ERRORS\" FROM " +
        "(SELECT COUNT(DISTINCT \"digest\") as \"legend_persistence_distinct_rows\" FROM " +
        "\"mydb\".\"staging_temp_staging_lp_yosulf\" as stage GROUP BY \"id\", \"name\", \"biz_date\") as stage";

    public static String dataErrorsSqlWithBizDateVersion = "SELECT \"id\",\"name\",\"biz_date\",COUNT(DISTINCT \"digest\") as \"legend_persistence_error_count\" FROM " +
        "\"mydb\".\"staging_temp_staging_lp_yosulf\" as stage GROUP BY \"id\", \"name\", \"biz_date\" HAVING \"legend_persistence_error_count\" > 1 LIMIT 20";

    public static String dataErrorCheckSqlWithBizDateAsVersionUpperCase = "SELECT MAX(\"LEGEND_PERSISTENCE_DISTINCT_ROWS\") as \"MAX_DATA_ERRORS\" " +
        "FROM (SELECT COUNT(DISTINCT \"DIGEST\") as \"LEGEND_PERSISTENCE_DISTINCT_ROWS\" FROM " +
        "\"MYDB\".\"STAGING_TEMP_STAGING_LP_YOSULF\" as stage GROUP BY \"ID\", \"NAME\", \"BIZ_DATE\") as stage";

    public static String dataErrorsSqlWithBizDateVersionUpperCase = "SELECT \"ID\",\"NAME\",\"BIZ_DATE\",COUNT(DISTINCT \"DIGEST\") as \"LEGEND_PERSISTENCE_ERROR_COUNT\" FROM \"MYDB\".\"STAGING_TEMP_STAGING_LP_YOSULF\" " +
        "as stage GROUP BY \"ID\", \"NAME\", \"BIZ_DATE\" HAVING \"LEGEND_PERSISTENCE_ERROR_COUNT\" > 1 LIMIT 20";

    public static String dataErrorCheckSqlWithAmount = "SELECT MAX(\"legend_persistence_distinct_rows\") as \"MAX_DATA_ERRORS\" FROM " +
        "(SELECT COUNT(DISTINCT \"amount\") as \"legend_persistence_distinct_rows\" FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" " +
        "as stage GROUP BY \"id\", \"name\", \"biz_date\") as stage";

    public static String dataErrorSqlWithAmount = "SELECT \"id\",\"name\",\"biz_date\",COUNT(DISTINCT \"amount\") as \"legend_persistence_error_count\" FROM " +
        "\"mydb\".\"staging_temp_staging_lp_yosulf\" as stage GROUP BY \"id\", \"name\", \"biz_date\" HAVING \"legend_persistence_error_count\" > 1 LIMIT 20";
}
