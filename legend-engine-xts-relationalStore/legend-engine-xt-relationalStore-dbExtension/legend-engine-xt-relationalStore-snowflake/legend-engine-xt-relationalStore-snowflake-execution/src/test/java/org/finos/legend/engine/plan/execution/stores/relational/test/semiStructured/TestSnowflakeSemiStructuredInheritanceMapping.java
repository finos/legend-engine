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

public class TestSnowflakeSemiStructuredInheritanceMapping extends AbstractTestSnowflakeSemiStructured
{
    private static final String snowflakeMapping = "inheritance::mapping::SnowflakeMapping";
    private static final String snowflakeRuntime = "inheritance::runtime::SnowflakeRuntime";

    @Test
    public void testSemiStructuredPropertyAccessAtBaseClass()
    {
        String queryFunction = "inheritance::semiStructuredPropertyAccessAtBaseClass__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address Name, String, VARCHAR(8192), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Address Name\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['address']['name']::varchar as \"Firm Address Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address Name, String, VARCHAR(8192), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredPropertyAccessAtSubClass()
    {
        String queryFunction = "inheritance::semiStructuredPropertyAccessAtSubClass__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address 0 Line No, Integer, BIGINT, \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Address 0 Line No\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['address']['lines'][0]['lineno']::number as \"Firm Address 0 Line No\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address 0 Line No, Integer, BIGINT, \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredPropertyAccessAtSubClassNested()
    {
        String queryFunction = "inheritance::semiStructuredPropertyAccessAtSubClassNested__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address 0 Line No, Integer, BIGINT, \"\"), (Firm Address Street, String, VARCHAR(8192), \"\"), (Firm Address City, String, VARCHAR(8192), \"\"), (Firm Address State, String, VARCHAR(8192), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Address 0 Line No\", \"\"), (\"Firm Address Street\", \"\"), (\"Firm Address City\", \"\"), (\"Firm Address State\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['address']['lines'][0]['lineno']::number as \"Firm Address 0 Line No\", \"root\".FIRM_DETAILS['address']['lines'][0]['street']::varchar as \"Firm Address Street\", \"root\".FIRM_DETAILS['address']['lines'][1]['city']::varchar as \"Firm Address City\", \"root\".FIRM_DETAILS['address']['lines'][2]['state']::varchar as \"Firm Address State\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address 0 Line No, Integer, BIGINT, \"\"), (Firm Address Street, String, VARCHAR(8192), \"\"), (Firm Address City, String, VARCHAR(8192), \"\"), (Firm Address State, String, VARCHAR(8192), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredPropertyAccessAtSubClassNestedUsingProjectWithFunctions()
    {
        String queryFunction = "inheritance::semiStructuredPropertyAccessAtSubClassNestedUsingProjectWithFunctions__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address 0 Line No, Integer, BIGINT, \"\"), (Firm Address Street, String, VARCHAR(8192), \"\"), (Firm Address City, String, VARCHAR(8192), \"\"), (Firm Address State, String, VARCHAR(8192), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Address 0 Line No\", \"\"), (\"Firm Address Street\", \"\"), (\"Firm Address City\", \"\"), (\"Firm Address State\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['address']['lines'][0]['lineno']::number as \"Firm Address 0 Line No\", \"root\".FIRM_DETAILS['address']['lines'][0]['street']::varchar as \"Firm Address Street\", \"root\".FIRM_DETAILS['address']['lines'][1]['city']::varchar as \"Firm Address City\", \"root\".FIRM_DETAILS['address']['lines'][2]['state']::varchar as \"Firm Address State\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address 0 Line No, Integer, BIGINT, \"\"), (Firm Address Street, String, VARCHAR(8192), \"\"), (Firm Address City, String, VARCHAR(8192), \"\"), (Firm Address State, String, VARCHAR(8192), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredInheritanceMapping.pure";
    }
}
