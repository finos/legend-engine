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

public class NontemporalSnapshotTestScenarios extends BaseTest
{

    /*
    Test Scenarios of Non-temporal Snapshot
    Variables:
    1) Auditing: No Auditing, With Auditing
    2) DataSplit: Enabled, Disabled
    */

    public TestScenario NO_AUDTING__NO_DATASPLIT()
    {
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        return new TestScenario(mainTableWithBaseSchema, stagingTableWithBaseSchema, ingestMode);
    }

    public TestScenario NO_AUDTING__WITH_DATASPLIT()
    {
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder()
                .auditing(NoAuditing.builder().build())
                .dataSplitField(dataSplitField)
                .build();
        return new TestScenario(mainTableWithBaseSchema, stagingTableWithBaseSchemaHavingDataSplit, ingestMode);
    }

    public TestScenario WITH_AUDTING__NO_DATASPLIT()
    {
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder()
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .build();
        return new TestScenario(mainTableWithBaseSchema, stagingTableWithBaseSchema, ingestMode);
    }

    public TestScenario WITH_AUDTING__WITH_DATASPLIT()
    {
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder()
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeField).build())
                .dataSplitField("data_split")
                .build();
        return new TestScenario(mainTableWithBaseSchema, stagingTableWithBaseSchemaHavingDataSplit, ingestMode);
    }



}
