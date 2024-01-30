// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.postgres;

import io.dropwizard.testing.junit.ResourceTestRule;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.postgres.auth.AnonymousIdentityProvider;
import org.finos.legend.engine.postgres.auth.NoPasswordAuthenticationMethod;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.finos.legend.engine.postgres.handler.legend.LegendExecutionService;
import org.finos.legend.engine.postgres.handler.legend.LegendSessionFactory;
import org.finos.legend.engine.postgres.handler.legend.LegendTdsTestClient;
import org.finos.legend.engine.query.sql.api.execute.SqlExecuteTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;

public class PostgresServerAsyncTest
{

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PostgresServerAsyncTest.class);

    @ClassRule
    public static final ResourceTestRule resources;
    private static TestPostgresServer testPostgresServer;

    private static int numberOfExecutor = 20;
    private static int numberOfConcurrent = 20;

    static
    {
        Pair<PureModel, ResourceTestRule> pureModelResourceTestRulePair = SqlExecuteTest.getPureModelResourceTestRulePair();
        resources = pureModelResourceTestRulePair.getTwo();
    }

    @BeforeClass
    public static void setUp()
    {
        //limit postgres event thread pool to 2 threads
        System.setProperty("io.netty.eventLoopThreads", "2");

        LegendTdsTestClient client = new LegendTdsTestClient(resources)
        {
            @Override
            public InputStream executeQueryApi(String query)
            {
                try
                {
                    //sleep for 6 seconds because default Postgres connection time out is 5 second
                    LOGGER.info("Start sleep");
                    Thread.sleep(6000);
                    LOGGER.info("Stop sleep");
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
                return super.executeQueryApi(query);
            }
        };
        LegendSessionFactory legendSessionFactory = new LegendSessionFactory(new LegendExecutionService(client));

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(0);

        testPostgresServer = new TestPostgresServer(serverConfig, legendSessionFactory, (user, connectionProperties) -> new NoPasswordAuthenticationMethod(new AnonymousIdentityProvider()));
        testPostgresServer.startUp();
    }


    private void executeAsyncTest(String testName, Callable callableSupplier) throws Exception
    {
        AtomicInteger counter = new AtomicInteger(numberOfExecutor);

        List<Callable<Void>> testArrayList = new ArrayList<>();
        for (int i = 0; i < numberOfExecutor; i++)
        {
            String testNameWithCounter = testName + "_" + i;
            testArrayList.add(new Callable<Void>()
            {
                @Override
                public Void call() throws Exception
                {
                    try
                    {
                        LOGGER.info(testNameWithCounter + ": Start");
                        callableSupplier.call();
                        LOGGER.info(testNameWithCounter + ": Complete");
                        counter.decrementAndGet();
                        return (Void) null;
                    }
                    catch (Throwable e)
                    {
                        LOGGER.error(testNameWithCounter + ": Failed", e);
                        throw e;
                    }
                }
            });
        }
        ExecutorService service = Executors.newFixedThreadPool(numberOfConcurrent);
        List<Future<Void>> futureList = service.invokeAll(testArrayList, 100, TimeUnit.SECONDS);

        for (int i = 0; i < futureList.size(); i++)
        {
            Future<Void> voidFuture = futureList.get(i);
            voidFuture.get();
        }

        Assert.assertEquals(0, counter.get());
    }


    @Test
    public void testPreparedStatement() throws Exception
    {
        executeAsyncTest("testPreparedStatement", () ->
        {
            try (
                    Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                            "dummy", "dummy");
                    PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/personService\"");
                    ResultSet resultSet = statement.executeQuery()
            )
            {
                int rows = 0;
                while (resultSet.next())
                {
                    rows++;
                }
                Assert.assertEquals(4, rows);
            }
            return null;
        });
    }

    @Test
    public void testSimpleStatement() throws Exception
    {
        executeAsyncTest("testSimpleStatement", () ->
        {
            try (
                    Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                            "dummy", "dummy");
                    Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT * FROM service.\"/personService\"")
            )
            {
                int rows = 0;
                while (resultSet.next())
                {
                    rows++;
                }
                Assert.assertEquals(4, rows);
            }
            return null;
        });
    }


    @AfterClass
    public static void tearDown()
    {
        testPostgresServer.stopListening();
        testPostgresServer.shutDown();
    }
}