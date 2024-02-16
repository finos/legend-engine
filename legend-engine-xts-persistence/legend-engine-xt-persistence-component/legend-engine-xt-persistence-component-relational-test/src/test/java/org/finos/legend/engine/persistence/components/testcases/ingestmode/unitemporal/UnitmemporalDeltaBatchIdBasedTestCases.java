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
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.finos.legend.engine.persistence.components.scenarios.UnitemporalDeltaBatchIdBasedScenarios;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class UnitmemporalDeltaBatchIdBasedTestCases extends BaseTest
{

    UnitemporalDeltaBatchIdBasedScenarios scenarios = new UnitemporalDeltaBatchIdBasedScenarios();

    @Test
    void testUnitemporalDeltaNoDeleteIndNoDedupNoVersion()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .createStagingDataset(true)
                .enableConcurrentSafety(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaNoDeleteIndNoDedupNoVersion(operations);
    }

    public abstract void verifyUnitemporalDeltaNoDeleteIndNoDedupNoVersion(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaNoDeleteIndNoDedupAllVersionsWithoutPerform()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DEDUP__ALL_VERSION_WITHOUT_PERFORM();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyUnitemporalDeltaNoDeleteIndNoDedupAllVersionsWithoutPerform(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyUnitemporalDeltaNoDeleteIndNoDedupAllVersionsWithoutPerform(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testUnitemporalDeltaWithDeleteIndFilterDupsNoVersion()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_DEL_IND__FILTER_DUPS__NO_VERSIONING();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaWithDeleteIndFilterDupsNoVersion(operations);
    }

    public abstract void verifyUnitemporalDeltaWithDeleteIndFilterDupsNoVersion(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithDeleteIndNoDedupAllVersion()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_DEL_IND__NO_DEDUP__ALL_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyUnitemporalDeltaWithDeleteIndNoDedupAllVersion(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyUnitemporalDeltaWithDeleteIndNoDedupAllVersion(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testUnitemporalDeltaWithUpperCaseOptimizer()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .caseConversion(CaseConversion.TO_UPPER)
                .collectStatistics(true)
                .enableConcurrentSafety(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaWithUpperCaseOptimizer(operations);
    }

    public abstract void verifyUnitemporalDeltaWithUpperCaseOptimizer(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithCleanStagingData()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSION();
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
    void testUnitemporalDeltaNoDeleteIndWithOptimizationFilters()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__WITH_OPTIMIZATION_FILTERS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaNoDeleteIndWithOptimizationFilters(operations);
    }

    public abstract void verifyUnitemporalDeltaNoDeleteIndWithOptimizationFilters(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaNoDeleteIndWithOptimizationFiltersIncludesNullValues()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__WITH_OPTIMIZATION_FILTERS__INCLUDES_NULL_VALUES();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .executionTimestampClock(fixedClock_2000_01_01)
            .collectStatistics(true)
            .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaNoDeleteIndWithOptimizationFiltersIncludesNullValues(operations);
    }

    public abstract void verifyUnitemporalDeltaNoDeleteIndWithOptimizationFiltersIncludesNullValues(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaValidationBatchIdOutMissing()
    {
        try
        {
            UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                    .digestField(digestField)
                    .transactionMilestoning(BatchId.builder()
                            .batchIdInName(batchIdInField)
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
            Assertions.assertEquals("Cannot build BatchId, some of required attributes are not set [batchIdOutName]", e.getMessage());
        }
    }

    @Test
    void testUnitemporalDeltaValidationBatchIdInNotPrimaryKey()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();

        Dataset mainTable = scenario.getMainTable().withSchema(mainTableBatchIdBasedSchemaWithBatchIdInNotPrimary);
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

    @Test
    void testUnitemporalDeltaValidationOptimizationColumnsNotPresent()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__WITH_MISSING_OPTIMIZATION_FILTER();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        try
        {
            GeneratorResult queries = generator.generateOperations(scenario.getDatasets());
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Optimization filter [unknown_column] not found in Staging Schema", e.getMessage());
        }
    }

    @Test
    void testUnitemporalDeltaValidationOptimizationColumnUnsupportedDataType()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__WITH_OPTIMIZATION_FILTER_UNSUPPORTED_DATATYPE();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        try
        {
            GeneratorResult queries = generator.generateOperations(scenario.getDatasets());
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Optimization filter's data type [VARCHAR] is not supported", e.getMessage());
        }
    }

    @Test
    void testUnitemporalDeltaWithNoVersioningAndStagingFilters()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_VERSIONING__WITH_STAGING_FILTER();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(false)
                .putAllAdditionalMetadata(Collections.singletonMap("watermark", "my_watermark_value"))
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaWithNoVersionAndStagingFilter(operations);
    }

    public abstract void verifyUnitemporalDeltaWithNoVersionAndStagingFilter(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithNoVersioningAndFilteredDataset()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_VERSIONING__WITH_FILTERED_DATASET();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .executionTimestampClock(fixedClock_2000_01_01)
            .cleanupStagingData(false)
            .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaWithNoVersionAndFilteredDataset(operations);
    }

    public abstract void verifyUnitemporalDeltaWithNoVersionAndFilteredDataset(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithFilterDupsMaxVersionWithStagingFilter()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__FILTER_DUPS__MAX_VERSION__WITH_STAGING_FILTER();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(false)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        this.verifyUnitemporalDeltaWithFilterDupsMaxVersionWithStagingFilter(operations);
    }

    public abstract void verifyUnitemporalDeltaWithFilterDupsMaxVersionWithStagingFilter(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithFilterDupsMaxVersionWithFilteredDataset()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__FILTER_DUPS__MAX_VERSION__WITH_FILTERED_DATASET();

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .executionTimestampClock(fixedClock_2000_01_01)
            .cleanupStagingData(false)
            .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        this.verifyUnitemporalDeltaWithFilterDupsMaxVersionWithFilteredDataset(operations);
    }

    public abstract void verifyUnitemporalDeltaWithFilterDupsMaxVersionWithFilteredDataset(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithNoDedupMaxVersionWithoutPerformAndStagingFilters()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEDUP__MAX_VERSION_WITHOUT_PERFORM__WITH_STAGING_FILTER();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(false)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        this.verifyUnitemporalDeltaWithNoDedupMaxVersionWithoutPerformAndStagingFilters(operations);
    }

    public abstract void verifyUnitemporalDeltaWithNoDedupMaxVersionWithoutPerformAndStagingFilters(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithFailOnDupsMaxVersioningWithoutPerform()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__FAIL_ON_DUPS__MAX_VERSIONING_WITHOUT_PERFORM__NO_STAGING_FILTER();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .batchSuccessStatusValue("SUCCEEDED")
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        this.verifyUnitemporalDeltaWithFailOnDupsMaxVersioningWithoutPerform(operations);
    }

    public abstract void verifyUnitemporalDeltaWithFailOnDupsMaxVersioningWithoutPerform(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithNoDedupMaxVersioningAndUpperCaseWithoutStagingFilters()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEDUP__MAX_VERSIONING__NO_STAGING_FILTER();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .caseConversion(CaseConversion.TO_UPPER)
                .putAllAdditionalMetadata(Collections.singletonMap("watermark", "my_watermark_value"))
                .sampleRowCount(10)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        this.verifyUnitemporalDeltaWithNoDedupMaxVersioningAndUpperCaseWithoutStagingFilters(operations);
    }

    public abstract void verifyUnitemporalDeltaWithNoDedupMaxVersioningAndUpperCaseWithoutStagingFilters(GeneratorResult operations);

    public abstract RelationalSink getRelationalSink();
}
