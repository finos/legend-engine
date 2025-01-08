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
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.DeleteTargetData;
import org.finos.legend.engine.persistence.components.ingestmode.partitioning.Partitioning;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.finos.legend.engine.persistence.components.scenarios.UnitemporalSnapshotBatchIdBasedScenarios;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public abstract class UnitmemporalSnapshotBatchIdBasedTestCases extends BaseTest
{

    UnitemporalSnapshotBatchIdBasedScenarios scenarios = new UnitemporalSnapshotBatchIdBasedScenarios();

    @Test
    void testUnitemporalSnapshotWithoutPartitionNoDedupNoVersion()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .skipMainAndMetadataDatasetCreation(true)
                .enableConcurrentSafety(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithoutPartitionNoDedupNoVersion(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithoutPartitionNoDedupNoVersion(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithoutPartitionFailOnDupsNoVersion()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITHOUT_PARTITIONS__FAIL_ON_DUPS__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .skipMainAndMetadataDatasetCreation(true)
                .enableConcurrentSafety(true)
                .ingestRunId(ingestRunId)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithoutPartitionFailOnDupsNoVersion(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithoutPartitionFailOnDupsNoVersion(GeneratorResult operations);


    @Test
    void testUnitemporalSnapshotWithoutPartitionWithNoOpEmptyBatchHandling()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperationsForEmptyBatch(scenario.getDatasets());
        verifyUnitemporalSnapshotWithoutPartitionWithNoOpEmptyBatchHandling(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithoutPartitionWithNoOpEmptyBatchHandling(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithoutPartitionWithUpperCaseOptimizer()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .caseConversion(CaseConversion.TO_UPPER)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithoutPartitionWithUpperCaseOptimizer(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithoutPartitionWithUpperCaseOptimizer(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionNoDedupNoVersion()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_PARTITIONS__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionNoDedupNoVersion(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionNoDedupNoVersion(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionFiltersNoDedupNoVersion()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_PARTITION_FILTER__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionFiltersNoDedupNoVersion(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionFiltersNoDedupNoVersion(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersion()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_PARTITION_SPEC_LIST__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersion(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersion(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersionInUpperCase()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_PARTITION_SPEC_LIST__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .caseConversion(CaseConversion.TO_UPPER)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersionInUpperCase(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersionInUpperCase(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionSpecListWithEmptyBatchHandling()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_PARTITION_SPEC_LIST__NO_DEDUP__NO_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperationsForEmptyBatch(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionSpecListWithEmptyBatchHandling(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionSpecListWithEmptyBatchHandling(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithCleanStagingData()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION();
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
    void testUnitemporalSnapshotWithPartitionNoDedupNoVersionNoDigest()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_PARTITIONS__NO_DEDUP__NO_VERSION__NO_DIGEST();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionNoDedupNoVersionNoDigest(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionNoDedupNoVersionNoDigest(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionFiltersNoDedupNoVersionNoDigest()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_PARTITION_FILTER__NO_DEDUP__NO_VERSION__NO_DIGEST();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionFiltersNoDedupNoVersionNoDigest(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionFiltersNoDedupNoVersionNoDigest(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersionNoDigest()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_PARTITION_SPEC_LIST__NO_DEDUP__NO_VERSION__NO_DIGEST();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersionNoDigest(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersionNoDigest(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersionInUpperCaseNoDigest()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_PARTITION_SPEC_LIST__NO_DEDUP__NO_VERSION__NO_DIGEST();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .caseConversion(CaseConversion.TO_UPPER)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersionInUpperCaseNoDigest(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionSpecListNoDedupNoVersionInUpperCaseNoDigest(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionSpecListWithEmptyBatchHandlingNoDigest()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITH_PARTITION_SPEC_LIST__NO_DEDUP__NO_VERSION__NO_DIGEST();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperationsForEmptyBatch(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionSpecListWithEmptyBatchHandlingNoDigest(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionSpecListWithEmptyBatchHandlingNoDigest(GeneratorResult operations);

    @Test
    void testUnitemporalSnasphotValidationBatchIdInMissing()
    {
        try
        {
            UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                    .digestField(digestField)
                    .transactionMilestoning(BatchId.builder()
                            .batchIdOutName(batchIdOutField)
                            .build())
                    .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList(partitionKeys)).putAllPartitionValuesByField(partitionFilter).build())
                    .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build BatchId, some of required attributes are not set [batchIdInName]", e.getMessage());
        }
    }

    @Test
    void testUnitemporalSnapshotValidationBatchIdInNotPrimaryKey()
    {
        TestScenario scenario = scenarios.BATCH_ID_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION();
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
    void testUnitemporalSnapshotValidationMainDatasetMissing()
    {
        try
        {
            Datasets datasets = Datasets.of(null, stagingTableWithBaseSchemaAndDigest);

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("mainDataset", e.getMessage());
        }
    }

    @Test
    void testUnitemporalSnapshotAllVersionValidation()
    {
        try
        {
            UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                    .digestField(digestField)
                    .transactionMilestoning(BatchId.builder()
                            .batchIdInName(batchIdInField)
                            .batchIdOutName(batchIdOutField)
                            .build())
                    .versioningStrategy(AllVersionsStrategy.builder().versioningField("xyz").build())
                    .build();
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build UnitemporalSnapshot, AllVersionsStrategy not supported", e.getMessage());
        }
    }

    @Test
    void testUnitemporalSnapshotPartitionFilterWithMultiValuesForMultipleKeysValidation()
    {
        try
        {
            UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                    .digestField(digestField)
                    .transactionMilestoning(BatchId.builder()
                            .batchIdInName(batchIdInField)
                            .batchIdOutName(batchIdOutField)
                            .build())
                    .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList(partitionKeysMulti)).putAllPartitionValuesByField(partitionFilterWithMultiValuesForMultipleKeys).build())
                    .emptyDatasetHandling(DeleteTargetData.builder().build())
                    .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Can not build Partitioning, in partitionValuesByField at most one of the partition keys can have more than one value, all other partition keys must have exactly one value", e.getMessage());
        }
    }

    @Test
    void testUnitemporalSnapshotPartitionFilterWithMultiValuesForOneKeyValidation()
    {
        try
        {
            UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                    .digestField(digestField)
                    .transactionMilestoning(BatchId.builder()
                            .batchIdInName(batchIdInField)
                            .batchIdOutName(batchIdOutField)
                            .build())
                    .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList(partitionKeysMulti)).putAllPartitionValuesByField(partitionFilterWithMultiValuesForOneKey).build())
                    .emptyDatasetHandling(DeleteTargetData.builder().build())
                    .build();
        }
        catch (Exception e)
        {
            Assertions.fail("No Exception expected for multi values for one key");
        }
    }

    @Test
    void testUnitemporalSnapshotBothPartitionFilterAndPartitionSpecListProvidedValidation()
    {
        try
        {
            UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                    .digestField(digestField)
                    .transactionMilestoning(BatchId.builder()
                            .batchIdInName(batchIdInField)
                            .batchIdOutName(batchIdOutField)
                            .build())
                    .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList(partitionKeysMulti)).putAllPartitionValuesByField(partitionFilterWithMultiValuesForOneKey).addAllPartitionSpecList(partitionSpecList()).build())
                    .emptyDatasetHandling(DeleteTargetData.builder().build())
                    .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Can not build Partitioning, Provide either partitionValuesByField or partitionSpecList, both not supported together", e.getMessage());
        }
    }

    @Test
    void testUnitemporalSnapshotPartitionKeyMismatchValidation()
    {
        try
        {
            UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                    .digestField(digestField)
                    .transactionMilestoning(BatchId.builder()
                            .batchIdInName(batchIdInField)
                            .batchIdOutName(batchIdOutField)
                            .build())
                    .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList(partitionKeys)).addAllPartitionSpecList(partitionSpecList()).build())
                    .emptyDatasetHandling(DeleteTargetData.builder().build())
                    .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Can not build Partitioning, size of each partitionSpec must be same as size of partitionFields", e.getMessage());
        }
    }


    public abstract RelationalSink getRelationalSink();
}
