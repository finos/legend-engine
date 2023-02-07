// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.pg.postgres;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.finos.legend.engine.pg.postgres.legend.LegendSessionFactory;
import org.finos.legend.engine.pg.postgres.legend.LegendTdsClient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class PostgresServerTest
{
    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(options().dynamicPort(), false);
    private static TestPostgresServer testPostgresServer;

    @BeforeClass
    public static void setUp()
    {
        CookieStore cookieStore = new BasicCookieStore();
        LegendTdsClient client = new LegendTdsClient("localhost", "" + wireMockRule.port(), "SAMPLE-123", cookieStore);
        LegendSessionFactory legendSessionFactory = new LegendSessionFactory(client);
        testPostgresServer = new TestPostgresServer(0, legendSessionFactory);
        testPostgresServer.startUp();
        wireMockRule.stubFor(post(urlEqualTo("/api/sql/v1/execution/execute/SAMPLE-123"))
                .willReturn(aResponse()
                        .withBody("{}"))
        );
        wireMockRule.stubFor(post(urlEqualTo("/api/sql/v1/execution/execute/SAMPLE-123"))
                .withRequestBody(equalTo("SELECT * FROM service.\"/personService\""))
                .willReturn(aResponse()
                        .withBody("{ \"builder\": { \"_type\": \"tdsBuilder\", \"columns\": [ { \"name\": \"Age\", \"type\": \"Integer\", \"relationalType\": \"INTEGER\" }, { \"name\": \"First Name\", \"type\": \"String\", \"relationalType\": \"VARCHAR(200)\" }, { \"name\": \"Last Name\", \"type\": \"String\", \"relationalType\": \"VARCHAR(200)\" } ] }, \"activities\": [ { \"_type\": \"relational\", \"sql\": \"select \\\"root\\\".AGE as \\\"Age\\\", \\\"root\\\".FIRSTNAME as \\\"First Name\\\", \\\"root\\\".LASTNAME as \\\"Last Name\\\" from personTable as \\\"root\\\"\" } ], \"result\": { \"columns\": [ \"Age\", \"First Name\", \"Last Name\" ], \"rows\": [ { \"values\": [ 23, \"Peter\", \"Smith\" ] }, { \"values\": [ 30, \"Leonid\", \"Shtivelman\" ] }, { \"values\": [ 25, \"Vignesh\", \"Manickavasagam\" ] }, { \"values\": [ 31, \"Andrew\", \"Ormerod\" ] }, { \"values\": [ 32, \"Pierre\", \"De Belen\" ] } ] } }")
                )
        );
    }

    @Test
    public void testMetadata() throws SQLException
    {
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                "dummy", "dummy");
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/personService\"");
             ResultSet resultSet = statement.executeQuery()
        )
        {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            Assert.assertEquals(3, resultSetMetaData.getColumnCount());
            Assert.assertEquals("Age", resultSetMetaData.getColumnName(1));
            Assert.assertEquals("First Name", resultSetMetaData.getColumnName(2));
            Assert.assertEquals("Last Name", resultSetMetaData.getColumnName(3));
            Assert.assertEquals("int4", resultSetMetaData.getColumnTypeName(1));
            Assert.assertEquals("varchar", resultSetMetaData.getColumnTypeName(2));
            Assert.assertEquals("varchar", resultSetMetaData.getColumnTypeName(3));
        }
    }

    @Test
    public void testNumberOfRows() throws SQLException
    {
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
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
            Assert.assertEquals(5, rows);
        }
    }

    @AfterClass
    public static void tearDown()
    {
        testPostgresServer.stopListening();
        testPostgresServer.shutDown();
    }
}