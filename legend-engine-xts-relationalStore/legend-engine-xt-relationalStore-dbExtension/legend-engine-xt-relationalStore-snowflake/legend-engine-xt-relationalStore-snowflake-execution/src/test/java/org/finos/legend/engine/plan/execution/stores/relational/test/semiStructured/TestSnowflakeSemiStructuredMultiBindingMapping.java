// Copyright 2023 Goldman Sachs
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

public class TestSnowflakeSemiStructuredMultiBindingMapping extends AbstractTestSnowflakeSemiStructured
{
    private static final String snowflakeMapping = "multiBinding::mapping::SnowflakeMapping";
    private static final String snowflakeRuntime = "multiBinding::runtime::SnowflakeRuntime";

    @Test
    public void testSemiStructuredPropertyAccessFromSingleBindingMapping()
    {
        String queryFunction = "multiBinding::semiStructuredPropertyAccessFromSingleBindingMapping__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Address Name, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Address Name\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".ADDRESS_DETAILS['name']::varchar as \"Address Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Address Name, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredPropertyAccessFromMultipleBindingMapping()
    {
        String queryFunction = "multiBinding::semiStructuredPropertyAccessFromMultipleBindingMapping__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(65536), \"\"), (Address Name, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Legal Name\", \"\"), (\"Address Name\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Legal Name\", \"root\".ADDRESS_DETAILS['name']::varchar as \"Address Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(65536), \"\"), (Address Name, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testSemiStructuredRelOpWithJoinPropertyAccessFromMultipleBindingMapping()
    {
        String queryFunction = "multiBinding::semiStructuredRelOpWithJoinPropertyAccessFromMultipleBindingMapping__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(65536), \"\"), (Address Name, String, VARCHAR(65536), \"\"), (Manager Firm Legal Name, String, VARCHAR(65536), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Legal Name\", \"\"), (\"Address Name\", \"\"), (\"Manager Firm Legal Name\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['legalName']::varchar as \"Firm Legal Name\", \"root\".ADDRESS_DETAILS['name']::varchar as \"Address Name\", \"person_table_1\".FIRM_DETAILS['legalName']::varchar as \"Manager Firm Legal Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_1\" on (\"root\".MANAGERID = \"person_table_1\".ID)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(65536), \"\"), (Address Name, String, VARCHAR(65536), \"\"), (Manager Firm Legal Name, String, VARCHAR(65536), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }


    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredMultiBindingMapping.pure";
    }
}
