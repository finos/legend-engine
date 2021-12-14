package org.finos.legend.engine.plan.execution.stores.relational.connection;


import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManagerPOJO;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManagerPOJO.RelationalStoreInfo;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TestRelationalExecutionStatistics extends AlloyTestServer
{

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

    public static final String TEST_EXECUTION_PLAN = LOGICAL_MODEL + STORE_MODEL + MAPPING + RUNTIME + TEST_FUNCTION;


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


    public static String getPoolName()
    {
        return "DBPool_Static_host:127.0.0.1_port:" + serverPort + "_db:testDB_type:DefaultH2__UNKNOWN_";
    }


    @Test
    public void canGetConnectionStatistics()
    {
        ConnectionStateManager connectionStateManager = ConnectionStateManager.getInstance();

        SingleExecutionPlan executionPlan = buildPlan(TEST_EXECUTION_PLAN);
        Assert.assertNotNull(executionPlan);

        Assert.assertFalse(connectionStateManager.findByPoolName(getPoolName()).isPresent());
        Assert.assertNotNull(executePlan(executionPlan, Maps.mutable.empty()));


        Optional<ConnectionStateManagerPOJO.ConnectionPool> pool = connectionStateManager.findByPoolName(getPoolName());
        Assert.assertTrue(pool.isPresent());
        Assert.assertEquals(1, pool.get().dynamic.totalConnections);
        Assert.assertEquals(1, pool.get().dynamic.idleConnections);
        Assert.assertEquals(0, pool.get().dynamic.activeConnections);
        Assert.assertEquals(0, pool.get().dynamic.threadsAwaitingConnection);
        Assert.assertEquals(1, pool.get().statistics.getRequestedConnections());

        Assert.assertNotNull(executePlan(executionPlan, Maps.mutable.empty()));

        Optional<ConnectionStateManagerPOJO.ConnectionPool> poolAfter = connectionStateManager.findByPoolName(getPoolName());
        Assert.assertTrue(poolAfter.isPresent());
        Assert.assertEquals(1, poolAfter.get().dynamic.totalConnections);
        Assert.assertEquals(1, poolAfter.get().dynamic.idleConnections);
        Assert.assertEquals(0, poolAfter.get().dynamic.activeConnections);
        Assert.assertEquals(0, poolAfter.get().dynamic.threadsAwaitingConnection);
        Assert.assertEquals(2, poolAfter.get().statistics.getRequestedConnections());
    }

    @Test
    public void canGetAggregatedStats()
    {
        ConnectionStateManager connectionStateManager = ConnectionStateManager.getInstance();

        SingleExecutionPlan executionPlan = buildPlan(TEST_EXECUTION_PLAN);
        Assert.assertNotNull(executionPlan);
        Assert.assertNotNull(executePlan(executionPlan, Maps.mutable.empty()));
        Assert.assertNotNull(executePlan(executionPlan, Maps.mutable.empty()));


        List<RelationalStoreInfo> stores = new ArrayList<>(connectionStateManager.getConnectionStateManagerPOJO().getStores());
        Assert.assertFalse(stores.isEmpty());
        Assert.assertEquals(1, stores.get(0).aggregatedPoolStats.totalConnections);
        Assert.assertEquals(1, stores.get(0).aggregatedPoolStats.idleConnections);
        Assert.assertEquals(0, stores.get(0).aggregatedPoolStats.activeConnections);
        Assert.assertEquals(0, stores.get(0).aggregatedPoolStats.threadsAwaitingConnection);
    }


}
