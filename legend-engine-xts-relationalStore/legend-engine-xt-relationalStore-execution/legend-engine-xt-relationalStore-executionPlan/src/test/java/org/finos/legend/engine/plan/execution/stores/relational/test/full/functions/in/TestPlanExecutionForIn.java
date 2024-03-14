//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.full.functions.in;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.execution.stores.relational.connection.AlloyTestServer;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.finos.legend.engine.plan.execution.nodes.helpers.freemarker.FreeMarkerExecutor.overridePropertyForTemplateModel;
import static org.finos.legend.engine.plan.execution.nodes.helpers.freemarker.FreeMarkerExecutor.processRecursively;

public class TestPlanExecutionForIn extends AlloyTestServer
{
    private static final int LEGACY_H2_VERSION = 1;

    public static final String LOGICAL_MODEL = "###Pure\n" +
            "Class test::Person\n" +
            "{\n" +
            "  fullName: String[1];\n" +
            "  birthTime: DateTime[0..1];\n" +
            "  firmName: String[1];\n" +
            "}\n" +
            "Class test::Address\n" +
            "{\n" +
            "    name : String[1];\n" +
            "    street : String[0..1];\n" +
            "}\n";

    public static final String STORE_MODEL = "###Relational\n" +
            "Database test::DB\n" +
            "(\n" +
            "  Table PERSON (\n" +
            "    fullName VARCHAR(100) PRIMARY KEY,\n" +
            "    firmName VARCHAR(100),\n" +
            "    addressName VARCHAR(100),\n" +
            "    birthTime TIMESTAMP\n" +
            "  )\n" +
            "  Table Address(\n" +
            "    name VARCHAR(255) PRIMARY KEY\n" +
            "  )\n" +
            "\n" +
            "  Table Street(\n" +
            "    name VARCHAR(255) PRIMARY KEY,\n" +
            "    address_name VARCHAR(255) \n" +
            "  )\n" +
            "Join Address_Street(Address.name = Street.address_name)\n" +
            ")\n\n\n";

    public static final String MAPPING = "###Mapping\n" +
            "Mapping test::Map\n" +
            "(\n" +
            "  test::Person: Relational\n" +
            "  {\n" +
            "    ~primaryKey\n" +
            "    (\n" +
            "      [test::DB]PERSON.fullName\n" +
            "    )\n" +
            "    ~mainTable [test::DB]PERSON\n" +
            "    fullName: [test::DB]PERSON.fullName,\n" +
            "    birthTime: [test::DB]PERSON.birthTime,\n" +
            "    firmName: [test::DB]PERSON.firmName\n" +
            "  }\n" +
            "   test::Address: Relational\n" +
            "   {\n" +
            "     ~mainTable [test::DB]Address\n" +
            "     name: [test::DB]Address.name,\n" +
            "     street: [test::DB]@Address_Street | Street.name\n" +
            "   }\n" +
            ")\n\n\n";

    public static final String RUNTIME = "###Runtime\n" +
            "Runtime test::Runtime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    test::Map\n" +
            "  ];\n" +
            "  connections:\n" +
            "  [\n" +
            "    test::DB:\n" +
            "    [\n" +
            "      c1: #{\n" +
            "        RelationalDatabaseConnection\n" +
            "        {\n" +
            "          type: H2;\n" +
            "          specification: LocalH2 {};\n" +
            "          auth: DefaultH2;\n" +
            "        }\n" +
            "      }#\n" +
            "    ]\n" +
            "  ];\n" +
            "}\n";

