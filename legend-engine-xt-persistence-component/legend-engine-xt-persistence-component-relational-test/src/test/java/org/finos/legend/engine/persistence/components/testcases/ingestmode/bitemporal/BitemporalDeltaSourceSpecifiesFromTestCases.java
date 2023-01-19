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
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.scenarios.BitemporalDeltaSourceSpecifiesFromOnlyScenarios;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.junit.jupiter.api.Test;

import java.util.List;

public abstract class BitemporalDeltaSourceSpecifiesFromTestCases extends BaseTest
{
    BitemporalDeltaSourceSpecifiesFromOnlyScenarios scenarios = new BitemporalDeltaSourceSpecifiesFromOnlyScenarios();

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
    void testBitemporalDeltaBatchIdBasedNoDeleteIndWithDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__WITH_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyBitemporalDeltaBatchIdBasedNoDeleteIndWithDataSplits(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyBitemporalDeltaBatchIdBasedNoDeleteIndWithDataSplits(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);


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
    void testBitemporalDeltaBatchIdBasedWithDeleteIndWithDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_DEL_IND__WITH_DATA_SPLITS__USING_DEFAULT_TEMP_TABLE();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyBitemporalDeltaBatchIdBasedWithDeleteIndWithDataSplits(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyBitemporalDeltaBatchIdBasedWithDeleteIndWithDataSplits(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);


    @Test
    void testBitemporalDeltaBatchIdBasedNoDeleteIndNoDataSplitsFilterDuplicates()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS__FILTER_DUPLICATES();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyBitemporalDeltaBatchIdBasedNoDeleteIndNoDataSplitsFilterDuplicates(operations);
    }

    public abstract void verifyBitemporalDeltaBatchIdBasedNoDeleteIndNoDataSplitsFilterDuplicates(GeneratorResult operations);

    @Test
    void testBitemporalDeltaBatchIdBasedNoDeleteIndWithDataSplitsFilterDuplicates()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__WITH_DATA_SPLITS__FILTER_DUPLICATES();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyBitemporalDeltaBatchIdBasedNoDeleteIndWithDataSplitsFilterDuplicates(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyBitemporalDeltaBatchIdBasedNoDeleteIndWithDataSplitsFilterDuplicates(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testBitemporalDeltaBatchIdBasedWithDeleteIndNoDataSplitsFilterDuplicates()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_DEL_IND__NO_DATA_SPLITS__FILTER_DUPLICATES();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyBitemporalDeltaBatchIdBasedWithDeleteIndNoDataSplitsFilterDuplicates(operations);
    }

    public abstract void verifyBitemporalDeltaBatchIdBasedWithDeleteIndNoDataSplitsFilterDuplicates(GeneratorResult operations);

    @Test
    void testBitemporalDeltaBatchIdBasedWithDeleteIndWithDataSplitsFilterDuplicates()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_DEL_IND__WITH_DATA_SPLITS__FILTER_DUPLICATES();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyBitemporalDeltaBatchIdBasedWithDeleteIndWithDataSplitsFilterDuplicates(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyBitemporalDeltaBatchIdBasedWithDeleteIndWithDataSplitsFilterDuplicates(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testBitemporalDeltaBatchIdBasedWithPlaceholders()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .batchIdPattern("{BATCH_ID_PATTERN}")
                .batchStartTimestampPattern("{BATCH_START_TS_PATTERN}")
                .batchEndTimestampPattern("{BATCH_END_TS_PATTERN}")
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyBitemporalDeltaBatchIdBasedWithPlaceholders(operations);
    }

    public abstract void verifyBitemporalDeltaBatchIdBasedWithPlaceholders(GeneratorResult operations);

    @Test
    void testBitemporalDeltaBatchIdAndTimeBasedNoDeleteIndNoDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__NO_DEL_IND__NO_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyBitemporalDeltaBatchIdAndTimeBasedNoDeleteIndNoDataSplits(operations);
    }

    public abstract void verifyBitemporalDeltaBatchIdAndTimeBasedNoDeleteIndNoDataSplits(GeneratorResult operations);

    @Test
    void testBitemporalDeltaDateTimeBasedNoDeleteIndNoDataSplits()
    {
        TestScenario scenario = scenarios.DATETIME_BASED__NO_DEL_IND__NO_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyBitemporalDeltaDateTimeBasedNoDeleteIndNoDataSplits(operations);
    }

    public abstract void verifyBitemporalDeltaDateTimeBasedNoDeleteIndNoDataSplits(GeneratorResult operations);


    public abstract RelationalSink getRelationalSink();
}