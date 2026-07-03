// Copyright 2025 Goldman Sachs
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
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.finos.legend.engine.postgres.handler.legend.LegendTdsTestClient;
import org.finos.legend.engine.postgres.protocol.sql.SQLManager;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.sql.LegendExecutionService;
import org.finos.legend.engine.postgres.protocol.wire.auth.identity.AnonymousIdentityProvider;
import org.finos.legend.engine.postgres.protocol.wire.auth.method.NoPasswordAuthenticationMethod;
import org.finos.legend.engine.postgres.protocol.wire.serialization.Messages;
import org.finos.legend.engine.query.sql.api.execute.SqlExecuteTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.postgresql.PGProperty;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests that reproduce JDBC connection hanging / timeout issues and verify
 * their fixes to prevent regressions.
 *
 * <h3>Original bugs (section 1.x):</h3>
 * <ul>
 *   <li>1.1 ? Slow query blocking new connections via Netty I/O thread starvation</li>
 *   <li>1.2 ? Non-thread-safe {@code liveConnections} set causing corruption under concurrency</li>
 *   <li>1.3 ? Per-session {@code CachedThreadPool} never shut down, leaking threads</li>
 *   <li>1.4 ? {@code exceptionCaught} not closing the channel, leaving connections in broken state</li>
 *   <li>1.5 ? {@code ForkJoinPool.commonPool()} saturation blocking task chain execution</li>
 * </ul>
 *
 * <h3>Fix verification (section 2.x):</h3>
 * <ul>
 *   <li>2.1 ? Shared executor: all sessions reuse a single thread pool, no per-session leak</li>
 *   <li>2.2 ? Thread-safe liveConnections: concurrent open/close/iterate does not corrupt</li>
 *   <li>2.3 ? Bounded connectionsHistory: capped at MAX_HISTORY, old entries evicted</li>
 *   <li>2.4 ? Rapid connection churn: sustained open/close cycles don't exhaust resources</li>
 * </ul>
 */
