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
import org.junit.Ignore;
import org.junit.Test;

public class TestSemiStructuredFlattening extends AbstractTestSemiStructured
{
    private static final String snowflakeMapping = "flatten::mapping::SnowflakeMapping";
    private static final String snowflakeRuntime = "flatten::runtime::SnowflakeRuntime";

    @Test
    public void testSemiStructuredPrimitivePropertyFlattening()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredPrimitivePropertyFlattening", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, \"\", \"\"), (Firm Other Name, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Other Name\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"ss_flatten_0\".value::varchar as \"Firm Other Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join lateral flatten(input => \"root\".FIRM_DETAILS['otherNames']::varchar, outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" on (1 = 1)\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);
    }

    @Test
    public void testSemiStructuredComplexPropertyFlattening()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredComplexPropertyFlattening", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, \"\", \"\"), (Firm Address Name, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Address Name\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"ss_flatten_0\".value['name']::varchar as \"Firm Address Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join lateral flatten(input => \"root\".FIRM_DETAILS['addresses'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" on (1 = 1)\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);
    }

    @Test
    public void testSemiStructuredPrimitivePropertyArrayIndexing()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredPrimitivePropertyArrayIndexing", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, \"\", \"\"), (Firm Other Name 0, String, \"\", \"\"), (Firm Other Name 2, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Other Name 0\", \"\"), (\"Firm Other Name 2\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"root\".FIRM_DETAILS['otherNames'][0]::varchar as \"Firm Other Name 0\", \"root\".FIRM_DETAILS['otherNames'][2]::varchar as \"Firm Other Name 2\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);
    }

    @Test
    public void testSemiStructuredComplexPropertyArrayIndexing()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredComplexPropertyArrayIndexing", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, \"\", \"\"), (Firm Address 0 Name, String, \"\", \"\"), (Firm Address 2 Name, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Address 0 Name\", \"\"), (\"Firm Address 2 Name\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"root\".FIRM_DETAILS['addresses'][0]['name']::varchar as \"Firm Address 0 Name\", \"root\".FIRM_DETAILS['addresses'][2]['name']::varchar as \"Firm Address 2 Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);
    }

    @Test
    public void testSemiStructuredComplexPropertyFlatteningFollowedBySubType()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredComplexPropertyFlatteningFollowedBySubType", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, \"\", \"\"), (Firm Address Line 0 Line No, Integer, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Address Line 0 Line No\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"ss_flatten_0\".value['lines'][0]['lineno'] as \"Firm Address Line 0 Line No\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join lateral flatten(input => \"root\".FIRM_DETAILS['addresses'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" on (1 = 1)\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);
    }

    @Test
    public void testSemiStructuredComplexPropertyArrayIndexingFollowedBySubType()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredComplexPropertyArrayIndexingFollowedBySubType", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, \"\", \"\"), (Firm Address 0 Line 0 Line No, Integer, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Address 0 Line 0 Line No\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"root\".FIRM_DETAILS['addresses'][0]['lines'][0]['lineno'] as \"Firm Address 0 Line 0 Line No\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);
    }

    @Test
    public void testSemiStructuredPrimitivePropertyFiltering()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredPrimitivePropertyFiltering", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\")]\n" +
                "  sql = select distinct \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join lateral flatten(input => \"root\".FIRM_DETAILS['otherNames']::varchar, outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" on (1 = 1) where \"ss_flatten_0\".value::varchar = 'A'\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);
    }

    @Test
    public void testSemiStructuredPrimitivePropertyFilteringInProject()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredPrimitivePropertyFilteringInProject", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, \"\", \"\"), (Firm Other Name, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Other Name\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"ss_flatten_0\".value::varchar as \"Firm Other Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join lateral flatten(input => \"root\".FIRM_DETAILS['otherNames']::varchar, outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" on (1 = 1 and \"ss_flatten_0\".value::varchar like 'A%')\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);
    }

    @Test
    @Ignore
    public void testSemiStructuredComplexPropertyFiltering()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredComplexPropertyFiltering", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);
    }

    @Test
    public void testSemiStructuredComplexPropertyFilteringInProject()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredComplexPropertyFilteringInProject", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, \"\", \"\"), (Firm Address Name 1, String, \"\", \"\"), (Firm Address Name 2, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Address Name 1\", \"\"), (\"Firm Address Name 2\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"ss_flatten_0\".value['name']::varchar as \"Firm Address Name 1\", \"ss_flatten_1\".value['name']::varchar as \"Firm Address Name 2\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join lateral flatten(input => \"root\".FIRM_DETAILS['addresses'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" on (1 = 1 and \"ss_flatten_0\".value['name']::varchar = 'A') left outer join lateral flatten(input => \"root\".FIRM_DETAILS['addresses'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_1\" on (1 = 1 and \"ss_flatten_1\".value['name']::varchar = 'B')\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);
    }

    @Test
    public void testSemiStructuredSubAggregation()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredSubAggregation", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, \"\", \"\"), (Firm Address Names, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Address Names\", INT)]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"person_table_1\".aggCol as \"Firm Address Names\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join (select \"person_table_2\".ID as ID, listagg(\"ss_flatten_0\".value['name']::varchar, ';') as aggCol from PERSON_SCHEMA.PERSON_TABLE as \"person_table_2\" left outer join lateral flatten(input => \"person_table_2\".FIRM_DETAILS['addresses'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" on (1 = 1) group by \"person_table_2\".ID) as \"person_table_1\" on (\"root\".ID = \"person_table_1\".ID)\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);
    }

    @Test
    public void testSemiStructuredSubAggregationDeep()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredSubAggregationDeep", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, \"\", \"\"), (Firm Address Line No Sum, Integer, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Address Line No Sum\", INT)]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"person_table_1\".aggCol as \"Firm Address Line No Sum\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join (select \"person_table_2\".ID as ID, sum(\"ss_flatten_1\".value['lineno']) as aggCol from PERSON_SCHEMA.PERSON_TABLE as \"person_table_2\" left outer join lateral flatten(input => \"person_table_2\".FIRM_DETAILS['addresses'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" on (1 = 1) left outer join lateral flatten(input => \"ss_flatten_0\".value['lines'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_1\" on (1 = 1) group by \"person_table_2\".ID) as \"person_table_1\" on (\"root\".ID = \"person_table_1\".ID)\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);
    }


    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredFlattening.pure";
    }
}
