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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.finos.legend.engine.postgres.handler.legend.LegendSessionFactory;
import org.finos.legend.engine.postgres.handler.legend.LegendTdsClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

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
import java.util.TimeZone;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.finos.legend.engine.postgres.handler.legend.LegendResultSet.TIMESTAMP_FORMATTER;

public class PostgresServerTypeMappingTest
{

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(options().dynamicPort(), false);
    private static TestPostgresServer testPostgresServer;

    @BeforeClass
    public static void setUpClass() throws Exception
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
    }


    @Test
    public void testBoolean() throws Exception
    {
        validate("Boolean", "true", "bool", Boolean.TRUE);
        validate("Boolean", "false", "bool", Boolean.FALSE);
    }

    @Test
    public void testDateTime() throws Exception
    {
        String timeStamp = "2020-06-07T04:15:27.000000000+0000";
        Instant temporalAccessor = (Instant) TIMESTAMP_FORMATTER.parse(timeStamp, Instant::from);
        Timestamp expected = new Timestamp(temporalAccessor.toEpochMilli());

        validate("DateTime", "\"" + timeStamp + "\"", "timestamp", expected);
    }

    @Test
    public void testDateAsTimeStamp() throws Exception
    {
        String timeStamp = "2020-06-07T04:15:27.000000000+0000";
        Instant temporalAccessor = (Instant) TIMESTAMP_FORMATTER.parse(timeStamp, Instant::from);
        Timestamp expected = new Timestamp(temporalAccessor.toEpochMilli());
        validate("Date", "\"" + timeStamp + "\"", "timestamp", expected);
    }


    @Test
    public void testDateAsDate() throws Exception
    {
        String timeStamp = "2020-06-07";
        LocalDate temporalAccessor = TIMESTAMP_FORMATTER.parse(timeStamp, LocalDate::from);
        Timestamp expected = new Timestamp(temporalAccessor.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());

        validate("Date", "\"" + timeStamp + "\"", "timestamp", expected);
    }

    @Test
    public void testStrictDate() throws Exception
    {

        String date = "2020-06-07";
        LocalDate localDate = LocalDate.parse(date, ISO_LOCAL_DATE);
        long toEpochDay = localDate.atStartOfDay(TimeZone.getDefault().toZoneId()).toInstant().toEpochMilli();
        Date expected = new Date(toEpochDay);

        validate("StrictDate", "\"" + date + "\"", "date", expected);
    }


    @Test
    public void testFloat() throws Exception
    {
        validate("Float", "5.5", "float4", 5.5F);
    }

    @Test
    public void testInteger() throws Exception
    {
        validate("Integer", "5", "int4", 5);
    }

    @Test
    public void testNumberAsInteger() throws Exception
    {
        validate("Number", "5", "float8", 5.0D);
    }

    @Test
    public void testNumberAsDouble() throws Exception
    {
        validate("Number", "5.5", "float8", 5.5D);
    }


    public void validate(String legendDataType, String legendValue, String expectedColumnType, Object expectedValue) throws Exception
    {

        String message = buildLegendResponseMessage(legendDataType, legendValue);
        wireMockRule.stubFor(post(urlEqualTo("/api/sql/v1/execution/execute/SAMPLE-123"))
                .withRequestBody(equalTo("SELECT * FROM service.\"/testData\""))
                .willReturn(aResponse().withBody(message))
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
                "  \"_UUID\": \"b609c103-45f8-47ff-90f9-c5fee561d108\",\n" +
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
}
