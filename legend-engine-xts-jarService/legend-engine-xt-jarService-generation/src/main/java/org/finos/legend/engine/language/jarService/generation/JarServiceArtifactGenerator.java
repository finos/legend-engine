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

package org.finos.legend.engine.language.jarService.generation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.functionActivator.generation.FunctionActivatorGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.jarService.deployment.JarServiceArtifact;
import org.finos.legend.engine.protocol.jarService.deployment.model.GenerationInfoData;
import org.finos.legend.engine.protocol.jarService.deployment.model.lineage.Lineage;
import org.finos.legend.engine.protocol.jarService.metamodel.JarService;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.CompositeExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_DeploymentOwnership;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_jarService_JarService;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_jarservice_generation_generation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.function.InvalidFunctionDescriptorException;
import org.slf4j.Logger;

import java.util.Map;
import java.util.ServiceLoader;

import static org.finos.legend.pure.generated.platform_pure_essential_meta_graph_elementToPath.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_;

public class JarServiceArtifactGenerator
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JarServiceArtifactGenerator.class);
    static final MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));

    public static GenerationInfoData renderArtifact(PureModel pureModel, Root_meta_external_function_activator_jarService_JarService activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        ExecutionPlan plan = generatePlan(pureModel, activator, inputModel, clientVersion, routerExtensions);
        Lineage lineage = new Lineage();
        return new GenerationInfoData(plan, lineage);
    }

    public static JarServiceArtifact renderServiceArtifact(PureModel pureModel, Root_meta_external_function_activator_jarService_JarService activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        ExecutionPlan plan = generatePlan(pureModel, activator, inputModel, clientVersion, routerExtensions);
        Lineage lineage = new Lineage();
        return new JarServiceArtifact(JarServiceArtifactGenerator.fetchJarService(activator, (PureModelContextData)inputModel, pureModel), ((Root_meta_external_function_activator_DeploymentOwnership) activator._ownership())._id(), FunctionActivatorGenerator.generateActions(activator, pureModel), ((PureModelContextData)inputModel).origin != null ? (AlloySDLC) ((PureModelContextData)inputModel).origin.sdlcInfo : null);
    }

    public static ExecutionPlan generatePlan(PureModel pureModel, Root_meta_external_function_activator_jarService_JarService activator, PureModelContext inputModel, String clientVersion,Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        ListIterable<PlanTransformer> transformers =  generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);
        if (core_jarservice_generation_generation.Root_meta_external_function_activator_jarService_generation_isMultiEenvironmentService_JarService_1__Boolean_1_(activator, pureModel.getExecutionSupport()))
        {
            Map<String, SingleExecutionPlan> plans = Maps.mutable.empty();
            String execKey = core_jarservice_generation_generation.Root_meta_external_function_activator_jarService_generation_getEnvironmentkey_JarService_1__String_1_(activator, pureModel.getExecutionSupport());
            core_jarservice_generation_generation.Root_meta_external_function_activator_jarService_generation_rebuildServiceUsingFlattenedParams_JarService_1__Pair_MANY_(activator, pureModel.getExecutionSupport()).forEach(p ->
                    {
                        ExecutionPlan plan = PlanGenerator.generateExecutionPlan((ConcreteFunctionDefinition) p._second()._function(), null, null, null, pureModel,
                                clientVersion, PlanPlatform.JAVA, null, routerExtensions.apply(pureModel), transformers);
                        plans.put(p._first(), (SingleExecutionPlan) plan);

                    }
            );
            return new CompositeExecutionPlan(plans,execKey, Lists.mutable.withAll(plans.keySet()));
        }
        else
        {
           return PlanGenerator.generateExecutionPlan((ConcreteFunctionDefinition)activator._function(), null, null, null, pureModel,
                    clientVersion,  PlanPlatform.JAVA, null, routerExtensions.apply(pureModel), transformers);
        }
    }

    public static String generateLineage(PureModel pureModel, Root_meta_external_function_activator_jarService_JarService activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        //TODO fix multiEnv
        return core_jarservice_generation_generation.Root_meta_external_function_activator_jarService_generation_computeLineage_JarService_1__Extension_MANY__String_1_(activator, routerExtensions.apply(pureModel), pureModel.getExecutionSupport());
    }

    public static PureModelContextData fetchJarService(Root_meta_external_function_activator_jarService_JarService activator, PureModelContextData data, PureModel pureModel)
    {
        MutableList<PackageableElement> elements = Lists.mutable.withAll(data.getElements());
        JarService h = (JarService)elements.select(e -> e instanceof JarService && elementToPath(activator, pureModel).equals(e.getPath())).getFirst();
        org.finos.legend.engine.protocol.pure.m3.function.Function f = (org.finos.legend.engine.protocol.pure.m3.function.Function)elements.select(e ->
        {
            try
            {
                return e instanceof org.finos.legend.engine.protocol.pure.m3.function.Function && e.getPath().equals(FunctionDescriptor.functionDescriptorToId(h.function.path));
            }
            catch (InvalidFunctionDescriptorException invalidFunctionDescriptorException)
            {
                invalidFunctionDescriptorException.printStackTrace();
            }
            return false;
        }).getFirst();
        return PureModelContextData.newBuilder()
                .withElements(Lists.mutable.with(h,f))
                .withOrigin(data.origin).build();
    }

    public static String elementToPath(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element, PureModel pureModel)
    {
        return Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_(element, pureModel.getExecutionSupport());
    }

    public static String fullName(PackageableElement e)
    {
        return e._package + "::" + e.name;
    }

}
