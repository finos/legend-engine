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
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.DeleteTargetData;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;

import java.util.Arrays;

public class UnitemporalSnapshotBatchIdDateTimeBasedScenarios extends BaseTest
{

    /*
    Test Scenarios for Non-temporal Snapshot
    Variables:
    1) transactionMilestoning = BatchIdAndDateTimeBased
    2) partition : Enabled, Disabled
    3) DataSplit: Enabled, Disabled
    4) partitionValuesByField: Enabled, Disabled
    5) Versioning: NoVersioning, MaxVersioning
    5) Deduplication: AllowDups, FailOnDups, FilterDups

    Valid Combinations:
    1) Without Partition, No Dedup No Versioning
    2) Without Partition, MaxVersioning, Allow Dups
    2) Without Partition, MaxVersioning, Filter Dups
    3) With Partition, No Dedup No Versioning
    5) With Partition Filter, No Dedup No Versioning
    */

    public TestScenario BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__NO_DEDUP_NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .emptyDatasetHandling(DeleteTargetData.builder().build())
                .build();
        return new TestScenario(mainTableWithBatchIdAndTime, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__MAX_VERSION_ALLOW_DUPS()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .deduplicationStrategy(AllowDuplicates.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder().versioningField("biz_date").build())
                .emptyDatasetHandling(DeleteTargetData.builder().build())
                .build();
        return new TestScenario(mainTableWithBatchIdAndTime, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_AND_TIME_BASED__WITHOUT_PARTITIONS__MAX_VERSION_FILTER_DUPS()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder().versioningField("biz_date").build())
                .emptyDatasetHandling(DeleteTargetData.builder().build())
                .build();
        return new TestScenario(mainTableWithBatchIdAndTime, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_AND_TIME_BASED__WITH_PARTITIONS__NO_DEDUP_NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .addAllPartitionFields(Arrays.asList(partitionKeys))
                .build();
        return new TestScenario(mainTableWithBatchIdAndTime, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_AND_TIME_BASED__WITH_PARTITION_FILTER__NO_DEDUP_NO_VERSION()
    {
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
                .emptyDatasetHandling(DeleteTargetData.builder().build())
                .build();
        return new TestScenario(mainTableWithBatchIdAndTime, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }
}