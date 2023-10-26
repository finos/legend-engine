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
import org.finos.legend.engine.persistence.components.scenarios.UnitemporalSnapshotDateTimeBasedScenarios;
import org.junit.jupiter.api.Test;

import static org.finos.legend.engine.persistence.components.BaseTest.assertDerivedMainDataset;

public class UnitemporalSnapshotDateTimeBasedDerivationTest
{

    UnitemporalSnapshotDateTimeBasedScenarios scenarios = new UnitemporalSnapshotDateTimeBasedScenarios();

    @Test
    void testUnitemporalSnapshotWithoutPartitionNoDedupNoVersion()
    {
        TestScenario scenario = scenarios.DATETIME_BASED__WITHOUT_PARTITIONS__NO_DEDUP__NO_VERSION();
        assertDerivedMainDataset(scenario);
    }

    @Test
    void testUnitemporalSnapshotWithoutPartitionFailOnDupsMaxVersion()
    {
        TestScenario scenario = scenarios.DATETIME_BASED__WITHOUT_PARTITIONS__FAIL_ON_DUP__MAX_VERSION();
        assertDerivedMainDataset(scenario);
    }

    @Test
    void testUnitemporalSnapshotWithPartitionNoDedupNoVersion()
    {
        TestScenario scenario = scenarios.DATETIME_BASED__WITH_PARTITIONS__NO_DEDUP__NO_VERSION();
        assertDerivedMainDataset(scenario);
    }

    @Test
    void testUnitemporalSnapshotWithPartitionFilterNoDedupNoVersion()
    {
        TestScenario scenario = scenarios.DATETIME_BASED__WITH_PARTITION_FILTER__NO_DEDUP__NO_VERSION();
        assertDerivedMainDataset(scenario);
    }
}
