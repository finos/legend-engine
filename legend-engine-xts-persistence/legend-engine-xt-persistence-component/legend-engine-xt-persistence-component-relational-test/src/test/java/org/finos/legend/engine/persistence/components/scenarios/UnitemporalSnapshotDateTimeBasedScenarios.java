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
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DigestBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;

import java.util.Arrays;

public class UnitemporalSnapshotDateTimeBasedScenarios extends BaseTest
{

    /*
    Test Scenarios for Non-temporal Snapshot
    Variables:
    1) transactionMilestoning = Datetime based
    2) partition : Enabled, Disabled
    3) partitionValuesByField: Enabled, Disabled
    4) Versioning: NoVersioning, MaxVersioning
    5) Deduplication: AllowDups, FailOnDups, FilterDups

    Valid Combinations:
    1) Without Partition, No Dedup No Versioning
    2) Without Partition, FailOnDups MaxVersioning
    3) With Partition, No Dedup No Versioning
    4) With Partition Filter, No Dedup No Versioning
    */

    public TestScenario DATETIME_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(TransactionDateTime.builder()
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .build();
        return new TestScenario(mainTableWithDateTime, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario DATETIME_BASED__WITHOUT_PARTITIONS__FAIL_ON_DUP__MAX_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(TransactionDateTime.builder()
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .versioningStrategy(MaxVersionStrategy.builder().versioningField("biz_date").mergeDataVersionResolver(DigestBasedResolver.INSTANCE).build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .build();
        return new TestScenario(mainTableWithDateTime, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario DATETIME_BASED__WITH_PARTITIONS__NO_DEDUP__NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(TransactionDateTime.builder()
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .addAllPartitionFields(Arrays.asList(partitionKeys))
                .build();
        return new TestScenario(mainTableWithDateTime, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario DATETIME_BASED__WITH_PARTITION_FILTER__NO_DEDUP__NO_VERSION()
    {
        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestField)
                .transactionMilestoning(TransactionDateTime.builder()
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .addAllPartitionFields(Arrays.asList(partitionKeys))
                .putAllPartitionValuesByField(partitionFilter)
                .build();
        return new TestScenario(mainTableWithDateTime, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }
}