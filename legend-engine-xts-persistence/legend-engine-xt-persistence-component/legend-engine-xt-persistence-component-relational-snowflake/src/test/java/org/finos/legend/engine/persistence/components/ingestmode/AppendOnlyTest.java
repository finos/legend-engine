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
import org.finos.legend.engine.persistence.components.SnowflakeTestArtifacts;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.snowflake.SnowflakeSink;
import org.finos.legend.engine.persistence.components.scenarios.AppendOnlyScenarios;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class AppendOnlyTest extends org.finos.legend.engine.persistence.components.ingestmode.nontemporal.AppendOnlyTest
{
    String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage";
    String rowsUpdated = "SELECT 0 as \"rowsUpdated\"";
    String rowsTerminated = "SELECT 0 as \"rowsTerminated\"";
    String rowsDeleted = "SELECT 0 as \"rowsDeleted\"";

    @Override
    @Test
    public void testAppendOnlyNoAuditingAllowDuplicatesNoVersioningNoFilterExistingRecordsUdfDigestGeneration()
    {
        TestScenario scenario = new AppendOnlyScenarios().NO_AUDITING__NO_DEDUP__NO_VERSIONING__NO_FILTER_EXISTING_RECORDS__UDF_DIGEST_GENERATION();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyNoAuditingAllowDuplicatesNoVersioningNoFilterExistingRecordsUdfDigestGeneration(operations);
    }

    @Override
    public void verifyAppendOnlyNoAuditingAllowDuplicatesNoVersioningNoFilterExistingRecordsUdfDigestGeneration(GeneratorResult operations)
    {
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String insertSql = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\"," +
            "LAKEHOUSE_MD5(CONCAT('amount',stage.\"amount\",'biz_date',stage.\"biz_date\",'id',stage.\"id\",'name',stage.\"name\"))," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
            "FROM \"mydb\".\"staging\" as stage)";
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTableCreateQueryWithNoPKs, preActionsSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(0));

        // Stats
        String rowsInserted = "SELECT COUNT(*) as \"rowsInserted\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')";
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
        Assertions.assertEquals(rowsDeleted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, operations.postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
    }

    @Override
    @Test
    public void testAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecordsUdfDigestGeneration()
    {
        TestScenario scenario = new AppendOnlyScenarios().WITH_AUDITING__FAIL_ON_DUPS__ALL_VERSION__NO_FILTER_EXISTING_RECORDS__UDF_DIGEST_GENERATION();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .ingestRunId(ingestRunId)
            .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecordsUdfDigestGeneration(operations, dataSplitRangesOneToTwo);
    }

    @Override
    public void verifyAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecordsUdfDigestGeneration(List<GeneratorResult> generatorResults, List<DataSplitRange> dataSplitRanges)
    {
        String insertSql  = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_update_time\", \"batch_id\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\"," +
            "LAKEHOUSE_MD5(CONCAT('biz_date',stage.\"biz_date\",'name',stage.\"name\"))," +
            "'2000-01-01 00:00:00.000000',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
            "FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, generatorResults.get(0).preActionsSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), generatorResults.get(0).preActionsSql().get(1));
        Assertions.assertEquals(SnowflakeTestArtifacts.expectedBaseTempStagingTableWithCountAndDataSplit, generatorResults.get(0).preActionsSql().get(2));

        Assertions.assertEquals(AnsiTestArtifacts.expectedTempStagingCleanupQuery, generatorResults.get(0).deduplicationAndVersioningSql().get(0));
        Assertions.assertEquals(SnowflakeTestArtifacts.expectedInsertIntoBaseTempStagingWithAllVersionAndFilterDuplicates, generatorResults.get(0).deduplicationAndVersioningSql().get(1));

        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(0)), generatorResults.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(1)), generatorResults.get(1).ingestSql().get(0));
        Assertions.assertEquals(2, generatorResults.size());

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), generatorResults.get(0).metadataIngestSql().get(0));

        // Stats
        String incomingRecordCount = "SELECT COALESCE(SUM(stage.\"legend_persistence_count\"),0) as \"incomingRecordCount\" FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" as stage " +
            "WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
        String rowsInserted = "SELECT COUNT(*) as \"rowsInserted\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')";

        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCount, dataSplitRanges.get(0)), generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCount, dataSplitRanges.get(1)), generatorResults.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }

    @Override
    @Test
    public void testAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecordsUdfDigestGenerationTypeConversionUdf()
    {
        TestScenario scenario = new AppendOnlyScenarios().WITH_AUDITING__FAIL_ON_DUPS__ALL_VERSION__NO_FILTER_EXISTING_RECORDS__UDF_DIGEST_GENERATION__TYPE_CONVERSION_UDF();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .ingestRunId(ingestRunId)
            .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecordsUdfDigestGenerationTypeConversionUdf(operations, dataSplitRangesOneToTwo);
    }

    @Override
    public void verifyAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecordsUdfDigestGenerationTypeConversionUdf(List<GeneratorResult> generatorResults, List<DataSplitRange> dataSplitRanges)
    {
        String insertSql  = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_update_time\", \"batch_id\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\"," +
            "LAKEHOUSE_MD5(CONCAT('amount',doubleToString(stage.\"amount\"),'biz_date',dateToString(stage.\"biz_date\"),'name',stage.\"name\"))," +
            "'2000-01-01 00:00:00.000000',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
            "FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, generatorResults.get(0).preActionsSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), generatorResults.get(0).preActionsSql().get(1));
        Assertions.assertEquals(SnowflakeTestArtifacts.expectedBaseTempStagingTableWithCountAndDataSplit, generatorResults.get(0).preActionsSql().get(2));

        Assertions.assertEquals(AnsiTestArtifacts.expectedTempStagingCleanupQuery, generatorResults.get(0).deduplicationAndVersioningSql().get(0));
        Assertions.assertEquals(SnowflakeTestArtifacts.expectedInsertIntoBaseTempStagingWithAllVersionAndFilterDuplicates, generatorResults.get(0).deduplicationAndVersioningSql().get(1));

        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(0)), generatorResults.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(insertSql, dataSplitRanges.get(1)), generatorResults.get(1).ingestSql().get(0));
        Assertions.assertEquals(2, generatorResults.size());

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), generatorResults.get(0).metadataIngestSql().get(0));

        // Stats
        String incomingRecordCount = "SELECT COALESCE(SUM(stage.\"legend_persistence_count\"),0) as \"incomingRecordCount\" FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" as stage " +
            "WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
        String rowsInserted = "SELECT COUNT(*) as \"rowsInserted\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')";

        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCount, dataSplitRanges.get(0)), generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(enrichSqlWithDataSplits(incomingRecordCount, dataSplitRanges.get(1)), generatorResults.get(1).postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsUpdated, generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_UPDATED));
        Assertions.assertEquals(rowsDeleted, generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_DELETED));
        Assertions.assertEquals(rowsInserted, generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_INSERTED));
        Assertions.assertEquals(rowsTerminated, generatorResults.get(0).postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }

    @Override
    public RelationalSink getRelationalSink()
    {
        return SnowflakeSink.get();
    }

    @Override
    protected String getExpectedMetadataTableIngestQuery()
    {
        return SnowflakeTestArtifacts.expectedMetadataTableIngestQuery;
    }

    @Override
    protected String getExpectedMetadataTableIngestQueryWithUpperCase()
    {
        return SnowflakeTestArtifacts.expectedMetadataTableIngestQueryWithUpperCase;
    }

    @Override
    protected String getExpectedMetadataTableIngestQueryWithAdditionalMetadata()
    {
        return SnowflakeTestArtifacts.expectedMetadataTableIngestQueryWithAdditionalMetadata;
    }

    @Override
    protected String getExpectedMetadataTableIngestQueryWithAdditionalMetadataWithUpperCase()
    {
        return SnowflakeTestArtifacts.expectedMetadataTableIngestQueryWithAdditionalMetadataWithUpperCase;
    }

    protected String getExpectedMetadataTableCreateQuery()
    {
        return SnowflakeTestArtifacts.expectedMetadataTableCreateQuery;
    }

    protected String getExpectedMetadataTableCreateQueryWithUpperCase()
    {
        return SnowflakeTestArtifacts.expectedMetadataTableCreateQueryWithUpperCase;
    }
}
