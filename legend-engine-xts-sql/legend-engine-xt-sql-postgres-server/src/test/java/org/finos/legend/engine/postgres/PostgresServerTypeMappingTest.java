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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

import java.util.TimeZone;
import org.finos.legend.engine.postgres.auth.AnonymousIdentityProvider;
import org.finos.legend.engine.postgres.auth.NoPasswordAuthenticationMethod;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.finos.legend.engine.postgres.handler.legend.LegendExecutionService;
import org.finos.legend.engine.postgres.handler.legend.LegendHttpClient;

import static org.finos.legend.engine.postgres.handler.legend.LegendResultSet.TIMESTAMP_FORMATTER;

import org.finos.legend.engine.postgres.handler.legend.LegendSessionFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.finos.legend.engine.postgres.handler.legend.LegendDataType.*;


public class PostgresServerTypeMappingTest
{

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(options().dynamicPort(), false);
    private static TestPostgresServer testPostgresServer;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @BeforeClass
    public static void setUpClass()
    {
        LegendExecutionService client = new LegendExecutionService(new LegendHttpClient("http", "localhost", String.valueOf(wireMockRule.port())));
        LegendSessionFactory legendSessionFactory = new LegendSessionFactory(client);
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(0);
        testPostgresServer = new TestPostgresServer(serverConfig, legendSessionFactory,
                (user, connectionProperties) -> new NoPasswordAuthenticationMethod(new AnonymousIdentityProvider()),
                new Messages((exception) -> exception.getMessage()));
        testPostgresServer.startUp();
        //stub to handle miscellaneous message that we don't care about
        wireMockRule.stubFor(post(urlEqualTo("/api/sql/v1/execution/executeQueryString"))
                .willReturn(aResponse()
                        .withBody("{}"))
        );
    }


    @Test
    public void testString() throws Exception
    {
        validate(STRING, "\"foo\"", "varchar", "foo");
        validate(STRING, "\"foo\"", "varchar", "foo");
        validate(STRING, "null", "varchar", null);
        validate(STRING, "\"\"", "varchar", "");
    }

    @Test
    public void testEnum() throws Exception
    {
        validate("demo::employeeType", "\"foo\"", "varchar", "foo");
        validate("demo::employeeType", "\"foo\"", "varchar", "foo");
        validate("demo::employeeType", "null", "varchar", null);
    }

    @Test
    public void testBoolean() throws Exception
    {
        validate(BOOLEAN, "true", "bool", Boolean.TRUE);
        validate(BOOLEAN, "false", "bool", Boolean.FALSE);
        validate(BOOLEAN, "null", "bool", null);
    }

    @Test()
    public void testBooleanInvalidData() throws Exception
    {
        expectedException.expect(Exception.class);
        expectedException.expectMessage("ERROR: Unexpected data type for value '1234....' in column 'column1'." +
                " Expected data type 'java.lang.Boolean', actual data type 'java.lang.Long'");
        validate(BOOLEAN, "1234", "bool", null);
    }

    @Test
    public void testDateTime() throws Exception
    {
        String timeStamp = "2020-06-07T04:15:27.000000000+0000";
        Instant temporalAccessor = TIMESTAMP_FORMATTER.parse(timeStamp, Instant::from);
        Timestamp expected = new Timestamp(temporalAccessor.toEpochMilli());

        validate(DATE_TIME, "\"" + timeStamp + "\"", "timestamp", expected);
        validate(DATE_TIME, "null", "timestamp", null);
    }

    @Test()
    public void testDateTimeInvalidData() throws Exception
    {
        //invalid date format
        String timeStamp = "20200607T04:15:27.000000000+0000";
        expectedException.expect(Exception.class);
        expectedException.expectMessage("ERROR: Unexpected value '20200....' in column 'column1'." +
                " Expected data type 'java.lang.String', value format 'Date (YYYY-MM-DD) or Timestamp (YYYY-MM-DDThh:mm:ss.000000000+0000)");
        validate(DATE_TIME, "\"" + timeStamp + "\"", null, null);
    }

    @Test
    public void testDateAsTimeStamp() throws Exception
    {
        String timeStamp = "2020-06-07T04:15:27.000000000+0000";
        Instant temporalAccessor = (Instant) TIMESTAMP_FORMATTER.parse(timeStamp, Instant::from);
        Timestamp expected = new Timestamp(temporalAccessor.toEpochMilli());
        validate(DATE, "\"" + timeStamp + "\"", "timestamp", expected);
    }

    @Test()
    public void testDateAsTimeStampInvalidData() throws Exception
    {
        //invalid date format
        String timeStamp = "2020-Jun-07T04:15:27.000000000+0000";
        expectedException.expect(Exception.class);
        expectedException.expectMessage("ERROR: Unexpected value '2020-....' in column 'column1'." +
                " Expected data type 'java.lang.String', value format 'Date (YYYY-MM-DD) or Timestamp (YYYY-MM-DDThh:mm:ss.000000000+0000)");
        validate(DATE, "\"" + timeStamp + "\"", null, null);
    }

    @Test
    public void testDateAsDate() throws Exception
    {
        String timeStamp = "2020-06-07";
        LocalDate temporalAccessor = TIMESTAMP_FORMATTER.parse(timeStamp, LocalDate::from);
        Timestamp expected = new Timestamp(temporalAccessor.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());

        validate(DATE, "\"" + timeStamp + "\"", "timestamp", expected);
    }

    @Test()
    public void testDateAsDateInvalidData() throws Exception
    {
        //invalid date format
        String timeStamp = "20200607";
        expectedException.expect(Exception.class);
        expectedException.expectMessage("ERROR: Unexpected value '20200....' in column 'column1'. " +
                "Expected data type 'java.lang.String', value format 'Date (YYYY-MM-DD) or Timestamp (YYYY-MM-DDThh:mm:ss.000000000+0000)'");
        validate(DATE, "\"" + timeStamp + "\"", null, null);
    }

