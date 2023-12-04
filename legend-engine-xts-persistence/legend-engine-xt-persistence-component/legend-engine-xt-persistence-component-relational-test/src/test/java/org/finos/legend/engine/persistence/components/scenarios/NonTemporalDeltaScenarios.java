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
import org.finos.legend.engine.persistence.components.ingestmode.versioning.*;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DigestBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersionColumnBasedResolver;

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

    public TestScenario NO_AUDTING__NO_DEDUP__NO_VERSIONING()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(NoAuditing.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaAndDigest, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario NO_AUDTING__WITH_DELETE_INDICATOR__NO_DEDUP__NO_VERSIONING()
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

    public TestScenario NO_AUDTING__NO_DEDUP__ALL_VERSION()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("biz_date").dataSplitFieldName(dataSplitField).mergeDataVersionResolver(DigestBasedResolver.INSTANCE).build())
                .deduplicationStrategy(AllowDuplicates.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaAndDigest, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario NO_AUDTING__NO_DEDUP__ALL_VERSION_WITHOUT_PERFORM()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("biz_date").dataSplitFieldName(dataSplitField).performStageVersioning(false).mergeDataVersionResolver(DigestBasedResolver.INSTANCE).build())
                .deduplicationStrategy(AllowDuplicates.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaAndDigest, stagingTableWithBaseSchemaHavingDigestAndDataSplit, ingestMode);
    }

    public TestScenario WITH_AUDTING__FILTER_DUPLICATES__NO_VERSIONING()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditField, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario WITH_AUDTING__FAIL_ON_DUPS__ALL_VERSION()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("biz_date").mergeDataVersionResolver(DigestBasedResolver.INSTANCE).dataSplitFieldName(dataSplitField).build())
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

    public TestScenario NO_VERSIONING__WITH_FILTERED_DATASET()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(NoAuditing.builder().build())
            .build();
        return new TestScenario(mainTableWithBaseSchemaAndDigest, filteredStagingTableWithComplexFilter, ingestMode);
    }

    public TestScenario FILTER_DUPS__MAX_VERSION__WITH_STAGING_FILTER()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(NoAuditing.builder().build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(version.name())
                .mergeDataVersionResolver(VersionColumnBasedResolver.builder().versionComparator(VersionComparator.GREATER_THAN).build())
                .performStageVersioning(true)
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();
        return new TestScenario(mainTableWithVersion, stagingTableWithVersionAndSnapshotId, ingestMode);
    }

    public TestScenario NO_DEDUP__MAX_VERSION_WITHOUT_PERFORM__WITH_STAGING_FILTER()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(NoAuditing.builder().build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(version.name())
                .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                .performStageVersioning(false)
                .build())
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .build();
        return new TestScenario(mainTableWithVersion, stagingTableWithVersionAndSnapshotId, ingestMode);
    }

    public TestScenario NO_DEDUP__MAX_VERSION_WITHOUT_PERFORM()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(NoAuditing.builder().build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(version.name())
                .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                .performStageVersioning(false)
                .build())
            .build();
        return new TestScenario(mainTableWithVersion, stagingTableWithVersion, ingestMode);
    }

    public TestScenario NO_DEDUP__MAX_VERSION()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestField)
            .auditing(NoAuditing.builder().build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(version.name())
                .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN_EQUAL_TO))
                .performStageVersioning(true)
                .build())
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .build();
        return new TestScenario(mainTableWithVersion, stagingTableWithVersion, ingestMode);
    }
}
