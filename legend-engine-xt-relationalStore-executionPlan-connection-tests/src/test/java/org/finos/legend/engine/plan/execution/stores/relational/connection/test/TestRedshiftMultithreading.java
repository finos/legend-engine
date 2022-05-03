package org.finos.legend.engine.plan.execution.stores.relational.connection.test;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TestRedshiftMultithreading
{
    public static final String REDSHIFT_CLUSTER_NAME = "jdbc:redshift://redshift-load-balancer-cb907ffe8879f6c2.elb.us-east-1.amazonaws.com:5439/dev";
    public static final String REDSHIFT_USERNAME = "XXXXXX";
    public static final String REDSHIFT_PASSWORD = "XXXXX";

    @BeforeClass
    public static void setup() throws ClassNotFoundException
    {
        Class.forName("com.amazon.redshift.jdbc.Driver");
    }

    @Ignore
    @Test
    public void testSingleThreadedConnectionUsage() throws Exception
    {
        try(Connection connection = this.buildConnection(REDSHIFT_CLUSTER_NAME, REDSHIFT_USERNAME, REDSHIFT_PASSWORD))
        {
            this.useConnection(connection);
        }
    }

    @Ignore
    @Test
    public void testMultithreadedConnectionUsage() throws Exception
    {
        Connection connection = this.buildConnection(REDSHIFT_CLUSTER_NAME, REDSHIFT_USERNAME, REDSHIFT_PASSWORD);
        try
        {
            ExecutorService executor = Executors.newFixedThreadPool(5);
            MutableList<Future> futures = Lists.mutable.empty();
            for (int i = 0; i < 30; i++)
            {
                Future<?> future = executor.submit(() -> {
                    try {
                        this.useConnection(connection);
                    } catch (Exception e) {
                        System.out.println(e);
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                });
                futures.add(future);
            }

            for (Future future : futures)
            {
                System.out.println(future.get());
            }
            executor.shutdown();
            System.out.println("shutdown");
            executor.awaitTermination(5, TimeUnit.MINUTES);
        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }
    }

    public Connection buildConnection(String dbUrl, String userName, String password) throws Exception
    {
        Properties props = new Properties();
        props.setProperty("user", userName);
        props.setProperty("password", password);
        return DriverManager.getConnection(dbUrl, props);
    }

    public void useConnection(Connection connection) throws Exception
    {
        Statement stmt = connection.createStatement();
        String sql = "select * from pg_namespace;";
        ResultSet resultSet = stmt.executeQuery(sql);
        while (resultSet.next())
        {
            for (int i1 = 1; i1 < resultSet.getMetaData().getColumnCount() + 1; i1++)
            {
                String columnMetadata = resultSet.getMetaData().getColumnLabel(i1) + " = " + resultSet.getObject(i1);
            }
        }
        resultSet.close();
        stmt.close();
    }
}