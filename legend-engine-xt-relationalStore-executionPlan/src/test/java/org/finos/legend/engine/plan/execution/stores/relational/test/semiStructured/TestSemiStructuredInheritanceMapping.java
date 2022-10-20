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

public class TestSemiStructuredInheritanceMapping extends AbstractTestSemiStructured
{
    private static final String snowflakeMapping = "inheritance::mapping::SnowflakeMapping";
    private static final String snowflakeRuntime = "inheritance::runtime::SnowflakeRuntime";

    private static final String h2Mapping = "inheritance::mapping::H2Mapping";
    private static final String h2Runtime = "inheritance::runtime::H2Runtime";

    @Test
    public void testSemiStructuredPropertyAccessAtBaseClass()
    {
        String snowflakePlan = this.buildExecutionPlanString("inheritance::semiStructuredPropertyAccessAtBaseClass__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address Name, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Address Name\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['address']['name']::varchar as \"Firm Address Name\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("inheritance::semiStructuredPropertyAccessAtBaseClass__TabularDataSet_1_", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,A1\n" +
                "John,A1\n" +
                "John,A1\n" +
                "Anthony,A1\n" +
                "Fabrice,A2\n" +
                "Oliver,A3\n" +
                "David,A3\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredPropertyAccessAtSubClass()
    {
        String snowflakePlan = this.buildExecutionPlanString("inheritance::semiStructuredPropertyAccessAtSubClass__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address 0 Line No, Integer, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Address 0 Line No\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['address']['lines'][0]['lineno'] as \"Firm Address 0 Line No\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("inheritance::semiStructuredPropertyAccessAtSubClass__TabularDataSet_1_", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,1\n" +
                "John,1\n" +
                "John,1\n" +
                "Anthony,1\n" +
                "Fabrice,1\n" +
                "Oliver,1\n" +
                "David,1\n", h2Result.replace("\r\n", "\n"));
    }


    @Test
    public void testSemiStructuredPropertyAccessAtSubClassNested()
    {
        String snowflakePlan = this.buildExecutionPlanString("inheritance::semiStructuredPropertyAccessAtSubClassNested__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address 0 Line No, Integer, \"\", \"\"), (Firm Address Street, String, \"\", \"\"), (Firm Address City, String, \"\", \"\"), (Firm Address State, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Address 0 Line No\", \"\"), (\"Firm Address Street\", \"\"), (\"Firm Address City\", \"\"), (\"Firm Address State\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['address']['lines'][0]['lineno'] as \"Firm Address 0 Line No\", \"root\".FIRM_DETAILS['address']['lines'][0]['street']::varchar as \"Firm Address Street\", \"root\".FIRM_DETAILS['address']['lines'][1]['city']::varchar as \"Firm Address City\", \"root\".FIRM_DETAILS['address']['lines'][2]['state']::varchar as \"Firm Address State\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("inheritance::semiStructuredPropertyAccessAtSubClassNested__TabularDataSet_1_", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,1,s1,c1,s1\n" +
                "John,1,s1,c1,s1\n" +
                "John,1,s1,c1,s1\n" +
                "Anthony,1,s1,c1,s1\n" +
                "Fabrice,1,s2,c2,\n" +
                "Oliver,1,s3,,\n" +
                "David,1,s3,,\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredPropertyAccessAtSubClassNestedUsingProjectWithFunctions()
    {
        String snowflakePlan = this.buildExecutionPlanString("inheritance::semiStructuredPropertyAccessAtSubClassNestedUsingProjectWithFunctions__TabularDataSet_1_", snowflakeMapping, snowflakeRuntime);
        String snowflakeExpected = "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Address 0 Line No, Integer, \"\", \"\"), (Firm Address Street, String, \"\", \"\"), (Firm Address City, String, \"\", \"\"), (Firm Address State, String, \"\", \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Address 0 Line No\", \"\"), (\"Firm Address Street\", \"\"), (\"Firm Address City\", \"\"), (\"Firm Address State\", \"\")]\n" +
                "  sql = select \"root\".FIRSTNAME as \"First Name\", \"root\".FIRM_DETAILS['address']['lines'][0]['lineno'] as \"Firm Address 0 Line No\", \"root\".FIRM_DETAILS['address']['lines'][0]['street']::varchar as \"Firm Address Street\", \"root\".FIRM_DETAILS['address']['lines'][1]['city']::varchar as \"Firm Address City\", \"root\".FIRM_DETAILS['address']['lines'][2]['state']::varchar as \"Firm Address State\" from PERSON_SCHEMA.PERSON_TABLE as \"root\"\n" +
                "  connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                ")\n";
        Assert.assertEquals(snowflakeExpected, snowflakePlan);

        String h2Result = this.executeFunction("inheritance::semiStructuredPropertyAccessAtSubClassNestedUsingProjectWithFunctions__TabularDataSet_1_", h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,1,s1,c1,s1\n" +
                "John,1,s1,c1,s1\n" +
                "John,1,s1,c1,s1\n" +
                "Anthony,1,s1,c1,s1\n" +
                "Fabrice,1,s2,c2,\n" +
                "Oliver,1,s3,,\n" +
                "David,1,s3,,\n", h2Result.replace("\r\n", "\n"));
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredInheritanceMapping.pure";
    }
}
