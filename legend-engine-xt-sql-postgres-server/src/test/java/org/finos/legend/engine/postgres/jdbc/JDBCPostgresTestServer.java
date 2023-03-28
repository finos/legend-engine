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

package org.finos.legend.engine.postgres.jdbc;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.postgres.TestPostgresServer;
import org.finos.legend.engine.postgres.auth.AnonymousIdentityProvider;
import org.finos.legend.engine.postgres.auth.NoPasswordAuthenticationMethod;
import org.finos.legend.engine.postgres.handler.jdbc.JDBCSessionFactory;
import org.h2.tools.Server;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;

public class JDBCPostgresTestServer
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JDBCPostgresTestServer.class);

    private static Server clientServer;
    private static TestPostgresServer testPostgresServer;

    @BeforeClass
    public static void setUp() throws Exception
    {
        File h2server = Files.createTempDirectory("h2server").toFile();
        h2server.deleteOnExit();
        String h2serverAbsolutePath = h2server.getAbsolutePath();
        clientServer = Server.createPgServer("-baseDir", h2serverAbsolutePath, "-ifNotExists");
        clientServer.start();
        LOGGER.info("H2 Postgres Status:" + clientServer.getStatus());

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:" + clientServer.getPort() + "/testDB", "sa", ""))
        {
            exec("drop table if exists employeeTable", conn);
            exec("create table employeeTable(id INT, name VARCHAR(200), firmid INT, doh TIMESTAMP, type VARCHAR(200), active INT , skills VARCHAR(200))", conn);
            exec("insert into employeeTable (id, name, firmId, doh, type, active,skills) values (1, 'Alice',  0, '1983-03-15', 'FTC', 1, null)", conn);
            exec("insert into employeeTable (id, name, firmId, doh, type, active,skills) values (2, 'Bob',    0, '2003-07-19', 'FTE', 0, ',1,2,')", conn);
            exec("insert into employeeTable (id, name, firmId, doh, type, active,skills) values (3, 'Curtis', 0, '2012-08-25', 'FTO', null, ',3,2,')", conn);
        }


        testPostgresServer = new TestPostgresServer(0,
                new JDBCSessionFactory("jdbc:postgresql://localhost:" + clientServer.getPort() + "/testDB", "sa", "",
                        Sets.immutable.of("SET extra_float_digits = 3","SET application_name = 'PostgreSQL JDBC Driver'").castToSet()),
                (user, connectionProperties) -> new NoPasswordAuthenticationMethod(new AnonymousIdentityProvider()));
        testPostgresServer.startUp();
    }

    private static void exec(String sql, Connection connection) throws Exception
    {
        Statement statement = connection.createStatement();
        statement.executeUpdate(sql);
        statement.close();
    }


    @Test
    public void testPreparedStatementNoParams() throws Exception
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM employeeTable");
                ResultSet resultSet = statement.executeQuery()
        )
        {
            //test metadata
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            Assert.assertEquals(7, resultSetMetaData.getColumnCount());
            Assert.assertEquals("id", resultSetMetaData.getColumnName(1));
            Assert.assertEquals("name", resultSetMetaData.getColumnName(2));
            Assert.assertEquals("firmid", resultSetMetaData.getColumnName(3));
            Assert.assertEquals("int4", resultSetMetaData.getColumnTypeName(1));
            Assert.assertEquals("varchar", resultSetMetaData.getColumnTypeName(2));
            Assert.assertEquals("int4", resultSetMetaData.getColumnTypeName(3));

            resultSet.next();
            Assert.assertEquals(1, resultSet.getInt(1));
            Assert.assertEquals("Alice", resultSet.getString(2));
            Assert.assertEquals(0, resultSet.getInt(3));
        }
    }

    @Test
    public void testPreparedStatementWithParams() throws Exception
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM employeeTable where name = ?");
        )
        {
            statement.setString(1, "Curtis");
            try (
                    ResultSet resultSet = statement.executeQuery()
            )
            {
                resultSet.next();
                Assert.assertEquals(3, resultSet.getInt(1));
                Assert.assertEquals("Curtis", resultSet.getString(2));
                Assert.assertEquals(0, resultSet.getInt(3));
            }
        }
    }


    @Test
    public void testSimpleQuery() throws Exception
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM employeeTable")
        )
        {
            //test metadata
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            Assert.assertEquals(7, resultSetMetaData.getColumnCount());
            Assert.assertEquals("id", resultSetMetaData.getColumnName(1));
            Assert.assertEquals("name", resultSetMetaData.getColumnName(2));
            Assert.assertEquals("firmid", resultSetMetaData.getColumnName(3));
            Assert.assertEquals("int4", resultSetMetaData.getColumnTypeName(1));
            Assert.assertEquals("varchar", resultSetMetaData.getColumnTypeName(2));
            Assert.assertEquals("int4", resultSetMetaData.getColumnTypeName(3));

            resultSet.next();
            Assert.assertEquals(1, resultSet.getInt(1));
            Assert.assertEquals("Alice", resultSet.getString(2));
            Assert.assertEquals(0, resultSet.getInt(3));
        }
    }



    @AfterClass
    public static void tearDown()
    {
        if (testPostgresServer != null)
        {
            testPostgresServer.shutDown();
        }

        if (clientServer != null)
        {
            clientServer.shutdown();
        }
    }
}
