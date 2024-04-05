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

package org.finos.legend.engine.persistence.components.ingestmode.unitemporal;

import org.finos.legend.engine.persistence.components.AnsiTestArtifacts;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.testcases.ingestmode.unitemporal.UnitmemporalDeltaDateTimeBasedTestCases;
import org.junit.jupiter.api.Assertions;

import java.util.List;

import static org.finos.legend.engine.persistence.components.AnsiTestArtifacts.dataErrorCheckSqlWithBizDateVersion;
import static org.finos.legend.engine.persistence.components.AnsiTestArtifacts.maxDupsErrorCheckSql;
import static org.finos.legend.engine.persistence.components.AnsiTestArtifacts.dupRowsSql;
import static org.finos.legend.engine.persistence.components.AnsiTestArtifacts.dataErrorsSqlWithBizDateVersion;
import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.*;

public class UnitemporalDeltaDateTimeBasedTest extends UnitmemporalDeltaDateTimeBasedTestCases
{
    @Override
    public void verifyUnitemporalDeltaNoDeleteIndNoDedupNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink SET " +
                "sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000' " +
                "WHERE (sink.\"batch_time_out\" = '9999-12-31 23:59:59') AND " +
                "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
                "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
                "(sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_time_in\", \"batch_time_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
                "'2000-01-01 00:00:00.000000','9999-12-31 23:59:59' " +
                "FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
                "WHERE (sink.\"batch_time_out\" = '9999-12-31 23:59:59') " +
                "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableTimeBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));

        // Stats
        String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage";
        String rowsUpdated = "SELECT COUNT(*) as \"rowsUpdated\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000'";
        String rowsDeleted = "SELECT 0 as \"rowsDeleted\"";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_in\" = '2000-01-01 00:00:00.000000')-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000') as \"rowsInserted\"";
        String rowsTerminated = "SELECT 0 as \"rowsTerminated\"";
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalDeltaNoDeleteIndFailOnDupsAllVersionWithoutPerform(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink SET " +
                "sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000' " +
                "WHERE (sink.\"batch_time_out\" = '9999-12-31 23:59:59') AND " +
                "(EXISTS (SELECT * FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" as stage " +
                "WHERE ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
                "(sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_time_in\", \"batch_time_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
                "'2000-01-01 00:00:00.000000','9999-12-31 23:59:59' " +
                "FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" as stage " +
                "WHERE ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) AND " +
                "(NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
                "WHERE (sink.\"batch_time_out\" = '9999-12-31 23:59:59') " +
                "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\"))))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableTimeBasedCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), operations.get(0).preActionsSql().get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTempStagingTablePlusDigestWithDataSplitAndCount, operations.get(0).preActionsSql().get(2));

        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMilestoneQuery, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedUpsertQuery, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMilestoneQuery, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedUpsertQuery, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(0).metadataIngestSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(1).metadataIngestSql().get(0));
        Assertions.assertEquals(2, operations.size());

        String expectedInsertIntoBaseTempStagingWithFilterDuplicates = "INSERT INTO \"mydb\".\"staging_temp_staging_lp_yosulf\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"data_split\", \"legend_persistence_count\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"data_split\"," +
                "COUNT(*) as \"legend_persistence_count\" FROM \"mydb\".\"staging\" as stage " +
                "GROUP BY stage.\"id\", stage.\"name\", stage.\"amount\", stage.\"biz_date\", stage.\"digest\", stage.\"data_split\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedTempStagingCleanupQuery, operations.get(0).deduplicationAndVersioningSql().get(0));
        Assertions.assertEquals(expectedInsertIntoBaseTempStagingWithFilterDuplicates, operations.get(0).deduplicationAndVersioningSql().get(1));
        Assertions.assertEquals(maxDupsErrorCheckSql, operations.get(0).deduplicationAndVersioningErrorChecksSql().get(MAX_DUPLICATES));
        Assertions.assertEquals(dupRowsSql, operations.get(0).deduplicationAndVersioningErrorChecksSql().get(DUPLICATE_ROWS));

