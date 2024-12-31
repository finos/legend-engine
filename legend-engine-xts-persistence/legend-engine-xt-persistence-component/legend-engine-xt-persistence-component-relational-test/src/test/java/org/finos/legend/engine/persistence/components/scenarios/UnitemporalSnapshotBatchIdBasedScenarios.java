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

package org.finos.legend.engine.persistence.components.scenarios;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deletestrategy.DeleteAllStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.DeleteTargetData;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.NoOp;
import org.finos.legend.engine.persistence.components.ingestmode.partitioning.NoPartitioning;
import org.finos.legend.engine.persistence.components.ingestmode.partitioning.Partitioning;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;

import java.util.Arrays;

public class UnitemporalSnapshotBatchIdBasedScenarios extends BaseTest
{

    /*
    Test Scenarios for Non-temporal Snapshot
    Variables:
    1) transactionMilestoning = BatchId
    2) partition : Enabled, Disabled
    3) partitionValuesByField: Enabled, Disabled
    4) Versioning: NoVersioning, MaxVersioning
    5) Deduplication: AllowDups, FailOnDups, FilterDups

    Valid Combinations:
    1) Without Partition, No Dedup No Versioning
    2) Without Partition, FailOnDups No Versioning
    3) With Partition, No Dedup No Versioning
    4) With Partition Filter, No Dedup No Versioning
    5) With Partition, Delete All, No Dedup No Versioning
    6) With Partition Filter, Delete All, No Dedup No Versioning
    */

    public TestScenario BATCH_ID_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .emptyDatasetHandling(NoOp.builder().build())
                .partitioningStrategy(NoPartitioning.builder().build())
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__WITHOUT_PARTITIONS__FAIL_ON_DUPS__NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .emptyDatasetHandling(NoOp.builder().build())
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__WITH_PARTITIONS__NO_DEDUP__NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList(partitionKeys)).build())
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__WITH_PARTITION_FILTER__NO_DEDUP__NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList(partitionKeys)).putAllPartitionValuesByField(partitionFilter).build())
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__WITH_PARTITION_SPEC_LIST__NO_DEDUP__NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList(partitionKeysMulti)).addAllPartitionSpecList(partitionSpecList()).build())
                .emptyDatasetHandling(DeleteTargetData.builder().build())
                .build();
        return new TestScenario(mainTableMultiPartitionsBased, stagingTableWithMultiPartitions, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__WITH_PARTITIONS_DELETE_ALL__NO_DEDUP__NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList(partitionKeys)).deleteStrategy(DeleteAllStrategy.builder().build()).build())
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchemaWithoutDigest, stagingTableWithBaseSchema, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__WITH_PARTITIONS_DELETE_ALL__FILTER_DUPLICATES__MAX_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList(partitionKeys)).deleteStrategy(DeleteAllStrategy.builder().build()).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .versioningStrategy(MaxVersionStrategy.builder().versioningField("biz_date").build())
            .build();
        return new TestScenario(mainTableWithBatchIdBasedSchemaWithoutDigest, stagingTableWithBaseSchema, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__WITH_PARTITION_FILTER_DELETE_ALL__NO_DEDUP__NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList(partitionKeys)).putAllPartitionValuesByField(partitionFilter).deleteStrategy(DeleteAllStrategy.builder().build()).build())
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchemaWithoutDigest, stagingTableWithBaseSchema, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__WITH_PARTITION_SPEC_LIST_DELETE_ALL__NO_DEDUP__NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList(partitionKeysMulti)).addAllPartitionSpecList(partitionSpecList()).deleteStrategy(DeleteAllStrategy.builder().build()).build())
                .emptyDatasetHandling(DeleteTargetData.builder().build())
                .build();
        return new TestScenario(mainTableMultiPartitionsBasedWithoutDigest, stagingTableWithMultiPartitionsWithoutDigest, ingestMode);
    }

}