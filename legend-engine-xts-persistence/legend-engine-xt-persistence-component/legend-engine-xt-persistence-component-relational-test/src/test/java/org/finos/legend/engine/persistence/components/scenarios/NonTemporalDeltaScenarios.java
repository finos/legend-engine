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

import java.util.Arrays;
import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;

import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersionResolver;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;

public class NonTemporalDeltaScenarios extends BaseTest
{

    /*
    Test Scenarios for Non-temporal Delta
    Variables:
    1) Auditing: No Auditing, With Auditing
    2) MergeStrategy: No MergeStrategy, With Delete Indicator
    3) Deduplication: Allow duplicates, Filter duplicates, Fail on duplicates
    4) Versioning: No Versioning, Max Versioning, All Versioning
    */

    public TestScenario NO_AUDTING__NO_DEDUP_NO_VERSIONING()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(NoAuditing.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaAndDigest, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario NO_AUDTING__WITH_DELETE_INDICATOR__NO_DEDUP_NO_VERSIONING()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(NoAuditing.builder().build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorField)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .build();

        return new TestScenario(mainTableWithBaseSchemaAndDigest, stagingTableWithBaseSchemaAndDigestAndDeleteIndicator, ingestMode);
    }

    public TestScenario NO_AUDTING__ALLOW_DUPS_ALL_VERSION()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("biz_date").dataSplitFieldName(dataSplitField).versionResolver(VersionResolver.DIGEST_BASED).build())
                .deduplicationStrategy(AllowDuplicates.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaAndDigest, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario NO_AUDTING__ALLOW_DUPS_ALL_VERSION_WITHOUT_PERFORM()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("biz_date").dataSplitFieldName(dataSplitField).performStageVersioning(false).versionResolver(VersionResolver.DIGEST_BASED).build())
                .deduplicationStrategy(AllowDuplicates.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaAndDigest, stagingTableWithBaseSchemaHavingDigestAndDataSplit, ingestMode);
    }

    public TestScenario WITH_AUDTING__FILTER_DUPLICATES_NO_VERSIONING()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditField, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario WITH_AUDTING__FAIL_ON_DUPS_ALL_VERSION()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("biz_date").versionResolver(VersionResolver.DIGEST_BASED).dataSplitFieldName(dataSplitField).build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditField, stagingTableWithBaseSchemaAndDigest, ingestMode);
   }

    public TestScenario NO_VERSIONING__WITH_STAGING_FILTER()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(NoAuditing.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaAndDigest, stagingTableWithFilters, ingestMode);
    }

    public TestScenario MAX_VERSION__FILTER_DUPLICATES__WITH_STAGING_FILTER()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(NoAuditing.builder().build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(version.name())
                .versionResolver(VersionResolver.GREATER_THAN_ACTIVE_VERSION)
                .performStageVersioning(true)
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();
        return new TestScenario(mainTableWithVersion, stagingTableWithVersionAndSnapshotId, ingestMode);
    }

    public TestScenario MAX_VERSION_WITHOUT_PERFORM__ALLOW_DUPLICATES__WITH_STAGING_FILTER()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(NoAuditing.builder().build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(version.name())
                .versionResolver(VersionResolver.GREATER_THAN_ACTIVE_VERSION)
                .performStageVersioning(false)
                .build())
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .build();
        return new TestScenario(mainTableWithVersion, stagingTableWithVersionAndSnapshotId, ingestMode);
    }

    public TestScenario MAX_VERSION_WITHOUT_PERFORM__ALLOW_DUPLICATES()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(NoAuditing.builder().build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(version.name())
                .versionResolver(VersionResolver.GREATER_THAN_ACTIVE_VERSION)
                .performStageVersioning(false)
                .build())
            .build();
        return new TestScenario(mainTableWithVersion, stagingTableWithVersion, ingestMode);
    }

    public TestScenario MAX_VERSIONING__ALLOW_DUPLICATES()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(NoAuditing.builder().build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(version.name())
                .versionResolver(VersionResolver.GREATER_THAN_EQUAL_TO_ACTIVE_VERSION)
                .performStageVersioning(true)
                .build())
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .build();
        return new TestScenario(mainTableWithVersion, stagingTableWithVersion, ingestMode);
    }
}
