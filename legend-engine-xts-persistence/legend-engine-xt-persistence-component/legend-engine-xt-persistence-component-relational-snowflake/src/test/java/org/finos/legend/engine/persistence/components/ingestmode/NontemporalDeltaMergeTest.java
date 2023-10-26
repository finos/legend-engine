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

import org.finos.legend.engine.persistence.components.AnsiTestArtifacts;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.nontemporal.NontemporalDeltaTest;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.snowflake.SnowflakeSink;
import org.junit.jupiter.api.Assertions;

import java.util.List;

public class NontemporalDeltaMergeTest extends NontemporalDeltaTest
{

    @Override
    public RelationalSink getRelationalSink()
    {
        return SnowflakeSink.get();
    }

    @Override
    public void verifyNontemporalDeltaNoAuditingNoDedupNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO \"mydb\".\"main\" as sink " +
            "USING \"mydb\".\"staging\" as stage " +
            "ON (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") " +
            "WHEN MATCHED AND sink.\"digest\" <> stage.\"digest\" " +
            "THEN UPDATE SET " +
            "sink.\"id\" = stage.\"id\"," +
            "sink.\"name\" = stage.\"name\"," +
            "sink.\"amount\" = stage.\"amount\"," +
            "sink.\"biz_date\" = stage.\"biz_date\"," +
            "sink.\"digest\" = stage.\"digest\" " +
            "WHEN NOT MATCHED THEN " +
            "INSERT (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\") " +
            "VALUES (stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedStagingTableWithDigestCreateQuery, preActionsSqlList.get(1));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));

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

        String mergeSql = "MERGE INTO \"mydb\".\"main\" as sink " +
                "USING \"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
                "ON (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") " +
                "WHEN MATCHED AND sink.\"digest\" <> stage.\"digest\" " +
                "THEN UPDATE SET " +
                "sink.\"id\" = stage.\"id\"," +
                "sink.\"name\" = stage.\"name\"," +
                "sink.\"amount\" = stage.\"amount\"," +
                "sink.\"biz_date\" = stage.\"biz_date\"," +
                "sink.\"digest\" = stage.\"digest\"," +
                "sink.\"batch_update_time\" = '2000-01-01 00:00:00.000000' " +
                "WHEN NOT MATCHED THEN INSERT " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_update_time\") " +
                "VALUES (stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",'2000-01-01 00:00:00.000000')";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));

        // Stats
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNonTemporalDeltaNoAuditingNoDedupAllVersion(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String mergeSql = "MERGE INTO \"mydb\".\"main\" as sink " +
                "USING (SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
                "WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) " +
                "as stage ON (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") " +
                "WHEN MATCHED AND sink.\"digest\" <> stage.\"digest\" " +
                "THEN UPDATE SET sink.\"id\" = stage.\"id\",sink.\"name\" = stage.\"name\",sink.\"amount\" = stage.\"amount\",sink.\"biz_date\" = stage.\"biz_date\",sink.\"digest\" = stage.\"digest\" " +
                "WHEN NOT MATCHED " +
                "THEN INSERT (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\") " +
                "VALUES (stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(mergeSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(mergeSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));

        // Stats
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplitsTempStagingTable, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplitsTempStagingTable, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNonTemporalDeltaNoAuditingNoDedupAllVersionWithoutPerform(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String mergeSql = "MERGE INTO \"mydb\".\"main\" as sink " +
                "USING (SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\" FROM \"mydb\".\"staging\" as stage " +
                "WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) " +
                "as stage ON (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") " +
                "WHEN MATCHED AND sink.\"digest\" <> stage.\"digest\" " +
                "THEN UPDATE SET sink.\"id\" = stage.\"id\",sink.\"name\" = stage.\"name\",sink.\"amount\" = stage.\"amount\",sink.\"biz_date\" = stage.\"biz_date\",sink.\"digest\" = stage.\"digest\" " +
                "WHEN NOT MATCHED " +
                "THEN INSERT (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\") " +
                "VALUES (stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(mergeSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(mergeSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));

        // Stats
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplits, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplits, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNonTemporalDeltaWithWithAuditingFailOnDupsAllVersion(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String mergeSql = "MERGE INTO \"mydb\".\"main\" as sink " +
                "USING (SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\" FROM \"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
                "WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) " +
                "as stage ON (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") " +
                "WHEN MATCHED AND sink.\"digest\" <> stage.\"digest\" " +
                "THEN UPDATE SET sink.\"id\" = stage.\"id\",sink.\"name\" = stage.\"name\",sink.\"amount\" = stage.\"amount\",sink.\"biz_date\" = stage.\"biz_date\",sink.\"digest\" = stage.\"digest\",sink.\"batch_update_time\" = '2000-01-01 00:00:00.000000' " +
                "WHEN NOT MATCHED " +
                "THEN INSERT (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_update_time\") " +
                "VALUES (stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",'2000-01-01 00:00:00.000000')";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(mergeSql, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(mergeSql, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));

        // Stats
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplitsWithDuplicates, dataSplitRanges.get(0)), operations.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCountWithSplitsWithDuplicates, dataSplitRanges.get(1)), operations.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
    }

    @Override
    public void verifyNontemporalDeltaNoAuditingWithDeleteIndicatorNoDedupNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO \"mydb\".\"main\" as sink " +
                "USING \"mydb\".\"staging\" as stage " +
                "ON (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") " +
                "WHEN MATCHED AND (sink.\"digest\" <> stage.\"digest\") AND (stage.\"delete_indicator\" NOT IN ('yes','1','true')) " +
                "THEN UPDATE SET " +
                "sink.\"id\" = stage.\"id\"," +
                "sink.\"name\" = stage.\"name\"," +
                "sink.\"amount\" = stage.\"amount\"," +
                "sink.\"biz_date\" = stage.\"biz_date\"," +
                "sink.\"digest\" = stage.\"digest\" " +
                "WHEN NOT MATCHED THEN " +
                "INSERT (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\") " +
                "VALUES (stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));

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

        String mergeSql = "MERGE INTO \"MYDB\".\"MAIN\" as sink USING \"MYDB\".\"STAGING\" as stage " +
                "ON (sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\") WHEN MATCHED " +
                "AND sink.\"DIGEST\" <> stage.\"DIGEST\" THEN UPDATE SET sink.\"ID\" = stage.\"ID\"," +
                "sink.\"NAME\" = stage.\"NAME\",sink.\"AMOUNT\" = stage.\"AMOUNT\"," +
                "sink.\"BIZ_DATE\" = stage.\"BIZ_DATE\",sink.\"DIGEST\" = stage.\"DIGEST\" " +
                "WHEN NOT MATCHED THEN INSERT (\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"DIGEST\") " +
                "VALUES (stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"BIZ_DATE\",stage.\"DIGEST\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQueryWithUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));
    }

    @Override
    public void verifyNontemporalDeltaWithLessColumnsInStaging(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO \"mydb\".\"main\" as sink " +
                "USING \"mydb\".\"staging\" as stage " +
                "ON (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") " +
                "WHEN MATCHED AND sink.\"digest\" <> stage.\"digest\" " +
                "THEN UPDATE SET " +
                "sink.\"id\" = stage.\"id\",sink.\"name\" = stage.\"name\",sink.\"amount\" = stage.\"amount\",sink.\"digest\" = stage.\"digest\" " +
                "WHEN NOT MATCHED THEN INSERT " +
                "(\"id\", \"name\", \"amount\", \"digest\") " +
                "VALUES (stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"digest\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));
    }

    @Override
    public void verifyNontemporalDeltaWithNoVersionAndStagingFilter(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String mergeSql = "MERGE INTO \"mydb\".\"main\" as sink " +
                "USING " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\" FROM \"mydb\".\"staging\" as stage WHERE (stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-03')) as stage " +
                "ON (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") " +
                "WHEN MATCHED AND sink.\"digest\" <> stage.\"digest\" " +
                "THEN UPDATE SET " +
                "sink.\"id\" = stage.\"id\"," +
                "sink.\"name\" = stage.\"name\"," +
                "sink.\"amount\" = stage.\"amount\"," +
                "sink.\"biz_date\" = stage.\"biz_date\"," +
                "sink.\"digest\" = stage.\"digest\" " +
                "WHEN NOT MATCHED THEN " +
                "INSERT (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\") " +
                "VALUES (stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage WHERE (stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-03')";
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

        String mergeSql = "MERGE INTO \"mydb\".\"main\" as sink " +
            "USING " +
            "\"mydb\".\"staging_legend_persistence_temp_staging\" as stage " +
            "ON (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") " +
            "WHEN MATCHED AND stage.\"version\" > sink.\"version\" " +
            "THEN UPDATE SET sink.\"id\" = stage.\"id\",sink.\"name\" = stage.\"name\",sink.\"amount\" = stage.\"amount\",sink.\"biz_date\" = stage.\"biz_date\",sink.\"digest\" = stage.\"digest\",sink.\"version\" = stage.\"version\" " +
            "WHEN NOT MATCHED THEN INSERT (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"version\") VALUES (stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"version\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));

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

        String mergeSql = "MERGE INTO \"mydb\".\"main\" as sink " +
            "USING " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"version\" FROM \"mydb\".\"staging\" as stage WHERE stage.\"snapshot_id\" > 18972) as stage " +
            "ON (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") " +
            "WHEN MATCHED AND stage.\"version\" > sink.\"version\" " +
            "THEN UPDATE SET sink.\"id\" = stage.\"id\",sink.\"name\" = stage.\"name\",sink.\"amount\" = stage.\"amount\",sink.\"biz_date\" = stage.\"biz_date\",sink.\"digest\" = stage.\"digest\",sink.\"version\" = stage.\"version\" " +
            "WHEN NOT MATCHED THEN INSERT (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"version\") VALUES (stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"version\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));

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

        String mergeSql = "MERGE INTO \"mydb\".\"main\" as sink " +
            "USING " +
            "\"mydb\".\"staging\" as stage " +
            "ON (sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\") " +
            "WHEN MATCHED AND stage.\"version\" > sink.\"version\" " +
            "THEN UPDATE SET sink.\"id\" = stage.\"id\",sink.\"name\" = stage.\"name\",sink.\"amount\" = stage.\"amount\",sink.\"biz_date\" = stage.\"biz_date\",sink.\"digest\" = stage.\"digest\",sink.\"version\" = stage.\"version\" " +
            "WHEN NOT MATCHED THEN INSERT (\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"version\") VALUES (stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"version\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));

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

        String mergeSql = "MERGE INTO \"MYDB\".\"MAIN\" as sink " +
            "USING " +
            "\"MYDB\".\"STAGING_LEGEND_PERSISTENCE_TEMP_STAGING\" as stage " +
            "ON (sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\") " +
            "WHEN MATCHED AND stage.\"VERSION\" >= sink.\"VERSION\" " +
            "THEN UPDATE SET sink.\"ID\" = stage.\"ID\",sink.\"NAME\" = stage.\"NAME\",sink.\"AMOUNT\" = stage.\"AMOUNT\",sink.\"BIZ_DATE\" = stage.\"BIZ_DATE\",sink.\"DIGEST\" = stage.\"DIGEST\",sink.\"VERSION\" = stage.\"VERSION\" " +
            "WHEN NOT MATCHED THEN INSERT (\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"DIGEST\", \"VERSION\") VALUES (stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"BIZ_DATE\",stage.\"DIGEST\",stage.\"VERSION\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusVersionCreateQueryUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(mergeSql, milestoningSqlList.get(0));
    }
}
