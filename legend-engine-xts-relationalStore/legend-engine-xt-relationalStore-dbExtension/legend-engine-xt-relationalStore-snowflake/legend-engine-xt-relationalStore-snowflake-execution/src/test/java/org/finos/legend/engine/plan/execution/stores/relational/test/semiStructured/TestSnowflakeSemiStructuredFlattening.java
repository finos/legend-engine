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

import static org.junit.Assert.assertThrows;

public class TestSnowflakeSemiStructuredFlattening extends AbstractTestSnowflakeSemiStructured
{
    private static final String snowflakeMapping = "flatten::mapping::SnowflakeMapping";
    private static final String snowflakeRuntime = "flatten::runtime::SnowflakeRuntime";

    @Test
    public void testSemiStructuredPrimitivePropertyFlattening()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredPrimitivePropertyFlattening__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Other Name, String, VARCHAR(8192), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Other Name\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"ss_flatten_0\".VALUE::varchar as \"Firm Other Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" inner join lateral flatten(input => \"root\".FIRM_DETAILS['otherNames'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Other Name, String, VARCHAR(8192), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredComplexPropertyFlattening()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredComplexPropertyFlattening__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Address Name, String, VARCHAR(8192), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Address Name\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"ss_flatten_0\".VALUE['name']::varchar as \"Firm Address Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" inner join lateral flatten(input => \"root\".FIRM_DETAILS['addresses'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Address Name, String, VARCHAR(8192), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredPrimitivePropertyArrayIndexing()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredPrimitivePropertyArrayIndexing__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Other Name 0, String, VARCHAR(8192), \"\"), (Firm Other Name 2, String, VARCHAR(8192), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Other Name 0\", \"\"), (\"Firm Other Name 2\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"root\".FIRM_DETAILS['otherNames'][0]::varchar as \"Firm Other Name 0\", \"root\".FIRM_DETAILS['otherNames'][2]::varchar as \"Firm Other Name 2\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Other Name 0, String, VARCHAR(8192), \"\"), (Firm Other Name 2, String, VARCHAR(8192), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredComplexPropertyArrayIndexing()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredComplexPropertyArrayIndexing__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Address 0 Name, String, VARCHAR(8192), \"\"), (Firm Address 2 Name, String, VARCHAR(8192), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Address 0 Name\", \"\"), (\"Firm Address 2 Name\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"root\".FIRM_DETAILS['addresses'][0]['name']::varchar as \"Firm Address 0 Name\", \"root\".FIRM_DETAILS['addresses'][2]['name']::varchar as \"Firm Address 2 Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Address 0 Name, String, VARCHAR(8192), \"\"), (Firm Address 2 Name, String, VARCHAR(8192), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredComplexPropertyFlatteningFollowedBySubType()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredComplexPropertyFlatteningFollowedBySubType__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Address Line 0 Line No, Integer, BIGINT, \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Address Line 0 Line No\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"ss_flatten_0\".VALUE['lines'][0]['lineno']::number as \"Firm Address Line 0 Line No\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" inner join lateral flatten(input => \"root\".FIRM_DETAILS['addresses'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Address Line 0 Line No, Integer, BIGINT, \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredComplexPropertyArrayIndexingFollowedBySubType()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredComplexPropertyArrayIndexingFollowedBySubType__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Address 0 Line 0 Line No, Integer, BIGINT, \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Address 0 Line 0 Line No\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"root\".FIRM_DETAILS['addresses'][0]['lines'][0]['lineno']::number as \"Firm Address 0 Line 0 Line No\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Address 0 Line 0 Line No, Integer, BIGINT, \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredPrimitivePropertyFiltering()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredPrimitivePropertyFiltering__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\")]\n" +
                "      sql = select distinct \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" inner join lateral flatten(input => \"root\".FIRM_DETAILS['otherNames'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" where \"ss_flatten_0\".VALUE::varchar = 'A'\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredPrimitivePropertyFilteringInProject()
    {
        Exception e = assertThrows(RuntimeException.class, () ->
        {
            this.buildExecutionPlanString("flatten::semiStructuredPrimitivePropertyFilteringInProject__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        });
        Assert.assertTrue(e.getMessage().contains("Filter in column projections is not supported. Use a Post Filter if filtering is necessary"));
    }

    @Test
    @Ignore
    public void testSemiStructuredComplexPropertyFiltering()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredComplexPropertyFiltering__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);
    }

    @Test
    public void testSemiStructuredComplexPropertyFilteringInProject()
    {
        Exception e = assertThrows(RuntimeException.class, () ->
        {
            this.buildExecutionPlanString("flatten::semiStructuredComplexPropertyFilteringInProject__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        });
        Assert.assertTrue(e.getMessage().contains("Filter in column projections is not supported. Use a Post Filter if filtering is necessary"));
    }

    @Test
    public void testSemiStructuredSubAggregation()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredSubAggregation__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Address Names, String, VARCHAR(8192), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Address Names\", INT)]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"person_table_1\".aggCol as \"Firm Address Names\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join (select \"person_table_2\".ID as ID, listagg(\"ss_flatten_0\".VALUE['name']::varchar, ';') as aggCol from PERSON_SCHEMA.PERSON_TABLE as \"person_table_2\" inner join lateral flatten(input => \"person_table_2\".FIRM_DETAILS['addresses'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" group by \"person_table_2\".ID) as \"person_table_1\" on (\"root\".ID = \"person_table_1\".ID)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Address Names, String, VARCHAR(8192), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredSubAggregationDeep()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredSubAggregationDeep__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Address Line No Sum, Integer, BIGINT, \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Address Line No Sum\", INT)]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"person_table_1\".aggCol as \"Firm Address Line No Sum\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join (select \"person_table_2\".ID as ID, sum(\"ss_flatten_1\".VALUE['lineno']::number) as aggCol from PERSON_SCHEMA.PERSON_TABLE as \"person_table_2\" inner join lateral flatten(input => \"person_table_2\".FIRM_DETAILS['addresses'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" inner join lateral flatten(input => \"ss_flatten_0\".VALUE['lines'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_1\" group by \"person_table_2\".ID) as \"person_table_1\" on (\"root\".ID = \"person_table_1\".ID)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Address Line No Sum, Integer, BIGINT, \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredMultiLevelFlatten()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredMultiLevelFlattening__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Address Name Line No, Integer, BIGINT, \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Name\", \"\"), (\"Firm Address Name Line No\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Name\", \"ss_flatten_1\".VALUE['lineno']::number as \"Firm Address Name Line No\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" inner join lateral flatten(input => \"root\".FIRM_DETAILS['addresses'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" inner join lateral flatten(input => \"ss_flatten_0\".VALUE['lines'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_1\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Name, String, VARCHAR(8192), \"\"), (Firm Address Name Line No, Integer, BIGINT, \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredMultiFlatten()
    {
        String snowflakePlan = this.buildExecutionPlanString("flatten::semiStructuredMultiFlatten__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address Name, String, VARCHAR(8192), \"\"), (Firm Address Line 0 No, Integer, BIGINT, \"\"), (Firm Other Name, String, VARCHAR(8192), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Address Name\", \"\"), (\"Firm Address Line 0 No\", \"\"), (\"Firm Other Name\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"ss_flatten_0\".VALUE['name']::varchar as \"Firm Address Name\", \"ss_flatten_0\".VALUE['lines'][0]['lineno']::number as \"Firm Address Line 0 No\", \"ss_flatten_1\".VALUE::varchar as \"Firm Other Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" inner join lateral flatten(input => \"root\".FIRM_DETAILS['addresses'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_0\" inner join lateral flatten(input => \"root\".FIRM_DETAILS['otherNames'], outer => true, recursive => false, mode => 'array') as \"ss_flatten_1\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address Name, String, VARCHAR(8192), \"\"), (Firm Address Line 0 No, Integer, BIGINT, \"\"), (Firm Other Name, String, VARCHAR(8192), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);

        Assert.assertEquals("[PERSON_TABLE.FIRM_DETAILS <TableAliasColumn>, PERSON_TABLE.FIRSTNAME <TableAliasColumn>]", this.scanColumns("flatten::semiStructuredMultiFlatten__TabularDataSet_1_", snowflakeMapping));
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredFlattening.pure";
    }
}
