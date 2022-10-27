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

import java.util.List;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.scenarios.NonTemporalDeltaScenarios;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class NontemporalDeltaTestCases extends BaseTest
{
    NonTemporalDeltaScenarios scenarios = new NonTemporalDeltaScenarios();

    @Test
    void testNontemporalDeltaNoAuditingNoDataSplit()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DATASPLIT();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaNoAuditingNoDataSplit(operations);
    }

    public abstract void verifyNontemporalDeltaNoAuditingNoDataSplit(GeneratorResult operations);

    @Test
    void testNontemporalDeltaNoAuditingNoDataSplitWithDeleteIndicator()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DATASPLIT__WITH_DELETE_INDICATOR();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaNoAuditingNoDataSplitWithDeleteIndicator(operations);
    }

    public abstract void verifyNontemporalDeltaNoAuditingNoDataSplitWithDeleteIndicator(GeneratorResult operations);

    @Test
    void testNontemporalDeltaWithAuditingNoDataSplit()
    {
        TestScenario testScenario = scenarios.WITH_AUDTING__NO_DATASPLIT();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .executionTimestampClock(fixedClock_2000_01_01)
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaWithAuditingNoDataSplit(operations);
    }

    public abstract void verifyNontemporalDeltaWithAuditingNoDataSplit(GeneratorResult operations);

    @Test
    void testNonTemporalDeltaNoAuditingWithDataSplit()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__WITH_DATASPLIT();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(testScenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyNonTemporalDeltaNoAuditingWithDataSplit(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyNonTemporalDeltaNoAuditingWithDataSplit(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testNonTemporalDeltaWithWithAuditingWithDataSplit()
    {
        TestScenario testScenario = scenarios.WITH_AUDTING__WITH_DATASPLIT();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(testScenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyNonTemporalDeltaWithWithAuditingWithDataSplit(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyNonTemporalDeltaWithWithAuditingWithDataSplit(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testNontemporalDeltaWithUpperCaseOptimizer()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DATASPLIT();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .caseConversion(CaseConversion.TO_UPPER)
                .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaWithUpperCaseOptimizer(operations);
    }

    public abstract void verifyNontemporalDeltaWithUpperCaseOptimizer(GeneratorResult operations);

    @Test
    void testNontemporalDeltaWithLessColumnsInStaging()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DATASPLIT();
        Dataset stagingTable = testScenario.getStagingTable().withSchema(stagingTableSchemaWithLimitedColumns);
        Datasets datasets = Datasets.of(testScenario.getMainTable(), stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        verifyNontemporalDeltaWithLessColumnsInStaging(operations);
    }

    public abstract void verifyNontemporalDeltaWithLessColumnsInStaging(GeneratorResult operations);

    @Test
    void testNontemporalDeltaValidationPkFieldsMissing()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DATASPLIT();
        // Staging table has no pks
        Dataset stagingTable = testScenario.getStagingTable().withSchema(baseTableSchemaWithNoPrimaryKeys);
        Datasets datasets = Datasets.of(testScenario.getMainTable(), stagingTable);
        try
        {
            RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
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
    void testNontemporalDeltaValidationDateTimeFieldMissing()
    {
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
    public void testNontemporalDeltaPostActionSqlAndCleanStagingData()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DATASPLIT();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .cleanupStagingData(true)
            .collectStatistics(true)
            .build();
        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaPostActionSqlAndCleanStagingData(operations);
    }

    public abstract void verifyNontemporalDeltaPostActionSqlAndCleanStagingData(GeneratorResult operations);

    public abstract RelationalSink getRelationalSink();
}
