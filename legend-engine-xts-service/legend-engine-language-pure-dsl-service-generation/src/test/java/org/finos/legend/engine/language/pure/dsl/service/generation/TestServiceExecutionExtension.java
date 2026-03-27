// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.service.generation;

import org.finos.legend.engine.language.pure.dsl.service.generation.extension.ServiceExecutionExtension;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Execution;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class TestServiceExecutionExtension
{
    static class TestCustomExecution extends Execution
    {
        public String customProperty;

        public TestCustomExecution(String customProperty)
        {
            this.customProperty = customProperty;
        }
    }

    static class TestServiceExecutionExtensionImpl implements ServiceExecutionExtension
    {
        @Override
        public List<ServiceExecutionPlanGenerator> getExtraServiceExecutionPlanGenerators()
        {
            return Collections.singletonList(createTestGenerator(false));
        }
    }

    private static ServiceExecutionExtension.ServiceExecutionPlanGenerator createTestGenerator(boolean authDependent)
    {
        return (execution, context, pureModel, clientVersion, platform, planId, extensions, transformers) ->
        {
            if (execution instanceof TestCustomExecution)
            {
                SingleExecutionPlan plan = new SingleExecutionPlan();
                plan.authDependent = authDependent;
                return plan;
            }
            return null;
        };
    }

    @Test
    public void testExtensionCanGeneratePlanForCustomExecutionType()
    {
        TestServiceExecutionExtensionImpl extension = new TestServiceExecutionExtensionImpl();
        List<ServiceExecutionExtension.ServiceExecutionPlanGenerator> generators = extension.getExtraServiceExecutionPlanGenerators();
        Assert.assertNotNull(generators);
        Assert.assertEquals(1, generators.size());

        // Test that generator handles TestCustomExecution
        TestCustomExecution customExecution = new TestCustomExecution("testValue");
        Assert.assertEquals("testValue", customExecution.customProperty);

        ExecutionPlan result = generators.get(0).generate(customExecution, null, null, "vX_X_X", PlanPlatform.JAVA, null, null, null);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof SingleExecutionPlan);

        // Test that generator returns null for non-custom execution types
        Execution otherExecution = new Execution() {};
        ExecutionPlan nullResult = generators.get(0).generate(otherExecution, null, null, "vX_X_X", PlanPlatform.JAVA, null, null, null);
        Assert.assertNull("Generator should return null for unsupported execution types", nullResult);
    }
}