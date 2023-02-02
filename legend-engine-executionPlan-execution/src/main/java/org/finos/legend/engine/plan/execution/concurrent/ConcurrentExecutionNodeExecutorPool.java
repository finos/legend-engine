//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.concurrent;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.shared.core.operational.opentracing.TracedSupplier;
import org.finos.legend.engine.shared.core.url.StreamProvider;
import org.finos.legend.engine.shared.core.url.StreamProviderHolder;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

public final class ConcurrentExecutionNodeExecutorPool
{
    private static final AtomicReference<ConcurrentExecutionNodeExecutorPool> INSTANCE = new AtomicReference<>();

    private final int poolSize;
    private final ThreadPoolExecutor executor;
    private final Semaphore availableThreads;

    private ConcurrentExecutionNodeExecutorPool(int poolSize)
    {
        this.poolSize = poolSize;
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);
        this.availableThreads = new Semaphore(poolSize);
    }

    public static void initializeExecutorPool(int poolSize)
    {
        boolean success = INSTANCE.compareAndSet(null, new ConcurrentExecutionNodeExecutorPool(poolSize));
        if (!success)
        {
            throw new UnsupportedOperationException("Can't initialize ConcurrentExecutionNodeExecutorPool multiple times");
        }
    }

    public static void teardownExecutorPool()
    {
        ConcurrentExecutionNodeExecutorPool concurrentExecutionNodeExecutorPool = getExecutorPool();

        if (concurrentExecutionNodeExecutorPool == null)
        {
            throw new IllegalStateException("teardown called on un-initialized pool");
        }

        concurrentExecutionNodeExecutorPool.executor.shutdown();

        INSTANCE.set(null);
    }

    public static ConcurrentExecutionNodeExecutorPool getExecutorPool()
    {
        return INSTANCE.get();
    }

    public List<? extends Result> execute(final List<ExecutionNode> nodes, final MutableList<CommonProfile> profiles, final ExecutionState executionState)
    {
        if (INSTANCE.get() != null && availableThreads.tryAcquire(nodes.size()))
        {
            try (Scope scope = GlobalTracer.get().buildSpan("Parallel Execution Triggered").startActive(true))
            {
                return executeConcurrently(nodes, profiles, executionState);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            finally
            {
                availableThreads.release(nodes.size());
            }
        }
        else
        {
            try (Scope scope = GlobalTracer.get().buildSpan("Sequential Execution Triggered").startActive(true))
            {
                return ListIterate.collect(nodes, node -> node.accept(new ExecutionNodeExecutor(profiles, executionState)));
            }
        }
    }

    private List<Result> executeConcurrently(final List<ExecutionNode> nodes, final MutableList<CommonProfile> profiles, final ExecutionState executionState)
    {
        List<CompletableFuture<Pair<Result, ExecutionState>>> elements = FastList.newList();
        StreamProvider streamProvider = StreamProviderHolder.streamProviderThreadLocal.get();
        nodes.forEach(node -> elements.add(CompletableFuture.supplyAsync(
                TracedSupplier.reActivateSpan(() ->
                        {
                            try (Scope scope = GlobalTracer.get().buildSpan(String.format("Execution for child - %d", nodes.indexOf(node))).startActive(true))
                            {
                                StreamProviderHolder.streamProviderThreadLocal.set(streamProvider);
                                ExecutionState executionStateForThread = executionState.copy();
                                Result result = node.accept(new ExecutionNodeExecutor(Lists.mutable.withAll(profiles), executionStateForThread));
                                return Tuples.pair(result, executionStateForThread);
                            }
                        }
                ), executor
        )));

        CompletableFuture<Void> allElements = CompletableFuture.allOf(elements.toArray(new CompletableFuture[0]));

        List<Result> results = FastList.newList();

        allElements.whenComplete((v, th) ->
        {
            elements.forEach(e ->
            {
                Pair<Result, ExecutionState> resultExecutionStatePair = e.getNow(Tuples.pair(new ConstantResult("fail"), executionState));
                Result result = resultExecutionStatePair.getOne();
                ExecutionState state = resultExecutionStatePair.getTwo();

                results.add(result);
                executionState.activities.addAll(state.activities.select(a -> !executionState.activities.contains(a)));
            });
        }).join();

        return results;
    }

    @Override
    public String toString()
    {
        return "[" +
                "poolSize : " + poolSize +
                ", executor : " + executor.toString() +
                ", availableThreads : " + availableThreads.toString() +
                "]";
    }
}
