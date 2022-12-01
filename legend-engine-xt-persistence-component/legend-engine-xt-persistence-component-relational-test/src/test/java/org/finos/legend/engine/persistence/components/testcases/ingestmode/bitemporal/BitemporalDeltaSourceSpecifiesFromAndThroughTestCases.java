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

package org.finos.legend.engine.persistence.components.testcases.ingestmode.bitemporal;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.scenarios.BitemporalDeltaSourceSpecifiesFromAndThroughScenarios;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.junit.jupiter.api.Test;

import java.util.List;

public abstract class BitemporalDeltaSourceSpecifiesFromAndThroughTestCases extends BaseTest
{

    BitemporalDeltaSourceSpecifiesFromAndThroughScenarios scenarios = new BitemporalDeltaSourceSpecifiesFromAndThroughScenarios();

    @Test
    void testBitemporalDeltaBatchIdBasedNoDeleteIndNoDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyBitemporalDeltaBatchIdBasedNoDeleteIndNoDataSplits(operations);
    }

    public abstract void verifyBitemporalDeltaBatchIdBasedNoDeleteIndNoDataSplits(GeneratorResult operations);

    @Test
    void testBitemporalDeltaBatchIdDateTimeBasedNoDeleteIndWithDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__NO_DEL_IND__WITH_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyBitemporalDeltaBatchIdDateTimeBasedNoDeleteIndWithDataSplits(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyBitemporalDeltaBatchIdDateTimeBasedNoDeleteIndWithDataSplits(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);


    @Test
    void testBitemporalDeltaBatchIdBasedWithDeleteIndNoDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_DEL_IND__NO_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyBitemporalDeltaBatchIdBasedWithDeleteIndNoDataSplits(operations);
    }

    public abstract void verifyBitemporalDeltaBatchIdBasedWithDeleteIndNoDataSplits(GeneratorResult operations);

    @Test
    void testBitemporalDeltaDatetimeBasedWithDeleteIndWithDataSplits()
    {
        TestScenario scenario = scenarios.DATETIME_BASED__WITH_DEL_IND__WITH_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyBitemporalDeltaDatetimeBasedWithDeleteIndWithDataSplits(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyBitemporalDeltaDatetimeBasedWithDeleteIndWithDataSplits(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testBitemporalDeltaBatchIdBasedWithUpperCaseOptimizer()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .caseConversion(CaseConversion.TO_UPPER)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyBitemporalDeltaBatchIdBasedWithUpperCaseOptimizer(operations);
    }

    public abstract void verifyBitemporalDeltaBatchIdBasedWithUpperCaseOptimizer(GeneratorResult operations);

    public abstract RelationalSink getRelationalSink();
}
