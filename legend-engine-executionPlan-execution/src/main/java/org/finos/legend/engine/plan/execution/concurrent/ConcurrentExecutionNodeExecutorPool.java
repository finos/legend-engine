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
import io.opentracing.contrib.concurrent.TracedExecutorService;
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
import org.finos.legend.engine.shared.core.url.StreamProvider;
import org.finos.legend.engine.shared.core.url.StreamProviderHolder;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Pool management class. This class should be instantiated only during server spin up to help manage thread pool and guard system against thread explosions.
 */
public final class ConcurrentExecutionNodeExecutorPool implements AutoCloseable
{
    private final int poolSize;
    private final String poolDescription;
    private final ExecutorService executor;
    private final ExecutorService delegatedExecutor;
    private final Semaphore availableThreads;

    public ConcurrentExecutionNodeExecutorPool(int poolSize, String poolDescription)
    {
        this.poolSize = poolSize;
        this.poolDescription = poolDescription;
        this.delegatedExecutor = Executors.newFixedThreadPool(poolSize);
        this.executor = new TracedExecutorService(this.delegatedExecutor, GlobalTracer.get());
        this.availableThreads = new Semaphore(poolSize);
    }

    @Override
    public void close()
    {
        this.executor.shutdown();
    }

    public List<? extends Result> execute(final List<ExecutionNode> nodes, final MutableList<CommonProfile> profiles, final ExecutionState executionState)
    {
        if (!executor.isShutdown() && availableThreads.tryAcquire(nodes.size()))
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
        nodes.forEach(node -> elements.add(CompletableFuture.supplyAsync(() ->
                {
                    try (Scope scope = GlobalTracer.get().buildSpan(String.format("Execution for child - %d", nodes.indexOf(node))).startActive(true))
                    {
                        StreamProviderHolder.streamProviderThreadLocal.set(streamProvider);
                        ExecutionState executionStateForThread = executionState.copy();
                        Result result = node.accept(new ExecutionNodeExecutor(Lists.mutable.withAll(profiles), executionStateForThread));
                        return Tuples.pair(result, executionStateForThread);
                    }
                }, executor
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
                ", poolDescription : " + poolDescription +
                ", delegatedExecutor : " + delegatedExecutor.toString() +
                ", availableThreads : " + availableThreads.toString() +
                "]";
    }
}
