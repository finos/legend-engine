// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.postgres.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Span;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelLoader;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.stores.relational.test.H2TestServerResource;
import org.finos.legend.engine.postgres.TestPostgresServer;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.finos.legend.engine.postgres.protocol.sql.SQLManager;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.generic.GenericLegendExecution;
import org.finos.legend.engine.postgres.protocol.wire.auth.identity.AnonymousIdentityProvider;
import org.finos.legend.engine.postgres.protocol.wire.auth.method.NoPasswordAuthenticationMethod;
import org.finos.legend.engine.postgres.protocol.wire.serialization.Messages;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.server.Server;
import org.finos.legend.engine.server.ServerConfiguration;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.finos.legend.pure.runtime.java.extension.store.relational.shared.connectionManager.ConnectionManager;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;

public class PostgresServerGenericTest
{
    @Test
    public void testSimpleQueryOneProject() throws Exception
    {
        loader.setData(Maps.mutable.with(
                buildPointer("t_group", "t_name", "t_version"),
                PureGrammarParser.newInstance().parseModel("###Relational\n" +
                        "Database pack::DB\n" +
                        "(\n" +
                        "    Table myTab(name VARCHAR(200))\n" +
                        ")\n")));

        updateDB(Lists.mutable.with(
                "drop table if exists myTab",
                "CREATE Table myTab(name VARCHAR(200))",
                "insert into myTab (name) values ('value')"), h2TestServer.getPort());

        Assert.assertEquals(
                "name\n" +
                        "value\n", executePrepared("select name from tb('pack::DB.myTab') as t where t.name = 'value'", "projects|t_group:t_name:t_version"));
        Assert.assertEquals(
                "name\n" +
                        "value\n", execute("select name from tb('pack::DB.myTab') as t where t.name = 'value'", "projects|t_group:t_name:t_version"));
    }


    @Test
    public void testSimpleQueryMultipleProjects() throws Exception
    {
        loader.setData(Maps.mutable.with(
                buildPointer("t_group", "t_name", "t_version"),
                PureGrammarParser.newInstance().parseModel("###Relational\n" +
                        "Database pack::DB\n" +
                        "(\n" +
                        "    include pack::otherDB\n" +
                        "    Table myTab(id INT, name VARCHAR(200))\n" +
                        ")\n"),
                buildPointer("t_group", "t_name2", "t_version"),
                PureGrammarParser.newInstance().parseModel("###Relational\n" +
                        "Database pack::otherDB\n" +
                        "(\n" +
                        "    Table otherTab(fid INT, oName VARCHAR(200))\n" +
                        ")\n")));

        updateDB(Lists.mutable.with(
                "drop table if exists myTab",
                "CREATE Table myTab(id int, name VARCHAR(200))",
                "CREATE Table otherTab(fid int, oName VARCHAR(200))",
                "insert into myTab (id, name) values (1, 'value')",
                "insert into otherTab (fid, oName) values (1, 'oValue')"), h2TestServer.getPort());

        Assert.assertEquals(
                "name\n" +
                        "value\n", executePrepared("select t.name from (tb('pack::DB.myTab') as a join tb('pack::DB.otherTab') as b on a.id = b.fid) as t where t.name = 'value'", "projects|t_group:t_name:t_version|t_group:t_name2:t_version"));
        Assert.assertEquals(
                "name\n" +
                        "value\n", execute("select t.name from (tb('pack::DB.myTab') as a join tb('pack::DB.otherTab') as b on a.id = b.fid) as t where t.name = 'value'", "projects|t_group:t_name:t_version|t_group:t_name2:t_version"));
    }


    private static ObjectMapper MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private static TestPostgresServer testPostgresServer;

    private static H2TestServerResource h2TestServer;

    private static Server<?> server;

    private static int engineServerPort;

    private static TestModelLoader loader;


    @BeforeClass
    public static void setUp() throws Exception
    {
        initialize();
    }


    @AfterClass
    public static void tearDown() throws Exception
    {
        h2TestServer.shutDown();
        server.shutDown();
        testPostgresServer.stopListening();
        testPostgresServer.shutDown();
        ConnectionManager.clearTestConnections();
    }


