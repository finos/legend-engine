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
                .createStagingDataset(true)
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
                .createStagingDataset(true)
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
    void testUnitemporalSnasphotValidationBatchIdInMissing()
    {
        try
        {
            UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                    .digestField(digestField)
                    .transactionMilestoning(BatchId.builder()
                            .batchIdOutName(batchIdOutField)
                            .build())
                    .addAllPartitionFields(Arrays.asList(partitionKeys))
                    .putAllPartitionValuesByField(partitionFilter)
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


    public abstract RelationalSink getRelationalSink();
}
