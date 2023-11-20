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
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DigestBasedResolver;

import java.util.Arrays;

public class UnitemporalDeltaDateTimeBasedScenarios extends BaseTest
{

    /*
    Test Scenarios for Non-temporal Delta
    Variables:
    1) transactionMilestoning = DateTime
    2) deleteIndicator : Enabled, Disabled
    3) Deduplication: Allow duplicates, Filter duplicates, Fail on duplicates
    4) Versioning: No Versioning, Max Versioning, All Versioning
    */

    public TestScenario DATETIME_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSIONING()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(TransactionDateTime.builder()
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .build();
        return new TestScenario(mainTableWithDateTime, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario DATETIME_BASED__NO_DEL_IND__FAIL_ON_DUPS__ALL_VERSION_WITHOUT_PERFORM()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(TransactionDateTime.builder()
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("biz_date").mergeDataVersionResolver(DigestBasedResolver.INSTANCE).dataSplitFieldName(dataSplitField).performStageVersioning(false).build())
                .build();

        return new TestScenario(mainTableWithDateTime, stagingTableWithBaseSchemaHavingDigestAndDataSplit, ingestMode);
    }

    public TestScenario DATETIME_BASED__WITH_DEL_IND__NO_DEDUP__NO_VERSION()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(TransactionDateTime.builder()
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorField)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .build();
        return new TestScenario(mainTableWithDateTime, stagingTableWithDeleteIndicator, ingestMode);
    }

    public TestScenario DATETIME_BASED__WITH_DEL_IND__FILTER_DUPS__ALL_VERSION()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(TransactionDateTime.builder()
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorField)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("biz_date").mergeDataVersionResolver(DigestBasedResolver.INSTANCE).dataSplitFieldName(dataSplitField).performStageVersioning(true).build())
                .build();

        return new TestScenario(mainTableWithDateTime, stagingTableWithDeleteIndicator, ingestMode);
    }
}
