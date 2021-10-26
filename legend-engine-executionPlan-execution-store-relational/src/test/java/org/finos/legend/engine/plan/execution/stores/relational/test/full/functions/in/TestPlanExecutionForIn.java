package org.finos.legend.engine.plan.execution.stores.relational.test.full.functions.in;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.execution.stores.relational.connection.AlloyTestServer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public class TestPlanExecutionForIn extends AlloyTestServer
{

    private static final String LOGICAL_MODEL = "###Pure\n" +
            "Class test::Person\n" +
            "{\n" +
            "  fullName: String[1];\n" +
            "  birthTime: DateTime[0..1];\n" +
            "}\n\n\n";

    private static final String STORE_MODEL = "###Relational\n" +
            "Database test::DB\n" +
            "(\n" +
            "  Table PERSON (\n" +
            "    fullName VARCHAR(100) PRIMARY KEY,\n" +
            "    firmName VARCHAR(100),\n" +
            "    addressName VARCHAR(100),\n" +
            "    birthTime TIMESTAMP\n" +
            "  )\n" +
            ")\n\n\n";

    private static final String MAPPING = "###Mapping\n" +
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
            "    birthTime: [test::DB]PERSON.birthTime\n" +
            "  }\n" +
            ")\n\n\n";

    private static final String RUNTIME = "###Runtime\n" +
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
        statement.execute("insert into PERSON (fullName,firmName,addressName,birthTime) values ('P10','F1','A1','2020-12-17 20:00:00');");
    }

    @Test
    public void testInExecutionWithStringList()
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

        String expectedResWithEmptyList = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in (null)\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : []}}";
        String expectedResWithSingleValue = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in ('P1')\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]}]}}";
        String expectedResWithMultipleValues = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in ('P1','P2')\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]},{\"values\": [\"P2\"]}]}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan, paramWithEmptyList));
        Assert.assertEquals(expectedResWithSingleValue, executePlan(plan, paramWithSingleValue));
        Assert.assertEquals(expectedResWithMultipleValues, executePlan(plan, paramWithMultipleValues));
    }

    @Test
    public void testInExecutionWithIntegerList()
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

        String expectedResWithEmptyList = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where char_length(\\\"root\\\".fullName) in (null)\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : []}}";
        String expectedResWithSingleValue = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where char_length(\\\"root\\\".fullName) in (2)\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]},{\"values\": [\"P2\"]},{\"values\": [\"P3\"]},{\"values\": [\"P4\"]},{\"values\": [\"P5\"]}]}}";
        String expectedResWithMultipleValues = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where char_length(\\\"root\\\".fullName) in (2,3)\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]},{\"values\": [\"P10\"]},{\"values\": [\"P2\"]},{\"values\": [\"P3\"]},{\"values\": [\"P4\"]},{\"values\": [\"P5\"]}]}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan, paramWithEmptyList));
        Assert.assertEquals(expectedResWithSingleValue, executePlan(plan, paramWithSingleValue));
        Assert.assertEquals(expectedResWithMultipleValues, executePlan(plan, paramWithMultipleValues));
    }

    @Test
    public void testInExecutionWithDateTimeList()
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

        String expectedResWithEmptyList = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in (null)\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : []}}";
        String expectedResWithSingleValue = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in ('2020-12-12 20:00:00')\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]}]}}";
        String expectedResWithMultipleValues = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in ('2020-12-12 20:00:00','2020-12-13 20:00:00')\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]},{\"values\": [\"P2\"]}]}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan, paramWithEmptyList));
        Assert.assertEquals(expectedResWithSingleValue, executePlan(plan, paramWithSingleValue));
        Assert.assertEquals(expectedResWithMultipleValues, executePlan(plan, paramWithMultipleValues));
    }

    @Test
    public void testInExecutionWithDateTimeListAndTimeZone()
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

        String expectedResWithEmptyList = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in (null)\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : []}}";
        String expectedResWithSingleValue = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in ('2020-12-12 20:00:00')\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]}]}}";
        String expectedResWithMultipleValues = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in ('2020-12-12 20:00:00','2020-12-13 20:00:00')\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]},{\"values\": [\"P2\"]}]}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan, paramWithEmptyList));
        Assert.assertEquals(expectedResWithSingleValue, executePlan(plan, paramWithSingleValue));
        Assert.assertEquals(expectedResWithMultipleValues, executePlan(plan, paramWithMultipleValues));
    }


    private SingleExecutionPlan buildPlanForFetchFunction(String fetchFunction, boolean withTimeZone)
    {
        return super.buildPlan( LOGICAL_MODEL + STORE_MODEL + MAPPING + RUNTIME + fetchFunction, withTimeZone ? "US/Arizona" : null);
    }

}

