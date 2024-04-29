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

package org.finos.legend.engine.persistence.components.testcases.ingestmode.unitemporal;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.finos.legend.engine.persistence.components.scenarios.UnitemporalDeltaDateTimeBasedScenarios;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public abstract class UnitmemporalDeltaDateTimeBasedTestCases extends BaseTest
{
    UnitemporalDeltaDateTimeBasedScenarios scenarios = new UnitemporalDeltaDateTimeBasedScenarios();

    @Test
    void testUnitemporalDeltaNoDeleteIndNoDedupNoVersioning()
    {
        TestScenario scenario = scenarios.DATETIME_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSIONING();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaNoDeleteIndNoDedupNoVersioning(operations);
    }

    public abstract void verifyUnitemporalDeltaNoDeleteIndNoDedupNoVersioning(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaNoDeleteIndFailOnDupsAllVersionWithoutPerform()
    {
        TestScenario scenario = scenarios.DATETIME_BASED__NO_DEL_IND__FAIL_ON_DUPS__ALL_VERSION_WITHOUT_PERFORM();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .ingestRunId(ingestRunId)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyUnitemporalDeltaNoDeleteIndFailOnDupsAllVersionWithoutPerform(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyUnitemporalDeltaNoDeleteIndFailOnDupsAllVersionWithoutPerform(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testUnitemporalDeltaWithDeleteIndNoDedupNoVersioning()
    {
        TestScenario scenario = scenarios.DATETIME_BASED__WITH_DEL_IND__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaWithDeleteIndNoDedupNoVersioning(operations);
    }

    public abstract void verifyUnitemporalDeltaWithDeleteIndNoDedupNoVersioning(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithDeleteIndFilterDupsAllVersion()
    {
        TestScenario scenario = scenarios.DATETIME_BASED__WITH_DEL_IND__FILTER_DUPS__ALL_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .ingestRunId(ingestRunId)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyUnitemporalDeltaWithDeleteIndFilterDupsAllVersion(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyUnitemporalDeltaWithDeleteIndFilterDupsAllVersion(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testUnitemporalDeltaWithUpperCaseOptimizer()
    {
        TestScenario scenario = scenarios.DATETIME_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSIONING();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .caseConversion(CaseConversion.TO_UPPER)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaWithUpperCaseOptimizer(operations);
    }

    public abstract void verifyUnitemporalDeltaWithUpperCaseOptimizer(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithCleanStagingData()
    {
        TestScenario scenario = scenarios.DATETIME_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSIONING();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaWithCleanStagingData(operations);
    }

    public abstract void verifyUnitemporalDeltaWithCleanStagingData(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaValidationBatchTimeInMissing()
    {
        try
        {
            UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                    .digestField(digestField)
                    .transactionMilestoning(TransactionDateTime.builder()
                            .dateTimeOutName(batchTimeOutField)
                            .build())
                    .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build TransactionDateTime, some of required attributes are not set [dateTimeInName]", e.getMessage());
        }
    }

    @Test
    void testUnitemporalDeltaValidationBatchTimeInNotPrimaryKey()
    {
        TestScenario scenario = scenarios.DATETIME_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSIONING();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();

        Dataset mainTable = scenario.getMainTable().withSchema(mainTableTimeBasedSchemaWithBatchTimeInNotPrimary);
        Datasets datasets = Datasets.of(mainTable, scenario.getStagingTable());

        try
        {
            GeneratorResult queries = generator.generateOperations(datasets);
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Field \"batch_time_in\" must be a primary key", e.getMessage());
        }
    }


    public abstract RelationalSink getRelationalSink();

}
