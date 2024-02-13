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
import org.finos.legend.engine.persistence.components.ingestmode.digest.UDFBasedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DigestBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.NoVersioningStrategy;

import java.util.Arrays;

public class AppendOnlyScenarios extends BaseTest
{

    /*
    Test Scenarios for Append Only
    Variables:
    1) Auditing: No Auditing, With Auditing
    2) Versioning: NoVersion, MaxVersion, AllVersion
    3) Deduplication: Allow Duplicates, Filter Duplicates, Fail on Duplicates
    4) filterExistingRecords: true / false


    Valid Combinations:
    NoVersion:
    1) With Auditing, NoVersion, Allow Duplicates, true
    2) With Auditing, NoVersion, Filter Duplicates, true      - tested (perform deduplication, auditing, filter existing)
    3) With Auditing, NoVersion, Fail on Duplicates, true
    4) No Auditing, NoVersion, Allow Duplicates, false        - tested (the most basic case)
    5) With Auditing, NoVersion, Allow Duplicates, false
    6) No Auditing, NoVersion, Filter Duplicates, false
    7) With Auditing, NoVersion, Filter Duplicates, false
    8) No Auditing, NoVersion, Fail on Duplicates, false
    9) With Auditing, NoVersion, Fail on Duplicates, false

    MaxVersion:
    10) With Auditing, MaxVersion, Allow Duplicates, true
    11) With Auditing, MaxVersion, Filter Duplicates, true
    12) With Auditing, MaxVersion, Fail on Duplicates, true   - tested (perform deduplication and versioning, auditing, filter existing)
    13) With Auditing, MaxVersion, Allow Duplicates, false
    14) With Auditing, MaxVersion, Filter Duplicates, false   - tested (perform deduplication and versioning, auditing)
    15) With Auditing, MaxVersion, Fail on Duplicates, false

    AllVersion:
    16) With Auditing, AllVersion, Allow Duplicates, true
    17) With Auditing, AllVersion, Filter Duplicates, true    - tested (perform deduplication and versioning, data split, auditing, filter existing)
    18) With Auditing, AllVersion, Fail on Duplicates, true
    19) With Auditing, AllVersion, Allow Duplicates, false
    20) With Auditing, AllVersion, Filter Duplicates, false
    21) With Auditing, AllVersion, Fail on Duplicates, false  - tested (perform deduplication and versioning, data split, auditing)


    Invalid Combinations:
    NoAuditing + MaxVersion/AllVersion:
    22) No Auditing, MaxVersion, Allow Duplicates, true
    23) No Auditing, MaxVersion, Filter Duplicates, true
    24) No Auditing, MaxVersion, Fail on Duplicates, true
    25) No Auditing, MaxVersion, Allow Duplicates, false
    26) No Auditing, MaxVersion, Filter Duplicates, false
    27) No Auditing, MaxVersion, Fail on Duplicates, false
    28) No Auditing, AllVersion, Allow Duplicates, true
    29) No Auditing, AllVersion, Filter Duplicates, true
    30) No Auditing, AllVersion, Fail on Duplicates, true
    31) No Auditing, AllVersion, Allow Duplicates, false
    32) No Auditing, AllVersion, Filter Duplicates, false     - tested
    33) No Auditing, AllVersion, Fail on Duplicates, false

    NoAuditing + filterExistingRecords
    34) No Auditing, NoVersion, Allow Duplicates, true        - tested
    35) No Auditing, NoVersion, Filter Duplicates, true
    36) No Auditing, NoVersion, Fail on Duplicates, true
    */

