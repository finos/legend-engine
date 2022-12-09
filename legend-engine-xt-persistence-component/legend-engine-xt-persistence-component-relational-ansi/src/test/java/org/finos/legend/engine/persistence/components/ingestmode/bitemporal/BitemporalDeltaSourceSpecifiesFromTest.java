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
import org.finos.legend.engine.persistence.components.testcases.ingestmode.bitemporal.BitemporalDeltaSourceSpecifiesFromTestCases;
import org.junit.jupiter.api.Assertions;

import java.util.List;

import static org.finos.legend.engine.persistence.components.AnsiTestArtifacts.expectedMetadataTableIngestQueryWithPlaceHolders;

public class BitemporalDeltaSourceSpecifiesFromTest extends BitemporalDeltaSourceSpecifiesFromTestCases
{

    String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM \"mydb\".\"staging\" as stage";
    String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1";
    String rowsDeleted = "SELECT 0 as rowsDeleted";
    String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) as rowsInserted";
    String rowsTerminated = "SELECT 0 as rowsTerminated";

    @Override
    public void verifyBitemporalDeltaBatchIdBasedNoDeleteIndNoDataSplits(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_reference\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\" FROM \"mydb\".\"staging\" as stage) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),MIN(legend_persistence_x.\"legend_persistence_end_date\")) as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),'9999-12-31 23:59:59') as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" < legend_persistence_y.\"legend_persistence_start_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_reference\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",legend_persistence_x.\"legend_persistence_end_date\" as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",MIN(legend_persistence_y.\"legend_persistence_start_date\") as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date,\"validity_through_target\" as legend_persistence_end_date " +
                "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "WHERE NOT (EXISTS " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage " +
                "WHERE ((legend_persistence_x.\"id\" = stage.\"id\") AND (legend_persistence_x.\"name\" = stage.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" = stage.\"validity_from_reference\")))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
                "WHERE (EXISTS " +
                "(SELECT * FROM \"mydb\".\"temp\" as temp " +
                "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
                "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
                "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyTempTableCreateQuery, preActionsSql.get(2));

        Assertions.assertEquals(expectedStageToTemp, milestoningSql.get(0));
        Assertions.assertEquals(expectedMainToTemp, milestoningSql.get(1));
        Assertions.assertEquals(expectedUpdateMain, milestoningSql.get(2));
        Assertions.assertEquals(expectedTempToMain, milestoningSql.get(3));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), milestoningSql.get(4));

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyBitemporalDeltaBatchIdBasedNoDeleteIndWithDataSplits(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_reference\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"data_split\" FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),MIN(legend_persistence_x.\"legend_persistence_end_date\")) as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),'9999-12-31 23:59:59') as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" < legend_persistence_y.\"legend_persistence_start_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_reference\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",legend_persistence_x.\"legend_persistence_end_date\" as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",MIN(legend_persistence_y.\"legend_persistence_start_date\") as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date,\"validity_through_target\" as legend_persistence_end_date " +
                "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "WHERE NOT (EXISTS " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage " +
                "WHERE (((legend_persistence_x.\"id\" = stage.\"id\") AND (legend_persistence_x.\"name\" = stage.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" = stage.\"validity_from_reference\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
                "WHERE (EXISTS " +
                "(SELECT * FROM \"mydb\".\"temp\" as temp " +
                "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
                "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
                "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyMainTableCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyTempTableCreateQuery, operations.get(0).preActionsSql().get(2));

        Assertions.assertEquals(enrichSqlWithDataSplits(expectedStageToTemp, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMainToTemp, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(expectedUpdateMain, operations.get(0).ingestSql().get(2));
        Assertions.assertEquals(expectedTempToMain, operations.get(0).ingestSql().get(3));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), operations.get(0).ingestSql().get(4));

        Assertions.assertEquals(enrichSqlWithDataSplits(expectedStageToTemp, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMainToTemp, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(expectedUpdateMain, operations.get(1).ingestSql().get(2));
        Assertions.assertEquals(expectedTempToMain, operations.get(1).ingestSql().get(3));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), operations.get(1).ingestSql().get(4));

        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableIngestQuery, operations.get(0).metadataIngestSql().get(0));
        Assertions.assertEquals(2, operations.size());

        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
        verifyStats(operations.get(0), enrichSqlWithDataSplits(incomingRecordCount,dataSplitRanges.get(0)), rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
        verifyStats(operations.get(1), enrichSqlWithDataSplits(incomingRecordCount,dataSplitRanges.get(1)), rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyBitemporalDeltaBatchIdBasedWithDeleteIndNoDataSplits(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_reference\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"delete_indicator\" FROM \"mydb\".\"staging\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),MIN(legend_persistence_x.\"legend_persistence_end_date\")) as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),'9999-12-31 23:59:59') as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" < legend_persistence_y.\"legend_persistence_start_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_reference\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",legend_persistence_x.\"legend_persistence_end_date\" as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",MIN(legend_persistence_y.\"legend_persistence_start_date\") as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date,\"validity_through_target\" as legend_persistence_end_date " +
                "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "WHERE NOT (EXISTS " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage " +
                "WHERE (((legend_persistence_x.\"id\" = stage.\"id\") AND (legend_persistence_x.\"name\" = stage.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" = stage.\"validity_from_reference\")) AND (stage.\"delete_indicator\" NOT IN ('yes','1','true'))))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
                "WHERE (EXISTS " +
                "(SELECT * FROM \"mydb\".\"temp\" as temp " +
                "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
                "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
                "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        String expectedMainToTempForDeletion = "INSERT INTO \"mydb\".\"tempWithDeleteIndicator\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\", \"delete_indicator\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_x.\"validity_through_target\" as legend_persistence_end_date,(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999,(CASE WHEN legend_persistence_y.\"delete_indicator\" IS NULL THEN 0 ELSE 1 END) " +
                "FROM " +
                "(SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = 999999999) " +
                "AND (EXISTS " +
                "(SELECT * FROM \"mydb\".\"staging\" as stage " +
                "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
                "((sink.\"validity_from_target\" = stage.\"validity_from_reference\") OR (sink.\"validity_through_target\" = stage.\"validity_from_reference\")) " +
                "AND (stage.\"delete_indicator\" IN ('yes','1','true'))))) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT * FROM \"mydb\".\"staging\" as stage) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"validity_from_reference\"))";

        String expectedUpdateMainForDeletion = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
                "WHERE (EXISTS " +
                "(SELECT * FROM \"mydb\".\"tempWithDeleteIndicator\" as tempWithDeleteIndicator " +
                "WHERE ((sink.\"id\" = tempWithDeleteIndicator.\"id\") AND (sink.\"name\" = tempWithDeleteIndicator.\"name\")) AND (sink.\"validity_from_target\" = tempWithDeleteIndicator.\"validity_from_target\"))) " +
                "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMainForDeletion = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"legend_persistence_start_date\" as legend_persistence_start_date,MAX(legend_persistence_y.\"validity_through_target\") as legend_persistence_end_date,legend_persistence_x.\"batch_id_in\",legend_persistence_x.\"batch_id_out\" FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,COALESCE(MIN(legend_persistence_y.\"validity_from_target\"),'9999-12-31 23:59:59') as legend_persistence_end_date,legend_persistence_x.\"batch_id_in\",legend_persistence_x.\"batch_id_out\" " +
                "FROM \"mydb\".\"tempWithDeleteIndicator\" as legend_persistence_x " +
                "LEFT OUTER JOIN \"mydb\".\"tempWithDeleteIndicator\" as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"validity_from_target\" > legend_persistence_x.\"validity_from_target\") AND (legend_persistence_y.\"delete_indicator\" = 0) " +
                "WHERE legend_persistence_x.\"delete_indicator\" = 0 " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"amount\", legend_persistence_x.\"digest\", legend_persistence_x.\"validity_from_target\", legend_persistence_x.\"batch_id_in\", legend_persistence_x.\"batch_id_out\") as legend_persistence_x " +
                "LEFT OUTER JOIN \"mydb\".\"tempWithDeleteIndicator\" as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"validity_through_target\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"validity_through_target\" <= legend_persistence_x.\"legend_persistence_end_date\") AND (legend_persistence_y.\"delete_indicator\" <> 0) " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"amount\", legend_persistence_x.\"digest\", legend_persistence_x.\"legend_persistence_start_date\", legend_persistence_x.\"batch_id_in\", legend_persistence_x.\"batch_id_out\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyTempTableCreateQuery, preActionsSql.get(2));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyTempTableWithDeleteIndicatorCreateQuery, preActionsSql.get(3));

        Assertions.assertEquals(expectedStageToTemp, milestoningSql.get(0));
        Assertions.assertEquals(expectedMainToTemp, milestoningSql.get(1));
        Assertions.assertEquals(expectedUpdateMain, milestoningSql.get(2));
        Assertions.assertEquals(expectedTempToMain, milestoningSql.get(3));
        Assertions.assertEquals(expectedMainToTempForDeletion, milestoningSql.get(4));
        Assertions.assertEquals(expectedUpdateMainForDeletion, milestoningSql.get(5));
        Assertions.assertEquals(expectedTempToMainForDeletion, milestoningSql.get(6));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), milestoningSql.get(7));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"tempWithDeleteIndicator\"", "tempWithDeleteIndicator"), milestoningSql.get(8));

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))) as rowsInserted";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1)-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))) as rowsTerminated";
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyBitemporalDeltaBatchIdBasedWithDeleteIndWithDataSplits(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String tempName = operations.get(0).preActionsSql().get(2).split("CREATE TABLE IF NOT EXISTS ")[1].split("\\(")[0];
        String tempWithDeleteIndicatorName = operations.get(0).preActionsSql().get(3).split("CREATE TABLE IF NOT EXISTS ")[1].split("\\(")[0];

        String expectedBitemporalFromOnlyDefaultTempTableCreateQuery = "CREATE TABLE IF NOT EXISTS " + tempName +
                "(\"id\" INTEGER," +
                "\"name\" VARCHAR," +
                "\"amount\" DOUBLE," +
                "\"digest\" VARCHAR," +
                "\"batch_id_in\" INTEGER," +
                "\"batch_id_out\" INTEGER," +
                "\"validity_from_target\" DATETIME," +
                "\"validity_through_target\" DATETIME," +
                "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

        String expectedBitemporalFromOnlyDefaultTempTableWithDeleteIndicatorCreateQuery = "CREATE TABLE IF NOT EXISTS " + tempWithDeleteIndicatorName +
                "(\"id\" INTEGER," +
                "\"name\" VARCHAR," +
                "\"amount\" DOUBLE," +
                "\"digest\" VARCHAR," +
                "\"batch_id_in\" INTEGER," +
                "\"batch_id_out\" INTEGER," +
                "\"validity_from_target\" DATETIME," +
                "\"validity_through_target\" DATETIME," +
                "\"delete_indicator\" BOOLEAN," +
                "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

        String expectedStageToTemp = "INSERT INTO " + tempName + " " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_reference\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"delete_indicator\",stage.\"data_split\" FROM \"mydb\".\"staging\" as stage WHERE (stage.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),MIN(legend_persistence_x.\"legend_persistence_end_date\")) as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),'9999-12-31 23:59:59') as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage WHERE (stage.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" < legend_persistence_y.\"legend_persistence_start_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage WHERE (stage.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_reference\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedMainToTemp = "INSERT INTO " + tempName + " " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",legend_persistence_x.\"legend_persistence_end_date\" as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",MIN(legend_persistence_y.\"legend_persistence_start_date\") as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date,\"validity_through_target\" as legend_persistence_end_date " +
                "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage WHERE (stage.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "WHERE NOT (EXISTS " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage " +
                "WHERE ((((legend_persistence_x.\"id\" = stage.\"id\") AND (legend_persistence_x.\"name\" = stage.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" = stage.\"validity_from_reference\")) AND (stage.\"delete_indicator\" NOT IN ('yes','1','true'))) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
                "WHERE (EXISTS " +
                "(SELECT * FROM " + tempName + " as legend_persistence_temp " +
                "WHERE ((sink.\"id\" = legend_persistence_temp.\"id\") AND (sink.\"name\" = legend_persistence_temp.\"name\")) AND (sink.\"validity_from_target\" = legend_persistence_temp.\"validity_from_target\"))) " +
                "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
                "(SELECT legend_persistence_temp.\"id\",legend_persistence_temp.\"name\",legend_persistence_temp.\"amount\",legend_persistence_temp.\"digest\",legend_persistence_temp.\"batch_id_in\",legend_persistence_temp.\"batch_id_out\",legend_persistence_temp.\"validity_from_target\",legend_persistence_temp.\"validity_through_target\" FROM " + tempName + " as legend_persistence_temp)";

        String expectedMainToTempForDeletion = "INSERT INTO " + tempWithDeleteIndicatorName + " " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\", \"delete_indicator\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_x.\"validity_through_target\" as legend_persistence_end_date,(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999,(CASE WHEN legend_persistence_y.\"delete_indicator\" IS NULL THEN 0 ELSE 1 END) " +
                "FROM " +
                "(SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = 999999999) " +
                "AND (EXISTS " +
                "(SELECT * FROM \"mydb\".\"staging\" as stage " +
                "WHERE (((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
                "((sink.\"validity_from_target\" = stage.\"validity_from_reference\") OR (sink.\"validity_through_target\" = stage.\"validity_from_reference\")) " +
                "AND (stage.\"delete_indicator\" IN ('yes','1','true'))) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))))) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT * FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"validity_from_reference\"))";

        String expectedUpdateMainForDeletion = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
                "WHERE (EXISTS " +
                "(SELECT * FROM " + tempWithDeleteIndicatorName + " as legend_persistence_tempWithDeleteIndicator " +
                "WHERE ((sink.\"id\" = legend_persistence_tempWithDeleteIndicator.\"id\") AND (sink.\"name\" = legend_persistence_tempWithDeleteIndicator.\"name\")) AND (sink.\"validity_from_target\" = legend_persistence_tempWithDeleteIndicator.\"validity_from_target\"))) " +
                "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMainForDeletion = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"legend_persistence_start_date\" as legend_persistence_start_date,MAX(legend_persistence_y.\"validity_through_target\") as legend_persistence_end_date,legend_persistence_x.\"batch_id_in\",legend_persistence_x.\"batch_id_out\" FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,COALESCE(MIN(legend_persistence_y.\"validity_from_target\"),'9999-12-31 23:59:59') as legend_persistence_end_date,legend_persistence_x.\"batch_id_in\",legend_persistence_x.\"batch_id_out\" " +
                "FROM " + tempWithDeleteIndicatorName + " as legend_persistence_x " +
                "LEFT OUTER JOIN " + tempWithDeleteIndicatorName + " as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"validity_from_target\" > legend_persistence_x.\"validity_from_target\") AND (legend_persistence_y.\"delete_indicator\" = 0) " +
                "WHERE legend_persistence_x.\"delete_indicator\" = 0 " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"amount\", legend_persistence_x.\"digest\", legend_persistence_x.\"validity_from_target\", legend_persistence_x.\"batch_id_in\", legend_persistence_x.\"batch_id_out\") as legend_persistence_x " +
                "LEFT OUTER JOIN " + tempWithDeleteIndicatorName + " as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"validity_through_target\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"validity_through_target\" <= legend_persistence_x.\"legend_persistence_end_date\") AND (legend_persistence_y.\"delete_indicator\" <> 0) " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"amount\", legend_persistence_x.\"digest\", legend_persistence_x.\"legend_persistence_start_date\", legend_persistence_x.\"batch_id_in\", legend_persistence_x.\"batch_id_out\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyMainTableCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));
        Assertions.assertEquals(expectedBitemporalFromOnlyDefaultTempTableCreateQuery, operations.get(0).preActionsSql().get(2));
        Assertions.assertEquals(expectedBitemporalFromOnlyDefaultTempTableWithDeleteIndicatorCreateQuery, operations.get(0).preActionsSql().get(3));

        Assertions.assertEquals(enrichSqlWithDataSplits(expectedStageToTemp, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMainToTemp, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(expectedUpdateMain, operations.get(0).ingestSql().get(2));
        Assertions.assertEquals(expectedTempToMain, operations.get(0).ingestSql().get(3));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMainToTempForDeletion, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(4));
        Assertions.assertEquals(expectedUpdateMainForDeletion, operations.get(0).ingestSql().get(5));
        Assertions.assertEquals(expectedTempToMainForDeletion, operations.get(0).ingestSql().get(6));
        Assertions.assertEquals(getExpectedCleanupSql(tempName, "legend_persistence_temp"), operations.get(0).ingestSql().get(7));
        Assertions.assertEquals(getExpectedCleanupSql(tempWithDeleteIndicatorName, "legend_persistence_tempWithDeleteIndicator"), operations.get(0).ingestSql().get(8));

        Assertions.assertEquals(enrichSqlWithDataSplits(expectedStageToTemp, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMainToTemp, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(expectedUpdateMain, operations.get(1).ingestSql().get(2));
        Assertions.assertEquals(expectedTempToMain, operations.get(1).ingestSql().get(3));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMainToTempForDeletion, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(4));
        Assertions.assertEquals(expectedUpdateMainForDeletion, operations.get(1).ingestSql().get(5));
        Assertions.assertEquals(expectedTempToMainForDeletion, operations.get(1).ingestSql().get(6));
        Assertions.assertEquals(getExpectedCleanupSql(tempName, "legend_persistence_temp"), operations.get(1).ingestSql().get(7));
        Assertions.assertEquals(getExpectedCleanupSql(tempWithDeleteIndicatorName, "legend_persistence_tempWithDeleteIndicator"), operations.get(1).ingestSql().get(8));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(0).metadataIngestSql().get(0));
        Assertions.assertEquals(2, operations.size());

        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))) as rowsInserted";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1)-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))) as rowsTerminated";
        verifyStats(operations.get(0), enrichSqlWithDataSplits(incomingRecordCount,dataSplitRanges.get(0)), rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
        verifyStats(operations.get(1), enrichSqlWithDataSplits(incomingRecordCount,dataSplitRanges.get(1)), rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyBitemporalDeltaBatchIdBasedNoDeleteIndNoDataSplitsFilterDuplicates(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedStageToStageWithoutDuplicates = "INSERT INTO \"mydb\".\"stagingWithoutDuplicates\" " +
                "(\"id\", \"name\", \"amount\", \"validity_from_reference\", \"digest\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\" FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"digest\" = stage.\"digest\") AND (sink.\"batch_id_out\" = 999999999))))";

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_reference\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\" FROM \"mydb\".\"stagingWithoutDuplicates\" as stage) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),MIN(legend_persistence_x.\"legend_persistence_end_date\")) as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),'9999-12-31 23:59:59') as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" < legend_persistence_y.\"legend_persistence_start_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_reference\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",legend_persistence_x.\"legend_persistence_end_date\" as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",MIN(legend_persistence_y.\"legend_persistence_start_date\") as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date,\"validity_through_target\" as legend_persistence_end_date " +
                "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "WHERE NOT (EXISTS " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage " +
                "WHERE ((legend_persistence_x.\"id\" = stage.\"id\") AND (legend_persistence_x.\"name\" = stage.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" = stage.\"validity_from_reference\")))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
                "WHERE (EXISTS " +
                "(SELECT * FROM \"mydb\".\"temp\" as temp " +
                "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
                "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
                "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyTempTableCreateQuery, preActionsSql.get(2));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyStageWithoutDuplicatesTableCreateQuery, preActionsSql.get(3));

        Assertions.assertEquals(expectedStageToStageWithoutDuplicates, milestoningSql.get(0));
        Assertions.assertEquals(expectedStageToTemp, milestoningSql.get(1));
        Assertions.assertEquals(expectedMainToTemp, milestoningSql.get(2));
        Assertions.assertEquals(expectedUpdateMain, milestoningSql.get(3));
        Assertions.assertEquals(expectedTempToMain, milestoningSql.get(4));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), milestoningSql.get(5));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"stagingWithoutDuplicates\"", "stage"), milestoningSql.get(6));

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyBitemporalDeltaBatchIdBasedNoDeleteIndWithDataSplitsFilterDuplicates(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String expectedStageToStageWithoutDuplicates = "INSERT INTO \"mydb\".\"stagingWithoutDuplicates\" " +
                "(\"id\", \"name\", \"amount\", \"validity_from_reference\", \"digest\", \"data_split\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"data_split\" FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"digest\" = stage.\"digest\") AND (sink.\"batch_id_out\" = 999999999))))";

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_reference\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"data_split\" FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),MIN(legend_persistence_x.\"legend_persistence_end_date\")) as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),'9999-12-31 23:59:59') as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" < legend_persistence_y.\"legend_persistence_start_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_reference\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",legend_persistence_x.\"legend_persistence_end_date\" as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",MIN(legend_persistence_y.\"legend_persistence_start_date\") as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date,\"validity_through_target\" as legend_persistence_end_date " +
                "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "WHERE NOT (EXISTS " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage " +
                "WHERE (((legend_persistence_x.\"id\" = stage.\"id\") AND (legend_persistence_x.\"name\" = stage.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" = stage.\"validity_from_reference\")) AND ((stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
                "WHERE (EXISTS " +
                "(SELECT * FROM \"mydb\".\"temp\" as temp " +
                "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
                "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
                "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyMainTableCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyTempTableCreateQuery, operations.get(0).preActionsSql().get(2));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyStageWithDataSplitWithoutDuplicatesTableCreateQuery, operations.get(0).preActionsSql().get(3));

        Assertions.assertEquals(expectedStageToStageWithoutDuplicates, operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedStageToTemp, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMainToTemp, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(2));
        Assertions.assertEquals(expectedUpdateMain, operations.get(0).ingestSql().get(3));
        Assertions.assertEquals(expectedTempToMain, operations.get(0).ingestSql().get(4));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), operations.get(0).ingestSql().get(5));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"stagingWithoutDuplicates\"", "stage"), operations.get(0).ingestSql().get(6));

        Assertions.assertEquals(expectedStageToStageWithoutDuplicates, operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedStageToTemp, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMainToTemp, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(2));
        Assertions.assertEquals(expectedUpdateMain, operations.get(1).ingestSql().get(3));
        Assertions.assertEquals(expectedTempToMain, operations.get(1).ingestSql().get(4));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), operations.get(1).ingestSql().get(5));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"stagingWithoutDuplicates\"", "stage"), operations.get(1).ingestSql().get(6));

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(0).metadataIngestSql().get(0));

        Assertions.assertEquals(2, operations.size());
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
        verifyStats(operations.get(0), enrichSqlWithDataSplits(incomingRecordCount,dataSplitRanges.get(0)), rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
        verifyStats(operations.get(1), enrichSqlWithDataSplits(incomingRecordCount,dataSplitRanges.get(1)), rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyBitemporalDeltaBatchIdBasedWithDeleteIndNoDataSplitsFilterDuplicates(GeneratorResult operations)
    {

        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedStageToStageWithoutDuplicates = "INSERT INTO \"mydb\".\"stagingWithoutDuplicates\" " +
                "(\"id\", \"name\", \"amount\", \"validity_from_reference\", \"digest\", \"delete_indicator\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"delete_indicator\" FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"digest\" = stage.\"digest\") AND (sink.\"batch_id_out\" = 999999999))))";

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_reference\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"delete_indicator\" FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),MIN(legend_persistence_x.\"legend_persistence_end_date\")) as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),'9999-12-31 23:59:59') as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" < legend_persistence_y.\"legend_persistence_start_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_reference\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",legend_persistence_x.\"legend_persistence_end_date\" as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",MIN(legend_persistence_y.\"legend_persistence_start_date\") as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date,\"validity_through_target\" as legend_persistence_end_date " +
                "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage WHERE stage.\"delete_indicator\" NOT IN ('yes','1','true')) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "WHERE NOT (EXISTS " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"stagingWithoutDuplicates\" as stage " +
                "WHERE (((legend_persistence_x.\"id\" = stage.\"id\") AND (legend_persistence_x.\"name\" = stage.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" = stage.\"validity_from_reference\")) AND (stage.\"delete_indicator\" NOT IN ('yes','1','true'))))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
                "WHERE (EXISTS " +
                "(SELECT * FROM \"mydb\".\"temp\" as temp " +
                "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
                "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
                "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        String expectedMainToTempForDeletion = "INSERT INTO \"mydb\".\"tempWithDeleteIndicator\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\", \"delete_indicator\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_x.\"validity_through_target\" as legend_persistence_end_date,(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999,(CASE WHEN legend_persistence_y.\"delete_indicator\" IS NULL THEN 0 ELSE 1 END) " +
                "FROM " +
                "(SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = 999999999) " +
                "AND (EXISTS " +
                "(SELECT * FROM \"mydb\".\"stagingWithoutDuplicates\" as stage " +
                "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
                "((sink.\"validity_from_target\" = stage.\"validity_from_reference\") OR (sink.\"validity_through_target\" = stage.\"validity_from_reference\")) " +
                "AND (stage.\"delete_indicator\" IN ('yes','1','true'))))) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT * FROM \"mydb\".\"stagingWithoutDuplicates\" as stage) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"validity_from_reference\"))";

        String expectedUpdateMainForDeletion = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
                "WHERE (EXISTS " +
                "(SELECT * FROM \"mydb\".\"tempWithDeleteIndicator\" as tempWithDeleteIndicator " +
                "WHERE ((sink.\"id\" = tempWithDeleteIndicator.\"id\") AND (sink.\"name\" = tempWithDeleteIndicator.\"name\")) AND (sink.\"validity_from_target\" = tempWithDeleteIndicator.\"validity_from_target\"))) " +
                "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMainForDeletion = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"legend_persistence_start_date\" as legend_persistence_start_date,MAX(legend_persistence_y.\"validity_through_target\") as legend_persistence_end_date,legend_persistence_x.\"batch_id_in\",legend_persistence_x.\"batch_id_out\" FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,COALESCE(MIN(legend_persistence_y.\"validity_from_target\"),'9999-12-31 23:59:59') as legend_persistence_end_date,legend_persistence_x.\"batch_id_in\",legend_persistence_x.\"batch_id_out\" " +
                "FROM \"mydb\".\"tempWithDeleteIndicator\" as legend_persistence_x " +
                "LEFT OUTER JOIN \"mydb\".\"tempWithDeleteIndicator\" as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"validity_from_target\" > legend_persistence_x.\"validity_from_target\") AND (legend_persistence_y.\"delete_indicator\" = 0) " +
                "WHERE legend_persistence_x.\"delete_indicator\" = 0 " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"amount\", legend_persistence_x.\"digest\", legend_persistence_x.\"validity_from_target\", legend_persistence_x.\"batch_id_in\", legend_persistence_x.\"batch_id_out\") as legend_persistence_x " +
                "LEFT OUTER JOIN \"mydb\".\"tempWithDeleteIndicator\" as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"validity_through_target\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"validity_through_target\" <= legend_persistence_x.\"legend_persistence_end_date\") AND (legend_persistence_y.\"delete_indicator\" <> 0) " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"amount\", legend_persistence_x.\"digest\", legend_persistence_x.\"legend_persistence_start_date\", legend_persistence_x.\"batch_id_in\", legend_persistence_x.\"batch_id_out\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyTempTableCreateQuery, preActionsSql.get(2));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyTempTableWithDeleteIndicatorCreateQuery, preActionsSql.get(3));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyStageWithDeleteIndicatorWithoutDuplicatesTableCreateQuery, preActionsSql.get(4));

        Assertions.assertEquals(expectedStageToStageWithoutDuplicates, milestoningSql.get(0));
        Assertions.assertEquals(expectedStageToTemp, milestoningSql.get(1));
        Assertions.assertEquals(expectedMainToTemp, milestoningSql.get(2));
        Assertions.assertEquals(expectedUpdateMain, milestoningSql.get(3));
        Assertions.assertEquals(expectedTempToMain, milestoningSql.get(4));
        Assertions.assertEquals(expectedMainToTempForDeletion, milestoningSql.get(5));
        Assertions.assertEquals(expectedUpdateMainForDeletion, milestoningSql.get(6));
        Assertions.assertEquals(expectedTempToMainForDeletion, milestoningSql.get(7));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), milestoningSql.get(8));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"tempWithDeleteIndicator\"", "tempWithDeleteIndicator"), milestoningSql.get(9));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"stagingWithoutDuplicates\"", "stage"), milestoningSql.get(10));

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))) as rowsInserted";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1)-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))) as rowsTerminated";
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyBitemporalDeltaBatchIdBasedWithDeleteIndWithDataSplitsFilterDuplicates(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges)
    {
        String tempName = operations.get(0).preActionsSql().get(2).split("CREATE TABLE IF NOT EXISTS ")[1].split("\\(")[0];
        String tempWithDeleteIndicatorName = operations.get(0).preActionsSql().get(3).split("CREATE TABLE IF NOT EXISTS ")[1].split("\\(")[0];
        String stageWithoutDuplicatesName = operations.get(0).preActionsSql().get(4).split("CREATE TABLE IF NOT EXISTS ")[1].split("\\(")[0];

        String expectedBitemporalFromOnlyDefaultTempTableCreateQuery = "CREATE TABLE IF NOT EXISTS " + tempName +
                "(\"id\" INTEGER," +
                "\"name\" VARCHAR," +
                "\"amount\" DOUBLE," +
                "\"digest\" VARCHAR," +
                "\"batch_id_in\" INTEGER," +
                "\"batch_id_out\" INTEGER," +
                "\"validity_from_target\" DATETIME," +
                "\"validity_through_target\" DATETIME," +
                "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

        String expectedBitemporalFromOnlyDefaultTempTableWithDeleteIndicatorCreateQuery = "CREATE TABLE IF NOT EXISTS " + tempWithDeleteIndicatorName +
                "(\"id\" INTEGER," +
                "\"name\" VARCHAR," +
                "\"amount\" DOUBLE," +
                "\"digest\" VARCHAR," +
                "\"batch_id_in\" INTEGER," +
                "\"batch_id_out\" INTEGER," +
                "\"validity_from_target\" DATETIME," +
                "\"validity_through_target\" DATETIME," +
                "\"delete_indicator\" BOOLEAN," +
                "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"validity_from_target\"))";

        String expectedBitemporalFromOnlyStageWithDeleteIndicatorWithDataSplitWithoutDuplicatesTableCreateQuery = "CREATE TABLE IF NOT EXISTS " + stageWithoutDuplicatesName +
                "(\"id\" INTEGER," +
                "\"name\" VARCHAR," +
                "\"amount\" DOUBLE," +
                "\"validity_from_reference\" DATETIME," +
                "\"digest\" VARCHAR," +
                "\"delete_indicator\" VARCHAR," +
                "\"data_split\" BIGINT," +
                "PRIMARY KEY (\"id\", \"name\", \"validity_from_reference\", \"data_split\"))";

        String expectedStageToStageWithoutDuplicates = "INSERT INTO " + stageWithoutDuplicatesName + " " +
                "(\"id\", \"name\", \"amount\", \"validity_from_reference\", \"digest\", \"delete_indicator\", \"data_split\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\",stage.\"delete_indicator\",stage.\"data_split\" FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"digest\" = stage.\"digest\") AND (sink.\"batch_id_out\" = 999999999))))";

        String expectedStageToTemp = "INSERT INTO " + tempName + " " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_reference\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT legend_persistence_stageWithoutDuplicates.\"id\",legend_persistence_stageWithoutDuplicates.\"name\",legend_persistence_stageWithoutDuplicates.\"amount\",legend_persistence_stageWithoutDuplicates.\"validity_from_reference\",legend_persistence_stageWithoutDuplicates.\"digest\",legend_persistence_stageWithoutDuplicates.\"delete_indicator\",legend_persistence_stageWithoutDuplicates.\"data_split\" FROM " + stageWithoutDuplicatesName + " as legend_persistence_stageWithoutDuplicates WHERE (legend_persistence_stageWithoutDuplicates.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((legend_persistence_stageWithoutDuplicates.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (legend_persistence_stageWithoutDuplicates.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),MIN(legend_persistence_x.\"legend_persistence_end_date\")) as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),'9999-12-31 23:59:59') as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM " + stageWithoutDuplicatesName + " as legend_persistence_stageWithoutDuplicates WHERE (legend_persistence_stageWithoutDuplicates.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((legend_persistence_stageWithoutDuplicates.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (legend_persistence_stageWithoutDuplicates.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" < legend_persistence_y.\"legend_persistence_start_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM " + stageWithoutDuplicatesName + " as legend_persistence_stageWithoutDuplicates WHERE (legend_persistence_stageWithoutDuplicates.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((legend_persistence_stageWithoutDuplicates.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (legend_persistence_stageWithoutDuplicates.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_reference\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedMainToTemp = "INSERT INTO " + tempName + " " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999 " +
                "FROM " +
                "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",legend_persistence_x.\"legend_persistence_end_date\" as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",MIN(legend_persistence_y.\"legend_persistence_start_date\") as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date,\"validity_through_target\" as legend_persistence_end_date " +
                "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM " + stageWithoutDuplicatesName + " as legend_persistence_stageWithoutDuplicates WHERE (legend_persistence_stageWithoutDuplicates.\"delete_indicator\" NOT IN ('yes','1','true')) AND ((legend_persistence_stageWithoutDuplicates.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (legend_persistence_stageWithoutDuplicates.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "WHERE NOT (EXISTS " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM " + stageWithoutDuplicatesName + " as legend_persistence_stageWithoutDuplicates " +
                "WHERE ((((legend_persistence_x.\"id\" = legend_persistence_stageWithoutDuplicates.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_stageWithoutDuplicates.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" = legend_persistence_stageWithoutDuplicates.\"validity_from_reference\")) AND (legend_persistence_stageWithoutDuplicates.\"delete_indicator\" NOT IN ('yes','1','true'))) AND ((legend_persistence_stageWithoutDuplicates.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (legend_persistence_stageWithoutDuplicates.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
                "WHERE (EXISTS " +
                "(SELECT * FROM " + tempName + " as legend_persistence_temp " +
                "WHERE ((sink.\"id\" = legend_persistence_temp.\"id\") AND (sink.\"name\" = legend_persistence_temp.\"name\")) AND (sink.\"validity_from_target\" = legend_persistence_temp.\"validity_from_target\"))) " +
                "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
                "(SELECT legend_persistence_temp.\"id\",legend_persistence_temp.\"name\",legend_persistence_temp.\"amount\",legend_persistence_temp.\"digest\",legend_persistence_temp.\"batch_id_in\",legend_persistence_temp.\"batch_id_out\",legend_persistence_temp.\"validity_from_target\",legend_persistence_temp.\"validity_through_target\" FROM " + tempName + " as legend_persistence_temp)";

        String expectedMainToTempForDeletion = "INSERT INTO " + tempWithDeleteIndicatorName + " " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\", \"delete_indicator\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_x.\"validity_through_target\" as legend_persistence_end_date,(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999,(CASE WHEN legend_persistence_y.\"delete_indicator\" IS NULL THEN 0 ELSE 1 END) " +
                "FROM " +
                "(SELECT * FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = 999999999) " +
                "AND (EXISTS " +
                "(SELECT * FROM " + stageWithoutDuplicatesName + " as legend_persistence_stageWithoutDuplicates " +
                "WHERE (((sink.\"id\" = legend_persistence_stageWithoutDuplicates.\"id\") AND (sink.\"name\" = legend_persistence_stageWithoutDuplicates.\"name\")) AND " +
                "((sink.\"validity_from_target\" = legend_persistence_stageWithoutDuplicates.\"validity_from_reference\") OR (sink.\"validity_through_target\" = legend_persistence_stageWithoutDuplicates.\"validity_from_reference\")) " +
                "AND (legend_persistence_stageWithoutDuplicates.\"delete_indicator\" IN ('yes','1','true'))) AND ((legend_persistence_stageWithoutDuplicates.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (legend_persistence_stageWithoutDuplicates.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))))) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT * FROM " + stageWithoutDuplicatesName + " as legend_persistence_stageWithoutDuplicates WHERE (legend_persistence_stageWithoutDuplicates.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (legend_persistence_stageWithoutDuplicates.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"validity_from_reference\"))";

        String expectedUpdateMainForDeletion = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1 " +
                "WHERE (EXISTS " +
                "(SELECT * FROM " + tempWithDeleteIndicatorName + " as legend_persistence_tempWithDeleteIndicator " +
                "WHERE ((sink.\"id\" = legend_persistence_tempWithDeleteIndicator.\"id\") AND (sink.\"name\" = legend_persistence_tempWithDeleteIndicator.\"name\")) AND (sink.\"validity_from_target\" = legend_persistence_tempWithDeleteIndicator.\"validity_from_target\"))) " +
                "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMainForDeletion = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"legend_persistence_start_date\" as legend_persistence_start_date,MAX(legend_persistence_y.\"validity_through_target\") as legend_persistence_end_date,legend_persistence_x.\"batch_id_in\",legend_persistence_x.\"batch_id_out\" FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,COALESCE(MIN(legend_persistence_y.\"validity_from_target\"),'9999-12-31 23:59:59') as legend_persistence_end_date,legend_persistence_x.\"batch_id_in\",legend_persistence_x.\"batch_id_out\" " +
                "FROM " + tempWithDeleteIndicatorName + " as legend_persistence_x " +
                "LEFT OUTER JOIN " + tempWithDeleteIndicatorName + " as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"validity_from_target\" > legend_persistence_x.\"validity_from_target\") AND (legend_persistence_y.\"delete_indicator\" = 0) " +
                "WHERE legend_persistence_x.\"delete_indicator\" = 0 " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"amount\", legend_persistence_x.\"digest\", legend_persistence_x.\"validity_from_target\", legend_persistence_x.\"batch_id_in\", legend_persistence_x.\"batch_id_out\") as legend_persistence_x " +
                "LEFT OUTER JOIN " + tempWithDeleteIndicatorName + " as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"validity_through_target\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"validity_through_target\" <= legend_persistence_x.\"legend_persistence_end_date\") AND (legend_persistence_y.\"delete_indicator\" <> 0) " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"amount\", legend_persistence_x.\"digest\", legend_persistence_x.\"legend_persistence_start_date\", legend_persistence_x.\"batch_id_in\", legend_persistence_x.\"batch_id_out\")";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyMainTableCreateQuery, operations.get(0).preActionsSql().get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableCreateQuery, operations.get(0).preActionsSql().get(1));
        Assertions.assertEquals(expectedBitemporalFromOnlyDefaultTempTableCreateQuery, operations.get(0).preActionsSql().get(2));
        Assertions.assertEquals(expectedBitemporalFromOnlyDefaultTempTableWithDeleteIndicatorCreateQuery, operations.get(0).preActionsSql().get(3));
        Assertions.assertEquals(expectedBitemporalFromOnlyStageWithDeleteIndicatorWithDataSplitWithoutDuplicatesTableCreateQuery, operations.get(0).preActionsSql().get(4));

        Assertions.assertEquals(expectedStageToStageWithoutDuplicates, operations.get(0).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedStageToTemp, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMainToTemp, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(2));
        Assertions.assertEquals(expectedUpdateMain, operations.get(0).ingestSql().get(3));
        Assertions.assertEquals(expectedTempToMain, operations.get(0).ingestSql().get(4));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMainToTempForDeletion, dataSplitRanges.get(0)), operations.get(0).ingestSql().get(5));
        Assertions.assertEquals(expectedUpdateMainForDeletion, operations.get(0).ingestSql().get(6));
        Assertions.assertEquals(expectedTempToMainForDeletion, operations.get(0).ingestSql().get(7));
        Assertions.assertEquals(getExpectedCleanupSql(tempName, "legend_persistence_temp"), operations.get(0).ingestSql().get(8));
        Assertions.assertEquals(getExpectedCleanupSql(tempWithDeleteIndicatorName, "legend_persistence_tempWithDeleteIndicator"), operations.get(0).ingestSql().get(9));
        Assertions.assertEquals(getExpectedCleanupSql(stageWithoutDuplicatesName, "legend_persistence_stageWithoutDuplicates"), operations.get(0).ingestSql().get(10));

        Assertions.assertEquals(expectedStageToStageWithoutDuplicates, operations.get(1).ingestSql().get(0));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedStageToTemp, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(1));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMainToTemp, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(2));
        Assertions.assertEquals(expectedUpdateMain, operations.get(1).ingestSql().get(3));
        Assertions.assertEquals(expectedTempToMain, operations.get(1).ingestSql().get(4));
        Assertions.assertEquals(enrichSqlWithDataSplits(expectedMainToTempForDeletion, dataSplitRanges.get(1)), operations.get(1).ingestSql().get(5));
        Assertions.assertEquals(expectedUpdateMainForDeletion, operations.get(1).ingestSql().get(6));
        Assertions.assertEquals(expectedTempToMainForDeletion, operations.get(1).ingestSql().get(7));
        Assertions.assertEquals(getExpectedCleanupSql(tempName, "legend_persistence_temp"), operations.get(1).ingestSql().get(8));
        Assertions.assertEquals(getExpectedCleanupSql(tempWithDeleteIndicatorName, "legend_persistence_tempWithDeleteIndicator"), operations.get(1).ingestSql().get(9));
        Assertions.assertEquals(getExpectedCleanupSql(stageWithoutDuplicatesName, "legend_persistence_stageWithoutDuplicates"), operations.get(1).ingestSql().get(10));

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), operations.get(0).metadataIngestSql().get(0));

        Assertions.assertEquals(2, operations.size());
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM \"mydb\".\"staging\" as stage WHERE (stage.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (stage.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}')";
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))) as rowsInserted";
        String rowsTerminated = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1)-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\") AND (sink2.\"validity_from_target\" = sink.\"validity_from_target\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'))))) as rowsTerminated";
        verifyStats(operations.get(0), enrichSqlWithDataSplits(incomingRecordCount,dataSplitRanges.get(0)), rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
        verifyStats(operations.get(1), enrichSqlWithDataSplits(incomingRecordCount,dataSplitRanges.get(1)), rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyBitemporalDeltaBatchIdBasedWithPlaceholders(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_reference\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",{BATCH_ID_PATTERN},999999999 " +
                "FROM " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\" FROM \"mydb\".\"staging\" as stage) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),MIN(legend_persistence_x.\"legend_persistence_end_date\")) as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),'9999-12-31 23:59:59') as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" < legend_persistence_y.\"legend_persistence_start_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_reference\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\",{BATCH_ID_PATTERN},999999999 " +
                "FROM " +
                "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",legend_persistence_x.\"legend_persistence_end_date\" as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",MIN(legend_persistence_y.\"legend_persistence_start_date\") as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date,\"validity_through_target\" as legend_persistence_end_date " +
                "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "WHERE NOT (EXISTS " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage " +
                "WHERE ((legend_persistence_x.\"id\" = stage.\"id\") AND (legend_persistence_x.\"name\" = stage.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" = stage.\"validity_from_reference\")))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = {BATCH_ID_PATTERN}-1 " +
                "WHERE (EXISTS " +
                "(SELECT * FROM \"mydb\".\"temp\" as temp " +
                "WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) " +
                "AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"validity_from_target\", \"validity_through_target\") " +
                "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" FROM \"mydb\".\"temp\" as temp)";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyTempTableCreateQuery, preActionsSql.get(2));

        Assertions.assertEquals(expectedStageToTemp, milestoningSql.get(0));
        Assertions.assertEquals(expectedMainToTemp, milestoningSql.get(1));
        Assertions.assertEquals(expectedUpdateMain, milestoningSql.get(2));
        Assertions.assertEquals(expectedTempToMain, milestoningSql.get(3));
        Assertions.assertEquals(expectedMetadataTableIngestQueryWithPlaceHolders, metadataIngestSql.get(0));
    }

    @Override
    public void verifyBitemporalDeltaBatchIdAndTimeBasedNoDeleteIndNoDataSplits(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_reference\" as legend_persistence_start_date," +
                "legend_persistence_y.\"legend_persistence_end_date\",(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
                "FROM " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\" FROM \"mydb\".\"staging\" as stage) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),MIN(legend_persistence_x.\"legend_persistence_end_date\")) as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),'9999-12-31 23:59:59') as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" < legend_persistence_y.\"legend_persistence_start_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_reference\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),999999999,'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
                "FROM " +
                "(SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_id_in\",sink.\"batch_id_out\",sink.\"batch_time_in\"," +
                "sink.\"batch_time_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" FROM \"mydb\".\"main\" as sink " +
                "WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",legend_persistence_x.\"legend_persistence_end_date\" as legend_persistence_end_date " +
                "FROM (SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",MIN(legend_persistence_y.\"legend_persistence_start_date\") as legend_persistence_end_date " +
                "FROM (SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date,\"validity_through_target\" as legend_persistence_end_date FROM \"mydb\".\"main\" as sink " +
                "WHERE sink.\"batch_id_out\" = 999999999) as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) " +
                "AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") " +
                "AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "WHERE NOT (EXISTS (SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage " +
                "WHERE ((legend_persistence_x.\"id\" = stage.\"id\") AND (legend_persistence_x.\"name\" = stage.\"name\")) " +
                "AND (legend_persistence_x.\"legend_persistence_start_date\" = stage.\"validity_from_reference\")))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) " +
                "AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink SET " +
                "sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main')-1," +
                "sink.\"batch_time_out\" = '2000-01-01 00:00:00' " +
                "WHERE (EXISTS " +
                "(SELECT * FROM \"mydb\".\"temp\" as temp WHERE ((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) " +
                "AND (sink.\"validity_from_target\" = temp.\"validity_from_target\"))) AND (sink.\"batch_id_out\" = 999999999)";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\", \"validity_from_target\", \"validity_through_target\") " +
                "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_id_in\",temp.\"batch_id_out\"," +
                "temp.\"batch_time_in\",temp.\"batch_time_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" " +
                "FROM \"mydb\".\"temp\" as temp)";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyMainTableBatchIdAndTimeBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyTempTableBatchIdAndTimeBasedCreateQuery, preActionsSql.get(2));

        Assertions.assertEquals(expectedStageToTemp, milestoningSql.get(0));
        Assertions.assertEquals(expectedMainToTemp, milestoningSql.get(1));
        Assertions.assertEquals(expectedUpdateMain, milestoningSql.get(2));
        Assertions.assertEquals(expectedTempToMain, milestoningSql.get(3));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), milestoningSql.get(4));

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyBitemporalDeltaDateTimeBasedNoDeleteIndNoDataSplits(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedStageToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_time_in\", \"batch_time_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\",legend_persistence_x.\"validity_from_reference\" as legend_persistence_start_date," +
                "legend_persistence_y.\"legend_persistence_end_date\",'2000-01-01 00:00:00','9999-12-31 23:59:59' " +
                "FROM " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"validity_from_reference\",stage.\"digest\" FROM \"mydb\".\"staging\" as stage) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),MIN(legend_persistence_x.\"legend_persistence_end_date\")) as legend_persistence_end_date " +
                "FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\",COALESCE(MIN(legend_persistence_y.\"legend_persistence_start_date\"),'9999-12-31 23:59:59') as legend_persistence_end_date " +
                "FROM " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage) as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '9999-12-31 23:59:59') as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" < legend_persistence_y.\"legend_persistence_start_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "LEFT OUTER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) AND (legend_persistence_x.\"validity_from_reference\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedMainToTemp = "INSERT INTO \"mydb\".\"temp\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"validity_from_target\", \"validity_through_target\", \"batch_time_in\", \"batch_time_out\") " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"amount\",legend_persistence_x.\"digest\"," +
                "legend_persistence_x.\"validity_from_target\" as legend_persistence_start_date,legend_persistence_y.\"legend_persistence_end_date\"," +
                "'2000-01-01 00:00:00','9999-12-31 23:59:59' FROM (SELECT sink.\"id\",sink.\"name\",sink.\"amount\",sink.\"digest\",sink.\"batch_time_in\"," +
                "sink.\"batch_time_out\",sink.\"validity_from_target\",sink.\"validity_through_target\" " +
                "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '9999-12-31 23:59:59') as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\"," +
                "legend_persistence_x.\"legend_persistence_end_date\" as legend_persistence_end_date FROM " +
                "(SELECT legend_persistence_x.\"id\",legend_persistence_x.\"name\",legend_persistence_x.\"legend_persistence_start_date\"," +
                "MIN(legend_persistence_y.\"legend_persistence_start_date\") as legend_persistence_end_date " +
                "FROM (SELECT \"id\",\"name\",\"validity_from_target\" as legend_persistence_start_date,\"validity_through_target\" as legend_persistence_end_date " +
                "FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '9999-12-31 23:59:59') as legend_persistence_x " +
                "INNER JOIN " +
                "(SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date FROM \"mydb\".\"staging\" as stage) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) " +
                "AND (legend_persistence_y.\"legend_persistence_start_date\" > legend_persistence_x.\"legend_persistence_start_date\") " +
                "AND (legend_persistence_y.\"legend_persistence_start_date\" < legend_persistence_x.\"legend_persistence_end_date\") " +
                "GROUP BY legend_persistence_x.\"id\", legend_persistence_x.\"name\", legend_persistence_x.\"legend_persistence_start_date\") as legend_persistence_x " +
                "WHERE NOT (EXISTS (SELECT \"id\",\"name\",\"validity_from_reference\" as legend_persistence_start_date " +
                "FROM \"mydb\".\"staging\" as stage WHERE ((legend_persistence_x.\"id\" = stage.\"id\") AND " +
                "(legend_persistence_x.\"name\" = stage.\"name\")) AND (legend_persistence_x.\"legend_persistence_start_date\" = stage.\"validity_from_reference\")))) as legend_persistence_y " +
                "ON ((legend_persistence_x.\"id\" = legend_persistence_y.\"id\") AND (legend_persistence_x.\"name\" = legend_persistence_y.\"name\")) " +
                "AND (legend_persistence_x.\"validity_from_target\" = legend_persistence_y.\"legend_persistence_start_date\"))";

        String expectedUpdateMain = "UPDATE \"mydb\".\"main\" as sink SET " +
                "sink.\"batch_time_out\" = '2000-01-01 00:00:00' " +
                "WHERE (EXISTS (SELECT * FROM \"mydb\".\"temp\" as temp WHERE " +
                "((sink.\"id\" = temp.\"id\") AND (sink.\"name\" = temp.\"name\")) AND " +
                "(sink.\"validity_from_target\" = temp.\"validity_from_target\"))) AND (sink.\"batch_time_out\" = '9999-12-31 23:59:59')";

        String expectedTempToMain = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"batch_time_in\", \"batch_time_out\", \"validity_from_target\", \"validity_through_target\") " +
                "(SELECT temp.\"id\",temp.\"name\",temp.\"amount\",temp.\"digest\",temp.\"batch_time_in\",temp.\"batch_time_out\",temp.\"validity_from_target\",temp.\"validity_through_target\" " +
                "FROM \"mydb\".\"temp\" as temp)";

        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyMainTableDateTimeBasedCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBitemporalFromOnlyTempTableDateTimeBasedCreateQuery, preActionsSql.get(2));

        Assertions.assertEquals(expectedStageToTemp, milestoningSql.get(0));
        Assertions.assertEquals(expectedMainToTemp, milestoningSql.get(1));
        Assertions.assertEquals(expectedUpdateMain, milestoningSql.get(2));
        Assertions.assertEquals(expectedTempToMain, milestoningSql.get(3));
        Assertions.assertEquals(getExpectedCleanupSql("\"mydb\".\"temp\"", "temp"), milestoningSql.get(4));

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM \"mydb\".\"staging\" as stage";
        String rowsUpdated = "SELECT COUNT(*) as rowsUpdated FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '2000-01-01 00:00:00'";
        String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_in\" = '2000-01-01 00:00:00')-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_time_out\" = '2000-01-01 00:00:00') as rowsInserted";

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
}
