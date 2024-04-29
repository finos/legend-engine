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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedExecutionParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ExecutionParameters;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_MultiExecutionParameters;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_SingleExecutionParameters;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionOption;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionOptionContext;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionOptionContext_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_ExecutionContext;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.core_service_service_helperFunctions;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;

public class ServicePlanGenerator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicePlanGenerator.class);

    public static ExecutionPlan generateServiceExecutionPlan(Service service, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return generateServiceExecutionPlan(service, context, pureModel, clientVersion, platform, null, extensions, transformers);
    }

    public static ExecutionPlan generateServiceExecutionPlan(Service service, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return generateServiceExecutionPlan(service, context, pureModel, clientVersion, platform, planId, extensions, transformers, null);
    }

    public static ExecutionPlan generateServiceExecutionPlan(Service service, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers, ForkJoinPool pool)
    {
        return generateExecutionPlan(service.getPath(), service.execution, context, pureModel, clientVersion, platform, planId, extensions, transformers, pool);
    }

    public static ExecutionPlan generateExecutionPlan(Execution execution, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return generateExecutionPlan(null, execution, context, pureModel, clientVersion, platform, planId, extensions, transformers, null);
    }

    public static SingleExecutionPlan generateSingleExecutionPlan(PureSingleExecution singleExecution, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return generateSingleExecutionPlan(singleExecution, context, pureModel, clientVersion, platform, null, extensions, transformers);
    }

    public static SingleExecutionPlan generateSingleExecutionPlan(PureSingleExecution singleExecution, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        Mapping mapping = singleExecution.mapping != null ? pureModel.getMapping(singleExecution.mapping) : null;
        Root_meta_core_runtime_Runtime runtime = singleExecution.runtime != null ? HelperRuntimeBuilder.buildPureRuntime(singleExecution.runtime, pureModel.getContext()) : null;
        LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(singleExecution.func.body, singleExecution.func.parameters, pureModel.getContext());
        return getSingleExecutionPlan(singleExecution.executionOptions, context, pureModel, clientVersion, platform, planId, extensions, transformers, mapping, runtime, lambda);
    }

    public static CompositeExecutionPlan generateCompositeExecutionPlan(PureMultiExecution multiExecution, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return generateCompositeExecutionPlan(multiExecution, context, pureModel, clientVersion, platform, null, extensions, transformers);
    }

    public static CompositeExecutionPlan generateCompositeExecutionPlan(PureMultiExecution multiExecution, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        return generateCompositeExecutionPlan(null, multiExecution, context, pureModel, clientVersion, platform, planId, extensions, transformers, null);
    }

    // Helpers

    private static ExecutionPlan generateExecutionPlan(String servicePath, Execution execution, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers, ForkJoinPool pool)
    {
        if (execution instanceof PureSingleExecution)
        {
            return generateSingleExecutionPlan((PureSingleExecution) execution, context, pureModel, clientVersion, platform, planId, extensions, transformers);
        }
        if (execution instanceof PureMultiExecution)
        {
            return generateCompositeExecutionPlan(servicePath, (PureMultiExecution) execution, context, pureModel, clientVersion, platform, planId, extensions, transformers, pool);
        }
        throw new IllegalArgumentException("Unsupported execution type: " + execution);
    }

    private static SingleExecutionPlan getSingleExecutionPlan(List<ExecutionOption> executionOptions, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers, Mapping mapping, Root_meta_core_runtime_Runtime runtime, LambdaFunction<?> lambda)
    {
        return PlanGenerator.generateExecutionPlan(lambda, mapping, runtime, (executionOptions == null) ? context : getExecutionOptionContext(executionOptions, pureModel), pureModel, clientVersion, platform, planId, extensions, transformers);
    }

    private static Root_meta_pure_executionPlan_ExecutionOptionContext getExecutionOptionContext(List<ExecutionOption> executionOptions, PureModel pureModel)
    {
        return new Root_meta_pure_executionPlan_ExecutionOptionContext_Impl("", null, pureModel.getClass("meta::pure::executionPlan::ExecutionOptionContext"))
                ._executionOptions(ListIterate.collect(executionOptions, option -> processExecutionOption(option, pureModel.getContext())));
    }

    private static Root_meta_pure_executionPlan_ExecutionOption processExecutionOption(ExecutionOption executionOption, CompileContext context)
    {
        return context.getCompilerExtensions().getExtraExecutionOptionProcessors().stream()
                .map(processor -> processor.value(executionOption, context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported execution option type '" + executionOption.getClass() + "'"));
    }

    private static CompositeExecutionPlan generateCompositeExecutionPlan(String servicePath, PureMultiExecution multiExecution, Root_meta_pure_runtime_ExecutionContext context, PureModel pureModel, String clientVersion, PlanPlatform platform, String planId, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers, ForkJoinPool pool)
    {
        LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(multiExecution.func.body, multiExecution.func.parameters, pureModel.getContext());
        if (multiExecution.executionParameters != null && !multiExecution.executionParameters.isEmpty())
        {
            return generateCompositeExecutionPlan(
                    multiExecution.executionParameters,
                    ServicePlanGenerator::getExecutionKey,
                    (ep, key, i) -> getSingleExecutionPlan(ep.executionOptions, context, pureModel, clientVersion, platform, (planId != null ? planId + "_" + i : null), extensions, transformers, pureModel.getMapping(ep.mapping), HelperRuntimeBuilder.buildPureRuntime(ep.runtime, pureModel.getContext()), lambda),
                    multiExecution.executionKey,
                    servicePath,
                    pool);
        }
        else
        {
            // creating plans for execution environment in the execution plan
            Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance pureExecEnv = core_service_service_helperFunctions.Root_meta_legend_service_getExecutionEnvironmentFromFunctionDefinition_FunctionDefinition_1__ExecutionEnvironmentInstance_1_(lambda, pureModel.getExecutionSupport());
            MutableList<Root_meta_legend_service_metamodel_ExecutionParameters> execParams = Lists.mutable.withAll(pureExecEnv._executionParameters());
            String execKey = core_service_service_helperFunctions.Root_meta_legend_service_getKeyFromFunctionDefinition_FunctionDefinition_1__String_1_(lambda, pureModel.getExecutionSupport());
            return generateCompositeExecutionPlan(
                    execParams,
                    ServicePlanGenerator::getExecutionKey,
                    (ep, key, i) -> getSingleExecutionPlan(null, context, pureModel, clientVersion, platform, (planId != null ? planId + "_" + i : null), extensions, transformers, null, null,
                            (LambdaFunction<?>) core_service_service_helperFunctions.Root_meta_legend_service_assignValueInFunctionDefinitionForKey_FunctionDefinition_1__String_1__FunctionDefinition_1_(lambda, key, pureModel.getExecutionSupport())),
                    execKey,
                    servicePath,
                    pool);
        }
    }

    private static <P> CompositeExecutionPlan generateCompositeExecutionPlan(List<P> execParams, Function<? super P, ? extends String> keyFn, SingleExecutionPlanGenerator<? super P> planFn, String execKey, String servicePath, ForkJoinPool pool)
    {
        if (pool != null)
        {
            return pool.invoke(new MultiExecutionRecursiveTask<>(execParams, keyFn, planFn, execKey, servicePath));
        }

        long start = System.nanoTime();
        if (servicePath != null)
        {
            LOGGER.debug("Generating {} plans for {}", execParams.size(), servicePath);
        }
        MutableMap<String, SingleExecutionPlan> plans = Maps.mutable.ofInitialCapacity(execParams.size());
        MutableList<String> keys = Lists.mutable.ofInitialCapacity(execParams.size());
        ListIterate.forEachWithIndex(execParams, (ep, i) ->
        {
            long planStart = System.nanoTime();
            if (servicePath != null)
            {
                LOGGER.debug("Generating {} plan {}", servicePath, i);
            }

            String key;
            try
            {
                key = keyFn.apply(ep);
            }
            catch (Throwable t)
            {
                if (servicePath != null)
                {
                    LOGGER.error("Error generating {} plan {} key", servicePath, i, t);
                }
                throw t;
            }
            keys.add(key);

            SingleExecutionPlan plan;
            try
            {
                plan = planFn.generate(ep, key, i);
            }
            catch (Throwable t)
            {
                if (servicePath != null)
                {
                    LOGGER.error("Error generating {} plan {}, key '{}'", servicePath, i, key, t);
                }
                throw t;
            }
            if (plans.put(key, plan) != null)
            {
                throw new IllegalStateException("Conflict for key '" + key + "' for " + servicePath);
            }

            if ((servicePath != null) && LOGGER.isDebugEnabled())
            {
                long planEnd = System.nanoTime();
                LOGGER.debug("Finished generating {} plan {}, key '{}' ({})", servicePath, i, key, formatNanoDurationForLogging(planStart, planEnd));
            }
        });

        if ((servicePath != null) && LOGGER.isDebugEnabled())
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished generating {} plans for {} ({})", execParams.size(), servicePath, formatNanoDurationForLogging(start, end));
        }
        return new CompositeExecutionPlan(plans, execKey, keys);
    }

    private static String getExecutionKey(KeyedExecutionParameter executionParameter)
    {
        return executionParameter.key;
    }

    private static String getExecutionKey(Root_meta_legend_service_metamodel_ExecutionParameters executionParameter)
    {
        if (executionParameter instanceof Root_meta_legend_service_metamodel_SingleExecutionParameters)
        {
            return getExecutionKey((Root_meta_legend_service_metamodel_SingleExecutionParameters) executionParameter);
        }
        if (executionParameter instanceof Root_meta_legend_service_metamodel_MultiExecutionParameters)
        {
            return getExecutionKey((Root_meta_legend_service_metamodel_MultiExecutionParameters) executionParameter);
        }
        throw new UnsupportedOperationException("Cannot handle execution parameter: " + executionParameter);
    }

    private static String getExecutionKey(Root_meta_legend_service_metamodel_SingleExecutionParameters executionParameter)
    {
        return executionParameter._key();
    }

    private static String getExecutionKey(Root_meta_legend_service_metamodel_MultiExecutionParameters executionParameter)
    {
        return executionParameter._masterKey();
    }

    private static class MultiExecutionRecursiveTask<P> extends RecursiveTask<CompositeExecutionPlan>
    {
        private final List<P> executionParameters;
        private final Function<? super P, ? extends String> keyFn;
        private final SingleExecutionPlanGenerator<? super P> planFn;
        private final String execKey;
        private final String servicePath;
        private final AtomicReferenceArray<SingleExecutionPlan> plans;
        private final AtomicReferenceArray<String> keys;
        private volatile boolean terminated;

        private MultiExecutionRecursiveTask(List<P> executionParameters, Function<? super P, ? extends String> keyFn, SingleExecutionPlanGenerator<? super P> planFn, String execKey, String servicePath)
        {
            this.executionParameters = executionParameters;
            this.keyFn = keyFn;
            this.planFn = planFn;
            this.execKey = execKey;
            this.servicePath = servicePath;
            this.plans = new AtomicReferenceArray<>(this.executionParameters.size());
            this.keys = new AtomicReferenceArray<>(this.executionParameters.size());
        }

        @Override
        public void reinitialize()
        {
            this.terminated = false;
            super.reinitialize();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            this.terminated = true;
            return super.cancel(mayInterruptIfRunning);
        }

        @Override
        public void completeExceptionally(Throwable ex)
        {
            this.terminated = true;
            super.completeExceptionally(ex);
        }

        @Override
        protected CompositeExecutionPlan compute()
        {
            long start = System.nanoTime();

            int size = this.executionParameters.size();
            if (shouldLog())
            {
                LOGGER.debug("Generating {} plans for {}", size, this.servicePath);
            }

            // Compute plans
            computeForRange(0, size);
            if (this.terminated)
            {
                if (isCompletedAbnormally())
                {
                    return null;
                }

                // This should never happen: terminated should only be true if there was abnormal completion (exception or cancellation)
                if (shouldLog())
                {
                    LOGGER.warn("Plan generation for {} terminated without abnormal completion", this.servicePath);
                }
                StringBuilder builder = new StringBuilder("Unknown error during multi-execution plan generation");
                if (this.servicePath != null)
                {
                    builder.append(" of ").append(this.servicePath);
                }
                throw new IllegalStateException(builder.toString());
            }

            // Assemble result
            MutableMap<String, SingleExecutionPlan> planMap = Maps.mutable.ofInitialCapacity(size);
            MutableList<String> keyList = Lists.mutable.ofInitialCapacity(size);
            for (int i = 0; i < size; i++)
            {
                String key = this.keys.get(i);
                if (key == null)
                {
                    if (shouldLog())
                    {
                        LOGGER.warn("No key for plan {} of {}", i, this.servicePath);
                    }
                    StringBuilder builder = new StringBuilder("No key generated for plan ").append(i);
                    if (this.servicePath != null)
                    {
                        builder.append(" of ").append(this.servicePath);
                    }
                    throw new IllegalStateException(builder.toString());
                }
                keyList.add(key);

                SingleExecutionPlan plan = this.plans.get(i);
                if (plan == null)
                {
                    if (shouldLog())
                    {
                        LOGGER.warn("No plan generated for key {} ({}) of {}", i, key, this.servicePath);
                    }
                    StringBuilder builder = new StringBuilder("No plan generated for key ").append(i).append(" (").append(key).append(")");
                    if (this.servicePath != null)
                    {
                        builder.append(" of ").append(this.servicePath);
                    }
                    throw new IllegalStateException(builder.toString());
                }
                if (planMap.put(key, plan) != null)
                {
                    StringBuilder builder = new StringBuilder("Conflict for key '").append(key).append("'");
                    if (this.servicePath != null)
                    {
                        builder.append(" of ").append(this.servicePath);
                    }
                    throw new IllegalStateException(builder.toString());
                }
            }
            if (shouldLog() && LOGGER.isDebugEnabled())
            {
                long end = System.nanoTime();
                LOGGER.debug("Finished generating {} plans for {} ({})", size, this.servicePath, formatNanoDurationForLogging(start, end));
            }
            return new CompositeExecutionPlan(planMap, this.execKey, keyList);
        }

        private void computeForRange(int start, int end)
        {
            if (this.terminated)
            {
                return;
            }

            try
            {
                int length = end - start;
                if (length == 1)
                {
                    computeForIndex(start);
                }
                else
                {
                    int midPoint = start + (length / 2);
                    invokeAll(new RangeTask(start, midPoint), new RangeTask(midPoint, end));
                }
            }
            catch (Throwable t)
            {
                this.terminated = true;
                throw t;
            }
        }

        private void computeForIndex(int index)
        {
            long start = System.nanoTime();
            if (shouldLog())
            {
                LOGGER.debug("Generating {} plan {}", this.servicePath, index);
            }
            P executionParameter = this.executionParameters.get(index);

            String key;
            try
            {
                key = this.keyFn.apply(executionParameter);
            }
            catch (Throwable t)
            {
                if (shouldLog())
                {
                    LOGGER.error("Error generating {} plan key {}", this.servicePath, index, t);
                }
                throw t;
            }

            SingleExecutionPlan plan;
            try
            {
                plan = this.planFn.generate(executionParameter, key, index);
            }
            catch (Throwable t)
            {
                if (shouldLog())
                {
                    LOGGER.error("Error generating {} plan {} ({})", this.servicePath, index, key, t);
                }
                throw t;
            }

            this.keys.set(index, key);
            this.plans.set(index, plan);
            if (shouldLog() && LOGGER.isDebugEnabled())
            {
                long end = System.nanoTime();
                LOGGER.debug("Finished generating {} plan {}, key {} ({})", this.servicePath, index, key, formatNanoDurationForLogging(start, end));
            }
        }

        private boolean shouldLog()
        {
            return this.servicePath != null;
        }

        private class RangeTask extends RecursiveAction
        {
            private final int start;
            private final int end;

            private RangeTask(int start, int end)
            {
                this.start = start;
                this.end = end;
            }

            @Override
            protected void compute()
            {
                computeForRange(this.start, this.end);
            }
        }
    }

    private interface SingleExecutionPlanGenerator<P>
    {
        SingleExecutionPlan generate(P executionParameter, String key, int index);
    }

    private static String formatNanoDurationForLogging(long startNanos, long endNanos)
    {
        return formatNanoDurationForLogging(endNanos - startNanos);
    }

    private static String formatNanoDurationForLogging(long durationNanos)
    {
        return (durationNanos == 0) ? "0s" : String.format("%.9fs", durationNanos / 1_000_000_000.0);
    }
}
