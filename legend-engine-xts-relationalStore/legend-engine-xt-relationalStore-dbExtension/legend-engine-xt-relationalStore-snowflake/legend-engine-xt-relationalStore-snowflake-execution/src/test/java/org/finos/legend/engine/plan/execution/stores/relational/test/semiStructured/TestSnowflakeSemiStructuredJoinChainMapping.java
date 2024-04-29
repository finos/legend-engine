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

public class TestSnowflakeSemiStructuredJoinChainMapping extends AbstractTestSnowflakeSemiStructured
{
    private static final String snowflakeMapping = "joinChain::mapping::SnowflakeMapping";
    private static final String snowflakeRuntime = "joinChain::runtime::SnowflakeRuntime";

    @Test
    public void testSingleJoinInChain()
    {
        String queryFunction = "joinChain::singleJoinInChain__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Manager Firm Legal Name, String, VARCHAR(8192), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Manager Firm Legal Name\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"person_table_1\".FIRM_DETAILS['legalName']::varchar as \"Manager Firm Legal Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_1\" on (\"root\".MANAGERID = \"person_table_1\".ID)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Manager Firm Legal Name, String, VARCHAR(8192), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    @Test
    public void testMultipleJoinsInChain()
    {
        String queryFunction = "joinChain::multipleJoinsInChain__TabularDataSet_1_";
        String snowflakePlan = this.buildExecutionPlanString(queryFunction, snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Manager Firm Legal Name, String, VARCHAR(8192), \"\"), (Manager Manager Firm Legal Name, String, VARCHAR(8192), \"\"), (Manager Manager Firm Legal Name Dup1, String, VARCHAR(8192), \"\"), (Manager Manager Firm Legal Name Dup2, String, VARCHAR(8192), \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Manager Firm Legal Name\", \"\"), (\"Manager Manager Firm Legal Name\", \"\"), (\"Manager Manager Firm Legal Name Dup1\", \"\"), (\"Manager Manager Firm Legal Name Dup2\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"person_table_1\".FIRM_DETAILS['legalName']::varchar as \"Manager Firm Legal Name\", \"person_table_2\".FIRM_DETAILS['legalName']::varchar as \"Manager Manager Firm Legal Name\", \"person_table_3\".FIRM_DETAILS['legalName']::varchar as \"Manager Manager Firm Legal Name Dup1\", \"person_table_5\".FIRM_DETAILS['legalName']::varchar as \"Manager Manager Firm Legal Name Dup2\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_1\" on (\"root\".MANAGERID = \"person_table_1\".ID) left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_2\" on (\"person_table_1\".MANAGERID = \"person_table_2\".ID) left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_3\" on (\"person_table_1\".MANAGERID = \"person_table_3\".ID) left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_4\" on (\"root\".MANAGERID = \"person_table_4\".ID) left outer join PERSON_SCHEMA.PERSON_TABLE as \"person_table_5\" on (\"person_table_4\".MANAGERID = \"person_table_5\".ID)\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Manager Firm Legal Name, String, VARCHAR(8192), \"\"), (Manager Manager Firm Legal Name, String, VARCHAR(8192), \"\"), (Manager Manager Firm Legal Name Dup1, String, VARCHAR(8192), \"\"), (Manager Manager Firm Legal Name Dup2, String, VARCHAR(8192), \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
        Assert.assertEquals("[PERSON_TABLE.FIRM_DETAILS <RelationalOperationElementWithJoin>, PERSON_TABLE.FIRSTNAME <TableAliasColumn>, PERSON_TABLE.ID <JoinTreeNode>, PERSON_TABLE.MANAGERID <JoinTreeNode>]", this.scanColumns(queryFunction, snowflakeMapping));
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredJoinChainMapping.pure";
    }
}
