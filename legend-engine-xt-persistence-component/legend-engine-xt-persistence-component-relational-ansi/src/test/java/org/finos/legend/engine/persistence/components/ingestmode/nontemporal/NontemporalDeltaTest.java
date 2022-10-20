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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.finos.legend.engine.persistence.components.IngestModeTest;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NontemporalDeltaTest extends IngestModeTest
{

    @Test
    void testGeneratePhysicalPlan()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(NoAuditing.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String updateSql = "UPDATE \"mydb\".\"main\" as sink SET " +
            "sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"biz_date\" = (SELECT stage.\"biz_date\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) " +
            "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE " +
            "((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\") " +
            "(SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "(sink.\"digest\" = stage.\"digest\"))))";

        Assertions.assertEquals(expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Test
    void testGeneratePhysicalPlanWithUpperCaseOptimizer()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(NoAuditing.builder().build())
            .build();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String updateSql = "UPDATE \"MYDB\".\"MAIN\" as sink " +
            "SET sink.\"ID\" = (SELECT stage.\"ID\" FROM \"MYDB\".\"STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (sink.\"DIGEST\" <> stage.\"DIGEST\"))," +
            "sink.\"NAME\" = (SELECT stage.\"NAME\" FROM \"MYDB\".\"STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (sink.\"DIGEST\" <> stage.\"DIGEST\"))," +
            "sink.\"AMOUNT\" = (SELECT stage.\"AMOUNT\" FROM \"MYDB\".\"STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (sink.\"DIGEST\" <> stage.\"DIGEST\"))," +
            "sink.\"BIZ_DATE\" = (SELECT stage.\"BIZ_DATE\" FROM \"MYDB\".\"STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (sink.\"DIGEST\" <> stage.\"DIGEST\"))," +
            "sink.\"DIGEST\" = (SELECT stage.\"DIGEST\" FROM \"MYDB\".\"STAGING\" as stage WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (sink.\"DIGEST\" <> stage.\"DIGEST\")) " +
            "WHERE EXISTS (SELECT * FROM \"MYDB\".\"STAGING\" as stage" +
            " WHERE ((sink.\"ID\" = stage.\"ID\") " +
            "AND (sink.\"NAME\" = stage.\"NAME\")) " +
            "AND (sink.\"DIGEST\" <> stage.\"DIGEST\"))";

        String insertSql = "INSERT INTO \"MYDB\".\"MAIN\" (\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"DIGEST\") " +
            "(SELECT * FROM \"MYDB\".\"STAGING\" as stage WHERE NOT (EXISTS (SELECT * FROM \"MYDB\".\"MAIN\" as sink " +
            "WHERE ((sink.\"ID\" = stage.\"ID\") " +
            "AND (sink.\"NAME\" = stage.\"NAME\")) " +
            "AND (sink.\"DIGEST\" = stage.\"DIGEST\"))))";

        Assertions.assertEquals(expectedBaseTablePlusDigestCreateQueryWithUpperCase, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Test
    void testMilestoningWithLessColumnsInStaging()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(stagingTableSchemaWithLimitedColumns)
            .build();

        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(NoAuditing.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String updateSql = "UPDATE \"mydb\".\"main\" as sink SET " +
            "sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) " +
            "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE " +
            "((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" (\"id\", \"name\", \"amount\", \"digest\") " +
            "(SELECT * FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "(sink.\"digest\" = stage.\"digest\"))))";

        Assertions.assertEquals(expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Test
    void testGeneratePhysicalPlanWithUpdateBatchTimeField()
    {

        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigestAndUpdateBatchTimeField)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String updateSql = "UPDATE \"mydb\".\"main\" as sink " +
            "SET sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"biz_date\" = (SELECT stage.\"biz_date\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
            "sink.\"batch_update_time\" = '2000-01-01 00:00:00' " +
            "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" " +
            "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_update_time\") " +
            "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\",'2000-01-01 00:00:00' " +
            "FROM \"mydb\".\"staging\" as stage " +
            "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
            "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
            "(sink.\"digest\" = stage.\"digest\"))))";

        Assertions.assertEquals(expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
    }

    @Test
    void testIncrementalDeltaMilestoningValidationPkFieldsMissing()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithNoPrimaryKeys)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithNoPrimaryKeys)
            .build();

        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(NoAuditing.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        try
        {
            RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(ingestMode)
                .relationalSink(AnsiSqlSink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();

            GeneratorResult operations = generator.generateOperations(datasets);

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Primary key list must not be empty", e.getMessage());
        }
    }

    @Test
    void testIncrementalDeltaMilestoningValidationupdateBatchTimeFieldMissing()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        try
        {
            NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(DateTimeAuditing.builder().build())
                .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build DateTimeAuditing, some of required attributes are not set [dateTimeField]", e.getMessage());
        }
    }

    @Test
    public void testPostRunStatisticsSqlWithoutUpdateBatchColumn()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(NoAuditing.builder().build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM \"mydb\".\"staging\" as stage";
        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
    }

    @Test
    public void testPostRunStatisticsWithUpdateBatchColumn()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigestAndUpdateBatchTimeField)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> expectedSQL = new ArrayList<>();
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM \"mydb\".\"staging\" as stage";
        String rowsTerminated = "SELECT 0 as rowsTerminated";

        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));
    }

    @Test
    public void testPostRunStatisticsAndPostActionSqlWithUpdateBatchColumnAndCleanStagingData()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchemaWithDigestAndUpdateBatchTimeField)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(baseTableSchemaWithDigest)
            .build();

        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(AnsiSqlSink.get())
            .cleanupStagingData(true)
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        String incomingRecordCount = "SELECT COUNT(*) as incomingRecordCount FROM \"mydb\".\"staging\" as stage";
        String rowsTerminated = "SELECT 0 as rowsTerminated";

        Assertions.assertEquals(incomingRecordCount, operations.postIngestStatisticsSql().get(StatisticName.INCOMING_RECORD_COUNT));
        Assertions.assertEquals(rowsTerminated, operations.postIngestStatisticsSql().get(StatisticName.ROWS_TERMINATED));

        List<String> postActionsSql = operations.postActionsSql();
        List<String> expectedSQL = new ArrayList<>();
        expectedSQL.add(expectedStagingCleanupQuery);
        assertIfListsAreSameIgnoringOrder(expectedSQL, postActionsSql);
    }

    @Test
    void testGeneratePhysicalPlanWithDeleteIndicator()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchemaWithDigest)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableSchemaWithDeleteIndicator)
                .build();

        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(NoAuditing.builder().build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorField)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .build();


        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(ingestMode)
                .relationalSink(AnsiSqlSink.get())
                .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();

        String updateSql = "UPDATE \"mydb\".\"main\" as sink SET " +
                "sink.\"id\" = (SELECT stage.\"id\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
                "sink.\"name\" = (SELECT stage.\"name\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
                "sink.\"amount\" = (SELECT stage.\"amount\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
                "sink.\"biz_date\" = (SELECT stage.\"biz_date\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))," +
                "sink.\"digest\" = (SELECT stage.\"digest\" FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\")) " +
                "WHERE EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE " +
                "((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" <> stage.\"digest\"))";

        String insertSql = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\") " +
                "(SELECT * FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink " +
                "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND " +
                "(sink.\"digest\" = stage.\"digest\"))))";

        String deleteSql = "DELETE FROM \"mydb\".\"main\" as sink " +
                "WHERE EXISTS (SELECT " +
                "stage.\"id\"," +
                "stage.\"name\"," +
                "stage.\"amount\"," +
                "stage.\"biz_date\"," +
                "stage.\"digest\" " +
                "FROM \"mydb\".\"staging\" as stage " +
                "WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) " +
                "AND (sink.\"digest\" = stage.\"digest\")" + 
                "AND (stage.\"delete_indicator\" IN ('yes','1','true')))";

        Assertions.assertEquals(expectedBaseTablePlusDigestCreateQuery, preActionsSqlList.get(0));
        Assertions.assertEquals(updateSql, milestoningSqlList.get(0));
        Assertions.assertEquals(insertSql, milestoningSqlList.get(1));
        Assertions.assertEquals(deleteSql, milestoningSqlList.get(2));
    }
}
