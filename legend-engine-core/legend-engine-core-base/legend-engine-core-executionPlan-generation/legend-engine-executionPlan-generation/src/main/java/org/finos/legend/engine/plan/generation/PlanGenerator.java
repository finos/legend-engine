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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.execution.Console;
import org.slf4j.Logger;

import static org.finos.legend.pure.generated.platform_pure_essential_tools_debug_debug.Root_meta_pure_tools_debug__DebugContext_1_;

public class PlanGenerator
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PlanGenerator.class);

    public static String generateExecutionPlanAsString(FunctionDefinition<?> l, Mapping mapping, Root_meta_core_runtime_Runtime pureRuntime, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return PlanGenerator.serializeToJSON(PlanGenerator.generateExecutionPlanAsPure(l, mapping, pureRuntime, context, pureModel, platform, planId, extensions), clientVersion, pureModel, extensions, transformers);
    }

    public static PlanWithDebug generateExecutionPlanDebug(FunctionDefinition<?> l, Mapping mapping, Root_meta_core_runtime_Runtime pureRuntime, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        Pair<Root_meta_pure_executionPlan_ExecutionPlan, String> res = PlanGenerator.generateExecutionPlanAsPureDebug(l, mapping, pureRuntime, context, pureModel, platform, planId, extensions);
        return new PlanWithDebug(PlanGenerator.stringToPlan(PlanGenerator.serializeToJSON(res.getOne(), clientVersion, pureModel, extensions, transformers)), res.getTwo());
    }

    public static SingleExecutionPlan generateExecutionPlan(FunctionDefinition<?> l, Mapping mapping, Root_meta_core_runtime_Runtime pureRuntime, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return PlanGenerator.stringToPlan(generateExecutionPlanAsString(l, mapping, pureRuntime, context, pureModel, clientVersion, platform, planId, extensions, transformers));
    }

    public static SingleExecutionPlan generateExecutionPlanWithTrace(FunctionDefinition<?> l, Mapping mapping, Root_meta_core_runtime_Runtime pureRuntime, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, Identity identity, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        Root_meta_pure_executionPlan_ExecutionPlan plan = generateExecutionPlanAsPure(l, mapping, pureRuntime, context, pureModel, platform, null, extensions);
        return transformExecutionPlan(plan, pureModel, clientVersion, identity, extensions, transformers);
    }

    public static SingleExecutionPlan transformExecutionPlan(Root_meta_pure_executionPlan_ExecutionPlan plan, PureModel pureModel, String clientVersion, Identity identity, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Serialize plan to JSON").startActive(true))
        {
            String jsonPlan = serializeToJSON(plan, clientVersion, pureModel, extensions, transformers);
            scope.span().setTag("plan", jsonPlan);
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.PLAN_GENERATED, jsonPlan).toString());
            return stringToPlan(jsonPlan);
        }
    }

    public static Pair<Root_meta_pure_executionPlan_ExecutionPlan, String> generateExecutionPlanAsPureDebug(FunctionDefinition<?> l, Mapping mapping, Root_meta_core_runtime_Runtime pureRuntime, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        return generateExecutionPlanAsPure(l, mapping, pureRuntime, context, pureModel, platform, planId, true, extensions);
    }

    public static Root_meta_pure_executionPlan_ExecutionPlan generateExecutionPlanAsPure(FunctionDefinition<?> l, Mapping mapping, Root_meta_core_runtime_Runtime pureRuntime, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        return generateExecutionPlanAsPure(l, mapping, pureRuntime, context, pureModel, platform, planId, false, extensions).getOne();
    }

    public static Pair<Root_meta_pure_executionPlan_ExecutionPlan, String> generateExecutionPlanAsPure(FunctionDefinition<?> l, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, PlanPlatform platform, String planId, boolean debug, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        return generateExecutionPlanAsPure(l, null, null, context, pureModel, platform, planId, debug, extensions);
    }

    private static Pair<Root_meta_pure_executionPlan_ExecutionPlan, String> generateExecutionPlanAsPure(FunctionDefinition<?> l, Mapping mapping, Root_meta_core_runtime_Runtime pureRuntime, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, PlanPlatform platform, String planId, boolean debug, RichIterable<? extends Root_meta_pure_extension_Extension> extensions)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Generate Plan").startActive(true))
        {
            Root_meta_pure_executionPlan_ExecutionPlan plan;
            String debugInfo = "";

            if (debug)
            {
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                Console console = pureModel.getExecutionSupport().getConsole();
                console.enable();
                try (PrintStream ps = new PrintStream(bs, true, StandardCharsets.UTF_8.name()))
                {
                    console.setPrintStream(ps);
                    if (mapping == null)
                    {
                        plan = context == null ?
                                core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__Extension_MANY__DebugContext_1__ExecutionPlan_1_(l, extensions, Root_meta_pure_tools_debug__DebugContext_1_(pureModel.getExecutionSupport()), pureModel.getExecutionSupport())
                                : core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__ExecutionContext_1__Extension_MANY__DebugContext_1__ExecutionPlan_1_(l, context, extensions, Root_meta_pure_tools_debug__DebugContext_1_(pureModel.getExecutionSupport()), pureModel.getExecutionSupport());
                    }
                    else
                    {
                        plan = context == null ?
                                core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__Mapping_1__Runtime_1__Extension_MANY__DebugContext_1__ExecutionPlan_1_(l, mapping, pureRuntime, extensions, Root_meta_pure_tools_debug__DebugContext_1_(pureModel.getExecutionSupport()), pureModel.getExecutionSupport())
                                : core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__Mapping_1__Runtime_1__ExecutionContext_1__Extension_MANY__DebugContext_1__ExecutionPlan_1_(l, mapping, pureRuntime, context, extensions, Root_meta_pure_tools_debug__DebugContext_1_(pureModel.getExecutionSupport()), pureModel.getExecutionSupport());
                    }
                    debugInfo = bs.toString(StandardCharsets.UTF_8.name());
                }
                catch (Exception e)
                {
                    debugInfo = bs.toString();
                    throw new RuntimeException(e.getMessage() + "-- Debug details before exception: " + debugInfo + "--", e);
                }
                finally
                {
                    console.disable();
                }
            }
            else
            {
                if (mapping == null)
                {
                    plan = context == null ?
                            core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__Extension_MANY__ExecutionPlan_1_(l, extensions, pureModel.getExecutionSupport())
                            : core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__ExecutionContext_1__Extension_MANY__ExecutionPlan_1_(l, context, extensions, pureModel.getExecutionSupport());

                }
                else
                {
                    plan = context == null ?
                            core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__Mapping_1__Runtime_1__Extension_MANY__ExecutionPlan_1_(l, mapping, pureRuntime, extensions, pureModel.getExecutionSupport())
                            : core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__Mapping_1__Runtime_1__ExecutionContext_1__Extension_MANY__ExecutionPlan_1_(l, mapping, pureRuntime, context, extensions, pureModel.getExecutionSupport());
                }
            }

            if (platform != null)
            {
                plan = platform.bindPlan(plan, planId, pureModel, extensions);
            }
            scope.span().log(String.valueOf(LoggingEventType.PLAN_GENERATED));
            return Tuples.pair(plan, debugInfo);
        }
    }

    public static String serializeToJSON(Root_meta_pure_executionPlan_ExecutionPlan purePlan, String clientVersion, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        String cl = clientVersion == null ? PureClientVersions.production : clientVersion;
        MutableList<? extends PlanTransformer> handlers = Iterate.selectWith(transformers, PlanTransformer::supports, cl, Lists.mutable.empty());
        Assert.assertTrue(handlers.size() == 1, () -> "Zero or more than one handler (" + handlers.size() + ") was found for protocol " + cl);
        Object transformed = handlers.get(0).transformToVersionedModel(purePlan, cl, extensions, pureModel.getExecutionSupport());
        return serializeToJSON(transformed, pureModel);
    }

    public static SingleExecutionPlan stringToPlan(String plan)
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
        return core_external_format_json_toJSON.Root_meta_json_toJSON_Any_MANY__Integer_$0_1$__Config_1__String_1_(
                Lists.mutable.with(protocolPlan),
                1000L,
                core_external_format_json_toJSON.Root_meta_json_config_Boolean_1__Boolean_1__Boolean_1__Boolean_1__Config_1_(false, false, true, true, pureModel.getExecutionSupport()),
                pureModel.getExecutionSupport()
        );
    }
}
