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

public class TestSimpleSemiStructuredMapping extends AbstractTestSemiStructured
{
    private static final String snowflakeMapping = "simple::mapping::SnowflakeMapping";
    private static final String snowflakeRuntime = "simple::runtime::SnowflakeRuntime";

    private static final String h2Mapping = "simple::mapping::H2Mapping";
    private static final String h2Runtime = "simple::runtime::H2Runtime";

    @Test
    public void testSingleSemiStructuredPropertyAccess()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::singleSemiStructuredPropertyAccess", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(Firm Legal Name, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"Firm Legal Name\", \"\")]\n" +
                "  sql = select \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Legal Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::singleSemiStructuredPropertyAccess", h2Mapping, h2Runtime);
        Assert.assertEquals("Firm X\n" +
                "Firm X\n" +
                "Firm X\n" +
                "Firm X\n" +
                "Firm A\n" +
                "Firm B\n" +
                "Firm B\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testCombinedPrimitiveAndSemiStructuredPropertyAccessParallel()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::combinedPrimitiveAndSemiStructuredPropertyAccessParallel", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Legal Name\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Legal Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::combinedPrimitiveAndSemiStructuredPropertyAccessParallel", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X\n" +
                "John,Firm X\n" +
                "John,Firm X\n" +
                "Anthony,Firm X\n" +
                "Fabrice,Firm A\n" +
                "Oliver,Firm B\n" +
                "David,Firm B\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testCombinedPrimitiveAndSemiStructuredPropertyAccess()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::combinedPrimitiveAndSemiStructuredPropertyAccess", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(Out Col, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"Out Col\", \"\")]\n" +
                "  sql = select concat(\"root\".FIRSTNAME, ' : ', \"root\".FIRM_DETAILS['legalName']::varchar) as \"Out Col\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::combinedPrimitiveAndSemiStructuredPropertyAccess", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter : Firm X\n" +
                "John : Firm X\n" +
                "John : Firm X\n" +
                "Anthony : Firm X\n" +
                "Fabrice : Firm A\n" +
                "Oliver : Firm B\n" +
                "David : Firm B\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testCombinedComplexAndSemiStructuredPropertyAccessParallel()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::combinedComplexAndSemiStructuredPropertyAccessParallel", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(Manager First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"Manager First Name\", VARCHAR(100)), (\"Firm Legal Name\", \"\")]\n" +
                "  sql = select \"person_table_1\".FIRSTNAME as \"Manager First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Legal Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_1\" on (\"root\".MANAGERID = \"person_table_1\".ID)\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::combinedComplexAndSemiStructuredPropertyAccessParallel", h2Mapping, h2Runtime);
        Assert.assertEquals("John,Firm X\n" +
                "Anthony,Firm X\n" +
                "John,Firm X\n" +
                ",Firm X\n" +
                ",Firm A\n" +
                "David,Firm B\n" +
                ",Firm B\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testCombinedComplexAndSemiStructuredPropertyAccess()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::combinedComplexAndSemiStructuredPropertyAccess", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(Out Col, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"Out Col\", \"\")]\n" +
                "  sql = select concat(case when \"person_table_1\".FIRSTNAME is null then 'NULL' else \"person_table_1\".FIRSTNAME end, ' : ', \"root\".FIRM_DETAILS['legalName']::varchar) as \"Out Col\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_1\" on (\"root\".MANAGERID = \"person_table_1\".ID)\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::combinedComplexAndSemiStructuredPropertyAccess", h2Mapping, h2Runtime);
        Assert.assertEquals("John : Firm X\n" +
                "Anthony : Firm X\n" +
                "John : Firm X\n" +
                "NULL : Firm X\n" +
                "NULL : Firm A\n" +
                "David : Firm B\n" +
                "NULL : Firm B\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testNestedSemiStructuredPropertyAccess()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::nestedSemiStructuredPropertyAccess", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(Firm Address Name, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"Firm Address Name\", \"\")]\n" +
                "  sql = select \"root\".FIRM_DETAILS['address']['name']::varchar as \"Firm Address Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::nestedSemiStructuredPropertyAccess", h2Mapping, h2Runtime);
        Assert.assertEquals("A1\n" +
                "A1\n" +
                "A1\n" +
                "A1\n" +
                "A2\n" +
                "A3\n" +
                "A3\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testMultipleSemiStructuredPropertyAccess()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::multipleSemiStructuredPropertyAccess", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(Firm Legal Name, String, \"\", \"\"), (Firm Address Name, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"Firm Legal Name\", \"\"), (\"Firm Address Name\", \"\")]\n" +
                "  sql = select \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Legal Name\", \"root\".FIRM_DETAILS['address']['name']::varchar as \"Firm Address Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::multipleSemiStructuredPropertyAccess", h2Mapping, h2Runtime);
        Assert.assertEquals("Firm X,A1\n" +
                "Firm X,A1\n" +
                "Firm X,A1\n" +
                "Firm X,A1\n" +
                "Firm A,A2\n" +
                "Firm B,A3\n" +
                "Firm B,A3\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testMultipleSemiStructuredPropertyAccessCombined()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::multipleSemiStructuredPropertyAccessCombined", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(Firm Legal Name And Address Name, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"Firm Legal Name And Address Name\", \"\")]\n" +
                "  sql = select concat(\"root\".FIRM_DETAILS['legalName']::varchar, \"root\".FIRM_DETAILS['address']['name']::varchar) as \"Firm Legal Name And Address Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::multipleSemiStructuredPropertyAccessCombined", h2Mapping, h2Runtime);
        Assert.assertEquals("Firm XA1\n" +
                "Firm XA1\n" +
                "Firm XA1\n" +
                "Firm XA1\n" +
                "Firm AA2\n" +
                "Firm BA3\n" +
                "Firm BA3\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testFilterWithSemiStructuredProperty()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::filterWithSemiStructuredProperty", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" where \"root\".FIRM_DETAILS['legalName']::varchar = 'Firm X'\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::filterWithSemiStructuredProperty", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter\n" +
                "John\n" +
                "John\n" +
                "Anthony\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testGroupByWithSemiStructuredProperty()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::groupByWithSemiStructuredProperty", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(Address, String, \"\", \"\"), (Names, String, VARCHAR(200), \"\")]\n" +
                "  resultColumns = [(\"Address\", \"\"), (\"Names\", \"\")]\n" +
                "  sql = select \"root\".FIRM_DETAILS['address']['name']::varchar as \"Address\", listagg(\"root\".FIRSTNAME, ';') as \"Names\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" group by \"root\".FIRM_DETAILS['address']['name']::varchar\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::groupByWithSemiStructuredProperty", h2Mapping, h2Runtime);
        Assert.assertEquals("A1,Peter;John;John;Anthony\n" +
                "A2,Fabrice\n" +
                "A3,Oliver;David\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSortByWithSemiStructuredProperty()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::sortByWithSemiStructuredProperty", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" order by \"root\".FIRM_DETAILS['legalName']::varchar\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::sortByWithSemiStructuredProperty", h2Mapping, h2Runtime);
        Assert.assertEquals("Fabrice\n" +
                "Oliver\n" +
                "David\n" +
                "Peter\n" +
                "John\n" +
                "John\n" +
                "Anthony\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testIsEmptyCheckOnSemiStructuredPrimitivePropertyAccess()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::isEmptyCheckOnSemiStructuredPrimitivePropertyAccess", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (First Address Street, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"First Address Street\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", case when \"root\".FIRM_DETAILS['address']['street']::varchar is null then 'NULL' else \"root\".FIRM_DETAILS['address']['street']::varchar end as \"First Address Street\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::isEmptyCheckOnSemiStructuredPrimitivePropertyAccess", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,S1\n" +
                "John,S1\n" +
                "John,S1\n" +
                "Anthony,S1\n" +
                "Fabrice,NULL\n" +
                "Oliver,S2\n" +
                "David,NULL\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testIsEmptyCheckOnSemiStructuredPropertyAccessAfterAt()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::isEmptyCheckOnSemiStructuredPropertyAccessAfterAt", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (First Address Line, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"First Address Line\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", case when \"root\".FIRM_DETAILS['address']['lines'][2]['details']::varchar is null then 'NULL' else \"root\".FIRM_DETAILS['address']['lines'][2]['details']::varchar end as \"First Address Line\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::isEmptyCheckOnSemiStructuredPropertyAccessAfterAt", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,D3\n" +
                "John,D3\n" +
                "John,D3\n" +
                "Anthony,D3\n" +
                "Fabrice,NULL\n" +
                "Oliver,NULL\n" +
                "David,NULL\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredDifferentDataTypePropertyAccess()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::semiStructuredDifferentDataTypePropertyAccess", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, \"\", \"\"), (Firm Employee Count, Integer, \"\", \"\"), (Firm MNC, Boolean, \"\", \"\"), (Firm Est Date, StrictDate, \"\", \"\"), (Firm Last Update, DateTime, \"\", \"\"), (Firm Address Street, String, \"\", \"\"), (Firm Entity Type, simple::model::EntityType, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Legal Name\", \"\"), (\"Firm Employee Count\", \"\"), (\"Firm MNC\", \"\"), (\"Firm Est Date\", \"\"), (\"Firm Last Update\", \"\"), (\"Firm Address Street\", \"\"), (\"Firm Entity Type\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Legal Name\", \"root\".FIRM_DETAILS['employeeCount'] as \"Firm Employee Count\", \"root\".FIRM_DETAILS['mnc'] as \"Firm MNC\", \"root\".FIRM_DETAILS['estDate']::date as \"Firm Est Date\", \"root\".FIRM_DETAILS['lastUpdate']::timestamp as \"Firm Last Update\", \"root\".FIRM_DETAILS['address']['street']::varchar as \"Firm Address Street\", \"root\".FIRM_DETAILS['entityType']::varchar as \"Firm Entity Type\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::semiStructuredDifferentDataTypePropertyAccess", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,4,true,2010-03-04,2022-01-16T01:00:00.000000000+0000,S1,Organization\n" +
                "John,Firm X,4,true,2010-03-04,2022-01-16T01:00:00.000000000+0000,S1,Organization\n" +
                "John,Firm X,4,true,2010-03-04,2022-01-16T01:00:00.000000000+0000,S1,Organization\n" +
                "Anthony,Firm X,4,true,2010-03-04,2022-01-16T01:00:00.000000000+0000,S1,Organization\n" +
                "Fabrice,Firm A,1,false,2012-11-13,2022-02-14T03:00:00.000000000+0000,,\n" +
                "Oliver,Firm B,2,true,2017-07-07,2022-09-01T06:00:00.000000000+0000,S2,Company\n" +
                "David,Firm B,2,true,2017-07-07,2022-09-01T06:00:00.000000000+0000,,Company\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredArrayElementAccessPrimitive()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::semiStructuredArrayElementAccessPrimitive", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Other Name 0, String, \"\", \"\"), (Firm Other Name 1, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Other Name 0\", \"\"), (\"Firm Other Name 1\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['otherNames'][0]::varchar as \"Firm Other Name 0\", \"root\".FIRM_DETAILS['otherNames'][1]::varchar as \"Firm Other Name 1\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::semiStructuredArrayElementAccessPrimitive", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,O1,O2\n" +
                "John,O1,O2\n" +
                "John,O1,O2\n" +
                "Anthony,O1,O2\n" +
                "Fabrice,O3,O4\n" +
                "Oliver,O5,O6\n" +
                "David,O5,O6\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredArrayElementAccessComplex()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::semiStructuredArrayElementAccessComplex", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address Line 0, String, \"\", \"\"), (Firm Address Line 1, String, \"\", \"\"), (Firm Address Line 2, String, \"\", \"\"), (Firm Address Line 3, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Address Line 0\", \"\"), (\"Firm Address Line 1\", \"\"), (\"Firm Address Line 2\", \"\"), (\"Firm Address Line 3\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['address']['lines'][0]['details']::varchar as \"Firm Address Line 0\", \"root\".FIRM_DETAILS['address']['lines'][1]['details']::varchar as \"Firm Address Line 1\", \"root\".FIRM_DETAILS['address']['lines'][2]['details']::varchar as \"Firm Address Line 2\", \"root\".FIRM_DETAILS['address']['lines'][3]['details']::varchar as \"Firm Address Line 3\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::semiStructuredArrayElementAccessComplex", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,D1,D2,D3,\n" +
                "John,D1,D2,D3,\n" +
                "John,D1,D2,D3,\n" +
                "Anthony,D1,D2,D3,\n" +
                "Fabrice,D4,D5,,\n" +
                "Oliver,D5,D6,,\n" +
                "David,D5,D6,,\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredPropertyAccessAtNestedProperty()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::semiStructuredPropertyAccessAtNestedProperty", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, \"\", \"\"), (Manager Firm Name, String, \"\", \"\"), (Manager Manager Firm Name, String, \"\", \"\"), (Manager Manager Manager Firm Name, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Manager Firm Name\", \"\"), (\"Manager Manager Firm Name\", \"\"), (\"Manager Manager Manager Firm Name\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"person_table_1\".FIRM_DETAILS['legalName']::varchar as \"Manager Firm Name\", \"person_table_2\".FIRM_DETAILS['legalName']::varchar as \"Manager Manager Firm Name\", \"person_table_3\".FIRM_DETAILS['legalName']::varchar as \"Manager Manager Manager Firm Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_1\" on (\"root\".MANAGERID = \"person_table_1\".ID) left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_2\" on (\"person_table_1\".MANAGERID = \"person_table_2\".ID) left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_3\" on (\"person_table_2\".MANAGERID = \"person_table_3\".ID)\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::semiStructuredPropertyAccessAtNestedProperty", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,Firm X,Firm X,\n" +
                "John,Firm X,Firm X,,\n" +
                "John,Firm X,Firm X,Firm X,\n" +
                "Anthony,Firm X,,,\n" +
                "Fabrice,Firm A,,,\n" +
                "Oliver,Firm B,Firm B,,\n" +
                "David,Firm B,,,\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testFilterWithSemiStructuredPropertyAccessAtNestedProperty()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::filterWithSemiStructuredPropertyAccessAtNestedProperty", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_1\" on (\"root\".MANAGERID = \"person_table_1\".ID) left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_2\" on (\"person_table_1\".MANAGERID = \"person_table_2\".ID) where \"person_table_2\".FIRM_DETAILS['legalName']::varchar = 'Firm X'\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::filterWithSemiStructuredPropertyAccessAtNestedProperty", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter\n" +
                "John\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testIfElseLogicOnEnumProperties()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::ifElseLogicOnEnumProperties", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(Enum Return, simple::model::EntityType, \"\", \"\")]\n" +
                "  resultColumns = [(\"Enum Return\", \"\")]\n" +
                "  sql = select case when \"root\".FIRSTNAME = 'John' then \"root\".FIRM_DETAILS['entityType']::varchar else \"root\".FIRM_DETAILS['entityType']::varchar end as \"Enum Return\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::ifElseLogicOnEnumProperties", h2Mapping, h2Runtime);
        Assert.assertEquals("Organization\n" +
                "Organization\n" +
                "Organization\n" +
                "Organization\n" +
                "\n" +
                "Company\n" +
                "Company\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testFilterOnEnumPropertyWithEnumConst()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::filterOnEnumPropertyWithEnumConst", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" where \"root\".FIRM_DETAILS['entityType']::varchar = 'Organization'\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::filterOnEnumPropertyWithEnumConst", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter\n" +
                "John\n" +
                "John\n" +
                "Anthony\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testFilterOnEnumPropertyWithStringConst()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::filterOnEnumPropertyWithStringConst", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" where \"root\".FIRM_DETAILS['entityType']::varchar = 'Organization'\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::filterOnEnumPropertyWithStringConst", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter\n" +
                "John\n" +
                "John\n" +
                "Anthony\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testGroupByOnEnumProperty()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::groupByOnEnumProperty", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(Address, simple::model::EntityType, \"\", \"\"), (Names, String, VARCHAR(200), \"\")]\n" +
                "  resultColumns = [(\"Address\", \"\"), (\"Names\", \"\")]\n" +
                "  sql = select \"root\".FIRM_DETAILS['entityType']::varchar as \"Address\", listagg(\"root\".FIRSTNAME, ';') as \"Names\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" group by \"root\".FIRM_DETAILS['entityType']::varchar\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::groupByOnEnumProperty", h2Mapping, h2Runtime);
        Assert.assertEquals(",Fabrice\n" +
                "Company,Oliver;David\n" +
                "Organization,Peter;John;John;Anthony\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSortByOnEnumProperty()
    {
        String snowflakePlan = this.buildExecutionPlanString("simple::sortByOnEnumProperty", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100))]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" order by \"root\".FIRM_DETAILS['entityType']::varchar\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("simple::sortByOnEnumProperty", h2Mapping, h2Runtime);
        Assert.assertEquals("Fabrice\n" +
                "Oliver\n" +
                "David\n" +
                "Peter\n" +
                "John\n" +
                "John\n" +
                "Anthony\n", h2Result.replace("\r\n", "\n"));
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/simpleSemiStructuredMapping.pure";
    }
}
