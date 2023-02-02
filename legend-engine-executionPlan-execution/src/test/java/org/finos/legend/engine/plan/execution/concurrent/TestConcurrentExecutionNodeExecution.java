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

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.junit.Assert;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class TestConcurrentExecutionNodeExecution
{
    public static List<Result> executePlanConcurrently(PlanExecutor.ExecuteArgs executeArgs, int concurrentExecutionNodeExecutorPoolSize, int planExecutionCount, int planExecutionPoolSize, String executorPoolStateAssertMessage)
    {
        PlanExecutor planExecutor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        planExecutor.injectConcurrentExecutionNodeExecutorPoolOfSize(concurrentExecutionNodeExecutorPoolSize);
        ConcurrentExecutionNodeExecutorPool concurrentExecutionNodeExecutorPool = ConcurrentExecutionNodeExecutorPool.getExecutorPool();

        List<Result> results = FastList.newList();
        for (int i = 0; i < planExecutionCount; i += planExecutionPoolSize)
        {
            ExecutorService executorService = Executors.newFixedThreadPool(planExecutionPoolSize);
            List<CompletableFuture<Result>> resultsFuture = FastList.newList();

            IntStream.range(i, Math.min(planExecutionCount, i + planExecutionPoolSize)).forEach(j -> resultsFuture.add(CompletableFuture.supplyAsync(() -> planExecutor.executeWithArgs(executeArgs), executorService)));

            CompletableFuture<Void> allResultsFuture = CompletableFuture.allOf(resultsFuture.toArray(new CompletableFuture[0]));

            allResultsFuture.whenComplete((v, th) ->
            {
                resultsFuture.forEach(result ->
                {
                    try
                    {
                        results.add(result.get(1, TimeUnit.SECONDS));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                });
            }).join();
        }

        Assert.assertTrue(concurrentExecutionNodeExecutorPool.toString().contains(executorPoolStateAssertMessage));
        ConcurrentExecutionNodeExecutorPool.teardownExecutorPool();;

        return results;
    }
}
