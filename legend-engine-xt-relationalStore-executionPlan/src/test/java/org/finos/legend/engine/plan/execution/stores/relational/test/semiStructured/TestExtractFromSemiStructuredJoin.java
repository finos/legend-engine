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

public class TestExtractFromSemiStructuredJoin extends AbstractTestSemiStructured
{
    private static final String snowflakeMapping = "join::mapping::SnowflakeMapping";
    private static final String snowflakeRuntime = "join::runtime::SnowflakeRuntime";

    private static final String h2Mapping = "join::mapping::H2Mapping";
    private static final String h2Runtime = "join::runtime::H2Runtime";

    @Test
    public void testJoinOnSemiStructuredProperty()
    {
        String h2Result = this.executeFunction("join::testJoinOnSemiStructuredProperty__TabularDataSet_1_", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Smith,Firm X\n" +
                "John,Johnson,Firm X\n" +
                "John,Hill,Firm X\n" +
                "Anthony,Allen,Firm X\n" +
                "Fabrice,Roberts,Firm A\n" +
                "Oliver,Hill,Firm B\n" +
                "David,Harris,Firm B\n", h2Result.replace("\r\n", "\n"));

        String snowflakePlan = this.buildExecutionPlanString("join::testJoinOnSemiStructuredProperty__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected =
                "    Relational\n" +
                "    (\n" +
                "      type = TDS[(First Name, String, VARCHAR(100), \"\"), (Last Name, String, VARCHAR(100), \"\"), (Firm/Legal Name, String, \"\", \"\")]\n" +
                "      resultColumns = [(\"First Name\", VARCHAR(100)), (\"Last Name\", VARCHAR(100)), (\"Firm/Legal Name\", \"\")]\n" +
                "      sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".LASTNAME as \"Last Name\", \"firm_table_0\".FIRM_DETAILS['legalName']::varchar as \"Firm/Legal Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\" left outer join FIRM_SCHEMA.FIRM_TABLE as \"firm_table_0\" on (to_number(get_path(\"root\".FIRM, 'ID')) = to_number(get_path(\"firm_table_0\".FIRM_DETAILS, 'ID')))\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n";
        String TDSType = "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Last Name, String, VARCHAR(100), \"\"), (Firm/Legal Name, String, \"\", \"\")]\n";
        Assert.assertEquals(wrapPreAndFinallyExecutionSqlQuery(TDSType, snowflakeExpected), snowflakePlan);
    }

    public String modelResourcePath()
    {
       return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/extractFromSemiStructuredJoin.pure";
    }

}
