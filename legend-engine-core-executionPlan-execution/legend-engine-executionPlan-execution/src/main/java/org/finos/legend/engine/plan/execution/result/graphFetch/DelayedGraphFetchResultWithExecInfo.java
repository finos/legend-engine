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

package org.finos.legend.engine.plan.execution.result.graphFetch;

import org.finos.legend.engine.plan.execution.concurrent.ParallelGraphFetchExecutionExecutorPool;
import org.finos.legend.engine.plan.execution.graphFetch.StoreGraphFetchExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;

import java.util.List;
import java.util.concurrent.Future;

public class DelayedGraphFetchResultWithExecInfo extends Result
{
    private final Future<DelayedGraphFetchResult> delayedGraphFetchResultFuture;
    private final boolean executedInNewThread;
    private final String threadIdentifierKey;

    public DelayedGraphFetchResultWithExecInfo(Future<DelayedGraphFetchResult> delayedGraphFetchResultFuture, boolean executedInNewThread, String threadIdentifierKey)
    {
        super("success");
        this.threadIdentifierKey = threadIdentifierKey;
        this.delayedGraphFetchResultFuture = delayedGraphFetchResultFuture;
        this.executedInNewThread = executedInNewThread;
    }

    @Override
    public <T> T accept(ResultVisitor<T> resultVisitor)
    {
        throw new UnsupportedOperationException("no visitors");
    }

    public void possiblyReleaseThread(StoreGraphFetchExecutor executor, ParallelGraphFetchExecutionExecutorPool graphFetchExecutionNodeExecutorPool)
    {
        if (executedInNewThread)
        {
            executor.releaseThreads(graphFetchExecutionNodeExecutorPool, threadIdentifierKey, 1);
        }
    }

    public void cancel(StoreGraphFetchExecutor executor, ParallelGraphFetchExecutionExecutorPool graphFetchExecutionNodeExecutorPool)
    {
        if (this.executedInNewThread)
        {
            this.delayedGraphFetchResultFuture.cancel(true);
            executor.releaseThreads(graphFetchExecutionNodeExecutorPool, threadIdentifierKey, 1);
        }
    }

    public List<DelayedGraphFetchResultWithExecInfo> consume(StoreGraphFetchExecutor executor, ParallelGraphFetchExecutionExecutorPool graphFetchExecutionNodeExecutorPool) throws Exception
    {
        DelayedGraphFetchResult result = delayedGraphFetchResultFuture.get();
        possiblyReleaseThread(executor, graphFetchExecutionNodeExecutorPool);
        result.addNodeToParent();
        return result.executeChildren();
    }
}
