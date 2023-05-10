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
import java.util.List;

public abstract class UnitmemporalDeltaBatchIdBasedTestCases extends BaseTest
{

    UnitemporalDeltaBatchIdBasedScenarios scenarios = new UnitemporalDeltaBatchIdBasedScenarios();

    @Test
    void testUnitemporalDeltaNoDeleteIndNoDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaNoDeleteIndNoAuditing(operations);
    }

    public abstract void verifyUnitemporalDeltaNoDeleteIndNoAuditing(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaNoDeleteIndWithDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__WITH_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyUnitemporalDeltaNoDeleteIndWithDataSplits(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyUnitemporalDeltaNoDeleteIndWithDataSplits(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testUnitemporalDeltaWithDeleteIndNoDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_DEL_IND__NO_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaWithDeleteIndNoDataSplits(operations);
    }

    public abstract void verifyUnitemporalDeltaWithDeleteIndNoDataSplits(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithDeleteIndWithDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_DEL_IND__WITH_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyUnitemporalDeltaWithDeleteIndWithDataSplits(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyUnitemporalDeltaWithDeleteIndWithDataSplits(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testUnitemporalDeltaWithUpperCaseOptimizer()
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
        verifyUnitemporalDeltaWithUpperCaseOptimizer(operations);
    }

    public abstract void verifyUnitemporalDeltaWithUpperCaseOptimizer(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithCleanStagingData()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS();
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
    void testUnitemporalDeltaNoDeleteIndNoDataSplitsWithOptimizationFilters()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS__WITH_OPTIMIZATION_FILTERS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaNoDeleteIndNoAuditingWithOptimizationFilters(operations);
    }

    public abstract void verifyUnitemporalDeltaNoDeleteIndNoAuditingWithOptimizationFilters(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaNoDeleteIndNoDataSplitsWithOptimizationFiltersIncludesNullValues()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS__WITH_OPTIMIZATION_FILTERS__INCLUDES_NULL_VALUES();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .executionTimestampClock(fixedClock_2000_01_01)
            .collectStatistics(true)
            .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaNoDeleteIndNoAuditingWithOptimizationFiltersIncludesNullValues(operations);
    }

    public abstract void verifyUnitemporalDeltaNoDeleteIndNoAuditingWithOptimizationFiltersIncludesNullValues(GeneratorResult operations);

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
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS();
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
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS__WITH_MISSING_OPTIMIZATION_FILTER();
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
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS__WITH_OPTIMIZATION_FILTER_UNSUPPORTED_DATATYPE();
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
                .cleanupStagingData(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalDeltaWithNoVersionAndStagingFilter(operations);
    }

    public abstract void verifyUnitemporalDeltaWithNoVersionAndStagingFilter(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithMaxVersioningDedupEnabledAndStagingFiltersWithDedup()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__MAX_VERSIONING_WITH_GREATER_THAN__DEDUP__WITH_STAGING_FILTER();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        this.verifyUnitemporalDeltaWithMaxVersionDedupEnabledAndStagingFilter(operations);
    }

    public abstract void verifyUnitemporalDeltaWithMaxVersionDedupEnabledAndStagingFilter(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithMaxVersioningNoDedupAndStagingFilters()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__MAX_VERSIONING_WITH_GREATER_THAN__NO_DEDUP__WITH_STAGING_FILTER();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        this.verifyUnitemporalDeltaWithMaxVersionNoDedupAndStagingFilter(operations);
    }

    public abstract void verifyUnitemporalDeltaWithMaxVersionNoDedupAndStagingFilter(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithMaxVersioningNoDedupWithoutStagingFilters()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__MAX_VERSIONING_WITH_GREATER_THAN__NO_DEDUP__WITHOUT_STAGING_FILTER();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        this.verifyUnitemporalDeltaWithMaxVersioningNoDedupWithoutStagingFilters(operations);
    }

    public abstract void verifyUnitemporalDeltaWithMaxVersioningNoDedupWithoutStagingFilters(GeneratorResult operations);

    @Test
    void testUnitemporalDeltaWithMaxVersioningDedupEnabledAndUpperCaseWithoutStagingFilters()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__MAX_VERSIONING_WITH_GREATER_THAN_EQUAL__DEDUP__WITHOUT_STAGING_FILTER();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .caseConversion(CaseConversion.TO_UPPER)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        this.verifyUnitemporalDeltaWithMaxVersioningDedupEnabledAndUpperCaseWithoutStagingFilters(operations);
    }

    public abstract void verifyUnitemporalDeltaWithMaxVersioningDedupEnabledAndUpperCaseWithoutStagingFilters(GeneratorResult operations);

    public abstract RelationalSink getRelationalSink();
}