        // Stats
        String incomingRecordCount = "SELECT COALESCE(SUM(stage.\"legend_persistence_count\"),0) as \"incomingRecordCount\" FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
        String rowsUpdated = "SELECT COUNT(*) as \"rowsUpdated\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000'";
        String rowsDeleted = "SELECT 0 as \"rowsDeleted\"";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_in\" = '2000-01-01 00:00:00.000000')-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000') as \"rowsInserted\"";
        String rowsTerminated = "SELECT 0 as \"rowsTerminated\"";
        verifyStats(operations.get(0), enrichSqlWithDataSplits(incomingRecordCount, dataSplitRanges.get(0)), rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalDeltaWithDeleteIndNoDedupNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink SET " +
                "sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000' " +
                "WHERE " +
                "(sink.\"batch_time_out\" = '9999-12-31 23:59:59') AND " +
                "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
                "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) " +
                "AND ((sink.\"digest\" <> stage.\"digest\") OR (stage.\"delete_indicator\" IN ('yes','1','true')))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", " +
                "\"batch_time_in\", \"batch_time_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
                "'2000-01-01 00:00:00.000000','9999-12-31 23:59:59' FROM \"mydb\".\"staging\" as stage " +
                "WHERE (NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
                "WHERE (sink.\"batch_time_out\" = '9999-12-31 23:59:59') AND (sink.\"digest\" = stage.\"digest\") " +
                "AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\"))))) AND " +
                "(stage.\"delete_indicator\" NOT IN ('yes','1','true')))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableTimeBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));

        // Stats
        String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage";
        String rowsUpdated = "SELECT COUNT(*) as \"rowsUpdated\" FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00.000000')))";
        String rowsDeleted = "SELECT 0 as \"rowsDeleted\"";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_in\" = '2000-01-01 00:00:00.000000')-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00.000000')))) as \"rowsInserted\"";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000')-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00.000000')))) as \"rowsTerminated\"";
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalDeltaWithDeleteIndFilterDupsAllVersion(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink SET " +
                "sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000' " +
                "WHERE " +
                "(sink.\"batch_time_out\" = '9999-12-31 23:59:59') AND " +
                "(EXISTS (SELECT * FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" as stage " +
                "WHERE ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) " +
                "AND ((sink.\"digest\" <> stage.\"digest\") OR (stage.\"delete_indicator\" IN ('yes','1','true')))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", " +
                "\"batch_time_in\", \"batch_time_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
                "'2000-01-01 00:00:00.000000','9999-12-31 23:59:59' FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" as stage " +
                "WHERE ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) AND " +
                "(NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
                "WHERE (sink.\"batch_time_out\" = '9999-12-31 23:59:59') AND (sink.\"digest\" = stage.\"digest\") " +
                "AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\"))))) AND " +
                "(stage.\"delete_indicator\" NOT IN ('yes','1','true')))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableTimeBasedCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), operations.get(0).preActionsSql().get(1));

        String expectedInsertIntoBaseTempStagingPlusDigestWithAllVersionAndFilterDuplicates = "INSERT INTO \"mydb\".\"staging_temp_staging_lp_yosulf\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"delete_indicator\", \"legend_persistence_count\", \"data_split\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"delete_indicator\"," +
                "stage.\"legend_persistence_count\" as \"legend_persistence_count\"," +
                "DENSE_RANK() OVER (PARTITION BY stage.\"id\",stage.\"name\" ORDER BY stage.\"biz_date\" ASC) as \"data_split\" " +
                "FROM (SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",stage.\"delete_indicator\"," +
                "COUNT(*) as \"legend_persistence_count\" FROM \"mydb\".\"staging\" as stage GROUP BY stage.\"id\", stage.\"name\", " +
                "stage.\"amount\", stage.\"biz_date\", stage.\"digest\", stage.\"delete_indicator\") as stage)";

        Assertions.assertEquals(AnsiTestArtifacts.expectedTempStagingCleanupQuery, operations.get(0).deduplicationAndVersioningSql().get(0));
        Assertions.assertEquals(expectedInsertIntoBaseTempStagingPlusDigestWithAllVersionAndFilterDuplicates, operations.get(0).deduplicationAndVersioningSql().get(1));
        Assertions.assertEquals(dataErrorCheckSqlWithBizDateVersion, operations.get(0).deduplicationAndVersioningErrorChecksSql().get(MAX_DATA_ERRORS));
        Assertions.assertEquals(dataErrorsSqlWithBizDateVersion, operations.get(0).deduplicationAndVersioningErrorChecksSql().get(DATA_ERROR_ROWS));

        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMilestoneQuery, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedUpsertQuery, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMilestoneQuery, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedUpsertQuery, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(0).metadataIngestSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(1).metadataIngestSql().get(0));
        Assertions.assertEquals(2, operations.size());

        // Stats
        String incomingRecordCount = "SELECT COALESCE(SUM(stage.\"legend_persistence_count\"),0) as \"incomingRecordCount\" FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
        String rowsUpdated = "SELECT COUNT(*) as \"rowsUpdated\" FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00.000000')))";
        String rowsDeleted = "SELECT 0 as \"rowsDeleted\"";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_in\" = '2000-01-01 00:00:00.000000')-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00.000000')))) as \"rowsInserted\"";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000')-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00.000000')))) as \"rowsTerminated\"";
        verifyStats(operations.get(0), enrichSqlWithDataSplits(incomingRecordCount, dataSplitRanges.get(0)), rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalDeltaWithUpperCaseOptimizer(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"MYDB\".\"MAIN\" as sink SET sink.\"BATCH_TIME_OUT\" = '2000-01-01 00:00:00.000000' WHERE (sink.\"BATCH_TIME_OUT\" = '9999-12-31 23:59:59') AND (EXISTS (SELECT * FROM \"MYDB\".\"STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (sink.\"DIGEST\" <> stage.\"DIGEST\")))";

        String expectedUpsertQuery = "INSERT INTO \"MYDB\".\"MAIN\" (\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"DIGEST\", \"BATCH_TIME_IN\", \"BATCH_TIME_OUT\") (SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"BIZ_DATE\",stage.\"DIGEST\",'2000-01-01 00:00:00.000000','9999-12-31 23:59:59' FROM \"MYDB\".\"STAGING\" as stage WHERE NOT (EXISTS (SELECT * FROM \"MYDB\".\"MAIN\" as sink WHERE (sink.\"BATCH_TIME_OUT\" = '9999-12-31 23:59:59') AND (sink.\"DIGEST\" = stage.\"DIGEST\") AND ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableTimeBasedCreateQueryWithUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQueryWithUpperCase(), preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithUpperCase(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalDeltaWithCleanStagingData(GeneratorResult operations)
    {
        List<String> postActionsSql = operations.postActionsSql();
        Assertions.assertEquals(AnsiTestArtifacts.expectedStagingCleanupQuery, postActionsSql.get(0));
    }

    @Override
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

    protected String getExpectedMetadataTableCreateQuery()
    {
        return AnsiTestArtifacts.expectedMetadataTableCreateQuery;
    }

    protected String getExpectedMetadataTableCreateQueryWithUpperCase()
    {
        return AnsiTestArtifacts.expectedMetadataTableCreateQueryWithUpperCase;
    }
}
