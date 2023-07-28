package org.finos.legend.engine.plan.execution.stores.relational;

import org.finos.legend.engine.plan.execution.concurrent.ParallelGraphFetchExecutionExecutorPool;
import org.finos.legend.engine.plan.execution.graphFetch.IParallelGraphFetchExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalGraphFetchParallelExecutionConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

public class RelationalGraphFetchExecutor implements IParallelGraphFetchExecutor
{
    private final RelationalGraphFetchParallelExecutionConfig relationalGraphFetchParallelExecutionConfig;
    private final ConcurrentMap<String, Semaphore> openThreadsCountPerThreadConnectionKey = new ConcurrentHashMap<>();

    public RelationalGraphFetchExecutor()
    {
        this.relationalGraphFetchParallelExecutionConfig = new RelationalGraphFetchParallelExecutionConfig();
    }
    public RelationalGraphFetchExecutor(RelationalGraphFetchParallelExecutionConfig relationalGraphFetchParallelExecutionConfig)
    {
        this.relationalGraphFetchParallelExecutionConfig = relationalGraphFetchParallelExecutionConfig;
    }

    public synchronized boolean acquireThreads(ParallelGraphFetchExecutionExecutorPool graphFetchExecutionNodeExecutorPool, String dbConnectionKeyWithIdentity, int threadsToAcquire, Object... dbType)
    {
        boolean test1 = graphFetchExecutionNodeExecutorPool.acquireThreads(threadsToAcquire);
        if (!test1)
        {
            return false;
        }

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
