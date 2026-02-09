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

package org.finos.legend.engine.postgres.protocol.sql;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.postgres.PostgresServerException;
import org.finos.legend.engine.postgres.protocol.sql.dispatcher.ExecutionType;
import org.finos.legend.engine.postgres.protocol.sql.dispatcher.StatementDispatcherVisitor;
import org.finos.legend.engine.postgres.protocol.sql.handler.empty.EmptyPreparedStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.empty.EmptyStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.JDBCPostgresPreparedStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.JDBCPostgresStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.catalog.CatalogManager;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.catalog.SQLRewrite;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.LegendExecution;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.LegendPreparedStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.LegendStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.txn.TxnIsolationPreparedStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.txn.TxnIsolationStatement;
import org.finos.legend.engine.postgres.protocol.wire.session.Session;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.prepared.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.regular.PostgresStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SQLManager
{
    private static final Logger logger = LoggerFactory.getLogger(SQLManager.class);
    private static final int METADATA_MAX_CONNECTIONS = 100;
    private final MutableList<LegendExecution> clients;

    private final MutableMap<Integer, CatalogManager> catalogManagersBySession = new ConcurrentHashMap<>();

    private final HikariDataSource dataSource;

    public SQLManager(MutableList<LegendExecution> clients)
    {
        String dockerHost = System.getenv("DOCKER_HOST");

        DockerClient dockerClient = null;
        try
        {
            String used_DockerHost = dockerHost == null ? "unix:///var/run/docker.sock" : dockerHost;
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(used_DockerHost)
                    .build();
            logger.info("Using DOCKER_HOST: {}", used_DockerHost);

            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(METADATA_MAX_CONNECTIONS)
                    .connectionTimeout(Duration.ofSeconds(30))
                    .responseTimeout(Duration.ofSeconds(45))
                    .build();

            dockerClient = DockerClientImpl.getInstance(config, httpClient);

            List<Container> containers = ListIterate.select(dockerClient.listContainersCmd().withShowAll(true).exec(), x -> Arrays.asList(x.getNames()).contains("/postgres-metadata-server"));
            if (!containers.isEmpty())
            {
                logger.info("Container already exists (" + containers.size() + "), removing it.");
                dockerClient.removeContainerCmd(containers.get(0).getId()).withForce(true).exec();
            }

            String prefix = System.getenv("TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX");
            String imageName = (prefix == null ? "" : prefix) + "postgres:10.5";
            logger.info("Using imageName: {}", imageName);
            dockerClient.pullImageCmd(imageName).start().awaitCompletion();

            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withHostConfig(HostConfig.newHostConfig().withPortBindings(new PortBinding(Ports.Binding.empty(), ExposedPort.tcp(5432))))
                    .withName("postgres-metadata-server")
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();

            int port = Integer.parseInt(dockerClient.inspectContainerCmd(container.getId()).exec()
                    .getNetworkSettings()
                    .getPorts().getBindings().values().iterator().next()[0].getHostPortSpec());

            String givenHost = System.getenv("TESTCONTAINERS_HOST_OVERRIDE");
            String host = givenHost == null ? "localhost" : givenHost;
            String baseUrl = "jdbc:postgresql://" + host + ":" + port;
            logger.info("Connecting using host: {}", host);

            logger.info("Waiting for initialization");
            waitInitialization(baseUrl + "/postgres", "postgres", "");

            // Create metadata database
            try (Connection connection = DriverManager.getConnection(baseUrl + "/postgres", "postgres", "");
                 PreparedStatement preparedStatement = connection.prepareStatement("CREATE DATABASE legend_m;"))
            {
                logger.info("Postgres initialized on port " + port);
                preparedStatement.execute();
            }

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(baseUrl + "/legend_m");
            hikariConfig.setUsername("postgres");
            hikariConfig.setPassword("");
            hikariConfig.setMaximumPoolSize(METADATA_MAX_CONNECTIONS - 10); //leave some connections free for other DB operations
            hikariConfig.setMinimumIdle(10);
            hikariConfig.setPoolName("LegendMetadataPool");

            this.dataSource = new HikariDataSource(hikariConfig);

            logger.info("HikariCP connection pool initialized with max pool size: {}", hikariConfig.getMaximumPoolSize());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (dockerClient != null)
            {
                try
                {
                    dockerClient.close();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
        this.clients = clients;
    }

    public PostgresPreparedStatement buildPreparedStatement(String query, Session session)
    {
        try
        {
            if (query != null)
            {
                switch (getType(query))
                {
                    case Empty:
                        return new EmptyPreparedStatement();
                    case Legend:
                        return new LegendPreparedStatement(query, findClient(clients, session.getDatabaseName()), session.getDatabaseName(), session.getOptions(), session.getIdentity());
                    case Metadata_Generic:
                    {
                        // We still want to rewrite things like the current_database function.
                        return new JDBCPostgresPreparedStatement(this::getMetadataConnection, CatalogManager.reprocessQuery(query, new SQLRewrite(session, null)));
                    }
                    case Metadata_User_Specific:
                    {
                        CatalogManager catalogManager = catalogManagersBySession.getIfAbsentPut(session.getId(), () -> new CatalogManager(session.getIdentity(), session.getDatabaseName(), findClient(clients, session.getDatabaseName()), this::getMetadataConnection));
                        return new JDBCPostgresPreparedStatement(this::getMetadataConnection, CatalogManager.reprocessQuery(query, new SQLRewrite(session, catalogManager)));
                    }
                    case TX:
                        return new TxnIsolationPreparedStatement();
                }
            }
        }
        catch (SQLException e)
        {
            throw new PostgresServerException(e);
        }
        return null;
    }

    public PostgresStatement buildStatement(String query, Session session)
    {
        try
        {
            if (query != null)
            {
                switch (getType(query))
                {
                    case Empty:
                        return new EmptyStatement();
                    case Legend:
                        return new LegendStatement(findClient(clients, session.getDatabaseName()), session.getDatabaseName(), session.getOptions(), session.getIdentity());
                    case Metadata_Generic:
                        return new JDBCPostgresStatement(this::getMetadataConnection, new SQLRewrite(session, null));
                    case Metadata_User_Specific:
                        CatalogManager catalogManager = catalogManagersBySession.getIfAbsentPut(session.getId(), () -> new CatalogManager(session.getIdentity(), session.getDatabaseName(), findClient(clients, session.getDatabaseName()), () -> getMetadataConnection()));
                        return new JDBCPostgresStatement(this::getMetadataConnection, new SQLRewrite(session, catalogManager));
                    case TX:
                        return new TxnIsolationStatement();
                }
            }
        }
        catch (SQLException e)
        {
            throw new PostgresServerException(e);
        }
        return null;
    }

    private Connection getMetadataConnection()
    {
        try
        {
            return dataSource.getConnection();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    private LegendExecution findClient(MutableList<LegendExecution> clients, String database)
    {
        return clients.select(x -> x.supports(database)).getFirst();
    }

    private static ExecutionType getType(String query)
    {
        return query.isEmpty() ? ExecutionType.Empty : SQLGrammarParser.getSqlBaseParser(query, "query").singleStatement().accept(new StatementDispatcherVisitor());
    }

    public void waitInitialization(String url, String user, String password)
    {
        boolean initializing = true;
        do
        {
            try
            {
                Thread.sleep(100);
                try (Connection connection = DriverManager.getConnection(url, user, password);
                     PreparedStatement preparedStatement = connection.prepareStatement("select 1;"))
                {
                    preparedStatement.execute();
                }
                initializing = false;
            }
            catch (Exception e)
            {
                // Do Nothing
            }
        }
        while (initializing);
    }

    public void sessionClosing(Session session) throws SQLException
    {
        CatalogManager cm = this.catalogManagersBySession.remove(session.getId());
        if (cm != null)
        {
            cm.close();
        }
    }

    public void registerHikariMetrics(PrometheusRegistry registry)
    {
        HikariPoolMXBean poolMXBean = dataSource.getHikariPoolMXBean();
        String poolName = dataSource.getPoolName();

        Gauge activeConnections = Gauge.builder()
                .name("legend_metadata_hikari_active_connections")
                .help("Active connections in the HikariCP pool")
                .labelNames("pool")
                .register(registry);

        Gauge idleConnections = Gauge.builder()
                .name("legend_metadata_hikari_idle_connections")
                .help("Idle connections in the HikariCP pool")
                .labelNames("pool")
                .register(registry);

        Gauge totalConnections = Gauge.builder()
                .name("legend_metadata_hikari_total_connections")
                .help("Total connections in the HikariCP pool")
                .labelNames("pool")
                .register(registry);

        Gauge threadsAwaiting = Gauge.builder()
                .name("legend_metadata_hikari_threads_awaiting_connection")
                .help("Number of threads awaiting a connection from the HikariCP pool")
                .labelNames("pool")
                .register(registry);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() ->
        {
            activeConnections.labelValues(poolName).set(poolMXBean.getActiveConnections());
            idleConnections.labelValues(poolName).set(poolMXBean.getIdleConnections());
            totalConnections.labelValues(poolName).set(poolMXBean.getTotalConnections());
            threadsAwaiting.labelValues(poolName).set(poolMXBean.getThreadsAwaitingConnection());
        }, 0, 10, TimeUnit.SECONDS);

        logger.info("HikariCP metrics registered with Prometheus for pool: {}", poolName);
    }
}