    @Test
    public void testStrictDate() throws Exception
    {

        String date = "2020-06-07";
        LocalDate localDate = LocalDate.parse(date, ISO_LOCAL_DATE);
        long toEpochDay = localDate.atStartOfDay(TimeZone.getDefault().toZoneId()).toInstant().toEpochMilli();
        Date expected = new Date(toEpochDay);

        validate(STRICT_DATE, "\"" + date + "\"", "date", expected);
        validate(STRICT_DATE, "null", "date", null);
    }


    @Test()
    public void testStrictDateInvalidData() throws Exception
    {
        //invalid date format
        String date = "20200607";
        expectedException.expect(Exception.class);
        expectedException.expectMessage("ERROR: Unexpected value '20200....' in column 'column1'." +
                " Expected data type 'java.lang.String', value format 'Date (YYYY-MM-DD)'");
        validate(STRICT_DATE, "\"" + date + "\"", null, null);
    }

    @Test
    public void testFloat() throws Exception
    {
        validate(FLOAT, "5.5", "float8", 5.5D);
        validate(FLOAT, "null", "float8", null);
        validate(FLOAT, "2645198855588.533433343434", "float8", 2645198855588.533433343434D);
    }


    @Test()
    public void testFloatInvalidData() throws Exception
    {
        expectedException.expect(Exception.class);
        expectedException.expectMessage("ERROR: Unexpected data type for value 'foooo....' in column 'column1'." +
                " Expected data type 'java.lang.Number', actual data type 'java.lang.String'");
        validate(FLOAT, "\"fooooo\"", null, null);
    }

    @Test
    public void testInteger() throws Exception
    {
        validate(INTEGER, "5", "int8", 5L);
        validate(INTEGER, "2645198855588", "int8", 2645198855588L);
        validate(INTEGER, "null", "int8", null);
    }

    @Test()
    public void testIntegerInvalidData() throws Exception
    {
        expectedException.expect(Exception.class);
        expectedException.expectMessage("ERROR: Unexpected data type for value 'foo....' in column 'column1'. " +
                "Expected data type 'java.lang.Number', actual data type 'java.lang.String'");
        validate(INTEGER, "\"foo\"", null, null);
    }

    @Test
    public void testNumberAsInteger() throws Exception
    {
        validate(NUMBER, "5", "float8", 5.0D);
    }

    @Test()
    public void testNumberAsIntegerInvalidData() throws Exception
    {
        expectedException.expect(Exception.class);
        expectedException.expectMessage("ERROR: Unexpected data type for value 'foo....' in column 'column1'. " +
                "Expected data type 'java.lang.Number', actual data type 'java.lang.String'");
        validate(NUMBER, "\"foo\"", null, null);
    }

    @Test
    public void testNumberAsDouble() throws Exception
    {
        validate(NUMBER, "5.5", "float8", 5.5D);
    }


    public void validate(String legendDataType, String legendValue, String expectedColumnType, Object expectedValue) throws Exception
    {

        String schemaMessage = buildLegendSchemaMessage(legendDataType);
        wireMockRule.stubFor(post(urlEqualTo("/api/sql/v1/execution/getSchemaFromQueryString"))
                .withRequestBody(equalTo("SELECT * FROM service.\"/testData\""))
                .willReturn(aResponse().withBody(schemaMessage))
        );

        String responseMessage = buildLegendResponseMessage(legendDataType, legendValue);
        wireMockRule.stubFor(post(urlEqualTo("/api/sql/v1/execution/executeQueryString"))
                .withRequestBody(equalTo("SELECT * FROM service.\"/testData\""))
                .willReturn(aResponse().withBody(responseMessage))
        );
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/testData\"");
                ResultSet resultSet = statement.executeQuery()
        )
        {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            Assert.assertEquals(1, resultSetMetaData.getColumnCount());
            Assert.assertEquals("column1", resultSetMetaData.getColumnName(1));
            Assert.assertEquals(expectedColumnType, resultSetMetaData.getColumnTypeName(1));
            resultSet.next();
            Object object = resultSet.getObject(1);
            Assert.assertEquals(expectedValue, object);
        }
    }


    private static String buildLegendResponseMessage(String columnType, String value)
    {
        String responseTemplate = "{\n" +
                "  \"builder\": {\n" +
                "    \"_type\": \"tdsBuilder\",\n" +
                "    \"columns\": [\n" +
                "      {\n" +
                "        \"name\": \"column1\",\n" +
                "        \"type\": \"%s\",\n" +
                "        \"relationalType\": \"sqlType\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"activities\": [\n" +
                "    {\n" +
                "      \"_type\": \"relational\",\n" +
                "      \"sql\": \"select ...\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"result\": {\n" +
                "    \"columns\": [\n" +
                "      \"column1\"\n" +
                "    ],\n" +
                "    \"rows\": [\n" +
                "      {\n" +
                "        \"values\": [\n" +
                "          %s\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        return String.format(responseTemplate, columnType, value);
    }


    private static String buildLegendSchemaMessage(String columnType)
    {
        String responseTemplate = "{\n" +
                "  \"__TYPE\": \"meta::external::query::sql::Schema\",\n" +
                "  \"columns\": [\n" +
                "    {\n" +
                "      \"__TYPE\": \"meta::external::query::sql::PrimitiveValueSchemaColumn\",\n" +
                "      \"type\": \"%s\",\n" +
                "      \"name\": \"column1\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"enums\": []\n" +
                "}";
        return String.format(responseTemplate, columnType);
    }

}
