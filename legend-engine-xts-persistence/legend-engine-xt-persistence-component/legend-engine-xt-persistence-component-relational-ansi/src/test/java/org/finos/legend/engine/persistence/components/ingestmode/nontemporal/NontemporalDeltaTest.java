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
import org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.testcases.ingestmode.nontemporal.NontemporalDeltaTestCases;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.AnsiTestArtifacts.*;
import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.*;

public class NontemporalDeltaTest extends NontemporalDeltaTestCases
{
    protected String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage";
    protected String incomingRecordCountWithSplits = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage WHERE " +
            "(stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
    protected String incomingRecordCountWithSplitsTempStagingTable = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE " +
            "(stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
    protected String incomingRecordCountWithSplitsWithDuplicates = "SELECT COALESCE(SUM(stage.\"legend_persistence_count\"),0) as \"incomingRecordCount\" " +
            "FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE " +
            "(stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";

    protected String rowsTerminated = "SELECT 0 as \"rowsTerminated\"";
    protected String rowsDeleted = "SELECT 0 as \"rowsDeleted\"";
    protected String rowsDeletedWithDeleteIndicator = "SELECT COUNT(*) as \"rowsDeleted\" FROM \"mydb\".\"main\" as sink WHERE EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" = stage.\"digest\") AND (stage.\"delete_indicator\" IN ('yes','1','true')))";

    @Override
    public void verifyNontemporalDeltaNoAuditingNoDedupNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metadataIngestSqlList = operations.metadataIngestSql();
        List<String> initializeLockSql = operations.initializeLockSql();
        List<String> acquireLockSql = operations.acquireLockSql();
        List<String> deduplicationAndVersioningSql = operations.deduplicationAndVersioningSql();
        Map<DedupAndVersionErrorSqlType, String> deduplicationAndVersioningErrorChecksSql = operations.deduplicationAndVersioningErrorChecksSql();

        String updateSql = "UPDATE \"mydb\".\"main\" as sink SET " +
            "sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"biz_date\" = (SELECT stage.\"biz_date\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
            "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE " +
            "((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\"))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedStagingTableWithDigestCreateQuery, preActionsSqlList.get(1));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSqlList.get(2));
        Assertions.assertEquals(AnsiTestArtifacts.expectedLockInfoTableCreateQuery, preActionsSqlList.get(3));

