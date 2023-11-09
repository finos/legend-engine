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

package org.finos.legend.engine.language.hostedService.generation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.language.hostedService.generation.control.HostedServiceOwnerValidationService;
import org.finos.legend.engine.language.hostedService.generation.control.HostedServiceOwnerValidator;
import org.finos.legend.engine.protocol.hostedService.deployment.model.GenerationInfoData;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.hostedService.deployment.model.lineage.Lineage;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.CompositeExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_hostedService_HostedService;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_hostedService_Ownership;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_hostedservice_generation_generation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;

import java.util.Map;

public class HostedServiceArtifactGenerator
{
    public static GenerationInfoData renderArtifact(PureModel pureModel, Root_meta_external_function_activator_hostedService_HostedService activator, PureModelContext inputModel, String clientVersion, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        ExecutionPlan plan = generatePlan(pureModel, activator, inputModel, clientVersion, routerExtensions);
        Lineage lineage = new Lineage();
        return new GenerationInfoData(plan, lineage);
    }

    public static ExecutionPlan generatePlan(PureModel pureModel, Root_meta_external_function_activator_hostedService_HostedService activator, PureModelContext inputModel, String clientVersion,Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        if (core_hostedservice_generation_generation.Root_meta_external_function_activator_hostedService_generation_isMultiEenvironmentService_HostedService_1__Boolean_1_(activator, pureModel.getExecutionSupport()))
        {
            Map<String, SingleExecutionPlan> plans = Maps.mutable.empty();
            String execKey = core_hostedservice_generation_generation.Root_meta_external_function_activator_hostedService_generation_getEnvironmentkey_HostedService_1__String_1_(activator, pureModel.getExecutionSupport());
            core_hostedservice_generation_generation.Root_meta_external_function_activator_hostedService_generation_rebuildServiceUsingSingleExecutionParams_HostedService_1__Pair_MANY_(activator, pureModel.getExecutionSupport()).forEach(p ->
                    {
                        ExecutionPlan plan = PlanGenerator.generateExecutionPlan((ConcreteFunctionDefinition) p._second()._function(), null, null, null, pureModel,
                                clientVersion, PlanPlatform.JAVA, null, routerExtensions.apply(pureModel), LegendPlanTransformers.transformers);
                        plans.put(p._first(), (SingleExecutionPlan) plan);

                    }
            );
            return new CompositeExecutionPlan(plans,execKey, Lists.mutable.withAll(plans.keySet()));
        }
        else
        {
           return PlanGenerator.generateExecutionPlan((ConcreteFunctionDefinition)activator._function(), null, null, null, pureModel,
                    clientVersion,  PlanPlatform.JAVA, null, routerExtensions.apply(pureModel), LegendPlanTransformers.transformers);
        }
    }

    public boolean validateOwner(Identity identity, PureModel pureModel, Root_meta_external_function_activator_hostedService_HostedService activator, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        HostedServiceOwnerValidator<Root_meta_external_function_activator_hostedService_Ownership> service = getOwnerValidatorService(activator,pureModel);
        return  service.isOwner(identity, activator._ownership());
    }

    public HostedServiceOwnerValidator<Root_meta_external_function_activator_hostedService_Ownership> getOwnerValidatorService(Root_meta_external_function_activator_hostedService_HostedService activator, PureModel pureModel)
    {
        HostedServiceOwnerValidator<Root_meta_external_function_activator_hostedService_Ownership> service = HostedServiceOwnerValidationService.extensions().select(c -> c.supports(activator._ownership())).getFirst();
        if (service == null)
        {
            throw new RuntimeException(activator._ownership().getClass().getSimpleName() + "is not yet supported as an ownership model!");
        }
        return service;
    }
}
