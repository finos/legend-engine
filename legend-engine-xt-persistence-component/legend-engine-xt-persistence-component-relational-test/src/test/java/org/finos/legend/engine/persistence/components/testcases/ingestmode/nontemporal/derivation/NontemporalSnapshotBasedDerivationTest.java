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

package org.finos.legend.engine.persistence.components.testcases.ingestmode.nontemporal.derivation;

import org.finos.legend.engine.persistence.components.scenarios.NontemporalSnapshotTestScenarios;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.junit.jupiter.api.Test;

import static org.finos.legend.engine.persistence.components.BaseTest.assertDerivedMainDataset;

public class NontemporalSnapshotBasedDerivationTest
{

    NontemporalSnapshotTestScenarios scenarios = new NontemporalSnapshotTestScenarios();

    @Test
    void testNontemporalSnapshotNoAuditingNoDataSplit()
    {
        TestScenario scenario = scenarios.NO_AUDTING__NO_DATASPLIT();
        assertDerivedMainDataset(scenario);
    }

    @Test
    void testNontemporalSnapshotNoAuditingWithDataSplit()
    {
        TestScenario scenario = scenarios.NO_AUDTING__WITH_DATASPLIT();
        assertDerivedMainDataset(scenario);
    }

    @Test
    void testNontemporalSnapshotWithAuditingNoDataSplit()
    {
        TestScenario scenario = scenarios.WITH_AUDTING__NO_DATASPLIT();
        assertDerivedMainDataset(scenario);
    }

    @Test
    void testNontemporalSnapshotWithAuditingWithDataSplit()
    {
        TestScenario scenario = scenarios.WITH_AUDTING__WITH_DATASPLIT();
        assertDerivedMainDataset(scenario);
    }

}
