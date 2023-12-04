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

package org.finos.legend.engine.persistence.components.ingestmode.bitemporal;

import org.finos.legend.engine.persistence.components.AnsiTestArtifacts;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.testcases.ingestmode.bitemporal.BitemporalDeltaSourceSpecifiesFromAndThroughTestCases;
import org.junit.jupiter.api.Assertions;

import java.util.List;

import static org.finos.legend.engine.persistence.components.AnsiTestArtifacts.lockAcquiredQuery;
import static org.finos.legend.engine.persistence.components.AnsiTestArtifacts.lockInitializedQuery;

public class BitemporalDeltaSourceSpecifiesFromAndThroughTest extends BitemporalDeltaSourceSpecifiesFromAndThroughTestCases
{
    @Override
    public void verifyBitemporalDeltaBatchIdBasedNoDeleteIndNoDataSplits(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> initializeLockSql = operations.initializeLockSql();
        List<String> acquireLockSql = operations.acquireLockSql();
        List<String> postCleanupSql = operations.postCleanupSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1 " +
                "WHERE (sink.\"batch_id_out\" = 999999999) AND " +
                "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
                "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"validity_from_target\" = stage.\"validity_from_reference\") " +
                "AND (sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"validity_from_target\", \"validity_through_target\", \"digest\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"validity_through_reference\",stage.\"digest\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')," +
                "999999999 " +
                "FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
                "WHERE (sink.\"batch_id_out\" = 999999999) " +
                "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"validity_from_target\" = stage.\"validity_from_reference\"))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalStagingTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSql.get(2));
        Assertions.assertEquals(AnsiTestArtifacts.expectedLockInfoTableCreateQuery, preActionsSql.get(3));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        Assertions.assertEquals(lockInitializedQuery, initializeLockSql.get(0));
        Assertions.assertEquals(lockAcquiredQuery, acquireLockSql.get(0));
        Assertions.assertEquals(0, postCleanupSql.size());

        String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage";
        String rowsUpdated = "SELECT COUNT(*) as \"rowsUpdated\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1";
        String rowsDeleted = "SELECT 0 as \"rowsDeleted\"";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'))-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1) as \"rowsInserted\"";
        String rowsTerminated = "SELECT 0 as \"rowsTerminated\"";

        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyBitemporalDeltaBatchIdDateTimeBasedNoDeleteIndWithDataSplits(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink SET sink.\"batch_id_out\" = " +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata " +
                "WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1,sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000' " +
                "WHERE (sink.\"batch_id_out\" = 999999999) AND (EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE " +
                "((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) AND " +
                "((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
                "(sink.\"validity_from_target\" = stage.\"validity_from_reference\") AND (sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"validity_from_target\", " +
                "\"validity_through_target\", \"digest\", \"version\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"validity_through_reference\"," +
                "stage.\"digest\",stage.\"version\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata " +
                "WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'),999999999,'2000-01-01 00:00:00.000000','9999-12-31 23:59:59' " +
                "FROM \"mydb\".\"staging\" as stage WHERE (NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE " +
                "(sink.\"batch_id_out\" = 999999999) AND (sink.\"digest\" = stage.\"digest\") " +
                "AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
                "(sink.\"validity_from_target\" = stage.\"validity_from_reference\")))) AND " +
                "((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalMainTableWithVersionWithBatchIdDatetimeCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), operations.get(0).preActionsSql().get(1));

        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMilestoneQuery, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedUpsertQuery, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMilestoneQuery, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedUpsertQuery, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(0).metadataIngestSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(1).metadataIngestSql().get(0));
        Assertions.assertEquals(2, operations.size());

        String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= 1) AND (stage.\"data_split\" <= 1)";
        String rowsUpdated = "SELECT COUNT(*) as \"rowsUpdated\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1";
        String rowsDeleted = "SELECT 0 as \"rowsDeleted\"";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'))-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1) as \"rowsInserted\"";
        String rowsTerminated = "SELECT 0 as \"rowsTerminated\"";

        verifyStats(operations.get(0), incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyBitemporalDeltaBatchIdBasedWithDeleteIndNoDataSplits(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1 " +
                "WHERE (sink.\"batch_id_out\" = 999999999) AND " +
                "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
                "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"validity_from_target\" = stage.\"validity_from_reference\") " +
                "AND ((sink.\"digest\" <> stage.\"digest\") OR (stage.\"delete_indicator\" IN ('yes','1','true')))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"validity_from_target\", \"validity_through_target\", \"digest\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"validity_through_reference\",stage.\"digest\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')," +
                "999999999 " +
                "FROM \"mydb\".\"staging\" as stage " +
                "WHERE (NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
                "WHERE (sink.\"batch_id_out\" = 999999999) " +
                "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"validity_from_target\" = stage.\"validity_from_reference\")))) " +
                "AND (stage.\"delete_indicator\" NOT IN ('yes','1','true')))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage";
        String rowsUpdated = "SELECT COUNT(*) as \"rowsUpdated\" FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'))))";
        String rowsDeleted = "SELECT 0 as \"rowsDeleted\"";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'))-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'))))) as \"rowsInserted\"";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1)-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'))))) as \"rowsTerminated\"";

        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyBitemporalDeltaDatetimeBasedWithDeleteIndWithDataSplits(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink SET " +
                "sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000' " +
                "WHERE (sink.\"batch_time_out\" = '9999-12-31 23:59:59') AND " +
                "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
                "WHERE ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND " +
                "(stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) " +
                "AND (sink.\"validity_from_target\" = stage.\"validity_from_reference\") AND " +
                "((sink.\"digest\" <> stage.\"digest\") OR (stage.\"delete_indicator\" IN ('yes','1','true')))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"validity_from_target\", \"validity_through_target\", \"digest\", \"version\", " +
                "\"batch_time_in\", \"batch_time_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\"," +
                "stage.\"validity_through_reference\",stage.\"digest\",stage.\"version\",'2000-01-01 00:00:00.000000'," +
                "'9999-12-31 23:59:59' FROM \"mydb\".\"staging\" as stage WHERE " +
                "((NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '9999-12-31 23:59:59') " +
                "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND " +
                "(sink.\"name\" = stage.\"name\")) AND (sink.\"validity_from_target\" = stage.\"validity_from_reference\")))) " +
                "AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))) AND " +
                "(stage.\"delete_indicator\" NOT IN ('yes','1','true')))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalMainTableWithVersionBatchDateTimeCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), operations.get(0).preActionsSql().get(1));

        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMilestoneQuery, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedUpsertQuery, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMilestoneQuery, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedUpsertQuery, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(0).metadataIngestSql().get(0));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(1).metadataIngestSql().get(0));
        Assertions.assertEquals(2, operations.size());

        String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= 1) AND (stage.\"data_split\" <= 1)";
        String rowsUpdated = "SELECT COUNT(*) as \"rowsUpdated\" FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00.000000')))";
        String rowsDeleted = "SELECT 0 as \"rowsDeleted\"";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_in\" = '2000-01-01 00:00:00.000000')-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00.000000')))) as \"rowsInserted\"";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000')-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000') AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_time_in\" = '2000-01-01 00:00:00.000000')))) as \"rowsTerminated\"";
        verifyStats(operations.get(0), incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyBitemporalDeltaBatchIdBasedWithUpperCaseOptimizer(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"MYDB\".\"MAIN\" as sink " +
                "SET sink.\"BATCH_ID_OUT\" = (SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 " +
                "FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN')-1 " +
                "WHERE (sink.\"BATCH_ID_OUT\" = 999999999) AND (EXISTS (SELECT * FROM \"MYDB\".\"STAGING\" as stage " +
                "WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) " +
                "AND (sink.\"VALIDITY_FROM_TARGET\" = stage.\"VALIDITY_FROM_REFERENCE\") " +
                "AND (sink.\"DIGEST\" <> stage.\"DIGEST\")))";

        String expectedUpsertQuery = "INSERT INTO \"MYDB\".\"MAIN\" " +
                "(\"ID\", \"NAME\", \"AMOUNT\", \"VALIDITY_FROM_TARGET\", \"VALIDITY_THROUGH_TARGET\", \"DIGEST\", \"BATCH_ID_IN\", " +
                "\"BATCH_ID_OUT\") " +
                "(SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"VALIDITY_FROM_REFERENCE\",stage.\"VALIDITY_THROUGH_REFERENCE\"," +
                "stage.\"DIGEST\",(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN')" +
                ",999999999 " +
                "FROM \"MYDB\".\"STAGING\" as stage WHERE NOT (EXISTS " +
                "(SELECT * FROM \"MYDB\".\"MAIN\" as sink " +
                "WHERE (sink.\"BATCH_ID_OUT\" = 999999999) " +
                "AND (sink.\"DIGEST\" = stage.\"DIGEST\") " +
                "AND ((sink.\"ID\" = stage.\"ID\") " +
                "AND (sink.\"NAME\" = stage.\"NAME\")) " +
                "AND (sink.\"VALIDITY_FROM_TARGET\" = stage.\"VALIDITY_FROM_REFERENCE\"))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalMainTableCreateQueryUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQueryWithUpperCase(), preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithUpperCase(), metadataIngestSql.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as \"INCOMINGRECORDCOUNT\" FROM \"MYDB\".\"STAGING\" as stage";
        String rowsUpdated = "SELECT COUNT(*) as \"ROWSUPDATED\" FROM \"MYDB\".\"MAIN\" as sink WHERE sink.\"BATCH_ID_OUT\" = (SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN')-1";
        String rowsDeleted = "SELECT 0 as \"ROWSDELETED\"";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"MYDB\".\"MAIN\" as sink WHERE sink.\"BATCH_ID_IN\" = (SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN'))-(SELECT COUNT(*) FROM \"MYDB\".\"MAIN\" as sink WHERE sink.\"BATCH_ID_OUT\" = (SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN')-1) as \"ROWSINSERTED\"";
        String rowsTerminated = "SELECT 0 as \"ROWSTERMINATED\"";

        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyBitemporalDeltaBatchIdBasedWithUserDefinedInfiniteBatchId(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1 " +
                "WHERE (sink.\"batch_id_out\" = 123456) AND " +
                "(EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage " +
                "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"validity_from_target\" = stage.\"validity_from_reference\") " +
                "AND (sink.\"digest\" <> stage.\"digest\")))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"validity_from_target\", \"validity_through_target\", \"digest\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"validity_through_reference\",stage.\"digest\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')," +
                "123456 " +
                "FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
                "WHERE (sink.\"batch_id_out\" = 123456) " +
                "AND (sink.\"digest\" = stage.\"digest\") AND ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"validity_from_target\" = stage.\"validity_from_reference\"))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSql.get(1));
        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));

        String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage";
        String rowsUpdated = "SELECT COUNT(*) as \"rowsUpdated\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1";
        String rowsDeleted = "SELECT 0 as \"rowsDeleted\"";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'))-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1) as \"rowsInserted\"";
        String rowsTerminated = "SELECT 0 as \"rowsTerminated\"";

        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
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
