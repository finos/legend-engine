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

package org.finos.legend.engine.pure.dsl.persistence.cloud.compiler.test;

import org.finos.legend.engine.language.pure.dsl.persistence.cloud.compiler.toPureGraph.PersistenceCloudCompilerExtension;
import org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph.ValidationContext;
import org.finos.legend.engine.language.pure.dsl.persistence.compiler.validation.ValidationResult;
import org.finos.legend.engine.language.pure.dsl.persistence.compiler.validation.ValidationRuleSet;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.cloud.context.AwsGluePersistencePlatform;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValidationRuleSetTest
{
    @Test
    public void dpuCountEqualsMinimum()
    {
        ValidationRuleSet<ValidationContext> ruleSet = PersistenceCloudCompilerExtension.VALIDATION_RULE_SET;
        ValidationContext context = createPersistenceContextWithDpuCount(2);

        assertTrue(ruleSet.validate(context).valid());
    }

    @Test
    public void dpuCountLessThanMinimum()
    {
        ValidationRuleSet<ValidationContext> ruleSet = PersistenceCloudCompilerExtension.VALIDATION_RULE_SET;
        ValidationContext context = createPersistenceContextWithDpuCount(1);

        ValidationResult result = ruleSet.validate(context);
        assertTrue(result.invalid());
        assertEquals(1, result.reasons().size());
        assertEquals("Data processing units value must be at least 2", result.reasons().get(0));
    }

    @Test
    public void dpuCountGreaterThanMinimum()
    {
        ValidationRuleSet<ValidationContext> ruleSet = PersistenceCloudCompilerExtension.VALIDATION_RULE_SET;
        ValidationContext context = createPersistenceContextWithDpuCount(10);

        assertTrue(ruleSet.validate(context).valid());
    }

    private static ValidationContext createPersistenceContextWithDpuCount(int dataProcessingUnits)
    {
        AwsGluePersistencePlatform platform = new AwsGluePersistencePlatform();
        platform.dataProcessingUnits = dataProcessingUnits;
        PersistenceContext context = new PersistenceContext();
        context.platform = platform;

        return new ValidationContext(null, context, null);
    }
}
