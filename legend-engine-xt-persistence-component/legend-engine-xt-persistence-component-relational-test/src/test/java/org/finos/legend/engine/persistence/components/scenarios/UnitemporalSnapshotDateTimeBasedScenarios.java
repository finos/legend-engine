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

package org.finos.legend.engine.persistence.components.scenarios;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTime;

import java.util.Arrays;

public class UnitemporalSnapshotDateTimeBasedScenarios extends BaseTest
{

    /*
    Test Scenarios for Non-temporal Snapshot
    Variables:
    1) transactionMilestoning = Datetime based
    2) partition : Enabled, Disabled
    3) DataSplit: Enabled, Disabled
    4) partitionValuesByField: Enabled, Disabled

    Valid Combinations:
    1) Without Partition, No Data Splits
    2) Without Partition, With Data Splits -> TBD
    3) With Partition, No Data Splits
    4) With Partition, With Data Splits -> TBD
    5) Without Partition, No Data Splits, Partition Filter
    */

    public TestScenario DATETIME_BASED__WITHOUT_PARTITIONS__NO_DATA_SPLITS()
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

    public TestScenario DATETIME_BASED__WITHOUT_PARTITIONS__WITH_DATA_SPLITS()
    {
        return null;
    }

    public TestScenario DATETIME_BASED__WITH_PARTITIONS__NO_DATA_SPLITS()
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

    public TestScenario DATETIME_BASED__WITH_PARTITIONS__WITH_DATA_SPLITS()
    {
        return null;
    }


    public TestScenario DATETIME_BASED__WITH_PARTITION_FILTER__NO_DATA_SPLITS()
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