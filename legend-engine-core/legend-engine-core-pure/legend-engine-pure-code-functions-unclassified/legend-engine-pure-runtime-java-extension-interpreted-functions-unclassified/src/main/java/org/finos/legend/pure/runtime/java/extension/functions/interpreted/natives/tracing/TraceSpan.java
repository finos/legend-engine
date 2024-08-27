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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.tracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.MapCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class TraceSpan extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;

    private static final ExecutorService traceAsyncExecutor = Executors.newCachedThreadPool(new ThreadFactory()
    {
        private final ThreadGroup group = System.getSecurityManager() == null ? Thread.currentThread().getThreadGroup() : System.getSecurityManager().getThreadGroup();
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r)
        {
            Thread thread = new Thread(this.group, r, "trace-async-executor-thread-" + this.threadNumber.getAndIncrement(), 0);
            if (!thread.isDaemon())
            {
                thread.setDaemon(true);
            }
            if (thread.getPriority() != Thread.NORM_PRIORITY)
            {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            return thread;
        }
    });

    public TraceSpan(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.functionExecution = functionExecution;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params,
                                Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters,
                                Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters,
                                VariableContext variableContext, CoreInstance functionExpressionToUseInStack,
                                Profiler profiler,
                                InstantiationContext instantiationContext,
                                ExecutionSupport executionSupport,
                                Context context,
                                ProcessorSupport processorSupport)
    {
        if (Instance.instanceOf(Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport), M3Paths.Nil, processorSupport))
        {
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "Evaluate can't take an instance of Nil as a function");
        }

        // add check to disable tracing - use isRegistered()
        CoreInstance functionToApplyTo = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        String traceName = ((StringCoreInstance) Instance.getValueForMetaPropertyToManyResolved(params.get(1),
                M3Properties.values,
                processorSupport).getFirst()).getValue();

        if (!GlobalTracer.isRegistered())
        {
            return this.functionExecution.executeLambda(
                    LambdaFunctionCoreInstanceWrapper.toLambdaFunction(functionToApplyTo),
                    Lists.mutable.empty(),
                    resolvedTypeParameters,
                    resolvedMultiplicityParameters,
                    getParentOrEmptyVariableContext(variableContext),
                    functionExpressionToUseInStack,
                    profiler,
                    instantiationContext,
                    executionSupport);
        }

        return executeWithTrace(params,
                resolvedTypeParameters,
                resolvedMultiplicityParameters,
                variableContext,
                functionExpressionToUseInStack,
                profiler,
                instantiationContext,
                executionSupport,
                processorSupport,
                functionToApplyTo,
                traceName);
    }

    private CoreInstance executeWithTrace(ListIterable<? extends CoreInstance> params,
                                          Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters,
                                          Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters,
                                          VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler,
                                          InstantiationContext instantiationContext, ExecutionSupport executionSupport,
                                          ProcessorSupport processorSupport, CoreInstance functionToApplyTo, String traceName)
    {
        Span span = GlobalTracer.get().buildSpan(traceName).start();
        try (Scope scope = GlobalTracer.get().scopeManager().activate(span))
        {
            if (params.size() > 2)
            {
                boolean tagsCritical = true;
                if (params.size() > 3)
                {
                    tagsCritical = ((BooleanCoreInstance) Instance.getValueForMetaPropertyToOneResolved(params.get(3), M3Properties.values, processorSupport)).getValue();
                }

                resolveTagsAndAddToTrace(params, resolvedTypeParameters, resolvedMultiplicityParameters,
                        variableContext, functionExpressionToUseInStack, profiler,
                        instantiationContext, executionSupport, processorSupport,
                        tagsCritical, span);
            }

            return this.functionExecution.executeLambda(
                    LambdaFunctionCoreInstanceWrapper.toLambdaFunction(functionToApplyTo),
                    Lists.mutable.<CoreInstance>empty(),
                    resolvedTypeParameters,
                    resolvedMultiplicityParameters,
                    getParentOrEmptyVariableContext(variableContext),
                    functionExpressionToUseInStack,
                    profiler,
                    instantiationContext,
                    executionSupport);
        }
        finally
        {
            if (span != null)
            {
                span.finish();
            }
        }
    }

    private void resolveTagsAndAddToTrace(ListIterable<? extends CoreInstance> params,
                                          Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters,
                                          Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters,
                                          VariableContext variableContext, CoreInstance functionExpressionToUseInStack,
                                          Profiler profiler, InstantiationContext instantiationContext,
                                          ExecutionSupport executionSupport, ProcessorSupport processorSupport,
                                          boolean tagsCritical, Span span)
    {
        try
        {
            Future<?> future = traceAsyncExecutor.submit(() ->
            {
                try (Scope scope = GlobalTracer.get().scopeManager().activate(span))
                {
                    CoreInstance tagsFunction = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);
                    CoreInstance coreInstance = functionExecution.executeLambda(
                            LambdaFunctionCoreInstanceWrapper.toLambdaFunction(tagsFunction),
                            Lists.mutable.empty(), resolvedTypeParameters,
                            resolvedMultiplicityParameters,
                            getParentOrEmptyVariableContext(variableContext),
                            functionExpressionToUseInStack, profiler, instantiationContext,
                            executionSupport);
                    MutableMap<CoreInstance, CoreInstance> tagsMap = ((MapCoreInstance) Instance.getValueForMetaPropertyToManyResolved(coreInstance, M3Properties.values, processorSupport).getFirst()).getMap();
                    addTags(span, tagsMap);
                }
            });
            future.get(60, TimeUnit.SECONDS);
        }
        catch (TimeoutException e)
        {
            if (span != null)
            {
                span.setTag("Exception", "Timeout received before tags could be resolved");
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch (Exception e)
        {
            if (tagsCritical)
            {
                throw new RuntimeException(e);
            }
            if (span != null)
            {
                span.setTag("Exception", String.format("Unable to resolve tags - [%s]", e.getMessage()));
            }
        }
    }

    private void addTags(Span span, MutableMap<CoreInstance, CoreInstance> tagsMap)
    {
        if (span != null)
        {
            tagsMap.forEachKeyValue((k, v) -> span.setTag(PrimitiveUtilities.getStringValue(k), PrimitiveUtilities.getStringValue(v)));
        }
    }
}
