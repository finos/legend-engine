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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.CompositeExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Execution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionOption;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionOptionContext;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionOptionContext_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_SingleExecutionParameters;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_MultiExecutionParameters;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ExecutionParameters;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.ExecutionContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ServicePlanGenerator
{
    public static ExecutionPlan generateServiceExecutionPlan(Service service, ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, MutableList<PlanTransformer> transformers)
    {
        return generateServiceExecutionPlan(service, context, pureModel, clientVersion, platform, null, extensions, transformers);
    }

    public static ExecutionPlan generateServiceExecutionPlan(Service service, ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, MutableList<PlanTransformer> transformers)
    {
        return generateExecutionPlan(service.execution, context, pureModel, clientVersion, platform, planId, extensions, transformers);
    }

    public static ExecutionPlan generateExecutionPlan(Execution execution, ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, MutableList<PlanTransformer> transformers)
    {
        if (execution instanceof PureSingleExecution)
        {
            return generateSingleExecutionPlan((PureSingleExecution) execution, context, pureModel, clientVersion, platform, planId, extensions, transformers);
        }
        if (execution instanceof PureMultiExecution)
        {
            return generateCompositeExecutionPlan((PureMultiExecution) execution, context, pureModel, clientVersion, platform, planId, extensions, transformers);
        }
        throw new IllegalArgumentException("Unsupported execution type: " + execution);
    }

    public static SingleExecutionPlan generateSingleExecutionPlan(PureSingleExecution singleExecution, ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, MutableList<PlanTransformer> transformers)
    {
        return generateSingleExecutionPlan(singleExecution, context, pureModel, clientVersion, platform, null, extensions, transformers);
    }

    public static SingleExecutionPlan generateSingleExecutionPlan(PureSingleExecution singleExecution, ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, MutableList<PlanTransformer> transformers)
    {
        Mapping mapping = singleExecution.mapping != null ? pureModel.getMapping(singleExecution.mapping) : null;
        Runtime runtime = singleExecution.runtime != null ? HelperRuntimeBuilder.buildPureRuntime(singleExecution.runtime, pureModel.getContext()) : null;
        LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(singleExecution.func.body, singleExecution.func.parameters, pureModel.getContext());
        return getSingleExecutionPlan(singleExecution.executionOptions, context, pureModel, clientVersion, platform, planId, extensions, transformers, mapping, runtime, lambda);
    }

    private static SingleExecutionPlan getSingleExecutionPlan(List<ExecutionOption> executionOptions, ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, MutableList<PlanTransformer> transformers, Mapping mapping, Runtime runtime, LambdaFunction<?> lambda)
    {
        if (executionOptions != null)
        {
            return PlanGenerator.generateExecutionPlan(lambda, mapping, runtime, getExecutionOptionContext(executionOptions, pureModel), pureModel, clientVersion, platform, planId, extensions, transformers);
        }
        return PlanGenerator.generateExecutionPlan(lambda, mapping, runtime, context, pureModel, clientVersion, platform, planId, extensions, transformers);
    }

    private static Root_meta_pure_executionPlan_ExecutionOptionContext getExecutionOptionContext(List<ExecutionOption> executionOptions, PureModel pureModel)
    {
        return new Root_meta_pure_executionPlan_ExecutionOptionContext_Impl("", null, pureModel.getClass("meta::pure::executionPlan::ExecutionOptionContext"))._executionOptions(ListIterate.collect(executionOptions, option -> processExecutionOption(option, pureModel.getContext())));
    }

    private static Root_meta_pure_executionPlan_ExecutionOption processExecutionOption(ExecutionOption executionOption, CompileContext context)
    {
        return context.getCompilerExtensions().getExtraExecutionOptionProcessors().stream()
                .map(processor -> processor.value(executionOption, context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported execution option type '" + executionOption.getClass() + "'"));
    }

    public static CompositeExecutionPlan generateCompositeExecutionPlan(PureMultiExecution multiExecution, ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, MutableList<PlanTransformer> transformers)
    {
        return generateCompositeExecutionPlan(multiExecution, context, pureModel, clientVersion, platform, null, extensions, transformers);
    }

    public static CompositeExecutionPlan generateCompositeExecutionPlan(PureMultiExecution multiExecution, ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, MutableList<PlanTransformer> transformers)
    {
        LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(multiExecution.func.body, multiExecution.func.parameters, pureModel.getContext());
        if (multiExecution.executionParameters != null && !multiExecution.executionParameters.isEmpty())
        {
            Map<String, SingleExecutionPlan> plans = multiExecution.executionParameters.stream().collect(Collectors.toMap(
                    ep -> ep.key,
                    ep -> getSingleExecutionPlan(ep.executionOptions, context, pureModel, clientVersion, platform, (planId != null ? planId + "_" + multiExecution.executionParameters.indexOf(ep) : null), extensions, transformers, pureModel.getMapping(ep.mapping), HelperRuntimeBuilder.buildPureRuntime(ep.runtime, pureModel.getContext()),lambda)));
            List<String> keys = multiExecution.executionParameters.stream().map(es -> es.key).collect(Collectors.toList());
            return new CompositeExecutionPlan(plans, multiExecution.executionKey, keys);
        }
        // creating plans for execution environment in the execution plan
        else
        {
            Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance pureExecEnv = org.finos.legend.pure.generated.core_legend_service_helperFunctions.Root_meta_legend_service_getExecutionEnvironmentFromFunctionDefinition_FunctionDefinition_1__ExecutionEnvironmentInstance_1_(lambda, pureModel.getExecutionSupport());
            RichIterable<? extends Pair<? extends Root_meta_legend_service_metamodel_ExecutionParameters, Integer>> execEnvWithIndex = pureExecEnv._executionParameters().zipWithIndex();
            if (pureExecEnv._executionParameters().getFirst() instanceof Root_meta_legend_service_metamodel_SingleExecutionParameters)
            {
                Map<String, SingleExecutionPlan> plans = execEnvWithIndex.toMap(
                        ep -> ((Root_meta_legend_service_metamodel_SingleExecutionParameters)ep.getOne())._key(),
                        ep -> getSingleExecutionPlan(null, context, pureModel, clientVersion, platform, (planId != null ? planId + "_" + ep.getTwo().toString() : null), extensions, transformers, null, null,
                                (LambdaFunction<?>) org.finos.legend.pure.generated.core_legend_service_helperFunctions.Root_meta_legend_service_assignValueInFunctionDefinitionForKey_FunctionDefinition_1__String_1__FunctionDefinition_1_(lambda, ((Root_meta_legend_service_metamodel_SingleExecutionParameters)ep.getOne())._key(), pureModel.getExecutionSupport())));
                List<String> keys = execEnvWithIndex.collect(p -> ((Root_meta_legend_service_metamodel_SingleExecutionParameters)p.getOne())._key()).toList();
                String execKey = org.finos.legend.pure.generated.core_legend_service_helperFunctions.Root_meta_legend_service_getKeyFromFunctionDefinition_FunctionDefinition_1__String_1_(lambda, pureModel.getExecutionSupport());
                return new CompositeExecutionPlan(plans, execKey, keys);
            }
            else if (pureExecEnv._executionParameters().getFirst() instanceof Root_meta_legend_service_metamodel_MultiExecutionParameters)
            {
                Map<String, SingleExecutionPlan> plans = execEnvWithIndex.toMap(
                        ep -> ((Root_meta_legend_service_metamodel_MultiExecutionParameters) ep.getOne())._masterKey(),
                        ep -> getSingleExecutionPlan(null, context, pureModel, clientVersion, platform, (planId != null ? planId + "_" + ep.getTwo().toString() : null), extensions, transformers, null, null,
                                        (LambdaFunction<?>) org.finos.legend.pure.generated.core_legend_service_helperFunctions.Root_meta_legend_service_assignValueInFunctionDefinitionForKey_FunctionDefinition_1__String_1__FunctionDefinition_1_(lambda, ((Root_meta_legend_service_metamodel_MultiExecutionParameters)ep.getOne())._masterKey(), pureModel.getExecutionSupport())));
                List<String> keys = execEnvWithIndex.collect(p -> ((Root_meta_legend_service_metamodel_MultiExecutionParameters)p.getOne())._masterKey()).toList();
                String execKey = org.finos.legend.pure.generated.core_legend_service_helperFunctions.Root_meta_legend_service_getKeyFromFunctionDefinition_FunctionDefinition_1__String_1_(lambda, pureModel.getExecutionSupport());
                return new CompositeExecutionPlan(plans, execKey, keys);
            }
            else
            {
                throw new RuntimeException("Cannot build the execution plan");
            }
        }
    }
}
