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
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.finos.legend.engine.persistence.components.scenarios.UnitemporalDeltaBatchIdDateTimeBasedScenarios;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public abstract class UnitmemporalDeltaBatchIdDateTimeBasedTestCases extends BaseTest
{
    UnitemporalDeltaBatchIdDateTimeBasedScenarios scenarios = new UnitemporalDeltaBatchIdDateTimeBasedScenarios();

    @Test
    void testUnitemporalDeltaNoDeleteIndNoDedupNoVersion()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaNoDeleteIndNoDedupNoVersion(operations);
    }

    public abstract void verifyUnitemporalDeltaNoDeleteIndNoDedupNoVersion(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaNoDeleteIndFilterDupsAllVersionWithoutPerform()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__NO_DEL_IND__FILTER_DUPS__ALL_VERSION_WITHOUT_PERFORM();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .ingestRunId(ingestRunId)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyUnitemporalDeltaNoDeleteIndFilterDupsAllVersionWithoutPerform(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyUnitemporalDeltaNoDeleteIndFilterDupsAllVersionWithoutPerform(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testUnitemporalDeltaWithDeleteIndMultiValuesNoDedupNoVersion()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITH_DEL_IND_MULTI_VALUES__NO_DEDUP_NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .ingestRunId(ingestRunId)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaWithDeleteIndMultiValuesNoDedupNoVersion(operations);
    }

    public abstract void verifyUnitemporalDeltaWithDeleteIndMultiValuesNoDedupNoVersion(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithDeleteInd()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITH_DEL_IND__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaWithDeleteInd(operations);
    }

    public abstract void verifyUnitemporalDeltaWithDeleteInd(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithDeleteIndFailOnDupsAllVersion()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITH_DEL_IND__FAIL_ON_DUP__ALL_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .ingestRunId(ingestRunId)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyUnitemporalDeltaWithDeleteIndFailOnDupsAllVersion(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyUnitemporalDeltaWithDeleteIndFailOnDupsAllVersion(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testUnitemporalDeltaWithUpperCaseOptimizer()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSION();
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
    void testUnitemporalDeltaWithLessColumnsInStaging()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSION();
        Dataset stagingDataset = scenario.getStagingTable().withSchema(stagingTableSchemaWithLimitedColumns);
        Datasets datasets = Datasets.of(scenario.getMainTable(), stagingDataset);

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(datasets);
        verifyUnitemporalDeltaWithLessColumnsInStaging(operations);
    }

    public abstract void verifyUnitemporalDeltaWithLessColumnsInStaging(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithPlaceholders()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .batchIdPattern("{BATCH_ID_PATTERN}")
                .batchStartTimestampPattern("{BATCH_START_TS_PATTERN}")
                .batchEndTimestampPattern("{BATCH_END_TS_PATTERN}")
                .build();

        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaWithPlaceholders(operations);
    }

    public abstract void verifyUnitemporalDeltaWithPlaceholders(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithOnlySchemaSet()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSION();
        Dataset mainTable = getMainDatasetWithOnlySchemaSet(scenario.getMainTable().schema());
        Dataset stagingTable = getStagingDatasetWithOnlySchemaSet(scenario.getStagingTable().schema());
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .executionTimestampClock(fixedClock_2000_01_01)
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        verifyUnitemporalDeltaWithOnlySchemaSet(operations);
    }

    public abstract void verifyUnitemporalDeltaWithOnlySchemaSet(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithDbAndSchemaBothSet()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSION();
        Dataset mainTable = getMainDatasetWithDbAndSchemaBothSet(scenario.getMainTable().schema());
        Dataset stagingTable = getStagingDatasetWithDbAndSchemaBothSet(scenario.getStagingTable().schema());
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        verifyUnitemporalDeltaWithDbAndSchemaBothSet(operations);
    }

    public abstract void verifyUnitemporalDeltaWithDbAndSchemaBothSet(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithDbAndSchemaBothNotSet()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSION();
        Dataset mainTable = getMainDatasetWithDbAndSchemaBothNotSet(scenario.getMainTable().schema());
        Dataset stagingTable = getStagingDatasetWithDbAndSchemaBothNotSet(scenario.getStagingTable().schema());
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        verifyUnitemporalDeltaWithDbAndSchemaBothNotSet(operations);
    }

    public abstract void verifyUnitemporalDeltaWithDbAndSchemaBothNotSet(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithCleanStagingData()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSION();
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

    public abstract RelationalSink getRelationalSink();

    @Test
    void testUnitemporalDeltaValidationDigestMissing()
    {
        try
        {
            UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                    .transactionMilestoning(BatchIdAndDateTime.builder()
                            .batchIdInName(batchIdInField)
                            .batchIdOutName(batchIdOutField)
                            .dateTimeInName(batchTimeInField)
                            .dateTimeOutName(batchTimeOutField)
                            .build())
                    .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build UnitemporalDelta, some of required attributes are not set [digestField]", e.getMessage());
        }
    }

    @Test
    void testUnitemporalDeltaValidationBatchTimeOutMissing()
    {
        try
        {
            UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                    .digestField(digestField)
                    .transactionMilestoning(BatchIdAndDateTime.builder()
                            .batchIdInName(batchIdInField)
                            .batchIdOutName(batchIdOutField)
                            .dateTimeInName(batchTimeInField)
                            .build())
                    .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                            .deleteField(deleteIndicatorField)
                            .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                            .build())
                    .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build BatchIdAndDateTime, some of required attributes are not set [dateTimeOutName]", e.getMessage());
        }
    }

    @Test
    void testUnitemporalDeltaValidationDeleteIndicatorFieldMissing()
    {
        try
        {
            UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                    .digestField(digestField)
                    .transactionMilestoning(BatchIdAndDateTime.builder()
                            .batchIdInName(batchIdInField)
                            .batchIdOutName(batchIdOutField)
                            .dateTimeInName(batchTimeInField)
                            .dateTimeOutName(batchTimeOutField)
                            .build())
                    .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                            .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                            .build())
                    .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build DeleteIndicatorMergeStrategy, some of required attributes are not set [deleteField]", e.getMessage());
        }
    }

    @Test
    void testUnitemporalDeltaValidationDeleteIndicatorValuesMissing()
    {
        try
        {
            UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                    .digestField(digestField)
                    .transactionMilestoning(BatchIdAndDateTime.builder()
                            .batchIdInName(batchIdInField)
                            .batchIdOutName(batchIdOutField)
                            .dateTimeInName(batchTimeInField)
                            .dateTimeOutName(batchTimeOutField)
                            .build())
                    .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                            .deleteField(deleteIndicatorField)
                            .build())
                    .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build DeleteIndicatorMergeStrategy, [deleteValues] must contain at least one element", e.getMessage());
        }
    }

    @Test
    void testUnitemporalDeltaValidationBatchIdInNotPrimaryKey()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();

        Dataset mainTable = scenario.getMainTable().withSchema(mainTableSchemaWithBatchIdInNotPrimary);
        Datasets datasets = Datasets.of(mainTable, scenario.getStagingTable());

        try
        {
            GeneratorResult queries = generator.generateOperations(datasets);
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Field \"batch_id_in\" must be a primary key", e.getMessage());
        }
    }

}
