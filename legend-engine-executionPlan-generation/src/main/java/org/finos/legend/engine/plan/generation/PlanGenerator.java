// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.generation;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.ExecutionContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;

import java.io.IOException;

public class PlanGenerator
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    public static String generateExecutionPlanAsString(LambdaFunction<?> l, Mapping mapping, Runtime pureRuntime, ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return PlanGenerator.serializeToJSON(PlanGenerator.generateExecutionPlanAsPure(l, mapping, pureRuntime, context, pureModel, platform, planId, extensions), clientVersion, pureModel, extensions, transformers);
    }

    public static SingleExecutionPlan generateExecutionPlan(LambdaFunction<?> l, Mapping mapping, Runtime pureRuntime, ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return PlanGenerator.stringToPlan(generateExecutionPlanAsString(l, mapping, pureRuntime, context, pureModel, clientVersion, platform, planId, extensions, transformers));
    }

    public static SingleExecutionPlan generateExecutionPlanWithTrace(LambdaFunction<?> l, Mapping mapping, Runtime pureRuntime, ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, Iterable<? extends CommonProfile> profiles, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        Root_meta_pure_executionPlan_ExecutionPlan plan = generateExecutionPlanAsPure(l, mapping, pureRuntime, context, pureModel, platform, null, extensions);
        return transformExecutionPlan(plan, pureModel, clientVersion, profiles, extensions, transformers);
    }

    public static SingleExecutionPlan transformExecutionPlan(Root_meta_pure_executionPlan_ExecutionPlan plan, PureModel pureModel, String clientVersion, Iterable<? extends CommonProfile> profiles, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Serialize plan to JSON").startActive(true))
        {
            String jsonPlan = serializeToJSON(plan, clientVersion, pureModel, extensions, transformers);
            scope.span().setTag("plan", jsonPlan);
            LOGGER.info(new LogInfo(profiles, LoggingEventType.PLAN_GENERATED, jsonPlan).toString());
            return stringToPlan(jsonPlan);
        }
    }

    public static Root_meta_pure_executionPlan_ExecutionPlan generateExecutionPlanAsPure(LambdaFunction<?> l, Mapping mapping, Runtime pureRuntime, ExecutionContext context, PureModel pureModel, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Generate Plan").startActive(true))
        {
            Root_meta_pure_executionPlan_ExecutionPlan plan;

            if (mapping == null)
            {
                plan = context == null ?
                        core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__RouterExtension_MANY__ExecutionPlan_1_(l, extensions, pureModel.getExecutionSupport())
                        : core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__ExecutionContext_1__RouterExtension_MANY__ExecutionPlan_1_(l, context, extensions, pureModel.getExecutionSupport());
            }
            else
            {
//                plan = context == null ?
//                        core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__Mapping_1__Runtime_1__RouterExtension_MANY__DebugContext_1__ExecutionPlan_1_(l, mapping, pureRuntime, extensions, core_pure_tools_tools_extension.Root_meta_pure_tools_debug__DebugContext_1_(pureModel.getExecutionSupport()), pureModel.getExecutionSupport())
//                        : core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__Mapping_1__Runtime_1__ExecutionContext_1__RouterExtension_MANY__DebugContext_1__ExecutionPlan_1_(l, mapping, pureRuntime, context, extensions, core_pure_tools_tools_extension.Root_meta_pure_tools_debug__DebugContext_1_(pureModel.getExecutionSupport()), pureModel.getExecutionSupport());
                plan = context == null ?
                        core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__Mapping_1__Runtime_1__RouterExtension_MANY__ExecutionPlan_1_(l, mapping, pureRuntime, extensions, pureModel.getExecutionSupport())
                        : core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__Mapping_1__Runtime_1__ExecutionContext_1__RouterExtension_MANY__ExecutionPlan_1_(l, mapping, pureRuntime, context, extensions, pureModel.getExecutionSupport());
            }
            if (platform != null)
            {
                plan = platform.bindPlan(plan, planId, pureModel, extensions);
            }
            scope.span().log(String.valueOf(LoggingEventType.PLAN_GENERATED));
            return plan;
        }
    }

    private static String serializeToJSON(Root_meta_pure_executionPlan_ExecutionPlan purePlan, String clientVersion, PureModel pureModel, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        String cl = clientVersion == null ? PureClientVersions.latest : clientVersion;
        MutableList<? extends PlanTransformer> handlers = Iterate.selectWith(transformers, PlanTransformer::supports, cl, Lists.mutable.empty());
        Assert.assertTrue(handlers.size() == 1, () -> "Zero or more than one handler (" + handlers.size() + ") was found for protocol " + cl);
        Object transformed = handlers.get(0).transformToVersionedModel(purePlan, cl, extensions, pureModel.getExecutionSupport());
        return serializeToJSON(transformed, pureModel);
    }

    private static SingleExecutionPlan stringToPlan(String plan)
    {
        try
        {
            return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(plan, SingleExecutionPlan.class);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static String serializeToJSON(Object protocolPlan, PureModel pureModel)
    {
        return core_json_toJSON.Root_meta_json_toJSON_Any_MANY__Integer_$0_1$__Config_1__String_1_(
                Lists.mutable.with(protocolPlan),
                1000L,
                core_json_toJSON.Root_meta_json_config_Boolean_1__Boolean_1__Boolean_1__Boolean_1__Config_1_(false, false, true, true, pureModel.getExecutionSupport()),
                pureModel.getExecutionSupport()
        );
    }
}
