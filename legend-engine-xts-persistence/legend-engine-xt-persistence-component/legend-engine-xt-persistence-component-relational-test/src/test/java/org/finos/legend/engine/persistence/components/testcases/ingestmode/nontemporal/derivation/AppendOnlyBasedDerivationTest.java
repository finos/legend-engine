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

import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeCaseConverter;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.scenarios.AppendOnlyScenarios;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.finos.legend.engine.persistence.components.BaseTest.assertDerivedMainDataset;

public class AppendOnlyBasedDerivationTest
{
    AppendOnlyScenarios scenarios = new AppendOnlyScenarios();

    @Test
    void testAppendOnlyAllowDuplicatesNoAuditingNoVersioningNoFilterExistingRecords()
    {
        TestScenario scenario = scenarios.NO_AUDITING__NO_DEDUP__NO_VERSIONING__NO_FILTER_EXISTING_RECORDS();
        assertDerivedMainDataset(scenario);
        AppendOnly mode = (AppendOnly) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertTrue(mode.digestGenStrategy() instanceof UserProvidedDigestGenStrategy);
        Assertions.assertEquals("DIGEST", ((UserProvidedDigestGenStrategy) mode.digestGenStrategy()).digestField());
        Assertions.assertTrue(mode.auditing() instanceof NoAuditing);
        Assertions.assertTrue(mode.deduplicationStrategy() instanceof AllowDuplicates);
    }

    @Test
    void testAppendOnlyFailOnDuplicatesWithAuditingAllVersionNoFilterExistingRecords()
    {
        // Auditing column is a PK
        TestScenario scenario = scenarios.WITH_AUDITING__FAIL_ON_DUPS__ALL_VERSION__NO_FILTER_EXISTING_RECORDS();
        assertDerivedMainDataset(scenario);
        AppendOnly mode = (AppendOnly) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertTrue(mode.digestGenStrategy() instanceof UserProvidedDigestGenStrategy);
        Assertions.assertEquals("DIGEST", ((UserProvidedDigestGenStrategy) mode.digestGenStrategy()).digestField());
        Assertions.assertEquals("DATA_SPLIT", mode.dataSplitField().get());
        Assertions.assertTrue(mode.auditing() instanceof DateTimeAuditing);
        DateTimeAuditing auditing = (DateTimeAuditing) mode.auditing();
        Assertions.assertEquals("BATCH_UPDATE_TIME", auditing.dateTimeField());
        Assertions.assertTrue(mode.deduplicationStrategy() instanceof FailOnDuplicates);
    }

    @Test
    void testAppendOnlyAllowDuplicatesWithAuditingNoVersioningNoFilterExistingRecords()
    {
        // Auditing column is not a PK
        TestScenario scenario = scenarios.WITH_AUDITING__ALLOW_DUPLICATES__NO_VERSIONING__NO_FILTER_EXISTING_RECORDS();
        assertDerivedMainDataset(scenario);
        AppendOnly mode = (AppendOnly) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertTrue(mode.digestGenStrategy() instanceof UserProvidedDigestGenStrategy);
        Assertions.assertEquals("DIGEST", ((UserProvidedDigestGenStrategy) mode.digestGenStrategy()).digestField());
        Assertions.assertTrue(mode.auditing() instanceof DateTimeAuditing);
        DateTimeAuditing auditing = (DateTimeAuditing) mode.auditing();
        Assertions.assertEquals("BATCH_UPDATE_TIME", auditing.dateTimeField());
        Assertions.assertTrue(mode.deduplicationStrategy() instanceof AllowDuplicates);
    }
}
