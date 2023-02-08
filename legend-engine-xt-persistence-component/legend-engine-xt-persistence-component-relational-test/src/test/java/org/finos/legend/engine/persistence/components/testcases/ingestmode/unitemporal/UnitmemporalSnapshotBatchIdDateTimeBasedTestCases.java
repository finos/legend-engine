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
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.finos.legend.engine.persistence.components.scenarios.UnitemporalSnapshotBatchIdBasedScenarios;
import org.finos.legend.engine.persistence.components.scenarios.UnitemporalSnapshotBatchIdDateTimeBasedScenarios;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public abstract class UnitmemporalSnapshotBatchIdDateTimeBasedTestCases extends BaseTest
{

    UnitemporalSnapshotBatchIdDateTimeBasedScenarios scenarios = new UnitemporalSnapshotBatchIdDateTimeBasedScenarios();

    @Test
    void testUnitemporalSnapshotWithoutPartitionNoDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithoutPartitionNoDataSplits(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithoutPartitionNoDataSplits(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithoutPartitionForEmptyBatch()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperationsForEmptyBatch(scenario.getDatasets());
        verifyUnitemporalSnapshotWithoutPartitionForEmptyBatch(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithoutPartitionForEmptyBatch(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithoutPartitionWithUpperCaseOptimizer()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DATA_SPLITS();
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
    void testUnitemporalSnapshotWithPartitionNoDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITH_PARTITIONS__NO_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionNoDataSplits(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionNoDataSplits(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithPartitionFiltersNoDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITH_PARTITION_FILTER__NO_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyUnitemporalSnapshotWithPartitionFiltersNoDataSplits(operations);
    }

    public abstract void verifyUnitemporalSnapshotWithPartitionFiltersNoDataSplits(GeneratorResult operations);

    @Test
    void testUnitemporalSnapshotWithCleanStagingData()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DATA_SPLITS();
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
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DATA_SPLITS();
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
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DATA_SPLITS();
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
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DATA_SPLITS();
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

    public abstract RelationalSink getRelationalSink();
}