        Assertions.assertTrue(deduplicationAndVersioningSql.isEmpty());
        Assertions.assertTrue(deduplicationAndVersioningErrorChecksSql.isEmpty());

        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSqlList.get(0));

        Assertions.assertEquals(lockInitializedQuery, initializeLockSql.get(0));
        Assertions.assertEquals(lockAcquiredQuery, acquireLockSql.get(0));

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
        List<String> metadataIngestSqlList = operations.metadataIngestSql();
        List<String> deduplicationAndVersioningSql = operations.deduplicationAndVersioningSql();
        Map<DedupAndVersionErrorSqlType, String> deduplicationAndVersioningErrorChecksSql = operations.deduplicationAndVersioningErrorChecksSql();

        String updateSql = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"biz_date\" = (SELECT stage.\"biz_date\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"batch_update_time\" = '2000-01-01 00:00:00.000000'," +
            "sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
            "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_update_time\", \"batch_id\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",'2000-01-01 00:00:00.000000'," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
            "FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\"))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSqlList.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTempStagingTablePlusDigestWithCount, preActionsSqlList.get(2));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSqlList.get(0));

        Assertions.assertEquals(AnsiTestArtifacts.expectedTempStagingCleanupQuery, deduplicationAndVersioningSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedInsertIntoBaseTempStagingPlusDigestWithFilterDuplicates, deduplicationAndVersioningSql.get(1));
        Assertions.assertTrue(deduplicationAndVersioningErrorChecksSql.isEmpty());
        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNonTemporalDeltaNoAuditingNoDedupAllVersion(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String updateSql = "UPDATE \"mydb\".\"main\" as sink SET " +
                "sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"biz_date\" = (SELECT stage.\"biz_date\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
                "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE " +
                "(((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) " +
                "AND ((stage.\"data_split\" >= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
                "WHERE ((stage.\"data_split\" >= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) " +
                "AND (NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), operations.get(0).preActionsSql().get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTempStagingTablePlusDigestWithDataSplit, operations.get(0).preActionsSql().get(2));
        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(0).metadataIngestSql().get(0));

        Assertions.assertEquals(AnsiTestArtifacts.expectedTempStagingCleanupQuery, operations.get(0).deduplicationAndVersioningSql().get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedInsertIntoBaseTempStagingPlusDigestWithAllVersionAndAllowDups, operations.get(0).deduplicationAndVersioningSql().get(1));
        Assertions.assertEquals(AnsiTestArtifacts.dataErrorCheckSqlWithBizDateVersion, operations.get(0).deduplicationAndVersioningErrorChecksSql().get(MAX_DATA_ERRORS));
        Assertions.assertEquals(dataErrorsSqlWithBizDateVersion, operations.get(0).deduplicationAndVersioningErrorChecksSql().get(DATA_ERROR_ROWS));

        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));
        
        // Stats
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplitsTempStagingTable, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplitsTempStagingTable, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNonTemporalDeltaNoAuditingNoDedupAllVersionWithoutPerform(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String updateSql = "UPDATE \"mydb\".\"main\" as sink SET " +
                "sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"biz_date\" = (SELECT stage.\"biz_date\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
                "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE " +
                "(((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) " +
                "AND ((stage.\"data_split\" >= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM \"mydb\".\"staging\" as stage " +
                "WHERE ((stage.\"data_split\" >= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) " +
                "AND (NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));

        Assertions.assertTrue(operations.get(0).deduplicationAndVersioningSql().isEmpty());
        Assertions.assertTrue(operations.get(0).deduplicationAndVersioningErrorChecksSqlPlan().isEmpty());

        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));

        // Stats
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplits, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplits, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNonTemporalDeltaWithWithAuditingFailOnDupsAllVersion(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String updateSql = "UPDATE \"mydb\".\"main\" as sink SET " +
                "sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"biz_date\" = (SELECT stage.\"biz_date\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))," +
                "sink.\"batch_update_time\" = '2000-01-01 00:00:00.000000'," +
                "sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
                "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_update_time\", \"batch_id\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",'2000-01-01 00:00:00.000000'," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
                "WHERE ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) " +
                "AND (NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(updateSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));

        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplitsWithDuplicates, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplitsWithDuplicates, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));

        Assertions.assertEquals(expectedTempStagingCleanupQuery, operations.get(0).deduplicationAndVersioningSql().get(0));
        Assertions.assertEquals(expectedInsertIntoBaseTempStagingPlusDigestWithAllVersionAndFilterDuplicates, operations.get(0).deduplicationAndVersioningSql().get(1));

        Assertions.assertEquals(maxDupsErrorCheckSql, operations.get(0).deduplicationAndVersioningErrorChecksSql().get(MAX_DUPLICATES));
        Assertions.assertEquals(dupRowsSql, operations.get(0).deduplicationAndVersioningErrorChecksSql().get(DUPLICATE_ROWS));

        Assertions.assertEquals(dataErrorCheckSqlWithBizDateVersion, operations.get(0).deduplicationAndVersioningErrorChecksSql().get(MAX_DATA_ERRORS));
        Assertions.assertEquals(dataErrorsSqlWithBizDateVersion, operations.get(0).deduplicationAndVersioningErrorChecksSql().get(DATA_ERROR_ROWS));

        // Stats
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaNoAuditingWithDeleteIndicatorNoDedupNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metaIngestSqlList = operations.metadataIngestSql();

        String updateSql = "UPDATE \"mydb\".\"main\" as sink SET " +
                "sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\") AND (stage.\"delete_indicator\" NOT IN ('yes','1','true')))," +
                "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\") AND (stage.\"delete_indicator\" NOT IN ('yes','1','true')))," +
                "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\") AND (stage.\"delete_indicator\" NOT IN ('yes','1','true')))," +
                "sink.\"biz_date\" = (SELECT stage.\"biz_date\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\") AND (stage.\"delete_indicator\" NOT IN ('yes','1','true')))," +
                "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\") AND (stage.\"delete_indicator\" NOT IN ('yes','1','true')))," +
                "sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
                "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE " +
                "((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\") AND (stage.\"delete_indicator\" NOT IN ('yes','1','true')))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM \"mydb\".\"staging\" as stage " +
                "WHERE (stage.\"delete_indicator\" NOT IN ('yes','1','true')) AND (NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
                "WHERE (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")))))";

        String deleteSql = "DELETE FROM \"mydb\".\"main\" as sink " +
                "WHERE EXISTS " +
                "(SELECT * FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) " +
                "AND (sink.\"digest\" = stage.\"digest\") AND (stage.\"delete_indicator\" IN ('yes','1','true')))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(deleteSql, milestoningSqlList.get(2));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithAdditionalMetadata(), metaIngestSqlList.get(0));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(null, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsDeletedWithDeleteIndicator, operations.preIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaWithUpperCaseOptimizer(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String updateSql = "UPDATE \"MYDB\".\"MAIN\" as sink SET " +
                "sink.\"ID\" = (SELECT stage.\"ID\" FROM \"MYDB\".\"STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (sink.\"DIGEST\" <> stage.\"DIGEST\"))," +
                "sink.\"NAME\" = (SELECT stage.\"NAME\" FROM \"MYDB\".\"STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (sink.\"DIGEST\" <> stage.\"DIGEST\"))," +
                "sink.\"AMOUNT\" = (SELECT stage.\"AMOUNT\" FROM \"MYDB\".\"STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (sink.\"DIGEST\" <> stage.\"DIGEST\"))," +
                "sink.\"BIZ_DATE\" = (SELECT stage.\"BIZ_DATE\" FROM \"MYDB\".\"STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (sink.\"DIGEST\" <> stage.\"DIGEST\"))," +
                "sink.\"DIGEST\" = (SELECT stage.\"DIGEST\" FROM \"MYDB\".\"STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (sink.\"DIGEST\" <> stage.\"DIGEST\"))," +
                "sink.\"BATCH_ID\" = (SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN') " +
                "WHERE EXISTS (SELECT * FROM \"MYDB\".\"STAGING\" as stage " +
                "WHERE ((sink.\"ID\" = stage.\"ID\") " +
                "AND (sink.\"NAME\" = stage.\"NAME\")) " +
                "AND (sink.\"DIGEST\" <> stage.\"DIGEST\"))";

        String insertSql = "INSERT INTO \"MYDB\".\"MAIN\" (\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"DIGEST\", \"BATCH_ID\") " +
                "(SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"BIZ_DATE\",stage.\"DIGEST\"," +
                "(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN') " +
                "FROM \"MYDB\".\"STAGING\" as stage WHERE NOT (EXISTS (SELECT * FROM \"MYDB\".\"MAIN\" as sink " +
                "WHERE (sink.\"ID\" = stage.\"ID\") " +
                "AND (sink.\"NAME\" = stage.\"NAME\"))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQueryWithUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Override
    public void verifyNontemporalDeltaWithLessColumnsInStaging(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String updateSql = "UPDATE \"mydb\".\"main\" as sink SET " +
                "sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
                "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
                "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
                "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
                "sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
                "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE " +
                "((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"digest\", \"batch_id\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"digest\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
                "WHERE (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\"))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Override
    public void verifyNontemporalDeltaPostActionSqlAndCleanStagingData(GeneratorResult operations)
    {
        List<String> postActionsSql = operations.postActionsSql();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(AnsiTestArtifacts.expectedStagingCleanupQuery);
        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);
    }

    @Override
    public void verifyNontemporalDeltaWithNoVersionAndStagingFilter(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metaIngestSqlList = operations.metadataIngestSql();

        String updateSql = "UPDATE \"mydb\".\"main\" as sink SET " +
            "sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-03')))," +
            "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-03')))," +
            "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-03')))," +
            "sink.\"biz_date\" = (SELECT stage.\"biz_date\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-03')))," +
            "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-03')))," +
            "sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
            "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-03')))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM \"mydb\".\"staging\" as stage WHERE (NOT (EXISTS " +
                "(SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"id\" = stage.\"id\") AND " +
                "(sink.\"name\" = stage.\"name\")))) AND ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-03')))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithStagingFiltersAndAdditionalMetadata("{\"staging_filters\":{\"biz_date\":{\"LT\":\"2020-01-03\",\"GT\":\"2020-01-01\"}}}"), metaIngestSqlList.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage WHERE (stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-03')";
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

        String updateSql = "UPDATE \"mydb\".\"main\" as sink SET " +
            "sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"biz_date\" > '2020-01-10') OR ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-05'))))," +
            "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"biz_date\" > '2020-01-10') OR ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-05'))))," +
            "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"biz_date\" > '2020-01-10') OR ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-05'))))," +
            "sink.\"biz_date\" = (SELECT stage.\"biz_date\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"biz_date\" > '2020-01-10') OR ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-05'))))," +
            "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"biz_date\" > '2020-01-10') OR ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-05'))))," +
            "sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
            "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) AND ((stage.\"biz_date\" > '2020-01-10') OR ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-05'))))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM \"mydb\".\"staging\" as stage WHERE (NOT (EXISTS " +
            "(SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"id\" = stage.\"id\") AND " +
            "(sink.\"name\" = stage.\"name\")))) AND ((stage.\"biz_date\" > '2020-01-10') OR ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-05'))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithAdditionalMetadata(), metaIngestSqlList.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage WHERE (stage.\"biz_date\" > '2020-01-10') OR ((stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-05'))";
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

        String updateSql = "UPDATE \"mydb\".\"main\" as sink SET " +
                "sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\"))," +
                "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\"))," +
                "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\"))," +
                "sink.\"biz_date\" = (SELECT stage.\"biz_date\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\"))," +
                "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\"))," +
                "sink.\"version\" = (SELECT stage.\"version\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\"))," +
                "sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
                "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\"))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"version\", \"batch_id\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"version\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM " +
                "\"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\"))))";

        String expectedInsertIntoBaseTempStagingWithMaxVersionFilterDupsWithStagingFilters = "INSERT INTO \"mydb\".\"staging_legend_persistence_temp_staging\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"version\", \"legend_persistence_count\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"version\"," +
                "stage.\"legend_persistence_count\" as \"legend_persistence_count\" " +
                "FROM (SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"version\"," +
                "stage.\"legend_persistence_count\" as \"legend_persistence_count\"," +
                "DENSE_RANK() OVER (PARTITION BY stage.\"id\",stage.\"name\" ORDER BY stage.\"version\" DESC) as \"legend_persistence_rank\" " +
                "FROM (SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"version\",COUNT(*) as \"legend_persistence_count\" " +
                "FROM \"mydb\".\"staging\" as stage WHERE stage.\"snapshot_id\" > 18972 GROUP BY stage.\"id\", stage.\"name\", stage.\"amount\", stage.\"biz_date\", stage.\"digest\", stage.\"version\") as stage) " +
                "as stage WHERE stage.\"legend_persistence_rank\" = 1)";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSqlList.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTempStagingTableWithVersionAndCount, preActionsSqlList.get(2));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithStagingFilters("{\"staging_filters\":{\"snapshot_id\":{\"GT\":18972}}}"), operations.metadataIngestSql().get(0));

        Assertions.assertEquals(AnsiTestArtifacts.expectedTempStagingCleanupQuery, operations.deduplicationAndVersioningSql().get(0));
        Assertions.assertEquals(expectedInsertIntoBaseTempStagingWithMaxVersionFilterDupsWithStagingFilters, operations.deduplicationAndVersioningSql().get(1));

        Assertions.assertEquals(dataErrorCheckSql, operations.deduplicationAndVersioningErrorChecksSql().get(MAX_DATA_ERRORS));
        Assertions.assertEquals(dataErrorsSql, operations.deduplicationAndVersioningErrorChecksSql().get(DATA_ERROR_ROWS));

        String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage WHERE stage.\"snapshot_id\" > 18972";
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

        String updateSql = "UPDATE \"mydb\".\"main\" as sink SET " +
            "sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\")) AND (stage.\"snapshot_id\" > 18972))," +
            "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\")) AND (stage.\"snapshot_id\" > 18972))," +
            "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\")) AND (stage.\"snapshot_id\" > 18972))," +
            "sink.\"biz_date\" = (SELECT stage.\"biz_date\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\")) AND (stage.\"snapshot_id\" > 18972))," +
            "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\")) AND (stage.\"snapshot_id\" > 18972))," +
            "sink.\"version\" = (SELECT stage.\"version\" FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\")) AND (stage.\"snapshot_id\" > 18972))," +
            "sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
            "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\")) AND (stage.\"snapshot_id\" > 18972))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"version\", \"batch_id\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"version\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM \"mydb\".\"staging\" as stage WHERE (NOT (EXISTS " +
                "(SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"id\" = stage.\"id\") AND " +
                "(sink.\"name\" = stage.\"name\")))) AND (stage.\"snapshot_id\" > 18972))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));

        Assertions.assertTrue(operations.deduplicationAndVersioningSql().isEmpty());
        Assertions.assertTrue(operations.deduplicationAndVersioningErrorChecksSql().isEmpty());

        String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage WHERE stage.\"snapshot_id\" > 18972";
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

        String updateSql = "UPDATE \"mydb\".\"main\" as sink SET " +
            "sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\"))," +
            "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\"))," +
            "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\"))," +
            "sink.\"biz_date\" = (SELECT stage.\"biz_date\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\"))," +
            "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\"))," +
            "sink.\"version\" = (SELECT stage.\"version\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\"))," +
            "sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
            "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (stage.\"version\" > sink.\"version\"))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"version\", \"batch_id\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"version\"," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\"))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));

        Assertions.assertTrue(operations.deduplicationAndVersioningSql().isEmpty());
        Assertions.assertTrue(operations.deduplicationAndVersioningErrorChecksSql().isEmpty());

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

        String updateSql = "UPDATE \"MYDB\".\"MAIN\" as sink " +
                "SET sink.\"ID\" = (SELECT stage.\"ID\" FROM \"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (stage.\"VERSION\" >= sink.\"VERSION\"))," +
                "sink.\"NAME\" = (SELECT stage.\"NAME\" FROM \"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (stage.\"VERSION\" >= sink.\"VERSION\"))," +
                "sink.\"AMOUNT\" = (SELECT stage.\"AMOUNT\" FROM \"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (stage.\"VERSION\" >= sink.\"VERSION\"))," +
                "sink.\"BIZ_DATE\" = (SELECT stage.\"BIZ_DATE\" FROM \"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (stage.\"VERSION\" >= sink.\"VERSION\"))," +
                "sink.\"DIGEST\" = (SELECT stage.\"DIGEST\" FROM \"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (stage.\"VERSION\" >= sink.\"VERSION\"))," +
                "sink.\"VERSION\" = (SELECT stage.\"VERSION\" FROM \"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (stage.\"VERSION\" >= sink.\"VERSION\"))," +
                "sink.\"BATCH_ID\" = (SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN') " +
                "WHERE EXISTS (SELECT * FROM \"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (stage.\"VERSION\" >= sink.\"VERSION\"))";

        String insertSql = "INSERT INTO \"MYDB\".\"MAIN\" (\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"DIGEST\", \"VERSION\", \"BATCH_ID\") " +
            "(SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"BIZ_DATE\",stage.\"DIGEST\",stage.\"VERSION\"," +
            "(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN') FROM \"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"MYDB\".\"MAIN\" as sink WHERE (sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\"))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQueryUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQueryWithUpperCase(), preActionsSqlList.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTempStagingTablePlusDigestWithVersionUpperCase, preActionsSqlList.get(2));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithUpperCase(), operations.metadataIngestSql().get(0));

        String insertTempStagingTable = "INSERT INTO \"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" " +
                "(\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"DIGEST\", \"VERSION\") " +
                "(SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"BIZ_DATE\",stage.\"DIGEST\",stage.\"VERSION\" FROM " +
                "(SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"BIZ_DATE\",stage.\"DIGEST\",stage.\"VERSION\"," +
                "DENSE_RANK() OVER (PARTITION BY stage.\"ID\",stage.\"NAME\" ORDER BY stage.\"VERSION\" DESC) as \"LEGEND_PERSISTENCE_RANK\" " +
                "FROM \"MYDB\".\"STAGING\" as stage) as stage WHERE stage.\"LEGEND_PERSISTENCE_RANK\" = 1)";

        Assertions.assertEquals(expectedTempStagingCleanupQueryInUpperCase, operations.deduplicationAndVersioningSql().get(0));
        Assertions.assertEquals(insertTempStagingTable, operations.deduplicationAndVersioningSql().get(1));

        Assertions.assertEquals(dataErrorCheckSqlUpperCase, operations.deduplicationAndVersioningErrorChecksSql().get(MAX_DATA_ERRORS));
        Assertions.assertEquals(dataErrorsSqlUpperCase, operations.deduplicationAndVersioningErrorChecksSql().get(DATA_ERROR_ROWS));
    }

    public RelationalSink getRelationalSink()
    {
        return AnsiSqlSink.get();
    }

    protected String getExpectedMetadataTableIngestQuery()
    {
        return AnsiTestArtifacts.expectedMetadataTableIngestQuery;
    }

    protected String getExpectedMetadataTableIngestQueryWithUpperCase()
    {
        return AnsiTestArtifacts.expectedMetadataTableIngestQueryWithUpperCase;
    }

    protected String getExpectedMetadataTableIngestQueryWithAdditionalMetadata()
    {
        return AnsiTestArtifacts.expectedMetadataTableIngestQueryWithAdditionalMetadata;
    }

    protected String getExpectedMetadataTableIngestQueryWithAdditionalMetadataWithUpperCase()
    {
        return AnsiTestArtifacts.expectedMetadataTableIngestQueryWithAdditionalMetadataWithUpperCase;
    }

    protected String getExpectedMetadataTableCreateQuery()
    {
        return AnsiTestArtifacts.expectedMetadataTableCreateQuery;
    }

    protected String getExpectedMetadataTableCreateQueryWithUpperCase()
    {
        return AnsiTestArtifacts.expectedMetadataTableCreateQueryWithUpperCase;
    }

    protected String getExpectedMetadataTableIngestQueryWithStagingFilters(String stagingFilters)
    {
        return "INSERT INTO batch_metadata " +
            "(\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\", \"batch_source_info\") " +
            "(SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata " +
            "WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')," +
            "'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE'," +
            String.format("PARSE_JSON('%s'))", stagingFilters);
    }

    protected String getExpectedMetadataTableIngestQueryWithStagingFiltersAndAdditionalMetadata(String stagingFilters)
    {
        return "INSERT INTO batch_metadata " +
            "(\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\", \"batch_source_info\", \"additional_metadata\") " +
            "(SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata " +
            "WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')," +
            "'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE'," +
            String.format("PARSE_JSON('%s'),", stagingFilters) +
            "PARSE_JSON('{\"watermark\":\"my_watermark_value\"}'))";
    }
}
