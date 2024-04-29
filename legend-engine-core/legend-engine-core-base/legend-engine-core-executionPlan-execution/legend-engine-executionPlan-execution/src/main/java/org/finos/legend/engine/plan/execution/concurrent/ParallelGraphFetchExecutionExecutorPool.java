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

package org.finos.legend.engine.plan.execution.concurrent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.util.GlobalTracer;
import org.finos.legend.engine.plan.execution.graphFetch.ParallelGraphFetchExecutionConfig;
import org.finos.legend.engine.plan.execution.result.graphFetch.DelayedGraphFetchResult;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

@JsonSerialize(using = ParallelGraphFetchExecutionExecutorPoolSerializer.class)
public final class ParallelGraphFetchExecutionExecutorPool implements AutoCloseable
{
    private final String poolDescription;
    private final ParallelGraphFetchExecutionConfig parallelGraphFetchExecutionConfig;

    private final ExecutorService executor;
    private final ExecutorService delegatedExecutor;

    private final Semaphore availableThreads;

    public ParallelGraphFetchExecutionExecutorPool(ParallelGraphFetchExecutionConfig poolConfig, String poolDescription)
    {
        this.poolDescription = poolDescription;
        this.parallelGraphFetchExecutionConfig = poolConfig;

        int poolSize = (int) poolConfig.parallelExecutionPoolSize;
        this.delegatedExecutor = Executors.newFixedThreadPool(poolSize);
        this.executor = new TracedExecutorService(this.delegatedExecutor, GlobalTracer.get());

        this.availableThreads = new Semaphore(poolSize);
    }

    @Override
    public void close() throws Exception
    {
        this.executor.shutdown();
    }

    public Future<DelayedGraphFetchResult> submit(Callable<DelayedGraphFetchResult> task)
    {
        return this.executor.submit(task);
    }

    public void releaseThreads(int threadsToRelease)
    {
        availableThreads.release(threadsToRelease);
    }

    public boolean acquireThreads(int threadsToAcquire)
    {
        return availableThreads.tryAcquire(threadsToAcquire);
    }

    public void serialize(JsonGenerator jsonGenerator) throws IOException
    {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeFieldName("poolDescription");
        jsonGenerator.writeString(this.poolDescription);
        jsonGenerator.writeFieldName("poolConfiguration");
        jsonGenerator.writeObject(this.parallelGraphFetchExecutionConfig);
        jsonGenerator.writeFieldName("executor");
        jsonGenerator.writeString(this.delegatedExecutor.toString());
        jsonGenerator.writeFieldName("availableThreads");
        jsonGenerator.writeNumber(this.availableThreads.availablePermits());
        jsonGenerator.writeEndObject();
    }
}
