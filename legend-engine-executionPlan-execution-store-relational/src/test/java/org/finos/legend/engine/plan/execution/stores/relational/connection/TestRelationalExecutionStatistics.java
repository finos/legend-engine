package org.finos.legend.engine.plan.execution.stores.relational.connection;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

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

public class TestRelationalExecutionStatistics extends AlloyTestServer
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TestRelationalExecutionStatistics.class);
    private static final String TEST_FUNCTION = "###Pure\n" +
            "function test::fetch(): Any[1]\n" +
            "{\n" +
            "  {names:String[*] | test::Person.all()\n" +
            "                        ->project([x | $x.fullName], ['fullName'])}\n" +
            "}";

    private static final String LOGICAL_MODEL = "###Pure\n" +
            "Class test::Person\n" +
            "{\n" +
            "  fullName: String[1];\n" +
            "  birthTime: DateTime[0..1];\n" +
            "}\n\n\n";

    private static final String STORE_MODEL = "###Relational\n" +
            "Database test::DB\n" +
            "(\n" +
            "  Table PERSON (\n" +
            "    fullName VARCHAR(100) PRIMARY KEY,\n" +
            "    firmName VARCHAR(100),\n" +
            "    addressName VARCHAR(100),\n" +
            "    birthTime TIMESTAMP\n" +
            "  )\n" +
            ")\n\n\n";

    private static final String MAPPING = "###Mapping\n" +
            "Mapping test::Map\n" +
            "(\n" +
            "  test::Person: Relational\n" +
            "  {\n" +
            "    ~primaryKey\n" +
            "    (\n" +
            "      [test::DB]PERSON.fullName\n" +
            "    )\n" +
            "    ~mainTable [test::DB]PERSON\n" +
            "    fullName:  [test::DB]PERSON.fullName,\n" +
            "    birthTime: [test::DB]PERSON.birthTime\n" +
            "  }\n" +
            ")\n\n\n";

    private static final String RUNTIME = "###Runtime\n" +
            "Runtime test::Runtime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    test::Map\n" +
            "  ];\n" +
            "  connections:\n" +
            "  [\n" +
            "    test::DB:\n" +
            "    [\n" +
            "      c1: #{\n" +
            "        RelationalDatabaseConnection\n" +
            "        {\n" +
            "          type: H2;\n" +
            "          specification: LocalH2 {};\n" +
            "          auth: DefaultH2;\n" +
            "        }\n" +
            "      }#\n" +
            "    ]\n" +
            "  ];\n" +
            "}\n";

    private static final String TEST_EXECUTION_PLAN = LOGICAL_MODEL + STORE_MODEL + MAPPING + RUNTIME + TEST_FUNCTION;


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


    public String getPoolName()
    {
        return "DBPool_Static_host:127.0.0.1_port:" + serverPort + "_db:testDB_type:DefaultH2__UNKNOWN_";
    }


    @Test
    public void canGetConnectionStatistics()
    {

        SingleExecutionPlan executionPlan = buildPlan(TEST_EXECUTION_PLAN);
        Assert.assertNotNull(executionPlan);

        Assert.assertFalse(getRelationalExecutorInfo().findByPoolName(getPoolName()).isPresent());
        Assert.assertNotNull(executePlan(executionPlan, Maps.mutable.empty()));

        RelationalExecutorInfo info = getRelationalExecutorInfo();
        Assert.assertNotNull(info);
        Optional<RelationalExecutorInfo.ConnectionPool> pool = info.findByPoolName(getPoolName());
        Assert.assertTrue(pool.isPresent());
        Assert.assertEquals(1, pool.get().dynamic.totalConnections);
        Assert.assertEquals(1, pool.get().dynamic.idleConnections);
        Assert.assertEquals(0, pool.get().dynamic.activeConnections);
        Assert.assertEquals(0, pool.get().dynamic.threadsAwaitingConnection);

        Assert.assertNotNull(executePlan(executionPlan, Maps.mutable.empty()));

        RelationalExecutorInfo infoAfter = getRelationalExecutorInfo();
        Assert.assertNotNull(infoAfter);
        Optional<RelationalExecutorInfo.ConnectionPool> poolAfter = infoAfter.findByPoolName(getPoolName());
        Assert.assertTrue(poolAfter.isPresent());
        Assert.assertEquals(1, poolAfter.get().dynamic.totalConnections);
        Assert.assertEquals(1, poolAfter.get().dynamic.idleConnections);
        Assert.assertEquals(0, poolAfter.get().dynamic.activeConnections);
        Assert.assertEquals(0, poolAfter.get().dynamic.threadsAwaitingConnection);

        List<RelationalExecutorInfo.ConnectionPool> pools = getRelationalExecutorInfo().getPoolInformationByUser("_UNKNOWN_");
        Assert.assertNotNull(pools);
        Assert.assertEquals(1, pools.get(0).dynamic.totalConnections);
        Assert.assertEquals(2, pools.get(0).statistics.getRequestedConnections());
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

        RelationalExecutorInfo relationalExecutorInfo = getRelationalExecutorInfo();
        Assert.assertNotNull(relationalExecutorInfo);
        System.out.println(new ObjectMapper().writeValueAsString(relationalExecutorInfo));
        Assert.assertEquals(numberOfThreads, workerStates.size());

        workerStates.stream().forEach(thread -> Assert.assertTrue(thread.name, thread.ok));

        Optional<RelationalExecutorInfo.ConnectionPool> pool = getRelationalExecutorInfo().findByPoolName(getPoolName());
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
