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
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;

import java.util.Optional;

public class AppendOnlyScenarios extends BaseTest
{

    /*
    Test Scenarios for Non-temporal Delta
    Variables:
    1) Auditing: No Auditing, With Auditing
    2) DataSplit: Enabled, Disabled
    3) DeduplicationStrategy: Allow_Duplicates, Filter Duplicates, Fail on Duplicates

    Valid Combinations:
    1) Allow_Duplicates, No Auditing
    2) Allow_Duplicates, With Auditing
    3) Allow_Duplicates, With Auditing, With Data Splits

    4) Fail on Duplicates, No Auditing
    5) Fail on Duplicates, With Auditing

    6) Filter Duplicates, No Auditing
    7) Filter Duplicates, With Auditing
    8) Filter Duplicates, With Auditing, With Data Splits

    Invalid Combinations
    1) Any Deduplication Strategy, No Auditing, With Data Splits
    2) Fail on Duplicates, With Data Splits
    */

    public TestScenario ALLOW_DUPLICATES_NO_AUDITING()
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestField(digestField)
                .deduplicationStrategy(AllowDuplicates.builder().build())
                .auditing(NoAuditing.builder().build())
                .build();
        return new TestScenario(mainTableWithNoPrimaryKeys, stagingTableWithNoPrimaryKeys, ingestMode);
    }

    public TestScenario ALLOW_DUPLICATES_WITH_AUDITING()
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestField(digestField)
                .deduplicationStrategy(AllowDuplicates.builder().build())
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .build();
        return new TestScenario(mainTableWithNoPrimaryKeysHavingAuditField, stagingTableWithNoPrimaryKeys, ingestMode);
    }

    public TestScenario ALLOW_DUPLICATES_WITH_AUDITING__WITH_DATASPLIT()
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestField(digestField)
                .deduplicationStrategy(AllowDuplicates.builder().build())
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .dataSplitField(Optional.of(dataSplitField))
                .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditField, stagingTableWithBaseSchemaHavingDigestAndDataSplit, ingestMode);
    }

    public TestScenario FAIL_ON_DUPLICATES_NO_AUDITING()
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestField(digestField)
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .auditing(NoAuditing.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaAndDigest, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario FAIL_ON_DUPLICATES_WITH_AUDITING()
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestField(digestField)
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditField, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario FILTER_DUPLICATES_NO_AUDITING()
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestField(digestField)
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .auditing(NoAuditing.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaAndDigest, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario FILTER_DUPLICATES_NO_AUDITING_WITH_DATA_SPLIT()
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestField(digestField)
                .dataSplitField(Optional.of(dataSplitField))
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .auditing(NoAuditing.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaAndDigest, stagingTableWithBaseSchemaHavingDigestAndDataSplit, ingestMode);
    }

    public TestScenario FILTER_DUPLICATES_WITH_AUDITING()
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestField(digestField)
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditField, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario FILTER_DUPLICATES_WITH_AUDITING_WITH_DATA_SPLIT()
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestField(digestField)
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .dataSplitField(Optional.of(dataSplitField))
                .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditField, stagingTableWithBaseSchemaHavingDigestAndDataSplit, ingestMode);
    }
}
