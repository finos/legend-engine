// Copyright 2021 Goldman Sachs
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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_java_platform_binding_legendJavaPlatformBinding_store_m2m_m2mLegendJavaPlatformBindingExtension;
import org.finos.legend.pure.generated.core_pure_extensions_functions;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;
import java.util.ServiceLoader;

public class TestServicePlanGenerator
{

    private class DummyExecutionOption extends ExecutionOption
    {
    }

    @Test
    public void testSingleExecutionServiceGenerationJSON()
    {
        PureModelContextData data = loadModelDataFromResource("simpleJsonService.json");
        PureModel pureModel = new PureModel(data, Identity.getAnonymousIdentity(), DeploymentMode.TEST);
        Service service = data.getElementsOfType(Service.class).get(0);
        Assert.assertTrue(service.execution instanceof PureSingleExecution);
        ExecutionPlan plan = ServicePlanGenerator.generateServiceExecutionPlan(service, null, pureModel, "vX_X_X", PlanPlatform.JAVA, core_java_platform_binding_legendJavaPlatformBinding_store_m2m_m2mLegendJavaPlatformBindingExtension.Root_meta_pure_mapping_modelToModel_executionPlan_platformBinding_legendJava_inMemoryExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers);
        Assert.assertTrue(plan instanceof SingleExecutionPlan);
    }

    @Test
    public void testSingleExecutionServiceGenerationWithExecutionOptions()
    {
        PureModelContextData data = loadModelDataFromResource("simpleJsonService.json");
        PureModel pureModel = new PureModel(data, Identity.getAnonymousIdentity(), DeploymentMode.TEST);
        Service service = data.getElementsOfType(Service.class).get(0);
        Assert.assertTrue(service.execution instanceof PureSingleExecution);
        ((PureSingleExecution) service.execution).executionOptions = Lists.mutable.of(new DummyExecutionOption());
        try
        {
            ExecutionPlan plan = ServicePlanGenerator.generateServiceExecutionPlan(service, null, pureModel, "vX_X_X", PlanPlatform.JAVA, core_pure_extensions_functions.Root_meta_pure_extension_defaultExtensions__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers);
            Assert.fail("Expected Exception, since we did not include the dummy execution option in the compiler extensions");
        }
        catch (Exception e)
        {
            Assert.assertEquals("Unsupported execution option type 'class org.finos.legend.engine.language.pure.dsl.service.generation.TestServicePlanGenerator$DummyExecutionOption'", e.getMessage());
        }
    }

    @Test
    public void testGenerationWithFunctionUsingFromEnvironment() throws Exception
    {
        PureModelContextData data = PureGrammarParser.newInstance().parseModel(new Scanner(getClass().getClassLoader().getResource("ServiceSpecificationUsingFromEnvironment.pure").openStream(), "UTF-8").useDelimiter("\\A").next());
        PureModel pureModel = new PureModel(data, Identity.getAnonymousIdentity(), DeploymentMode.TEST);
        Service service = data.getElementsOfType(Service.class).get(0);
        Assert.assertTrue(service.execution instanceof PureSingleExecution);
        MutableList<PlanGeneratorExtension> extensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
        ExecutionPlan plan = ServicePlanGenerator.generateServiceExecutionPlan(service, null, pureModel, "vX_X_X", PlanPlatform.JAVA, routerExtensions, LegendPlanTransformers.transformers);
        Assert.assertTrue(plan instanceof SingleExecutionPlan);
    }

    private PureModelContextData loadModelDataFromResource(String resourceName)
    {
        try
        {
            return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(Objects.requireNonNull(getClass().getClassLoader().getResource(resourceName), "Can't find resource '" + resourceName + "'"), PureModelContextData.class);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
