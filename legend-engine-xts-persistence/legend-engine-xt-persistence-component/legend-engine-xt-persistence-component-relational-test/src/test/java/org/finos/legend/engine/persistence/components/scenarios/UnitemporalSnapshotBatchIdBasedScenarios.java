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
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.NoOp;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;

import java.util.Arrays;

public class UnitemporalSnapshotBatchIdBasedScenarios extends BaseTest
{

    /*
    Test Scenarios for Non-temporal Snapshot
    Variables:
    1) transactionMilestoning = BatchId
    2) partition : Enabled, Disabled
    3) DataSplit: Enabled, Disabled
    4) partitionValuesByField: Enabled, Disabled
    5) Versioning: NoVersioning, MaxVersioning
    5) Deduplication: AllowDups, FailOnDups, FilterDups

    Valid Combinations:
    1) Without Partition, No Dedup No Versioning
    2) Without Partition, NoVersion , FailOnDups
    3) With Partition, No Dedup No Versioning
    4) With Partition, NoVersion , FilterDups
    5) With Partition Filter, No Dedup No Versioning

    MaxVersioning, AllowDups
    MaxVersioning, FailOnDups
    MaxVersioning, FilterDups
    */

    public TestScenario BATCH_ID_BASED__WITHOUT_PARTITIONS__NO_DEDUP_NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .emptyDatasetHandling(NoOp.builder().build())
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__WITHOUT_PARTITIONS__WITH_DATA_SPLITS()
    {
        return null;
    }

    public TestScenario BATCH_ID_BASED__WITH_PARTITIONS__NO_DEDUP_NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .addAllPartitionFields(Arrays.asList(partitionKeys))
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__WITH_PARTITIONS__WITH_DATA_SPLITS()
    {
        return null;
    }


    public TestScenario BATCH_ID_BASED__WITH_PARTITION_FILTER__NO_DEDUP_NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .addAllPartitionFields(Arrays.asList(partitionKeys))
                .putAllPartitionValuesByField(partitionFilter)
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }
}