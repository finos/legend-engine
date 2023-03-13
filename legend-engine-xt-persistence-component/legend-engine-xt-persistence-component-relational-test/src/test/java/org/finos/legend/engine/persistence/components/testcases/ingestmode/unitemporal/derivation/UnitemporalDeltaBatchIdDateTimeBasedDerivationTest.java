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

package org.finos.legend.engine.persistence.components.testcases.ingestmode.unitemporal.derivation;

import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.finos.legend.engine.persistence.components.scenarios.UnitemporalDeltaBatchIdDateTimeBasedScenarios;
import org.junit.jupiter.api.Test;

import static org.finos.legend.engine.persistence.components.BaseTest.assertDerivedMainDataset;

public class UnitemporalDeltaBatchIdDateTimeBasedDerivationTest
{

    UnitemporalDeltaBatchIdDateTimeBasedScenarios scenarios = new UnitemporalDeltaBatchIdDateTimeBasedScenarios();

    @Test
    void testUnitemporalDeltaNoDeleteIndNoDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__NO_DEL_IND__NO_DATA_SPLITS();
        assertDerivedMainDataset(scenario);
    }

    @Test
    void testUnitemporalDeltaNoDeleteIndWithDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__NO_DEL_IND__WITH_DATA_SPLITS();
        assertDerivedMainDataset(scenario);
    }

    @Test
    void testUnitemporalDeltaWithDeleteIndNoDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITH_DEL_IND__NO_DATA_SPLITS();
        assertDerivedMainDataset(scenario);
    }

    @Test
    void testUnitemporalDeltaWithDeleteIndWithDataSplits()
    {
        TestScenario scenario = scenarios.BATCH_ID_AND_TIME_BASED__WITH_DEL_IND__WITH_DATA_SPLITS();
        assertDerivedMainDataset(scenario);
    }
}
