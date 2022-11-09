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

import java.util.Optional;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;

public class NonTemporalDeltaScenarios extends BaseTest
{

    /*
    Test Scenarios for Non-temporal Delta
    Variables:
    1) Auditing: No Auditing, With Auditing
    2) DataSplit: Enabled, Disabled
    3) MergeStrategy: No MergeStrategy, With Delete Indicator
    */

    public TestScenario NO_AUDTING__NO_DATASPLIT()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(NoAuditing.builder().build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaAndDigest, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario NO_AUDTING__NO_DATASPLIT__WITH_DELETE_INDICATOR()
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

    public TestScenario NO_AUDTING__WITH_DATASPLIT()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(NoAuditing.builder().build())
                .dataSplitField(Optional.of(dataSplitField))
                .build();
        return new TestScenario(mainTableWithBaseSchemaAndDigest, stagingTableWithBaseSchemaHavingDigestAndDataSplit, ingestMode);
    }

    public TestScenario WITH_AUDTING__NO_DATASPLIT()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditField, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario WITH_AUDTING__WITH_DATASPLIT()
    {
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .dataSplitField(Optional.of(dataSplitField))
                .build();
        return new TestScenario(mainTableWithBaseSchemaHavingDigestAndAuditField, stagingTableWithBaseSchemaHavingDigestAndDataSplit, ingestMode);
   }



}
