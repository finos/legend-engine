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
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.scenarios.AppendOnlyScenarios;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.finos.legend.engine.persistence.components.BaseTest.assertDerivedMainDataset;

public class AppendOnlyBasedDerivationTest
{
    AppendOnlyScenarios scenarios = new AppendOnlyScenarios();

    @Test
    void testAppendOnlyAllowDuplicatesNoAuditing()
    {
        TestScenario scenario = scenarios.ALLOW_DUPLICATES_NO_AUDITING();
        assertDerivedMainDataset(scenario);
        AppendOnly mode = (AppendOnly) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertEquals("DIGEST", mode.digestField().get());
        Assertions.assertTrue(mode.auditing() instanceof NoAuditing);
        Assertions.assertTrue(mode.deduplicationStrategy() instanceof AllowDuplicates);
    }

    @Test
    void testAppendOnlyAllowDuplicatesWithAuditing()
    {
        TestScenario scenario = scenarios.ALLOW_DUPLICATES_WITH_AUDITING();
        assertDerivedMainDataset(scenario);
        AppendOnly mode = (AppendOnly) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertEquals("DIGEST", mode.digestField().get());
        Assertions.assertTrue(mode.auditing() instanceof DateTimeAuditing);
        DateTimeAuditing auditing = (DateTimeAuditing) mode.auditing();
        Assertions.assertEquals("BATCH_UPDATE_TIME", auditing.dateTimeField());
        Assertions.assertTrue(mode.deduplicationStrategy() instanceof AllowDuplicates);
    }

    @Test
    void testAppendOnlyAllowDuplicatesWithAuditingWithDataSplit()
    {
        TestScenario scenario = scenarios.ALLOW_DUPLICATES_WITH_AUDITING__WITH_DATASPLIT();
        assertDerivedMainDataset(scenario);
        AppendOnly mode = (AppendOnly) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertEquals("DIGEST", mode.digestField().get());
        Assertions.assertEquals("DATA_SPLIT", mode.dataSplitField().get());
        Assertions.assertTrue(mode.auditing() instanceof DateTimeAuditing);
        DateTimeAuditing auditing = (DateTimeAuditing) mode.auditing();
        Assertions.assertEquals("BATCH_UPDATE_TIME", auditing.dateTimeField());
        Assertions.assertTrue(mode.deduplicationStrategy() instanceof AllowDuplicates);
    }

    @Test
    void testAppendOnlyFailOnDuplicatesNoAuditing()
    {
        TestScenario scenario = scenarios.FAIL_ON_DUPLICATES_NO_AUDITING();
        assertDerivedMainDataset(scenario);
        AppendOnly mode = (AppendOnly) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertEquals("DIGEST", mode.digestField().get());
        Assertions.assertTrue(mode.auditing() instanceof NoAuditing);
        Assertions.assertTrue(mode.deduplicationStrategy() instanceof FailOnDuplicates);
    }

    @Test
    void testAppendOnlyFailOnDuplicatesWithAuditing()
    {
        TestScenario scenario = scenarios.FAIL_ON_DUPLICATES_WITH_AUDITING();
        assertDerivedMainDataset(scenario);
        AppendOnly mode = (AppendOnly) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertEquals("DIGEST", mode.digestField().get());
        Assertions.assertTrue(mode.auditing() instanceof DateTimeAuditing);
        DateTimeAuditing auditing = (DateTimeAuditing) mode.auditing();
        Assertions.assertEquals("BATCH_UPDATE_TIME", auditing.dateTimeField());
        Assertions.assertTrue(mode.deduplicationStrategy() instanceof FailOnDuplicates);
    }

    @Test
    void testAppendOnlyFilterDuplicatesNoAuditing()
    {
        TestScenario scenario = scenarios.FILTER_DUPLICATES_NO_AUDITING();
        assertDerivedMainDataset(scenario);
        AppendOnly mode = (AppendOnly) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertEquals("DIGEST", mode.digestField().get());
        Assertions.assertTrue(mode.auditing() instanceof NoAuditing);
        Assertions.assertTrue(mode.deduplicationStrategy() instanceof FilterDuplicates);
    }

    @Test
    void testAppendOnlyFilterDuplicatesNoAuditingWithDataSplit()
    {
        TestScenario scenario = scenarios.FILTER_DUPLICATES_NO_AUDITING_WITH_DATA_SPLIT();
        assertDerivedMainDataset(scenario);
        AppendOnly mode = (AppendOnly) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertEquals("DIGEST", mode.digestField().get());
        Assertions.assertEquals("DATA_SPLIT", mode.dataSplitField().get());
        Assertions.assertTrue(mode.deduplicationStrategy() instanceof FilterDuplicates);
    }

    @Test
    void testAppendOnlyFilterDuplicatesWithAuditing()
    {
        TestScenario scenario = scenarios.FILTER_DUPLICATES_WITH_AUDITING();
        assertDerivedMainDataset(scenario);
        AppendOnly mode = (AppendOnly) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertEquals("DIGEST", mode.digestField().get());
        Assertions.assertTrue(mode.auditing() instanceof DateTimeAuditing);
        DateTimeAuditing auditing = (DateTimeAuditing) mode.auditing();
        Assertions.assertEquals("BATCH_UPDATE_TIME", auditing.dateTimeField());
        Assertions.assertTrue(mode.deduplicationStrategy() instanceof FilterDuplicates);
    }

    @Test
    void testAppendOnlyFilterDuplicatesWithAuditingWithDataSplit()
    {
        TestScenario scenario = scenarios.FILTER_DUPLICATES_WITH_AUDITING_WITH_DATA_SPLIT();
        assertDerivedMainDataset(scenario);
        AppendOnly mode = (AppendOnly) scenario.getIngestMode().accept(new IngestModeCaseConverter(String::toUpperCase));
        Assertions.assertEquals("DIGEST", mode.digestField().get());
        Assertions.assertEquals("DATA_SPLIT", mode.dataSplitField().get());
        Assertions.assertTrue(mode.auditing() instanceof DateTimeAuditing);
        DateTimeAuditing auditing = (DateTimeAuditing) mode.auditing();
        Assertions.assertEquals("BATCH_UPDATE_TIME", auditing.dateTimeField());
        Assertions.assertTrue(mode.deduplicationStrategy() instanceof FilterDuplicates);
    }
}
