package org.finos.legend.engine.plan.execution.stores.relational.connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManagerPOJO;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.finos.legend.engine.plan.execution.stores.relational.connection.TestRelationalExecutionStatistics.TEST_EXECUTION_PLAN;
import static org.finos.legend.engine.plan.execution.stores.relational.connection.TestRelationalExecutionStatistics.getPoolName;

public class TestConnectionStateManagerRelationalExecutorFlow extends AlloyTestServer
{
    @Override
    protected void insertTestData(Statement statement) throws SQLException
    {
        statement.execute("Drop table if exists PERSON;");
        statement.execute("Create Table PERSON(fullName VARCHAR(100) NOT NULL,firmName VARCHAR(100) NULL,addressName VARCHAR(100) NULL,birthTime TIMESTAMP NULL, PRIMARY KEY(fullName));");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P1','F1','A1','2020-12-12 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P2','F2','A2','2020-12-13 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P3',null,null,'2020-12-14 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P4',null,'A3','2020-12-15 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P5','F1','A1','2020-12-16 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P10','F1','A1','2020-12-17 20:00:00');");
    }

    @Test
    public void connectionPoolStateIsCorrect()
    {
        SingleExecutionPlan executionPlan = buildPlan(TEST_EXECUTION_PLAN);
        Assert.assertNotNull(executionPlan);

        executePlan(executionPlan);
        ConnectionStateManager connectionStateManager = ConnectionStateManager.getInstance();
        Assert.assertNotNull(connectionStateManager);

    }


    @Test
    @Ignore
    public void canGetConnectionStatisticsWithConcurrency() throws Exception
    {
        int numberOfThreads = 5000;
        AtomicInteger counter = new AtomicInteger(0);
        ThreadFactory threadFactory = (Runnable r) -> new Thread(r, "executer-" + counter.getAndIncrement());

        // start a bunch of threads where each thread acquires a LocalH2 connection
        List<Future<ExecutorState>> futures = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads, threadFactory);
        SingleExecutionPlan singleExecutionPlan = buildPlan(TEST_EXECUTION_PLAN, null);

        for (int i = 0; i < numberOfThreads; i++)
        {
            futures.add(executorService.submit(new TestRelationalExecutor(singleExecutionPlan)));
        }

        List<ExecutorState> workerStates = new ArrayList<>();
        for (Future<ExecutorState> future : futures)
        {
            workerStates.add(future.get());
        }


        executorService.shutdown();
        executorService.awaitTermination(100000, TimeUnit.MINUTES);

        ConnectionStateManager connectionStateManager = ConnectionStateManager.getInstance();
        System.out.println(new ObjectMapper().writeValueAsString(connectionStateManager));
        Assert.assertEquals(numberOfThreads, workerStates.size());

        workerStates.stream().forEach(thread -> Assert.assertTrue(thread.name, thread.ok));

        Optional<ConnectionStateManagerPOJO.ConnectionPool> pool = connectionStateManager.findByPoolName(getPoolName());
        Assert.assertTrue(getPoolName(), pool.isPresent());

        Assert.assertEquals(numberOfThreads, pool.get().statistics.getRequestedConnections());

    }

    private class ExecutorState
    {
        private final String name;
        private final boolean ok;

        public ExecutorState(String name, boolean ok)
        {
            this.name = name;
            this.ok = ok;
        }
    }

    private class TestRelationalExecutor implements Callable<ExecutorState>
    {
        private final SingleExecutionPlan singleExecutionPlan;

        private TestRelationalExecutor(SingleExecutionPlan singleExecutionPlan)
        {
            this.singleExecutionPlan = singleExecutionPlan;
        }

        @Override
        public ExecutorState call() throws Exception
        {
            String result = null;
            try
            {
                result = executePlan(this.singleExecutionPlan, Maps.mutable.empty());
            }
            catch (Exception e)
            {
                LOGGER.error("failed", e);
            }
            return new ExecutorState(Thread.currentThread().getName(), result != null);
        }
    }
}