public class TestConnectionHanging
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TestConnectionHanging.class);

    @ClassRule
    public static final ResourceTestRule resources = SqlExecuteTest.getResourceTestRule();

    /**
     * A {@link LegendTdsTestClient} subclass whose behaviour can be changed at
     * runtime ? delay, exceptions, etc. ? so that a single shared server can
     * exercise all test scenarios without restart.
     */
    private static class ConfigurableLegendClient extends LegendTdsTestClient
    {
        volatile int queryDelayMs = 0;
        volatile int schemaDelayMs = 0;
        volatile boolean throwOnQuery = false;
        volatile boolean throwOnSchema = false;

        ConfigurableLegendClient(ResourceTestRule resources)
        {
            super(resources);
        }

        void reset()
        {
            queryDelayMs = 0;
            schemaDelayMs = 0;
            throwOnQuery = false;
            throwOnSchema = false;
        }

        @Override
        public InputStream executeQueryApi(String query)
        {
            if (throwOnQuery)
            {
                throw new RuntimeException("Injected query failure for testing");
            }
            if (queryDelayMs > 0)
            {
                sleep(queryDelayMs);
            }
            return super.executeQueryApi(query);
        }

        @Override
        public InputStream executeSchemaApi(String query)
        {
            if (throwOnSchema)
            {
                throw new RuntimeException("Injected schema failure for testing");
            }
            if (schemaDelayMs > 0)
            {
                sleep(schemaDelayMs);
            }
            return super.executeSchemaApi(query);
        }

        private void sleep(int ms)
        {
            try
            {
                LOGGER.info("ConfigurableLegendClient sleeping {}ms", ms);
                Thread.sleep(ms);
                LOGGER.info("ConfigurableLegendClient woke up");
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    private static ConfigurableLegendClient client;
    private static TestPostgresServer testPostgresServer;

    @BeforeClass
    public static void setUp()
    {
        // Use only 1 Netty worker thread to amplify I/O-thread-blocking bugs
        System.setProperty("io.netty.eventLoopThreads", "1");

        client = new ConfigurableLegendClient(resources);

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(0);
        serverConfig.setHttpPort(0);

        testPostgresServer = new TestPostgresServer(
                serverConfig,
                new SQLManager(Lists.mutable.with(new LegendExecutionService(client))),
                (user, connectionProperties) ->
                        new NoPasswordAuthenticationMethod(new AnonymousIdentityProvider()),
                new Messages(Throwable::getMessage));
        testPostgresServer.startUp();
    }

    @Before
    public void resetClient()
    {
        client.reset();
    }

    @AfterClass
    public static void tearDown()
    {
        System.clearProperty("io.netty.eventLoopThreads");
        if (testPostgresServer != null)
        {
            testPostgresServer.stopListening();
            testPostgresServer.shutDown();
        }
    }

    private String jdbcUrl()
    {
        return "jdbc:postgresql://127.0.0.1:"
                + testPostgresServer.getLocalAddress().getPort()
                + "/postgres";
    }

    // -----------------------------------------------------------------------
    // Test 1.1 ? Slow query should NOT block a new connection from connecting
    // -----------------------------------------------------------------------

    /**
     * Reproduces: a slow Legend query occupies the session executor, but the
     * Netty I/O thread should remain free so that a second JDBC connection can
     * complete its handshake.
     *
     * <p>With only 1 Netty worker thread, if the I/O thread were blocked
     * (e.g. by {@code activeExecution.join()}), connection B would time out.
     */
    @Test(timeout = 60_000)
    public void testSlowQueryDoesNotBlockNewConnection() throws Exception
    {
        // Connection A's query will take 8 seconds
        client.queryDelayMs = 8_000;

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try
        {
            // Start slow query on connection A
            Future<Integer> slowQuery = executor.submit(() ->
            {
                Properties info = new Properties();
                PGProperty.USER.set(info, "dummy");
                PGProperty.PASSWORD.set(info, "dummy");
                PGProperty.PREFER_QUERY_MODE.set(info, "simple");

                try (Connection conn = DriverManager.getConnection(jdbcUrl(), info);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                             "SELECT * FROM service.\"/personService\""))
                {
                    int rows = 0;
                    while (rs.next())
                    {
                        rows++;
                    }
                    return rows;
                }
            });

            // Give connection A time to start its slow query
            Thread.sleep(1_000);

            // Connection B should connect and run a fast query while A is slow
            // Use a short login timeout so the test fails fast if B hangs
            Future<Integer> fastConnection = executor.submit(() ->
            {
                Properties info = new Properties();
                PGProperty.USER.set(info, "dummy");
                PGProperty.PASSWORD.set(info, "dummy");
                PGProperty.LOGIN_TIMEOUT.set(info, 5);
                PGProperty.PREFER_QUERY_MODE.set(info, "simple");

                try (Connection conn = DriverManager.getConnection(jdbcUrl(), info);
                     Statement stmt = conn.createStatement())
                {
                    // Issue a trivial query that does not go through Legend
                    try (ResultSet rs = stmt.executeQuery("SELECT 1"))
                    {
                        rs.next();
                        return rs.getInt(1);
                    }
                }
            });

            // Connection B must complete within the timeout ? if it doesn't,
            // the I/O thread is blocked by the slow query on A
            int fastResult = fastConnection.get(10, TimeUnit.SECONDS);
            Assert.assertEquals(1, fastResult);

            // Connection A should eventually complete too
            int slowResult = slowQuery.get(30, TimeUnit.SECONDS);
            Assert.assertEquals(4, slowResult);
        }
        finally
        {
            client.reset();
            executor.shutdownNow();
        }
    }

    // -----------------------------------------------------------------------
    // Test 1.2 ? Thread-safe liveConnections under concurrent open/close
    // -----------------------------------------------------------------------

    /**
     * Reproduces: {@code PostgresServer.liveConnections} uses a non-thread-safe
     * {@code Sets.mutable.empty()} (Eclipse Collections {@code UnifiedSet}).
     * Concurrent {@code open()} / {@code close()} from multiple Netty I/O
     * threads can corrupt the set's internal table, causing infinite loops
     * or {@code ConcurrentModificationException}.
     *
     * <p>This test opens and closes 50 connections concurrently. With a
     * corrupted set, the test either hangs (infinite loop) or throws.
     */
    @Test(timeout = 60_000)
    public void testConcurrentConnectionsThreadSafety() throws Exception
    {
        int numConnections = 50;
        int numConcurrent = 20;
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < numConnections; i++)
        {
            int idx = i;
            tasks.add(() ->
            {
                try
                {
                    LOGGER.info("ThreadSafety task {} starting", idx);
                    try (Connection conn = DriverManager.getConnection(
                            jdbcUrl(), "dummy", "dummy");
                         Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT 1"))
                    {
                        while (rs.next())
                        {
                            // consume
                        }
                    }
                    successes.incrementAndGet();
                    LOGGER.info("ThreadSafety task {} done", idx);
                }
                catch (Exception e)
                {
                    failures.incrementAndGet();
                    LOGGER.error("ThreadSafety task {} failed", idx, e);
                    throw e;
                }
                return null;
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(numConcurrent);
        try
        {
            List<Future<Void>> futures =
                    executor.invokeAll(tasks, 50, TimeUnit.SECONDS);
            for (Future<Void> f : futures)
            {
                f.get(); // re-throw any exception
            }
        }
        finally
        {
            executor.shutdownNow();
        }

        Assert.assertEquals("Some connections failed", 0, failures.get());
        Assert.assertEquals("Not all connections succeeded",
                numConnections, successes.get());
    }

    // -----------------------------------------------------------------------
    // Test 1.3 ? Shared executor: thread count stays bounded across sessions
    // -----------------------------------------------------------------------

    /**
     * Verifies that the server uses a shared executor pool rather than creating
     * a per-session {@code CachedThreadPool}. After opening and closing many
     * connections, the JVM thread count should remain close to baseline.
     *
     * <p>Pre-fix behaviour: each session created its own
     * {@code Executors.newCachedThreadPool()} that was never shut down. After
     * 100 connections, 100+ leaked threads were visible (keepAlive 60 s).
     *
     * <p>Post-fix: all sessions share a single {@code CachedThreadPool} owned
     * by {@code PostgresServer}. Thread count stays near baseline because
     * threads are reused, not created per session.
     */
    @Test(timeout = 60_000)
    public void testSessionThreadPoolCleanup() throws Exception
    {
        // Warm up ? ensure class loading and internal pools are initialized
        try (Connection conn = DriverManager.getConnection(
                jdbcUrl(), "dummy", "dummy");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1"))
        {
            while (rs.next())
            {
                // consume
            }
        }

        Thread.sleep(500);

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        int baselineThreads = threadMXBean.getThreadCount();
        LOGGER.info("Baseline thread count: {}", baselineThreads);

        int numConnections = 100;
        for (int i = 0; i < numConnections; i++)
        {
            try (Connection conn = DriverManager.getConnection(
                    jdbcUrl(), "dummy", "dummy");
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1"))
            {
                while (rs.next())
                {
                    // consume
                }
            }
        }

        // Measure immediately ? leaked CachedThreadPool threads would still be
        // alive (60 s keepAlive). With a shared pool, no new threads are created.
        int afterThreads = threadMXBean.getThreadCount();
        int threadDelta = afterThreads - baselineThreads;
        LOGGER.info("After {} connections: thread count {} (baseline {}, delta {})",
                numConnections, afterThreads, baselineThreads, threadDelta);

        // With a shared executor, the delta should be very small (< 15).
        // With per-session pools, delta ? numConnections (one leaked thread each).
        Assert.assertTrue(
                "Thread count grew by " + threadDelta + " after " + numConnections
                        + " connections ? per-session executor pools are likely leaking."
                        + " Expected delta < 15 (shared pool), got " + threadDelta + "."
                        + " Baseline=" + baselineThreads + " Final=" + afterThreads,
                threadDelta < 15);
    }

    // -----------------------------------------------------------------------
    // Test 1.4 ? exceptionCaught should close the channel cleanly
    // -----------------------------------------------------------------------

    /**
     * Reproduces: {@code PostgresWireProtocol.exceptionCaught()} logs but does
     * NOT close the channel for exceptions other than "Connection reset". This
     * leaves the connection in an undefined state where subsequent queries hang.
     *
     * <p>This test verifies that after a query triggers an exception, the
     * connection either: (a) allows a subsequent query to succeed (error was
     * reported via the protocol), or (b) is cleanly closed so the client fails
     * fast rather than hanging.
     */
    @Test(timeout = 30_000)
    public void testConnectionRecoveryAfterError() throws Exception
    {
        Properties info = new Properties();
        PGProperty.USER.set(info, "dummy");
        PGProperty.PASSWORD.set(info, "dummy");
        PGProperty.LOG_SERVER_ERROR_DETAIL.set(info, "false");

        try (Connection conn = DriverManager.getConnection(jdbcUrl(), info))
        {
            // First query ? triggers an error (unknown service)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM service.\"/nonExistentService\""))
            {
                Assert.assertThrows(PSQLException.class, stmt::executeQuery);
            }

            // Second query ? should either succeed or fail fast (not hang)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM service.\"/personService\"");
                 ResultSet rs = stmt.executeQuery())
            {
                int rows = 0;
                while (rs.next())
                {
                    rows++;
                }
                Assert.assertEquals(4, rows);
            }
        }
    }

    /**
     * A more aggressive variant: the Legend client itself throws an
     * unchecked exception during execution. Verify the connection does not
     * hang on a subsequent attempt.
     */
    @Test(timeout = 30_000)
    public void testConnectionRecoveryAfterLegendClientException() throws Exception
    {
        Properties info = new Properties();
        PGProperty.USER.set(info, "dummy");
        PGProperty.PASSWORD.set(info, "dummy");
        PGProperty.PREFER_QUERY_MODE.set(info, "simple");
        PGProperty.LOG_SERVER_ERROR_DETAIL.set(info, "false");

        // Make the client throw on query execution
        client.throwOnQuery = true;

        try (Connection conn = DriverManager.getConnection(jdbcUrl(), info);
             Statement stmt = conn.createStatement())
        {
            // This query should fail (client throws)
            try
            {
                stmt.executeQuery("SELECT * FROM service.\"/personService\"");
                Assert.fail("Expected exception");
            }
            catch (PSQLException e)
            {
                LOGGER.info("Got expected error: {}", e.getMessage());
            }

            // Stop throwing, issue a new query on the same connection
            client.throwOnQuery = false;

            // The connection should either work or be cleanly closed ? not hang
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT * FROM service.\"/personService\""))
            {
                int rows = 0;
                while (rs.next())
                {
                    rows++;
                }
                Assert.assertEquals(4, rows);
            }
        }
        catch (PSQLException e)
        {
            // Acceptable: connection was closed by the server after the error.
            // The important thing is it didn't hang.
            LOGGER.info("Connection was closed after error (acceptable): {}", e.getMessage());
        }
        finally
        {
            client.reset();
        }
    }

    // -----------------------------------------------------------------------
    // Test 1.5 ? ForkJoinPool.commonPool() saturation stalls task chains
    // -----------------------------------------------------------------------

    /**
     * Reproduces: {@code PostgresWireProtocol.addTaskToQueue()} and
     * {@code composeTaskInQueue()} use {@code thenRunAsync} /
     * {@code thenComposeAsync} without an explicit executor, defaulting to
     * {@code ForkJoinPool.commonPool()}. When the common pool is saturated
     * by other work, the task chain stalls and the client hangs.
     *
     * <p>This test saturates the common pool, then issues a query and verifies
     * it completes within a reasonable timeout.
     */
    @Test(timeout = 30_000)
    public void testQueryCompletesWhenCommonPoolIsSaturated() throws Exception
    {
        int parallelism = ForkJoinPool.commonPool().getParallelism();
        LOGGER.info("ForkJoinPool.commonPool parallelism: {}", parallelism);

        // Saturate the common pool with blocking tasks
        CountDownLatch blockingLatch = new CountDownLatch(1);
        List<CompletableFuture<Void>> blockers = new ArrayList<>();
        for (int i = 0; i < parallelism + 2; i++)
        {
            blockers.add(CompletableFuture.runAsync(() ->
            {
                try
                {
                    LOGGER.info("Blocker task running on {}", Thread.currentThread().getName());
                    blockingLatch.await();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }, ForkJoinPool.commonPool()));
        }

        // Give blockers time to start
        Thread.sleep(500);

        try
        {
            Properties info = new Properties();
            PGProperty.USER.set(info, "dummy");
            PGProperty.PASSWORD.set(info, "dummy");
            PGProperty.PREFER_QUERY_MODE.set(info, "simple");

            ExecutorService testExecutor = Executors.newSingleThreadExecutor();
            try
            {
                Future<Integer> queryResult = testExecutor.submit(() ->
                {
                    try (Connection conn = DriverManager.getConnection(jdbcUrl(), info);
                         Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT 1"))
                    {
                        rs.next();
                        return rs.getInt(1);
                    }
                });

                // The query should complete within 15 seconds even if the common
                // pool is saturated ? IF the server uses a dedicated executor.
                // If it relies on ForkJoinPool.commonPool(), this will time out.
                try
                {
                    int result = queryResult.get(15, TimeUnit.SECONDS);
                    Assert.assertEquals(1, result);
                    LOGGER.info("Query completed despite saturated ForkJoinPool ? "
                            + "server uses a dedicated executor (good)");
                }
                catch (java.util.concurrent.TimeoutException e)
                {
                    // This is the bug: the query hung because ForkJoinPool.commonPool()
                    // is saturated and the task chain couldn't make progress.
                    Assert.fail("Query timed out ? ForkJoinPool.commonPool() saturation "
                            + "blocked the wire-protocol task chain. The server should "
                            + "use a dedicated executor for thenRunAsync/thenComposeAsync.");
                }
            }
            finally
            {
                testExecutor.shutdownNow();
            }
        }
        finally
        {
            // Release blockers
            blockingLatch.countDown();
            for (CompletableFuture<Void> blocker : blockers)
            {
                try
                {
                    blocker.get(5, TimeUnit.SECONDS);
                }
                catch (Exception ignored)
                {
                    // expected
                }
            }
        }
    }

    // ===================================================================
    // Section 2 ? Fix verification tests (regression guards)
    // ===================================================================

    // -----------------------------------------------------------------------
    // Test 2.1 ? Shared executor: all sessions use the same thread pool
    // -----------------------------------------------------------------------

    /**
     * Verifies the shared executor by counting threads named {@code "pool-"}
     * (the default CachedThreadPool naming). With a shared pool, only one
     * set of pool-N threads should exist regardless of how many sessions
     * have been opened. With per-session pools, each session creates its own
     * pool-N group.
     */
    @Test(timeout = 60_000)
    public void testSharedExecutorAcrossSessions() throws Exception
    {
        // Warm up
        try (Connection conn = DriverManager.getConnection(jdbcUrl(), "dummy", "dummy");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1"))
        {
            while (rs.next())
            {
                // consume
            }
        }

        Thread.sleep(500);

        // Count the distinct pool-N prefixes in thread names before
        long baselinePoolIds = countDistinctPoolIds();
        LOGGER.info("Baseline distinct pool IDs: {}", baselinePoolIds);

        // Open and close 50 more connections
        for (int i = 0; i < 50; i++)
        {
            try (Connection conn = DriverManager.getConnection(jdbcUrl(), "dummy", "dummy");
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1"))
            {
                while (rs.next())
                {
                    // consume
                }
            }
        }

        long afterPoolIds = countDistinctPoolIds();
        long newPoolIds = afterPoolIds - baselinePoolIds;
        LOGGER.info("After 50 connections: distinct pool IDs = {} (new = {})",
                afterPoolIds, newPoolIds);

        // With a shared executor, 0 new pool IDs should be created (all reuse
        // the same pool). With per-session pools, 50 new pool-N groups appear.
        Assert.assertTrue(
                "Created " + newPoolIds + " new thread pool(s) for 50 sessions."
                        + " Expected 0 (shared executor). This indicates per-session"
                        + " CachedThreadPool creation is still happening.",
                newPoolIds <= 1);
    }

    /**
     * Counts distinct pool-N thread name prefixes (e.g. "pool-1", "pool-2").
     * Each CachedThreadPool gets its own incrementing pool ID.
     */
    private long countDistinctPoolIds()
    {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] threadIds = bean.getAllThreadIds();
        ThreadInfo[] infos = bean.getThreadInfo(threadIds);
        return java.util.Arrays.stream(infos)
                .filter(java.util.Objects::nonNull)
                .map(ThreadInfo::getThreadName)
                .filter(name -> name.startsWith("pool-"))
                .map(name ->
                {
                    // Extract "pool-N" prefix from "pool-N-thread-M"
                    int secondDash = name.indexOf('-', 5);
                    return secondDash > 0 ? name.substring(0, secondDash) : name;
                })
                .distinct()
                .count();
    }

    // -----------------------------------------------------------------------
    // Test 2.2 ? Thread-safe liveConnections under concurrent open/close/iterate
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code liveConnections} (now {@code ConcurrentHashMap.newKeySet()})
     * handles concurrent add / remove / iteration without corruption.
     *
     * <p>Opens 200 connections across 20 threads, while a background thread
     * continuously hits the HTTP {@code /server/info} endpoint (which iterates
     * the set). This creates three-way contention:
     * <ul>
     *   <li>Netty thread A: {@code liveConnections.add()} on connect</li>
     *   <li>Netty thread B: {@code liveConnections.remove()} on close</li>
     *   <li>Jetty thread: {@code liveConnections.collect()} in /server/info</li>
     * </ul>
     *
     * <p>Pre-fix (UnifiedSet): could hang in an infinite loop, throw
     * ConcurrentModificationException, or lose entries silently.
     */
    @Test(timeout = 90_000)
    public void testLiveConnectionsThreadSafetyWithIteration() throws Exception
    {
        int httpPort = testPostgresServer.getHttpPort();
        // If httpPort is 0 or unavailable, skip the HTTP polling part
        boolean canPollHttp = httpPort > 0;

        int numConnections = 200;
        int numConcurrent = 20;
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);
        AtomicInteger httpErrors = new AtomicInteger(0);
        AtomicInteger httpPolls = new AtomicInteger(0);
        AtomicBoolean stopPolling = new AtomicBoolean(false);

        // Background thread: poll /server/info to force iteration of liveConnections
        Thread poller = null;
        if (canPollHttp)
        {
            int port = httpPort;
            poller = new Thread(() ->
            {
                while (!stopPolling.get())
                {
                    try
                    {
                        URL url = new URL("http://127.0.0.1:" + port + "/server/info/");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(3_000);
                        conn.setReadTimeout(3_000);
                        if (conn.getResponseCode() == 200)
                        {
                            // Drain the body to force full iteration of the set
                            try (InputStream is = conn.getInputStream())
                            {
                                byte[] buf = new byte[4096];
                                while (is.read(buf) != -1)
                                {
                                    // drain
                                }
                            }
                            httpPolls.incrementAndGet();
                        }
                        else
                        {
                            httpErrors.incrementAndGet();
                        }
                    }
                    catch (Exception e)
                    {
                        httpErrors.incrementAndGet();
                    }
                    try
                    {
                        Thread.sleep(10);
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "server-info-poller");
            poller.setDaemon(true);
            poller.start();
        }

        // Concurrent connections ? open, query, close
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < numConnections; i++)
        {
            tasks.add(() ->
            {
                try
                {
                    try (Connection conn = DriverManager.getConnection(
                            jdbcUrl(), "dummy", "dummy");
                         Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT 1"))
                    {
                        while (rs.next())
                        {
                            // consume
                        }
                    }
                    successes.incrementAndGet();
                }
                catch (Exception e)
                {
                    failures.incrementAndGet();
                    LOGGER.error("Connection task failed", e);
                }
                return null;
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(numConcurrent);
        try
        {
            List<Future<Void>> futures =
                    executor.invokeAll(tasks, 60, TimeUnit.SECONDS);
            for (Future<Void> f : futures)
            {
                f.get();
            }
        }
        finally
        {
            executor.shutdownNow();
        }

        // Stop the poller and let things settle
        stopPolling.set(true);
        if (poller != null)
        {
            poller.join(5_000);
        }

        LOGGER.info("liveConnections thread-safety: {} ok, {} fail, {} httpPolls, {} httpErrors",
                successes.get(), failures.get(), httpPolls.get(), httpErrors.get());

        Assert.assertEquals("Some connections failed under concurrent load",
                0, failures.get());
        Assert.assertEquals("Not all connections succeeded",
                numConnections, successes.get());
        if (canPollHttp)
        {
            Assert.assertTrue("/server/info was never polled during test",
                    httpPolls.get() > 0);
            Assert.assertEquals("/server/info returned errors (possible set corruption)",
                    0, httpErrors.get());
        }
    }

    // -----------------------------------------------------------------------
    // Test 2.3 ? connectionsHistory is bounded (MAX_HISTORY = 1000)
    // -----------------------------------------------------------------------

    /**
     * Verifies that {@code connectionsHistory} does not grow without bound.
     * After opening more than MAX_HISTORY (1000) connections, the history list should
     * be capped at MAX_HISTORY entries with old entries evicted.
     *
     * <p>Pre-fix: {@code Lists.mutable.empty()} with no cap ? grew linearly
     * with total connections ever opened, consuming unbounded memory and
     * making {@code /server/info} responses increasingly large.
     *
     * <p>Post-fix: {@code Collections.synchronizedList} capped at 1000 entries
     * with FIFO eviction in {@code PostgresServer.close()}.
     */
    @Test(timeout = 120_000)
    public void testConnectionsHistoryBounded() throws Exception
    {
        int httpPort = testPostgresServer.getHttpPort();
        Assert.assertTrue("HTTP port not available for /server/info test",
                httpPort > 0);

        // Open more than MAX_HISTORY (1000) connections.
        // Use a smaller number (1100) to keep the test fast but exceed the cap.
        int numConnections = 1100;
        LOGGER.info("Opening {} connections to exceed MAX_HISTORY...", numConnections);

        Properties info = new Properties();
        PGProperty.USER.set(info, "dummy");
        PGProperty.PASSWORD.set(info, "dummy");
        PGProperty.PREFER_QUERY_MODE.set(info, "simple");

        for (int i = 0; i < numConnections; i++)
        {
            try (Connection conn = DriverManager.getConnection(jdbcUrl(), info);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1"))
            {
                while (rs.next())
                {
                    // consume
                }
            }
        }

        // Give the server a moment to process all close callbacks
        Thread.sleep(1_000);

        // Fetch /server/info and check history size
        URL url = new URL("http://127.0.0.1:" + httpPort + "/server/info/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(10_000);
        Assert.assertEquals("GET /server/info/ should return 200",
                200, conn.getResponseCode());

        String body;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)))
        {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
            }
            body = sb.toString();
        }

        // Count history entries (count occurrences of "name" inside the history array)
        int historySize = countJsonArrayEntries(body, "history");
        LOGGER.info("connectionsHistory size: {} (opened {} connections, MAX_HISTORY=1000)",
                historySize, numConnections);

        // History should be capped at 1000 (MAX_HISTORY), NOT equal to numConnections
        Assert.assertTrue(
                "connectionsHistory has " + historySize + " entries after "
                        + numConnections + " connections. Expected <= 1000 (MAX_HISTORY)."
                        + " History is not being capped.",
                historySize <= 1000);

        // It should also not be trivially empty (connections were tracked)
        Assert.assertTrue(
                "connectionsHistory is empty ? connections are not being tracked at all",
                historySize > 0);
    }

    // -----------------------------------------------------------------------
    // Test 2.4 ? Rapid connection churn doesn't exhaust resources
    // -----------------------------------------------------------------------

    /**
     * Verifies that the server can handle sustained rapid connection churn
     * without degradation. Opens and closes 500 connections across 10 threads,
     * then verifies thread count hasn't exploded and new connections still work.
     *
     * <p>This is the integration-level regression test: it combines the shared
     * executor, thread-safe collections, and bounded history into a single
     * realistic scenario.
     */
    @Test(timeout = 120_000)
    public void testRapidConnectionChurn() throws Exception
    {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        // Warm up
        try (Connection conn = DriverManager.getConnection(jdbcUrl(), "dummy", "dummy");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1"))
        {
            while (rs.next())
            {
                // consume
            }
        }
        Thread.sleep(500);

        int baselineThreads = threadMXBean.getThreadCount();
        LOGGER.info("Churn test baseline threads: {}", baselineThreads);

        int totalConnections = 500;
        int numWorkers = 10;
        int perWorker = totalConnections / numWorkers;
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(numWorkers);
        try
        {
            List<Future<?>> futures = new ArrayList<>();
            for (int w = 0; w < numWorkers; w++)
            {
                futures.add(executor.submit(() ->
                {
                    Properties info = new Properties();
                    PGProperty.USER.set(info, "dummy");
                    PGProperty.PASSWORD.set(info, "dummy");
                    PGProperty.PREFER_QUERY_MODE.set(info, "simple");

                    for (int i = 0; i < perWorker; i++)
                    {
                        try (Connection conn = DriverManager.getConnection(jdbcUrl(), info);
                             Statement stmt = conn.createStatement();
                             ResultSet rs = stmt.executeQuery("SELECT 1"))
                        {
                            while (rs.next())
                            {
                                // consume
                            }
                            successes.incrementAndGet();
                        }
                        catch (Exception e)
                        {
                            failures.incrementAndGet();
                        }
                    }
                    return null;
                }));
            }

            for (Future<?> f : futures)
            {
                f.get(90, TimeUnit.SECONDS);
            }
        }
        finally
        {
            executor.shutdownNow();
        }

        int afterThreads = threadMXBean.getThreadCount();
        int threadDelta = afterThreads - baselineThreads;
        LOGGER.info("Churn test: {} ok, {} fail, thread delta {} ({} ? {})",
                successes.get(), failures.get(), threadDelta,
                baselineThreads, afterThreads);

        // All connections must succeed
        Assert.assertEquals("Connection failures during churn",
                0, failures.get());
        Assert.assertEquals("Not all churn connections succeeded",
                totalConnections, successes.get());

        // Thread count should not grow proportionally to connection count.
        // With a shared executor: delta < 30 (some Netty/GC overhead).
        // With per-session pools: delta ? 500 (one leaked thread each).
        Assert.assertTrue(
                "Thread count grew by " + threadDelta + " after " + totalConnections
                        + " concurrent connections. Expected < 30 with shared executor.",
                threadDelta < 30);

        // Verify the server is still healthy ? new connections work fine
        try (Connection conn = DriverManager.getConnection(jdbcUrl(), "dummy", "dummy");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1"))
        {
            rs.next();
            Assert.assertEquals(1, rs.getInt(1));
        }
    }

    // -----------------------------------------------------------------------
    // Helper: crude JSON array size counter for /server/info responses
    // -----------------------------------------------------------------------

    /**
     * Counts elements in a JSON array by key name. Good enough for the simple
     * {@code /server/info} response structure.
     */
    private int countJsonArrayEntries(String json, String arrayKey)
    {
        String searchKey = "\"" + arrayKey + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx < 0)
        {
            return 0;
        }
        int bracketOpen = json.indexOf('[', keyIdx);
        if (bracketOpen < 0)
        {
            return 0;
        }
        int bracketClose = findMatchingBracket(json, bracketOpen);
        if (bracketClose < 0)
        {
            return 0;
        }
        String arrayContent = json.substring(bracketOpen + 1, bracketClose).trim();
        if (arrayContent.isEmpty())
        {
            return 0;
        }
        int depth = 0;
        int count = 1;
        for (char ch : arrayContent.toCharArray())
        {
            if (ch == '{' || ch == '[')
            {
                depth++;
            }
            else if (ch == '}' || ch == ']')
            {
                depth--;
            }
            else if (ch == ',' && depth == 0)
            {
                count++;
            }
        }
        return count;
    }

    private int findMatchingBracket(String json, int openIdx)
    {
        int depth = 0;
        for (int i = openIdx; i < json.length(); i++)
        {
            char ch = json.charAt(i);
            if (ch == '[')
            {
                depth++;
            }
            else if (ch == ']')
            {
                depth--;
                if (depth == 0)
                {
                    return i;
                }
            }
        }
        return -1;
    }
}