    public TestScenario NO_AUDITING__NO_DEDUP__NO_VERSIONING__NO_FILTER_EXISTING_RECORDS()
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestField).build())
                .deduplicationStrategy(AllowDuplicates.builder().build())
                .versioningStrategy(NoVersioningStrategy.builder().build())
                .auditing(NoAuditing.builder().build())
                .filterExistingRecords(false)
                .build();
        return new TestScenario(mainTableWithNoPrimaryKeys, stagingTableWithNoPrimaryKeys, ingestMode);
    }

    public TestScenario NO_AUDITING__NO_DEDUP__NO_VERSIONING__NO_FILTER_EXISTING_RECORDS__DERIVE_MAIN_SCHEMA()
    {
        TestScenario scenario = NO_AUDITING__NO_DEDUP__NO_VERSIONING__NO_FILTER_EXISTING_RECORDS();
        scenario.setMainTable(mainTableWithNoFields);
        return scenario;
    }

    public TestScenario WITH_AUDITING__FILTER_DUPS__NO_VERSIONING__WITH_FILTER_EXISTING_RECORDS()
    {
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestField).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .versioningStrategy(NoVersioningStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
            .filterExistingRecords(true)
            .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditField, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario WITH_AUDITING__FAIL_ON_DUPS__ALL_VERSION__NO_FILTER_EXISTING_RECORDS()
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestField).build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder()
                    .versioningField(bizDateField)
                    .dataSplitFieldName(dataSplitField)
                    .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                    .performStageVersioning(true)
                    .build())
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .filterExistingRecords(false)
                .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditField, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    // failure case
    public TestScenario NO_AUDITING__FILTER_DUPS__ALL_VERSION__NO_FILTER_EXISTING_RECORDS()
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestField).build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder()
                    .versioningField(bizDateField)
                    .dataSplitFieldName(dataSplitField)
                    .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                    .performStageVersioning(true)
                    .build())
                .auditing(NoAuditing.builder().build())
                .filterExistingRecords(false)
                .build();
        return new TestScenario(mainTableWithBaseSchemaAndDigest, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario WITH_AUDITING__FILTER_DUPS__ALL_VERSION__WITH_FILTER_EXISTING_RECORDS()
    {
        AppendOnly ingestMode = AppendOnly.builder()
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestField).build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder()
                    .versioningField(bizDateField)
                    .dataSplitFieldName(dataSplitField)
                    .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                    .performStageVersioning(true)
                    .build())
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .batchIdField(batchNumberField)
                .filterExistingRecords(true)
                .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditFieldAndBatchNumber, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario WITH_AUDITING__FAIL_ON_DUPS__MAX_VERSION__WITH_FILTER_EXISTING_RECORDS()
    {
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestField).build())
            .deduplicationStrategy(FailOnDuplicates.builder().build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(bizDateField)
                .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                .performStageVersioning(true)
                .build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
            .filterExistingRecords(true)
            .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditField, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario WITH_AUDITING__FILTER_DUPS__MAX_VERSION__NO_FILTER_EXISTING_RECORDS()
    {
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestField).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(bizDateField)
                .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                .performStageVersioning(true)
                .build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
            .filterExistingRecords(false)
            .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditField, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    // failure case
    public TestScenario NO_AUDITING__NO_DEDUP__NO_VERSIONING__WITH_FILTER_EXISTING_RECORDS()
    {
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestField).build())
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .versioningStrategy(NoVersioningStrategy.builder().build())
            .auditing(NoAuditing.builder().build())
            .filterExistingRecords(true)
            .build();
        return new TestScenario(mainTableWithNoPrimaryKeys, stagingTableWithNoPrimaryKeys, ingestMode);
    }

    public TestScenario WITH_AUDITING__ALLOW_DUPLICATES__NO_VERSIONING__NO_FILTER_EXISTING_RECORDS()
    {
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestField).build())
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .versioningStrategy(NoVersioningStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
            .filterExistingRecords(false)
            .build();
        return new TestScenario(mainTableWithNoPrimaryKeysHavingAuditField, stagingTableWithNoPrimaryKeys, ingestMode);
    }

    public TestScenario NO_AUDITING__NO_DEDUP__NO_VERSIONING__NO_FILTER_EXISTING_RECORDS__UDF_DIGEST_GENERATION()
    {
        AppendOnly ingestMode = AppendOnly.builder()
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .versioningStrategy(NoVersioningStrategy.builder().build())
            .auditing(NoAuditing.builder().build())
            .filterExistingRecords(false)
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestUdfName(digestUdf).digestField(digestField).build())
            .build();
        return new TestScenario(mainTableWithNoPrimaryKeys, stagingTableWithNoPrimaryKeysAndNoDigest, ingestMode);
    }

    public TestScenario WITH_AUDITING__FAIL_ON_DUPS__ALL_VERSION__NO_FILTER_EXISTING_RECORDS__UDF_DIGEST_GENERATION()
    {
        AppendOnly ingestMode = AppendOnly.builder()
            .deduplicationStrategy(FailOnDuplicates.builder().build())
            .versioningStrategy(AllVersionsStrategy.builder()
                .versioningField(bizDateField)
                .dataSplitFieldName(dataSplitField)
                .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                .performStageVersioning(true)
                .build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
            .filterExistingRecords(false)
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestUdfName(digestUdf).digestField(digestField).addAllFieldsToExcludeFromDigest(Arrays.asList(id.name(), amount.name())).build())
            .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditField, stagingTableWithBaseSchema, ingestMode);
    }
}
