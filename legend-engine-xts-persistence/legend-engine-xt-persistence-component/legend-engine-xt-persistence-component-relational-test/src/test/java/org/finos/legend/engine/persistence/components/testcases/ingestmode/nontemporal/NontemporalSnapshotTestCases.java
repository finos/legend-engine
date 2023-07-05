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

package org.finos.legend.engine.persistence.components.testcases.ingestmode.nontemporal;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.Planner;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.planner.Planners;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.scenarios.NontemporalSnapshotTestScenarios;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class NontemporalSnapshotTestCases extends BaseTest
{
    NontemporalSnapshotTestScenarios scenarios = new NontemporalSnapshotTestScenarios();

    @Test
    void testNontemporalSnapshotNoAuditingNoDataSplit()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DATASPLIT();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalSnapshotNoAuditingNoDataSplit(operations);
    }

    public abstract void verifyNontemporalSnapshotNoAuditingNoDataSplit(GeneratorResult operations);

    @Test
    void testNontemporalSnapshotNoAuditingWithDataSplit()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__WITH_DATASPLIT();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalSnapshotNoAuditingWithDataSplit(operations);
    }

    public abstract void verifyNontemporalSnapshotNoAuditingWithDataSplit(GeneratorResult operations);

    @Test
    void testNontemporalSnapshotWithAuditingNoDataSplit()
    {
        TestScenario testScenario = scenarios.WITH_AUDTING__NO_DATASPLIT();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .executionTimestampClock(fixedClock_2000_01_01)
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalSnapshotWithAuditingNoDataSplit(operations);
    }

    public abstract void verifyNontemporalSnapshotWithAuditingNoDataSplit(GeneratorResult operations);

    @Test
    void testNontemporalSnapshotWithAuditingWithDataSplit()
    {
        TestScenario testScenario = scenarios.WITH_AUDTING__WITH_DATASPLIT();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalSnapshotWithAuditingWithDataSplit(operations);
    }

    public abstract void verifyNontemporalSnapshotWithAuditingWithDataSplit(GeneratorResult operations);

    @Test
    void testNontemporalSnapshotWithUpperCaseOptimizer()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DATASPLIT();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .caseConversion(CaseConversion.TO_UPPER)
                .build();

        GeneratorResult queries = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalSnapshotWithUpperCaseOptimizer(queries);
    }

    public abstract void verifyNontemporalSnapshotWithUpperCaseOptimizer(GeneratorResult queries);

    @Test
    void testNontemporalSnapshotWithLessColumnsInStaging()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DATASPLIT();
        Dataset stagingTable = testScenario.getStagingTable().withSchema(baseTableShortenedSchema);
        Datasets datasets = Datasets.of(testScenario.getMainTable(), stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        verifyNontemporalSnapshotWithLessColumnsInStaging(operations);
    }

    public abstract void verifyNontemporalSnapshotWithLessColumnsInStaging(GeneratorResult operations);


    @Test
    void testNontemporalSnapshotMandatoryDatasetMissing()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchema)
            .build();

        try
        {
            Datasets datasets = Datasets.of(mainTable, null);
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e instanceof NullPointerException);
            Assertions.assertEquals("stagingDataset", e.getMessage());
        }
    }

    @Test
    void testNontemporalSnapshotDateTimeAuditingValidation()
    {
        try
        {
            NontemporalSnapshot.builder()
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
    public void testNontemporalSnapshotWithCleanStagingData()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DATASPLIT();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .cleanupStagingData(true)
            .collectStatistics(true)
            .build();
        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalSnapshotWithCleanStagingData(operations);
    }

    public abstract void verifyNontemporalSnapshotWithCleanStagingData(GeneratorResult operations);

    @Test
    public void testNontemporalSnapshotWithDropStagingData()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DATASPLIT();
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Resources resources = Resources.builder().externalDatasetImported(true).build();
        Planner planner = Planners.get(testScenario.getDatasets(), testScenario.getIngestMode(), options);
        RelationalTransformer transformer = new RelationalTransformer(getRelationalSink());

        // post actions
        LogicalPlan postActionsLogicalPlan = planner.buildLogicalPlanForPostActions(resources);
        SqlPlan physicalPlanForPostActions = transformer.generatePhysicalPlan(postActionsLogicalPlan);
        verifyNontemporalSnapshotWithDropStagingData(physicalPlanForPostActions);
    }

    public abstract void verifyNontemporalSnapshotWithDropStagingData(SqlPlan physicalPlanForPostActions);

    public abstract RelationalSink getRelationalSink();
}