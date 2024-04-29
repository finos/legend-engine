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

import org.finos.legend.engine.persistence.components.ingestmode.IngestModeCaseConverter;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.scenarios.NontemporalSnapshotTestScenarios;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.finos.legend.engine.persistence.components.BaseTest.assertDerivedMainDataset;

public class NontemporalSnapshotBasedDerivationTest
{

    NontemporalSnapshotTestScenarios scenarios = new NontemporalSnapshotTestScenarios();

    @Test
    void testNontemporalSnapshotNoAuditingNoDedupNoVersioning()
    {
        TestScenario scenario = scenarios.NO_AUDTING__NO_DEDUP__NO_VERSIONING();
        assertDerivedMainDataset(scenario);
        NontemporalSnapshot mode = (NontemporalSnapshot) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertTrue(mode.auditing() instanceof NoAuditing);
    }

    @Test
    void testNontemporalSnapshotWithAuditingFilterDupsNoVersioning()
    {
        TestScenario scenario = scenarios.WITH_AUDTING__FILTER_DUPLICATES__NO_VERSIONING();
        assertDerivedMainDataset(scenario);
        NontemporalSnapshot mode = (NontemporalSnapshot) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertTrue(mode.auditing() instanceof DateTimeAuditing);
        DateTimeAuditing auditing = (DateTimeAuditing) mode.auditing();
        Assertions.assertEquals("BATCH_UPDATE_TIME", auditing.dateTimeField());
    }

    @Test
    void testNontemporalSnapshotWithAuditingFailOnDupMaxVersion()
    {
        TestScenario scenario = scenarios.WITH_AUDTING__FAIL_ON_DUP__MAX_VERSION();
        assertDerivedMainDataset(scenario);
        NontemporalSnapshot mode = (NontemporalSnapshot) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertTrue(mode.auditing() instanceof DateTimeAuditing);
        DateTimeAuditing auditing = (DateTimeAuditing) mode.auditing();
        Assertions.assertEquals("BATCH_UPDATE_TIME", auditing.dateTimeField());
    }

}
