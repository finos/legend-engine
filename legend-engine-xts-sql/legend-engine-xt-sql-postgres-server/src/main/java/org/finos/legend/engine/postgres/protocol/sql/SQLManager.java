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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.postgres.PostgresServerException;
import org.finos.legend.engine.postgres.protocol.sql.dispatcher.ExecutionType;
import org.finos.legend.engine.postgres.protocol.sql.dispatcher.StatementDispatcherVisitor;
import org.finos.legend.engine.postgres.protocol.sql.handler.empty.EmptyPreparedStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.empty.EmptyStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.JDBCSessionHandler;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.catalog.CatalogManager;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.catalog.SQLRewrite;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.LegendExecution;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.LegendStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.LegendPreparedStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.txn.TxnIsolationPreparedStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.txn.TxnIsolationStatement;
import org.finos.legend.engine.postgres.protocol.wire.session.Session;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.prepared.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.regular.PostgresStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class SQLManager
{
    private static final Logger logger = LoggerFactory.getLogger(SQLManager.class);

    JDBCSessionHandler legendSessionHandler;
    MutableList<LegendExecution> clients;
    CatalogManager catalogManager;

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
                    .maxConnections(100)
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

            String host = System.getenv("TESTCONTAINERS_HOST_OVERRIDE");
            String used_host = host == null ? "localhost" : host;
            logger.info("Connecting using host: {}", used_host);
            JDBCSessionHandler metadataJDBCSessionHandler = new JDBCSessionHandler("jdbc:postgresql://" + used_host + ":" + port + "/postgres", "postgres", "");

            logger.info("Waiting for initialization");
            waitInitialization(metadataJDBCSessionHandler);
            logger.info("Postgres initialized on port " + port);

            metadataJDBCSessionHandler.prepareStatement("CREATE DATABASE legend_m;").execute();

            this.legendSessionHandler = new JDBCSessionHandler("jdbc:postgresql://" + used_host + ":" + port + "/legend_m", "postgres", "");

            this.catalogManager = new CatalogManager(legendSessionHandler);
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
                        return new LegendPreparedStatement(query, findClient(clients, session.getDatabase(), session.getOptions()), session.getDatabase(), session.getOptions(), session.getIdentity());
                    case Metadata:
                        String reprocessedQuery = SQLGrammarParser.getSqlBaseParser(query, "query").statement().accept(new SQLRewrite(session));
                        logger.info("Executing reprocessed query: {}", reprocessedQuery);
                        return legendSessionHandler.prepareStatement(reprocessedQuery);
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
                        return new LegendStatement(findClient(clients, session.getDatabase(), session.getOptions()), session.getIdentity());
                    case Metadata:
                        return legendSessionHandler.createStatement();
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

    private LegendExecution findClient(MutableList<LegendExecution> clients, String database, String options)
    {
        return clients.select(x -> x.supports(database)).getFirst();
    }

    private static ExecutionType getType(String query)
    {
        return query.isEmpty() ? ExecutionType.Empty : SQLGrammarParser.getSqlBaseParser(query, "query").singleStatement().accept(new StatementDispatcherVisitor());
    }

    public void waitInitialization(JDBCSessionHandler metadataJDBCSessionHandler)
    {
        boolean initializing = true;
        do
        {
            try
            {
                Thread.sleep(100);
                metadataJDBCSessionHandler.prepareStatement("select 1;").execute();
                initializing = false;
            }
            catch (Exception e)
            {
                // Do Nothing
            }
        }
        while (initializing);
    }
}
