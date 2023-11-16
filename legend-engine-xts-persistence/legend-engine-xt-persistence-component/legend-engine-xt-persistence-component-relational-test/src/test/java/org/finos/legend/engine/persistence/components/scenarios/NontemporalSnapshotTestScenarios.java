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
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.NoVersioningStrategy;

public class NontemporalSnapshotTestScenarios extends BaseTest
{

    /*
    Test Scenarios of Non-temporal Snapshot
    Variables:
    1) Auditing: No Auditing, With Auditing
    2) Deduplication: Allow duplicates, Filter duplicates, Fail on duplicates
    3) Versioning: No Versioning, Max Versioning

    Valid Scenarios:
    1. No Auditing , Allow Dups , No Versioining
    2. With Auditing, Filter Dups, No Versioining
    3. With Auditing, Fail on duplicates, Max version

    Invalid Scenario:
    1. All Versioning
    */

    public TestScenario NO_AUDTING__NO_DEDUP__NO_VERSIONING()
    {
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        return new TestScenario(mainTableWithBaseSchema, stagingTableWithBaseSchema, ingestMode);
    }

    public TestScenario WITH_AUDTING__FILTER_DUPLICATES__NO_VERSIONING()
    {
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder()
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .versioningStrategy(NoVersioningStrategy.builder().build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaHavingAuditField, stagingTableWithBaseSchema, ingestMode);
    }

    public TestScenario WITH_AUDTING__FAIL_ON_DUP__MAX_VERSION()
    {
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder()
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .versioningStrategy(MaxVersionStrategy.builder().versioningField(bizDateField).build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaHavingAuditField, stagingTableWithBaseSchema, ingestMode);
    }
}
