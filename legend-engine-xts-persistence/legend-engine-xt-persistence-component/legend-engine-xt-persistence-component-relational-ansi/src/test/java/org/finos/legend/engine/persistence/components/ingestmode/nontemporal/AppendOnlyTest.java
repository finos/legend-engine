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
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.testcases.ingestmode.nontemporal.AppendOnlyTestCases;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.AnsiTestArtifacts.lockAcquiredQuery;
import static org.finos.legend.engine.persistence.components.AnsiTestArtifacts.lockInitializedQuery;

public class AppendOnlyTest extends AppendOnlyTestCases
{
    String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage";
    String rowsInserted = "SELECT COUNT(*) as \"rowsInserted\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')";
    String rowsUpdated = "SELECT 0 as \"rowsUpdated\"";
    String rowsTerminated = "SELECT 0 as \"rowsTerminated\"";
    String rowsDeleted = "SELECT 0 as \"rowsDeleted\"";

    @Override
    public void verifyAppendOnlyNoAuditingNoDedupNoVersioningNoFilterExistingRecordsDeriveMainSchema(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metadataIngestSqlList = operations.metadataIngestSql();
        List<String> initializeLockSql = operations.initializeLockSql();
        List<String> acquireLockSql = operations.acquireLockSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
                "FROM \"mydb\".\"staging\" as stage)";
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTableCreateQueryWithNoPKs, preActionsSqlList.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseStagingTableCreateQueryWithNoPKs, preActionsSqlList.get(1));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSqlList.get(2));
        Assertions.assertEquals(AnsiTestArtifacts.expectedLockInfoTableCreateQuery, preActionsSqlList.get(3));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));

        Assertions.assertEquals(lockInitializedQuery, initializeLockSql.get(0));
        Assertions.assertEquals(lockAcquiredQuery, acquireLockSql.get(0));

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSqlList.get(0));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
    }

    @Override
    public void verifyAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecords(List<GeneratorResult> generatorResults, List<DataSplitRange> dataSplitRanges)
    {
        String insertSql  = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_update_time\", \"batch_id\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",'2000-01-01 00:00:00.000000',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
                "FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
                "WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, generatorResults.get(0).preActionsSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), generatorResults.get(0).preActionsSql().get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTempStagingTablePlusDigestWithCountAndDataSplit, generatorResults.get(0).preActionsSql().get(2));

        Assertions.assertEquals(AnsiTestArtifacts.expectedTempStagingCleanupQuery, generatorResults.get(0).deduplicationAndVersioningSql().get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedInsertIntoBaseTempStagingPlusDigestWithAllVersionAndFilterDuplicates, generatorResults.get(0).deduplicationAndVersioningSql().get(1));

        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(0)), generatorResults.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(1)), generatorResults.get(1).ingestSql().get(0));
        Assertions.assertEquals(2, generatorResults.size());

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), generatorResults.get(0).metadataIngestSql().get(0));

        // Stats
        String incomingRecordCount = "SELECT COALESCE(SUM(stage.\"legend_persistence_count\"),0) as \"incomingRecordCount\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
                "WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCount, dataSplitRanges.get(0)), generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCount, dataSplitRanges.get(1)), generatorResults.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }

    @Override
    public void verifyAppendOnlyWithAuditingFilterDuplicatesNoVersioningWithFilterExistingRecords(GeneratorResult queries)
    {
        List<String> preActionsSqlList = queries.preActionsSql();
        List<String> milestoningSqlList = queries.ingestSql();
        List<String> metadataIngestSqlList = queries.metadataIngestSql();
        List<String> deduplicationAndVersioningSql = queries.deduplicationAndVersioningSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_update_time\", \"batch_id\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",'2000-01-01 00:00:00.000000',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE ((sink.\"id\" = stage.\"id\") AND " +
                "(sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" = stage.\"digest\"))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSqlList.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTempStagingTablePlusDigestWithCount, preActionsSqlList.get(2));

        Assertions.assertEquals(AnsiTestArtifacts.expectedTempStagingCleanupQuery, deduplicationAndVersioningSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedInsertIntoBaseTempStagingPlusDigestWithFilterDuplicates, deduplicationAndVersioningSql.get(1));

        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSqlList.get(0));

        List<String> postActionsSql = queries.postActionsSql();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(AnsiTestArtifacts.expectedStagingCleanupQuery);
        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);

        // Stats
        Assertions.assertEquals(incomingRecordCount, queries.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, queries.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, queries.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, queries.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, queries.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }

    @Override
    public void verifyAppendOnlyWithAuditingFilterDuplicatesAllVersionWithFilterExistingRecords(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String insertSql = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_update_time\", \"batch_number\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",'2000-01-01 00:00:00.000000',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
                "FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
                "WHERE ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) AND " +
                "(NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
                "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
                "(sink.\"digest\" = stage.\"digest\")))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampAndBatchNumberCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), operations.get(0).preActionsSql().get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTempStagingTablePlusDigestWithCountAndDataSplit, operations.get(0).preActionsSql().get(2));

        Assertions.assertEquals(AnsiTestArtifacts.expectedTempStagingCleanupQuery, operations.get(0).deduplicationAndVersioningSql().get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedInsertIntoBaseTempStagingPlusDigestWithAllVersionAndFilterDuplicates, operations.get(0).deduplicationAndVersioningSql().get(1));

        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(2, operations.size());

        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithAdditionalMetadata(), operations.get(0).metadataIngestSql().get(0));

        // Stats
        String incomingRecordCount = "SELECT COALESCE(SUM(stage.\"legend_persistence_count\"),0) as \"incomingRecordCount\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
                "WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
        String rowsInserted = "SELECT COUNT(*) as \"rowsInserted\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_number\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')";
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCount, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCount, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }

    @Override
    public void verifyAppendOnlyWithUpperCaseOptimizer(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO \"MYDB\".\"MAIN\" " +
            "(\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"DIGEST\", \"BATCH_UPDATE_TIME\", \"BATCH_ID\") " +
            "(SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"BIZ_DATE\",stage.\"DIGEST\",'2000-01-01 00:00:00.000000',(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN') FROM \"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" as stage " +
            "WHERE NOT (EXISTS " +
            "(SELECT * FROM \"MYDB\".\"MAIN\" as sink WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (sink.\"DIGEST\" = stage.\"DIGEST\"))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQueryUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));
    }

    @Override
    public void verifyAppendOnlyWithLessColumnsInStaging(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"digest\", \"batch_update_time\", \"batch_id\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"digest\",'2000-01-01 00:00:00.000000',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE ((sink.\"id\" = stage.\"id\") AND " +
            "(sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" = stage.\"digest\"))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));
    }

    @Override
    public void verifyAppendOnlyWithAuditingFailOnDuplicatesMaxVersionWithFilterExistingRecords(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metadataIngestSqlList = operations.metadataIngestSql();
        List<String> deduplicationAndVersioningSql = operations.deduplicationAndVersioningSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_update_time\", \"batch_id\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",'2000-01-01 00:00:00.000000',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE ((sink.\"id\" = stage.\"id\") AND " +
            "(sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" = stage.\"digest\"))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSqlList.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTempStagingTablePlusDigestWithCount, preActionsSqlList.get(2));

        Assertions.assertEquals(AnsiTestArtifacts.expectedTempStagingCleanupQuery, deduplicationAndVersioningSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedInsertIntoBaseTempStagingPlusDigestWithMaxVersionAndFilterDuplicates, deduplicationAndVersioningSql.get(1));

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSqlList.get(0));

        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }

    @Override
    public void verifyAppendOnlyWithAuditingFilterDupsMaxVersionNoFilterExistingRecords(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metadataIngestSqlList = operations.metadataIngestSql();
        List<String> deduplicationAndVersioningSql = operations.deduplicationAndVersioningSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_update_time\", \"batch_id\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",'2000-01-01 00:00:00.000000',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage)";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSqlList.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTempStagingTablePlusDigestWithCount, preActionsSqlList.get(2));

        Assertions.assertEquals(AnsiTestArtifacts.expectedTempStagingCleanupQuery, deduplicationAndVersioningSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedInsertIntoBaseTempStagingPlusDigestWithMaxVersionAndFilterDuplicates, deduplicationAndVersioningSql.get(1));

        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSqlList.get(0));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }


    @Override
    @Test
    public void testAppendOnlyNoAuditingAllowDuplicatesNoVersioningNoFilterExistingRecordsUdfDigestGeneration()
    {
        // Digest UDF Generation not available for ANSI sink
    }

    @Override
    public void verifyAppendOnlyNoAuditingAllowDuplicatesNoVersioningNoFilterExistingRecordsUdfDigestGeneration(GeneratorResult operations)
    {
    }

    @Override
    @Test
    public void testAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecordsUdfDigestGeneration()
    {
        // Digest UDF Generation not available for ANSI sink
    }

    @Override
    public void verifyAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecordsUdfDigestGeneration(List<GeneratorResult> generatorResults, List<DataSplitRange> dataSplitRanges)
    {
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
}
