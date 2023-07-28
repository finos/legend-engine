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

package org.finos.legend.engine.plan.execution.stores.relational;

import org.finos.legend.engine.plan.execution.concurrent.ParallelGraphFetchExecutionExecutorPool;
import org.finos.legend.engine.plan.execution.graphFetch.StoreGraphFetchExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalGraphFetchExecutionConfig;
import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalGraphFetchParallelExecutionConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

public class RelationalGraphFetchExecutor implements StoreGraphFetchExecutor
{
    private final RelationalGraphFetchExecutionConfig relationalGraphFetchExecutionConfig;
    private final ConcurrentMap<String, Semaphore> openThreadsCountPerThreadConnectionKey = new ConcurrentHashMap<>();

    public RelationalGraphFetchExecutor()
    {
        this.relationalGraphFetchExecutionConfig = new RelationalGraphFetchExecutionConfig();
    }

    public RelationalGraphFetchExecutor(RelationalGraphFetchExecutionConfig relationalGraphFetchExecutionConfig)
    {
        this.relationalGraphFetchExecutionConfig = relationalGraphFetchExecutionConfig;
    }

    public boolean canExecuteInParallel()
    {
        return relationalGraphFetchExecutionConfig.canExecuteInParallel();
    }

    public synchronized boolean acquireThreads(ParallelGraphFetchExecutionExecutorPool graphFetchExecutionNodeExecutorPool, String dbConnectionKeyWithIdentity, int threadsToAcquire, Object... dbType)
    {
        boolean test1 = graphFetchExecutionNodeExecutorPool.acquireThreads(threadsToAcquire);
        if (!test1)
        {
            return false;
        }

        RelationalGraphFetchParallelExecutionConfig relationalGraphFetchParallelExecutionConfig = relationalGraphFetchExecutionConfig.getRelationalGraphFetchParallelExecutionConfig();

        openThreadsCountPerThreadConnectionKey.putIfAbsent(
                dbConnectionKeyWithIdentity, new Semaphore(relationalGraphFetchParallelExecutionConfig.getMaxConnectionsPerDatabaseForDatabase((String) dbType[0])));

        boolean test2 = openThreadsCountPerThreadConnectionKey.get(dbConnectionKeyWithIdentity).tryAcquire(threadsToAcquire);
        if (!test2)
        {
            graphFetchExecutionNodeExecutorPool.releaseThreads(threadsToAcquire);
            return false;
        }
        return true;
    }

    @Override
    public synchronized void releaseThreads(ParallelGraphFetchExecutionExecutorPool graphFetchExecutionNodeExecutorPool, String dbConnectionKeyWithIdentity, int threadsToRelease)
    {
        graphFetchExecutionNodeExecutorPool.releaseThreads(threadsToRelease);
        openThreadsCountPerThreadConnectionKey.get(dbConnectionKeyWithIdentity).release(threadsToRelease);
    }
}
