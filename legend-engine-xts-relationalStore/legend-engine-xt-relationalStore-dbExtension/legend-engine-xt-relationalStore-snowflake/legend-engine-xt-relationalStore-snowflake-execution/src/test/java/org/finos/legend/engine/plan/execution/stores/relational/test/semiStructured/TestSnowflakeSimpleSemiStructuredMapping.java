// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.semiStructured;

import org.junit.Assert;
import org.junit.Test;

public class TestSnowflakeSimpleSemiStructuredMapping extends AbstractTestSnowflakeSemiStructured
{
    private static final String snowflakeMapping = "simple::mapping::SnowflakeMapping";
    private static final String snowflakeRuntime = "simple::runtime::SnowflakeRuntime";

    @Test
    public void testSingleSemiStructuredPropertyAccess()
    {
        String queryFunction = "simple::singleSemiStructuredPropertyAccess__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Firm Legal Name, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Firm Legal Name\", \"\")]\n" +
                "      sql = select \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Legal Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Firm Legal Name, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testCombinedPrimitiveAndSemiStructuredPropertyAccessParallel()
    {
        String queryFunction = "simple::combinedPrimitiveAndSemiStructuredPropertyAccessParallel__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Legal Name\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Legal Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testCombinedPrimitiveAndSemiStructuredPropertyAccess()
    {
        String queryFunction = "simple::combinedPrimitiveAndSemiStructuredPropertyAccess__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Out Col, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Out Col\", \"\")]\n" +
                "      sql = select concat(\"root\".FIRSTNAME, ' : ', \"root\".FIRM_DETAILS['legalName']::varchar) as \"Out Col\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Out Col, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testCombinedComplexAndSemiStructuredPropertyAccessParallel()
    {
        String queryFunction = "simple::combinedComplexAndSemiStructuredPropertyAccessParallel__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Manager First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Manager First Name\", VARCHAR(100)), (\"Firm Legal Name\", \"\")]\n" +
                "      sql = select \"person_table_1\".FIRSTNAME as \"Manager First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Legal Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_1\" on (\"root\".MANAGERID = \"person_table_1\".ID)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Manager First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testCombinedComplexAndSemiStructuredPropertyAccess()
    {
        String queryFunction = "simple::combinedComplexAndSemiStructuredPropertyAccess__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Out Col, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Out Col\", \"\")]\n" +
                "      sql = select concat(case when \"person_table_1\".FIRSTNAME is null then 'NULL' else \"person_table_1\".FIRSTNAME end, ' : ', \"root\".FIRM_DETAILS['legalName']::varchar) as \"Out Col\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_1\" on (\"root\".MANAGERID = \"person_table_1\".ID)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Out Col, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testNestedSemiStructuredPropertyAccess()
    {
        String queryFunction = "simple::nestedSemiStructuredPropertyAccess__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Firm Address Name, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Firm Address Name\", \"\")]\n" +
                "      sql = select \"root\".FIRM_DETAILS['address']['name']::varchar as \"Firm Address Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Firm Address Name, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testMultipleSemiStructuredPropertyAccess()
    {
        String queryFunction = "simple::multipleSemiStructuredPropertyAccess__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Firm Legal Name, String, VARCHAR(65536), \"\"), (Firm Address Name, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Firm Legal Name\", \"\"), (\"Firm Address Name\", \"\")]\n" +
                "      sql = select \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Legal Name\", \"root\".FIRM_DETAILS['address']['name']::varchar as \"Firm Address Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Firm Legal Name, String, VARCHAR(65536), \"\"), (Firm Address Name, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testMultipleSemiStructuredPropertyAccessCombined()
    {
        String queryFunction = "simple::multipleSemiStructuredPropertyAccessCombined__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Firm Legal Name And Address Name, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"Firm Legal Name And Address Name\", \"\")]\n" +
                "      sql = select concat(\"root\".FIRM_DETAILS['legalName']::varchar, \"root\".FIRM_DETAILS['address']['name']::varchar) as \"Firm Legal Name And Address Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Firm Legal Name And Address Name, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testFilterWithSemiStructuredProperty()
    {
        String queryFunction = "simple::filterWithSemiStructuredProperty__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" where \"root\".FIRM_DETAILS['legalName']::varchar = 'Firm X'\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testGroupByWithSemiStructuredProperty()
    {
        String queryFunction = "simple::groupByWithSemiStructuredProperty__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Address, String, VARCHAR(65536), \"\"), (Names, String, VARCHAR(200), \"\")]\n" +
                "      resultColumns = [(\"Address\", \"\"), (\"Names\", \"\")]\n" +
                "      sql = select \"root\".FIRM_DETAILS['address']['name']::varchar as \"Address\", listagg(\"root\".FIRSTNAME, ';') as \"Names\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" group by \"Address\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Address, String, VARCHAR(65536), \"\"), (Names, String, VARCHAR(200), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSortByWithSemiStructuredProperty()
    {
        String queryFunction = "simple::sortByWithSemiStructuredProperty__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" order by \"root\".FIRM_DETAILS['legalName']::varchar\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testIsEmptyCheckOnSemiStructuredPrimitivePropertyAccess()
    {
        String queryFunction = "simple::isEmptyCheckOnSemiStructuredPrimitivePropertyAccess__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (First Address Street, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"First Address Street\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", case when \"root\".FIRM_DETAILS['address']['street']::varchar is null then 'NULL' else \"root\".FIRM_DETAILS['address']['street']::varchar end as \"First Address Street\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (First Address Street, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testIsEmptyCheckOnSemiStructuredPropertyAccessAfterAt()
    {
        String queryFunction = "simple::isEmptyCheckOnSemiStructuredPropertyAccessAfterAt__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (First Address Line, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"First Address Line\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", case when \"root\".FIRM_DETAILS['address']['lines'][2]['details']::varchar is null then 'NULL' else \"root\".FIRM_DETAILS['address']['lines'][2]['details']::varchar end as \"First Address Line\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (First Address Line, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredDifferentDataTypePropertyAccess()
    {
        String queryFunction = "simple::semiStructuredDifferentDataTypePropertyAccess__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(65536), \"\"), (Firm Employee Count, Integer, BIGINT, \"\"), (Firm MNC, Boolean, BIT, \"\"), (Firm Est Date, StrictDate, DATE, \"\"), (Firm Last Update, DateTime, TIMESTAMP, \"\"), (Firm Address Street, String, VARCHAR(65536), \"\"), (Firm Entity Type, simple::model::EntityType, \"\", \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Legal Name\", \"\"), (\"Firm Employee Count\", \"\"), (\"Firm MNC\", \"\"), (\"Firm Est Date\", \"\"), (\"Firm Last Update\", \"\"), (\"Firm Address Street\", \"\"), (\"Firm Entity Type\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Legal Name\", \"root\".FIRM_DETAILS['employeeCount'] as \"Firm Employee Count\", \"root\".FIRM_DETAILS['mnc'] as \"Firm MNC\", \"root\".FIRM_DETAILS['estDate']::date as \"Firm Est Date\", \"root\".FIRM_DETAILS['lastUpdate']::timestamp as \"Firm Last Update\", \"root\".FIRM_DETAILS['address']['street']::varchar as \"Firm Address Street\", \"root\".FIRM_DETAILS['entityType']::varchar as \"Firm Entity Type\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(65536), \"\"), (Firm Employee Count, Integer, BIGINT, \"\"), (Firm MNC, Boolean, BIT, \"\"), (Firm Est Date, StrictDate, DATE, \"\"), (Firm Last Update, DateTime, TIMESTAMP, \"\"), (Firm Address Street, String, VARCHAR(65536), \"\"), (Firm Entity Type, simple::model::EntityType, \"\", \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredArrayElementAccessPrimitive()
    {
        String queryFunction = "simple::semiStructuredArrayElementAccessPrimitive__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Other Name 0, String, VARCHAR(65536), \"\"), (Firm Other Name 1, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Other Name 0\", \"\"), (\"Firm Other Name 1\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['otherNames'][0]::varchar as \"Firm Other Name 0\", \"root\".FIRM_DETAILS['otherNames'][1]::varchar as \"Firm Other Name 1\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Other Name 0, String, VARCHAR(65536), \"\"), (Firm Other Name 1, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredArrayElementAccessComplex()
    {
        String queryFunction = "simple::semiStructuredArrayElementAccessComplex__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address Line 0, String, VARCHAR(65536), \"\"), (Firm Address Line 1, String, VARCHAR(65536), \"\"), (Firm Address Line 2, String, VARCHAR(65536), \"\"), (Firm Address Line 3, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Address Line 0\", \"\"), (\"Firm Address Line 1\", \"\"), (\"Firm Address Line 2\", \"\"), (\"Firm Address Line 3\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['address']['lines'][0]['details']::varchar as \"Firm Address Line 0\", \"root\".FIRM_DETAILS['address']['lines'][1]['details']::varchar as \"Firm Address Line 1\", \"root\".FIRM_DETAILS['address']['lines'][2]['details']::varchar as \"Firm Address Line 2\", \"root\".FIRM_DETAILS['address']['lines'][3]['details']::varchar as \"Firm Address Line 3\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address Line 0, String, VARCHAR(65536), \"\"), (Firm Address Line 1, String, VARCHAR(65536), \"\"), (Firm Address Line 2, String, VARCHAR(65536), \"\"), (Firm Address Line 3, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredPropertyAccessAtNestedProperty()
    {
        String queryFunction = "simple::semiStructuredPropertyAccessAtNestedProperty__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(65536), \"\"), (Manager Firm Name, String, VARCHAR(65536), \"\"), (Manager Manager Firm Name, String, VARCHAR(65536), \"\"), (Manager Manager Manager Firm Name, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Manager Firm Name\", \"\"), (\"Manager Manager Firm Name\", \"\"), (\"Manager Manager Manager Firm Name\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"person_table_1\".FIRM_DETAILS['legalName']::varchar as \"Manager Firm Name\", \"person_table_2\".FIRM_DETAILS['legalName']::varchar as \"Manager Manager Firm Name\", \"person_table_3\".FIRM_DETAILS['legalName']::varchar as \"Manager Manager Manager Firm Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_1\" on (\"root\".MANAGERID = \"person_table_1\".ID) left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_2\" on (\"person_table_1\".MANAGERID = \"person_table_2\".ID) left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_3\" on (\"person_table_2\".MANAGERID = \"person_table_3\".ID)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(65536), \"\"), (Manager Firm Name, String, VARCHAR(65536), \"\"), (Manager Manager Firm Name, String, VARCHAR(65536), \"\"), (Manager Manager Manager Firm Name, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredPropertyAccessAtNestedPropertyWithProjectFunctions()
    {
        String queryFunction = "simple::semiStructuredPropertyAccessAtNestedPropertyWithProjectFunctions__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(65536), \"\"), (Manager Firm Name, String, VARCHAR(65536), \"\"), (Manager Manager Firm Name, String, VARCHAR(65536), \"\"), (Manager Manager Manager Firm Name, String, VARCHAR(65536), \"\")]\n" +
                        "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Manager Firm Name\", \"\"), (\"Manager Manager Firm Name\", \"\"), (\"Manager Manager Manager Firm Name\", \"\")]\n" +
                        "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"person_table_1\".FIRM_DETAILS['legalName']::varchar as \"Manager Firm Name\", \"person_table_2\".FIRM_DETAILS['legalName']::varchar as \"Manager Manager Firm Name\", \"person_table_3\".FIRM_DETAILS['legalName']::varchar as \"Manager Manager Manager Firm Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_1\" on (\"root\".MANAGERID = \"person_table_1\".ID) left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_2\" on (\"person_table_1\".MANAGERID = \"person_table_2\".ID) left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_3\" on (\"person_table_2\".MANAGERID = \"person_table_3\".ID)\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(65536), \"\"), (Manager Firm Name, String, VARCHAR(65536), \"\"), (Manager Manager Firm Name, String, VARCHAR(65536), \"\"), (Manager Manager Manager Firm Name, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testFilterWithSemiStructuredPropertyAccessAtNestedProperty()
    {
        String queryFunction = "simple::filterWithSemiStructuredPropertyAccessAtNestedProperty__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_1\" on (\"root\".MANAGERID = \"person_table_1\".ID) left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_2\" on (\"person_table_1\".MANAGERID = \"person_table_2\".ID) where \"person_table_2\".FIRM_DETAILS['legalName']::varchar = 'Firm X'\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testIfElseLogicOnEnumProperties()
    {
        String queryFunction = "simple::ifElseLogicOnEnumProperties__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Enum Return, simple::model::EntityType, \"\", \"\")]\n" +
                "      resultColumns = [(\"Enum Return\", \"\")]\n" +
                "      sql = select case when \"root\".FIRSTNAME = 'John' then \"root\".FIRM_DETAILS['entityType']::varchar else \"root\".FIRM_DETAILS['entityType']::varchar end as \"Enum Return\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Enum Return, simple::model::EntityType, \"\", \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testFilterOnEnumPropertyWithEnumConst()
    {
        String queryFunction = "simple::filterOnEnumPropertyWithEnumConst__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" where \"root\".FIRM_DETAILS['entityType']::varchar = 'Organization'\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testFilterOnEnumPropertyWithStringConst()
    {
        String queryFunction = "simple::filterOnEnumPropertyWithStringConst__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" where \"root\".FIRM_DETAILS['entityType']::varchar = 'Organization'\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testFilterOnEnumPropertyWithWithIfElseLogicEnum()
    {
        String queryFunction = "simple::filterOnEnumPropertyWithIfElseLogicEnum__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                        "    (\n" +
                        "      type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                        "      resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                        "      sql = select \"root\".FIRSTNAME as \"First Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" where case when \"root\".FIRSTNAME = 'John' then \"root\".FIRM_DETAILS['entityType']::varchar else \"root\".FIRM_DETAILS['entityType']::varchar end = 'Organization'\n" +
                        "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                        "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testGroupByOnEnumProperty()
    {
        String queryFunction = "simple::groupByOnEnumProperty__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(Address, simple::model::EntityType, \"\", \"\"), (Names, String, VARCHAR(200), \"\")]\n" +
                "      resultColumns = [(\"Address\", \"\"), (\"Names\", \"\")]\n" +
                "      sql = select \"root\".FIRM_DETAILS['entityType']::varchar as \"Address\", listagg(\"root\".FIRSTNAME, ';') as \"Names\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" group by \"Address\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(Address, simple::model::EntityType, \"\", \"\"), (Names, String, VARCHAR(200), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSortByOnEnumProperty()
    {
        String queryFunction = "simple::sortByOnEnumProperty__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" order by \"root\".FIRM_DETAILS['entityType']::varchar\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/simpleSemiStructuredMapping.pure";
    }
}