    private static void updateDB(MutableList<String> with, int port)
    {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:tcp://127.0.0.1:" + port + "/mem:testDB;NON_KEYWORDS=ANY,ASYMMETRIC,AUTHORIZATION,CAST,CURRENT_PATH,CURRENT_ROLE,DAY,DEFAULT,ELSE,END,HOUR,KEY,MINUTE,MONTH,SECOND,SESSION_USER,SET,SOME,SYMMETRIC,SYSTEM_USER,TO,UESCAPE,USER,VALUE,WHEN,YEAR,OVER;MODE=LEGACY;DB_CLOSE_DELAY=-1 ", "sa", ""))
        {
            for (String sql : with)
            {
                try (PreparedStatement stat = connection.prepareStatement(sql))
                {
                    stat.executeUpdate();
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static class TestModelLoader implements ModelLoader
    {
        MutableMap<PureModelContext, PureModelContextData> data;

        public void setData(MutableMap<PureModelContext, PureModelContextData> data)
        {
            this.data = data;
        }

        @Override
        public boolean supports(PureModelContext context)
        {
            return data.containsKey(context);
        }

        @Override
        public PureModelContextData load(Identity identity, PureModelContext context, String clientVersion, Span parentSpan)
        {
            return data.get(context);
        }

        @Override
        public void setModelManager(ModelManager modelManager)
        {
        }

        @Override
        public boolean shouldCache(PureModelContext context)
        {
            return false;
        }

        @Override
        public PureModelContext cacheKey(PureModelContext context, Identity identity)
        {
            return context;
        }
    }

    public static String executePrepared(String query, String database) throws Exception
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/" + database + "?options='--compute=test'", new Properties());
                PreparedStatement statement = connection.prepareStatement(query);
                ResultSet resultSet = statement.executeQuery()
        )
        {
            return convertResultSetToCsvString(resultSet);
        }
    }

    public static String execute(String query, String database) throws Exception
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/" + database + "?options='--compute=test'", new Properties());
                Statement statement = connection.createStatement();
        )
        {
            statement.execute(query);
            try (ResultSet resultSet = statement.getResultSet();)
            {
                return convertResultSetToCsvString(resultSet);
            }
        }
    }

    public static PureModelContextPointer buildPointer(String group, String artifact, String version) throws Exception
    {
        return MAPPER.readValue("{\n" +
                "    \"_type\": \"pointer\",\n" +
                "    \"sdlcInfo\": {\n" +
                "      \"_type\": \"alloy\",\n" +
                "      \"baseVersion\": \"latest\",\n" +
                "      \"groupId\": \"" + group + "\",\n" +
                "      \"artifactId\": \"" + artifact + "\",\n" +
                "      \"version\": \"" + version + "\",\n" +
                "      \"packageableElementPointers\": []\n" +
                "     }\n" +
                "}", PureModelContextPointer.class);
    }

    public static String convertResultSetToCsvString(ResultSet resultSet) throws SQLException
    {
        StringBuilder csvBuilder = new StringBuilder();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Append column headers
        for (int i = 1; i <= columnCount; i++)
        {
            csvBuilder.append(metaData.getColumnLabel(i));
            if (i < columnCount)
            {
                csvBuilder.append(",");
            }
        }
        csvBuilder.append("\n");

        // Append data rows
        while (resultSet.next())
        {
            for (int i = 1; i <= columnCount; i++)
            {
                csvBuilder.append(resultSet.getString(i));
                if (i < columnCount)
                {
                    csvBuilder.append(",");
                }
            }
            csvBuilder.append("\n");
        }

        return csvBuilder.toString();
    }

    private static void initialize() throws Exception
    {
        //H2
        h2TestServer = new H2TestServerResource();
        h2TestServer.start();
        updateDB(
                Lists.mutable.with(
                        "CREATE Table IF NOT EXISTS myTab(name VARCHAR(200))",
                        "insert into myTab (name) values ('value')"
                ),
                h2TestServer.getPort()
        );

        // Engine
        engineServerPort = DynamicPortGenerator.generatePort();
        loader = new TestModelLoader();
        server = new Server()
        {
            @Override
            protected ModelLoader[] getModelLoaders(ServerConfiguration serverConfiguration)
            {
                return new ModelLoader[]{loader};
            }
        };
        System.setProperty("dw.server.connector.port", String.valueOf(engineServerPort));
        server.run("server", Objects.requireNonNull(PostgresServerGenericTest.class.getClassLoader().getResource("config/serverConfig.json")).getFile());
        System.out.println("Engine server started on port:" + engineServerPort);
        System.clearProperty("dw.server.connector.port");

        // Postgres
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(0);
        serverConfig.setHttpPort(0);
        testPostgresServer = new TestPostgresServer(
                serverConfig,
                new SQLManager(Lists.mutable.with(new GenericLegendExecution("http", "127.0.0.1", String.valueOf(engineServerPort)))),
                (user, connectionProperties) -> new NoPasswordAuthenticationMethod(new AnonymousIdentityProvider()),
                new Messages(Throwable::getMessage)
        );
        testPostgresServer.startUp();
    }
}
