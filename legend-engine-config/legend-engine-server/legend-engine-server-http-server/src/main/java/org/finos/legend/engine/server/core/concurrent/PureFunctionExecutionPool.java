//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.server.core.concurrent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.util.GlobalTracer;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@JsonSerialize(using = PureFunctionExecutionPoolSerializer.class)
public class PureFunctionExecutionPool implements AutoCloseable
{
    private final String poolDescription;
    private final PureFunctionExecutionPoolConfiguration poolConfig;
    private final ExecutorService executor;
    private final ThreadPoolExecutor delegatedExecutor;

    public PureFunctionExecutionPool(PureFunctionExecutionPoolConfiguration poolConfig, String poolDescription)
    {
        this.poolDescription = poolDescription;
        this.poolConfig = poolConfig;
        BlockingQueue<Runnable> queue = poolConfig.getQueueSize() != null && poolConfig.getQueueSize() > 0 ? new ArrayBlockingQueue<>(poolConfig.getQueueSize()) : new SynchronousQueue<>();
        this.delegatedExecutor = new ThreadPoolExecutor(poolConfig.getCorePoolSize(), poolConfig.getMaxPoolSize(), poolConfig.getKeepAliveTime(), TimeUnit.SECONDS, queue, new ThreadPoolExecutor.CallerRunsPolicy());
        this.executor = new TracedExecutorService(this.delegatedExecutor, GlobalTracer.get());
    }

    @Override
    public void close()
    {
        this.executor.shutdown();
    }

    public ExecutorService getExecutor()
    {
        return executor;
    }

    public void serialize(JsonGenerator jsonGenerator) throws IOException
    {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeFieldName("poolDescription");
        jsonGenerator.writeString(this.poolDescription);
        jsonGenerator.writeFieldName("poolConfiguration");
        jsonGenerator.writeObject(this.poolConfig);
        jsonGenerator.writeFieldName("executor");
        jsonGenerator.writeString(this.delegatedExecutor.toString());
        jsonGenerator.writeFieldName("activeThreads");
        jsonGenerator.writeNumber(this.delegatedExecutor.getActiveCount());
        jsonGenerator.writeEndObject();
    }
}