    @Override
    protected void insertTestData(Statement statement) throws SQLException
    {
        statement.execute("Drop table if exists PERSON;");
        statement.execute("Create Table PERSON(fullName VARCHAR(100) NOT NULL,firmName VARCHAR(100) NULL,addressName VARCHAR(100) NULL,birthTime TIMESTAMP NULL, PRIMARY KEY(fullName));");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P1','F1','A1','2020-12-12 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P2','F2','A2','2020-12-13 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P3',null,null,'2020-12-14 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P4',null,'A3','2020-12-15 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P5','F1','A1','2020-12-16 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('SpecialName''1','F2','A2','2020-12-13 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('SpecialName''2','F2','A2','2020-12-13 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('SpecialName&3','F2','A2','2022-12-13 20:00:00');");
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P10','F1','A1','2020-12-17 20:00:00');");

        statement.execute("Drop table if exists Address;");
        statement.execute("Create Table Address(name VARCHAR(100) NOT NULL, PRIMARY KEY(name));");
        statement.execute("insert into Address (name) values ('Hoboken');");
        statement.execute("insert into Address (name) values ('NYC');");

        statement.execute("Drop table if exists Street;");
        statement.execute("Create Table Street(name VARCHAR(100) NOT NULL, address_name VARCHAR(100), PRIMARY KEY(name));");
        statement.execute("insert into Street (name, address_name) values ('Hoboken','Hoboken');");

        statement.execute("Drop schema if exists user_view cascade;");
        statement.execute("create schema user_view;");
        statement.execute("Create Table user_view.UV_User_Roles_Public(CptyrRole VARCHAR(128) NOT NULL,RoleOrder INT NULL,UserID VARCHAR(32) NOT NULL, PRIMARY KEY(CptyrRole,UserID));");
        statement.execute("Create Table user_view.UV_INQUIRY__PL_CADM(rpt_inq_oid BIGINT NOT NULL, rpt_inq_sourceinquiryid VARCHAR(200) NULL, PRIMARY KEY(rpt_inq_oid));");
    }

    @Test
    public void testInExecutionWithStringListAndFilterPushDown() throws JsonProcessingException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[1]\n" +
                "{\n" +
                "{names:String[*]|test::Address.all()->filter(x| $x.name->in($names) && $x.street->in($names))\n" +
                "                                    ->project(x|$x.name, 'addressName')}\n" +
                "}";
        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction, false);
        Map<String, ?> paramWithMultipleValues = Maps.mutable.with("names", Lists.mutable.with("Hoboken", "NYC"));
        String expectedResWithMultipleValues = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"addressName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(255)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"addressName\\\" from Address as \\\"root\\\" left outer join Street as \\\"street_0\\\" on (\\\"root\\\".name = \\\"street_0\\\".address_name and \\\"street_0\\\".address_name in ('Hoboken','NYC')) where (\\\"root\\\".name in ('Hoboken','NYC') and \\\"street_0\\\".name in ('Hoboken','NYC'))\"}],\"result\":{\"columns\":[\"addressName\"],\"rows\":[{\"values\":[\"Hoboken\"]}]}}";//String expectedResWithMultipleValues = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"addressName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(255)\"}]}, \"activities\": [{\"_type\":\"relational\",\"comment\":\"Executed by user [a-zA-Z0-9_.-]* and [0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12} is the relational execution traceID\",\"sql\":\"select \\\"root\\\".name as \\\"addressName\\\" from Address as \\\"root\\\" left outer join Street as \\\"street_0\\\" on (\\\"root\\\".name = \\\"street_0\\\".address_name and \\\"street_0\\\".address_name in ('Hoboken','NYC')) where (\\\"root\\\".name in ('Hoboken','NYC') and \\\"street_0\\\".name in ('Hoboken','NYC'))\"}], \"result\" : {\"columns\" : [\"addressName\"], \"rows\" : [{\"values\": [\"Hoboken\"]}]}}";
        Assert.assertEquals(expectedResWithMultipleValues, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithMultipleValues)));
    }

    @Test
    public void testInExecutionWithString() throws JsonProcessingException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[1]\n" +
                "{\n" +
                "  {name:String[1] | test::Person.all()\n" +
                "                        ->filter(p:test::Person[1] | $p.fullName->in($name))\n" +
                "                        ->project([x | $x.fullName], ['fullName'])}\n" +
                "}";
        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction, false);
        Map<String, ?> params = Maps.mutable.with("name", "P1");
        String expected = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in ('P1')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"P1\"]}]}}";
        Assert.assertEquals(expected, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, params)));

        Map<String, ?> paramsWithQuotes = Maps.mutable.with("name", "SpecialName'1");
        String expectedWithQuotes = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in ('SpecialName''1')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"SpecialName'1\"]}]}}";
        Assert.assertEquals(expectedWithQuotes, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramsWithQuotes)));
    }

    @Test
    public void testInExecutionWithOptionalStringParam() throws JsonProcessingException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[1]\n" +
                "{\n" +
                "  {name:String[0..1] | test::Person.all()\n" +
                "                        ->filter(p:test::Person[1] | $p.fullName->in($name))\n" +
                "                        ->project([x | $x.fullName], ['fullName'])}\n" +
                "}";
        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction, false);

        Map<String, ?> paramWithValue = Maps.mutable.with("name", "P1");
        String expectedWithValue = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in ('P1')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"P1\"]}]}}";
        Assert.assertEquals(expectedWithValue, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithValue)));

        Map<String, ?> paramWithQuotes = Maps.mutable.with("name", "SpecialName'1");
        String expectedWithQuotes = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in ('SpecialName''1')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"SpecialName'1\"]}]}}";
        Assert.assertEquals(expectedWithQuotes, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithQuotes)));

        Map<String, ?> paramWithoutValue = Maps.mutable.with("name", Lists.mutable.empty());
        String expectedWithoutValue = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in (null)\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[]}}";
        Assert.assertEquals(expectedWithoutValue, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithoutValue)));
    }

    @Test
    public void testInExecutionWithStringList() throws JsonProcessingException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[1]\n" +
                "{\n" +
                "  {names:String[*] | test::Person.all()\n" +
                "                        ->filter(p:test::Person[1] | $p.fullName->in($names))\n" +
                "                        ->project([x | $x.fullName], ['fullName'])}\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction, false);
        Map<String, ?> paramWithEmptyList = Maps.mutable.with("names", Lists.mutable.empty());
        Map<String, ?> paramWithSingleValue = Maps.mutable.with("names", Lists.mutable.with("P1"));
        Map<String, ?> paramWithMultipleValues = Maps.mutable.with("names", Lists.mutable.with("P1", "P2"));
        Map<String, ?> paramWithQuotes = Maps.mutable.with("names", Lists.mutable.with("SpecialName'1"));
        Map<String, ?> paramListWithQuotes = Maps.mutable.with("names", Lists.mutable.with("SpecialName'1", "SpecialName'2"));
        Map<String, ?> paramWithAmpersand = Maps.mutable.with("names", Lists.mutable.with("SpecialName&3"));

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in (null)\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[]}}";
        String expectedResWithSingleValue = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in ('P1')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"P1\"]}]}}";
        String expectedResWithMultipleValues = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in ('P1','P2')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"P1\"]},{\"values\":[\"P2\"]}]}}";
        String expectedResWithQuotes = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in ('SpecialName''1')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"SpecialName'1\"]}]}}";
        String expectedResListWithQuotes = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in ('SpecialName''1','SpecialName''2')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"SpecialName'1\"]},{\"values\":[\"SpecialName'2\"]}]}}";
        String expectedResListWithAmpersand = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in ('SpecialName&3')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"SpecialName&3\"]}]}}";

        Assert.assertEquals(expectedResWithEmptyList, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithEmptyList)));
        Assert.assertEquals(expectedResWithSingleValue, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithSingleValue)));
        Assert.assertEquals(expectedResWithMultipleValues, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithMultipleValues)));
        Assert.assertEquals(expectedResWithQuotes, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithQuotes)));
        Assert.assertEquals(expectedResListWithQuotes, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramListWithQuotes)));
        Assert.assertEquals(expectedResListWithAmpersand, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithAmpersand)));
    }

    @Test
    public void testFreeMarkerProcessingForCombinationalPlaceholdersWithSpecialCharactersInPlan() throws Exception
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[1]\n" +
                "{\n" +
                "  {names:String[*], firmName:String[1], birthTime:DateTime[0..1]| test::Person.all()\n" +
                "                        ->filter(p:test::Person[1] | $p.fullName->in($names) && $p.firmName == $firmName && $p.birthTime == $birthTime)\n" +
                "                        ->project([x | $x.fullName], ['fullName'])}\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction, false);
        HashMap queryParameters = new HashMap();
        queryParameters.put("names", org.eclipse.collections.impl.factory.Lists.mutable.with("user1", "user2", "user3"));
        queryParameters.put("firmName", "abcd<@efg");

        //assert that sql string contains placeholders that we need to process conditionally
        ExecutionNode sqlNode  = plan.rootExecutionNode.executionNodes.get(2).executionNodes.get(0);
        String sql = ((SQLExecutionNode)sqlNode).sqlQuery;
        String expectedSQL = "select \"root\".fullName as \"fullName\" from PERSON as \"root\" where ((\"root\".fullName in (${inFilterClause_names}) and \"root\".firmName = '${firmName?replace(\"'\", \"''\")}') and (${optionalVarPlaceHolderOperationSelector(birthTime![], '\"root\".birthTime = ${varPlaceHolderToString(birthTime![] \"TIMESTAMP\\'\" \"\\'\" {} \"null\")}', '\"root\".birthTime is null')}))";

        int h2MajorVersion = H2Manager.getMajorVersion();
        if (h2MajorVersion == LEGACY_H2_VERSION)
        {
            expectedSQL = "select \"root\".fullName as \"fullName\" from PERSON as \"root\" where ((\"root\".fullName in (${inFilterClause_names}) and \"root\".firmName = '${firmName?replace(\"'\", \"''\")}') and (${optionalVarPlaceHolderOperationSelector(birthTime![], '\"root\".birthTime = ${varPlaceHolderToString(birthTime![] \"\\'\" \"\\'\" {} \"null\")}', '\"root\".birthTime is null')}))";
        }
        Assert.assertEquals(expectedSQL, sql);

        //executePlan with freemarker placeholders in sql Query
        String expectedResult = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where ((\\\"root\\\".fullName in ('user1','user2','user3') and \\\"root\\\".firmName = 'abcd<@efg') and (\\\"root\\\".birthTime is null))\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[]}}";
        Assert.assertEquals(expectedResult, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, queryParameters)));

        //check if old flow works as expected
        System.setProperty(overridePropertyForTemplateModel, "true");
        //in old flow, processing "<@" would fail ideally (this was our status quo)
        Assert.assertThrows(RuntimeException.class, () -> RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, queryParameters)));
        System.clearProperty(overridePropertyForTemplateModel);
    }

    @Test
    public void testInExecutionWithIntegerList() throws JsonProcessingException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[1]\n" +
                "{\n" +
                "  {nameLengths:Integer[*] | test::Person.all()\n" +
                "                               ->filter(p:test::Person[1] | $p.fullName->length()->in($nameLengths))\n" +
                "                               ->project([x | $x.fullName], ['fullName'])}\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction, false);
        Map<String, ?> paramWithEmptyList = Maps.mutable.with("nameLengths", Lists.mutable.empty());
        Map<String, ?> paramWithSingleValue = Maps.mutable.with("nameLengths", Lists.mutable.with(2));
        Map<String, ?> paramWithMultipleValues = Maps.mutable.with("nameLengths", Lists.mutable.with(2, 3));

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where char_length(\\\"root\\\".fullName) in (null)\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[]}}";
        String expectedResWithSingleValue = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where char_length(\\\"root\\\".fullName) in (2)\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"P1\"]},{\"values\":[\"P2\"]},{\"values\":[\"P3\"]},{\"values\":[\"P4\"]},{\"values\":[\"P5\"]}]}}";
        String expectedResWithMultipleValues = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where char_length(\\\"root\\\".fullName) in (2,3)\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"P1\"]},{\"values\":[\"P10\"]},{\"values\":[\"P2\"]},{\"values\":[\"P3\"]},{\"values\":[\"P4\"]},{\"values\":[\"P5\"]}]}}";

        Assert.assertEquals(expectedResWithEmptyList, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithEmptyList)));
        Assert.assertEquals(expectedResWithSingleValue, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithSingleValue)));
        Assert.assertEquals(expectedResWithMultipleValues, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithMultipleValues)));
    }

    @Test
    public void testInExecutionWithDateTimeList() throws JsonProcessingException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[1]\n" +
                "{\n" +
                "  {birthTime:DateTime[*] | test::Person.all()\n" +
                "                               ->filter(p:test::Person[1] | $p.birthTime->in($birthTime))\n" +
                "                               ->project([x | $x.fullName], ['fullName'])}\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction, false);
        Map<String, ?> paramWithEmptyList = Maps.mutable.with("birthTime", Lists.mutable.empty());
        Map<String, ?> paramWithSingleValue = Maps.mutable.with("birthTime", Lists.mutable.with("2020-12-12 20:00:00"));
        Map<String, ?> paramWithMultipleValues = Maps.mutable.with("birthTime", Lists.mutable.with("2020-12-12 20:00:00", "2020-12-13 20:00:00"));

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in (null)\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[]}}";
        String expectedResWithSingleValue = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in (TIMESTAMP'2020-12-12 20:00:00')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"P1\"]}]}}";
        String expectedResWithMultipleValues = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in (TIMESTAMP'2020-12-12 20:00:00',TIMESTAMP'2020-12-13 20:00:00')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"P1\"]},{\"values\":[\"P2\"]},{\"values\":[\"SpecialName'1\"]},{\"values\":[\"SpecialName'2\"]}]}}";

        int h2MajorVersion = H2Manager.getMajorVersion();
        if (h2MajorVersion == LEGACY_H2_VERSION)
        {
            expectedResWithSingleValue = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in ('2020-12-12 20:00:00')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"P1\"]}]}}";
            expectedResWithMultipleValues = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in ('2020-12-12 20:00:00','2020-12-13 20:00:00')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"P1\"]},{\"values\":[\"P2\"]},{\"values\":[\"SpecialName'1\"]},{\"values\":[\"SpecialName'2\"]}]}}";
        }

        Assert.assertEquals(expectedResWithEmptyList, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithEmptyList)));
        Assert.assertEquals(expectedResWithSingleValue, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithSingleValue)));
        Assert.assertEquals(expectedResWithMultipleValues, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithMultipleValues)));
    }

    @Test
    public void testInExecutionWithDateTimeListAndTimeZone() throws JsonProcessingException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[1]\n" +
                "{\n" +
                "  {birthTime:DateTime[*] | test::Person.all()\n" +
                "                               ->filter(p:test::Person[1] | $p.birthTime->in($birthTime))\n" +
                "                               ->project([x | $x.fullName], ['fullName'])}\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction, true);
        Map<String, ?> paramWithEmptyList = Maps.mutable.with("birthTime", Lists.mutable.empty());
        Map<String, ?> paramWithSingleValue = Maps.mutable.with("birthTime", Lists.mutable.with("2020-12-13 03:00:00"));
        Map<String, ?> paramWithMultipleValues = Maps.mutable.with("birthTime", Lists.mutable.with("2020-12-13 03:00:00", "2020-12-14 03:00:00"));

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in (null)\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[]}}";
        String expectedResWithSingleValue = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in (TIMESTAMP'2020-12-12 20:00:00')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"P1\"]}]}}";
        String expectedResWithMultipleValues = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in (TIMESTAMP'2020-12-12 20:00:00',TIMESTAMP'2020-12-13 20:00:00')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"P1\"]},{\"values\":[\"P2\"]},{\"values\":[\"SpecialName'1\"]},{\"values\":[\"SpecialName'2\"]}]}}";

        int h2MajorVersion = H2Manager.getMajorVersion();
        if (h2MajorVersion == LEGACY_H2_VERSION)
        {
            expectedResWithSingleValue = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in ('2020-12-12 20:00:00')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"P1\"]}]}}";
            expectedResWithMultipleValues = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in ('2020-12-12 20:00:00','2020-12-13 20:00:00')\"}],\"result\":{\"columns\":[\"fullName\"],\"rows\":[{\"values\":[\"P1\"]},{\"values\":[\"P2\"]},{\"values\":[\"SpecialName'1\"]},{\"values\":[\"SpecialName'2\"]}]}}";
        }

        Assert.assertEquals(expectedResWithEmptyList, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithEmptyList)));
        Assert.assertEquals(expectedResWithSingleValue, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithSingleValue)));
        Assert.assertEquals(expectedResWithMultipleValues, RelationalResultToJsonDefaultSerializer.removeComment(executePlan(plan, paramWithMultipleValues)));
    }

    @Test
    public void testTempTableFlowWithPostProcessor()
    {
        try
        {
            InputStream executionPlanJson = TestPlanExecutionForIn.class.getClassLoader().getResourceAsStream("org/finos/legend/engine/plan/execution/stores/relational/test/full/functions/in/tempTableExecutionPlanWithPostProcessor.json");
            String executionPlanJsonString = IOUtils.toString(executionPlanJson, StandardCharsets.UTF_8);
            SingleExecutionPlan plan = objectMapper.readValue(executionPlanJsonString, SingleExecutionPlan.class);
            String s = executePlan(plan);
            Assert.assertTrue(s.matches("\\{\\\"builder\\\": \\{\\\"_type\\\":\\\"tdsBuilder\\\",\\\"columns\\\":\\[\\{\\\"name\\\":\\\"Source Inquiry ID\\\",\\\"type\\\":\\\"String\\\",\\\"relationalType\\\":\\\"VARCHAR\\(64\\)\\\"}]}, \\\"activities\\\": \\[\\{\\\"_type\\\":\\\"relational\\\",\\\"sql\\\":\\\"SET LOCK_TIMEOUT 100000000;\\\"},\\{\\\"_type\\\":\\\"relational\\\",\\\"sql\\\":\\\"SET LOCK_TIMEOUT 100000000;\\\"},\\{\\\"_type\\\":\\\"relational\\\",\\\"sql\\\":\\\"SET LOCK_TIMEOUT 100000000;\\\"},\\{\\\"_type\\\":\\\"relational\\\",\\\"comment\\\":\\\"-- \\\\\\\"executionTraceID\\\\\\\" : \\\\\\\"[0-9a-f]{8}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{12}\\\\\\\"\\\",\\\"sql\\\":\\\"select case when \\\\\\\"root\\\\\\\".CptyrRole like 'ebusiness\\\\\\\\_%' then 'ebusiness_' else \\\\\\\"root\\\\\\\".CptyrRole end as \\\\\\\"userRole\\\\\\\" from user_view.UV_User_Roles_Public as \\\\\\\"root\\\\\\\" where \\(\\\\\\\"root\\\\\\\".UserID = 'Anonymous' and \\\\\\\"root\\\\\\\".CptyrRole in \\('cadm', 'sales_fg', 'rfq_mgmt', 'ebusiness_credit', 'ebusiness_rates', 'ebusiness_fx', 'ebusiness_commod', 'desk_sales_trading', 'sales_person'\\)\\)\\\"},\\{\\\"_type\\\":\\\"relational\\\",\\\"comment\\\":\\\"-- \\\\\\\"executionTraceID\\\\\\\" : \\\\\\\"[0-9a-f]{8}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{4}\\-[0-9a-f]{12}\\\\\\\"\\\",\\\"sql\\\":\\\"select \\\\\\\"root\\\\\\\".rpt_inq_sourceinquiryid as \\\\\\\"Source Inquiry ID\\\\\\\" from user_view.UV_Inquiry__PL_Cadm as \\\\\\\"root\\\\\\\" where \\\\\\\"root\\\\\\\".rpt_inq_sourceinquiryid in \\(null\\)\\\"}], \\\"result\\\" : \\{\\\"columns\\\" : \\[\\\"Source Inquiry ID\\\"], \\\"rows\\\" : \\[\\]\\}\\}"));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public SingleExecutionPlan buildPlanForFetchFunction(String fetchFunction, boolean withTimeZone)
    {
        return super.buildPlan(LOGICAL_MODEL + STORE_MODEL + MAPPING + RUNTIME + fetchFunction, withTimeZone ? "US/Arizona" : null);
    }
}

