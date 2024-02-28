// Copyright 2023 Goldman Sachs
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
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.FailEmptyBatch;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.NoOp;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.finos.legend.engine.persistence.components.scenarios.UnitemporalSnapshotBatchIdDateTimeBasedScenarios;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

public abstract class UnitmemporalSnapshotBatchIdDateTimeBasedTestCases extends BaseTest
{

    UnitemporalSnapshotBatchIdDateTimeBasedScenarios scenarios = new UnitemporalSnapshotBatchIdDateTimeBasedScenarios();

    @Test
    void testUnitemporalSnapshotWithoutPartitionNoDedupNoVersioning()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithoutPartitionNoDedupNoVersioning(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithoutPartitionNoDedupNoVersioning(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithoutPartitionNoDedupMaxVersion()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DEDUP__MAX_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .ingestRunId(ingestRunId)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithoutPartitionNoDedupMaxVersion(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithoutPartitionNoDedupMaxVersion(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithoutPartitionWithDeleteTargetDataEmptyBatchHandling()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperationsForEmptyBatch(scenario.getDatasets());
        verifyUnitemporalSnapshotWithoutPartitionWithDeleteTargetDataEmptyBatchHandling(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithoutPartitionWithDeleteTargetDataEmptyBatchHandling(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithoutPartitionWithUpperCaseOptimizerFilterDupsMaxVersion()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__FILTER_DUPS__MAX_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .caseConversion(CaseConversion.TO_UPPER)
                .putAllAdditionalMetadata(Collections.singletonMap("watermark", "my_watermark_value"))
                .batchSuccessStatusValue("SUCCEEDED")
                .ingestRunId(ingestRunId)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithoutPartitionWithUpperCaseOptimizerFilterDupsMaxVersion(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithoutPartitionWithUpperCaseOptimizerFilterDupsMaxVersion(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionNoDedupNoVersioning()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITH_PARTITIONS__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .putAllAdditionalMetadata(Collections.singletonMap("watermark", "my_watermark_value"))
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionNoDedupNoVersioning(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionNoDedupNoVersioning(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionWithDefaultEmptyDataHandling()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITH_PARTITIONS__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperationsForEmptyBatch(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionWithDefaultEmptyDataHandling(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionWithDefaultEmptyDataHandling(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionWithNoOpEmptyBatchHandling()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITH_PARTITIONS__NO_DEDUP__NO_VERSION();
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .addAllPartitionFields(Arrays.asList(partitionKeys))
                .emptyDatasetHandling(NoOp.builder().build())
                .build();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(ingestMode)
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperationsForEmptyBatch(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionWithDefaultEmptyDataHandling(operations);
    }

    @Test
    void testUnitemporalSnapshotWithPartitionFiltersNoDedupNoVersioning()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITH_PARTITION_FILTER__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionFiltersNoDedupNoVersioning(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionFiltersNoDedupNoVersioning(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionFiltersWithDeleteTargetDataEmptyDataHandling()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITH_PARTITION_FILTER__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperationsForEmptyBatch(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionFiltersWithDeleteTargetDataEmptyDataHandling(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionFiltersWithDeleteTargetDataEmptyDataHandling(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionFiltersWithNoOpEmptyDataHandling()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITH_PARTITION_FILTER__NO_DEDUP__NO_VERSION();
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .addAllPartitionFields(Arrays.asList(partitionKeys))
                .putAllPartitionValuesByField(partitionFilter)
                .emptyDatasetHandling(NoOp.builder().build())
                .build();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(ingestMode)
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperationsForEmptyBatch(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionWithDefaultEmptyDataHandling(operations);
    }

    @Test
    void testUnitemporalSnapshotWithCleanStagingData()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithCleanStagingData(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithCleanStagingData(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithLessColumnsInStaging()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION();
        Dataset stagingDataset = scenario.getStagingTable().withSchema(stagingTableSchemaWithLimitedColumns);
        Datasets datasets = Datasets.of(scenario.getMainTable(), stagingDataset);

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(datasets);
        verifyUnitemporalSnapshotWithLessColumnsInStaging(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithLessColumnsInStaging(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPlaceholders()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .batchIdPattern("{BATCH_ID_PATTERN}")
                .batchStartTimestampPattern("{BATCH_START_TS_PATTERN}")
                .batchEndTimestampPattern("{BATCH_END_TS_PATTERN}")
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPlaceholders(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPlaceholders(GeneratorResult operations);

    @Test
    void testUnitemporalSnasphotValidationBatchIdInMissing()
    {
        try
        {
            UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                    .digestField(digestField)
                    .transactionMilestoning(BatchIdAndDateTime.builder()
                            .batchIdOutName(batchIdOutField)
                            .dateTimeInName(batchTimeInField)
                            .dateTimeOutName(batchTimeOutField)
                            .build())
                    .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build BatchIdAndDateTime, some of required attributes are not set [batchIdInName]", e.getMessage());
        }
    }

    @Test
    void testUnitemporalSnapshotValidationBatchIdInNotPrimaryKey()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION();
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

    @Test
    void testUnitemporalSnapshotPartitionKeysValidation()
    {
        try
        {
            UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                    .digestField(digestField)
                    .transactionMilestoning(BatchIdAndDateTime.builder()
                            .batchIdInName(batchIdInField)
                            .batchIdOutName(batchIdOutField)
                            .dateTimeInName(batchTimeInField)
                            .dateTimeOutName(batchTimeOutField)
                            .build())
                    .addAllPartitionFields(Arrays.asList("business_date"))
                    .putAllPartitionValuesByField(partitionFilter)
                    .build();
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Can not build UnitemporalSnapshot, partitionKey: [biz_date] not specified in partitionFields", e.getMessage());
        }
    }

    @Test
    void testUnitemporalSnapshotFailOnEmptyBatch()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION();
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .emptyDatasetHandling(FailEmptyBatch.builder().build())
                .build();

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(ingestMode)
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();

        try
        {
            GeneratorResult queries = generator.generateOperationsForEmptyBatch(scenario.getDatasets());
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Encountered an Empty Batch, FailEmptyBatch is enabled, so failing the batch!", e.getMessage());
        }
    }


    public abstract RelationalSink getRelationalSink();
}
