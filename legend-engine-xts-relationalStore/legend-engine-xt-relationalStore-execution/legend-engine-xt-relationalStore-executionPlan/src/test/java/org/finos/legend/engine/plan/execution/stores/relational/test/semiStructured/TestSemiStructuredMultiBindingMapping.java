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

public class TestSemiStructuredMultiBindingMapping extends AbstractTestSemiStructured
{
    private static final String memSQLMapping = "multiBinding::mapping::MemSQLMapping";
    private static final String memSQLRuntime = "multiBinding::runtime::MemSQLRuntime";

    private static final String h2Mapping = "multiBinding::mapping::H2Mapping";
    private static final String h2Runtime = "multiBinding::runtime::H2Runtime";

    @Test
    public void testSemiStructuredPropertyAccessFromSingleBindingMapping()
    {
        String queryFunction = "multiBinding::semiStructuredPropertyAccessFromSingleBindingMapping__TabularDataSet_1_";
        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Address Name, String, VARCHAR(65536), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Address Name\", \"\")]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name`, `root`.ADDRESS_DETAILS::$name as `Address Name` from PERSON_SCHEMA.PERSON_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,A4\n" +
                "John,A5\n" +
                "John,A6\n" +
                "Anthony,A7\n" +
                "Fabrice,A8\n" +
                "Oliver,A9\n" +
                "David,A10\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredPropertyAccessFromMultipleBindingMapping()
    {
        String queryFunction = "multiBinding::semiStructuredPropertyAccessFromMultipleBindingMapping__TabularDataSet_1_";
        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(65536), \"\"), (Address Name, String, VARCHAR(65536), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Legal Name\", \"\"), (\"Address Name\", \"\")]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name`, `root`.FIRM_DETAILS::$legalName as `Firm Legal Name`, `root`.ADDRESS_DETAILS::$name as `Address Name` from PERSON_SCHEMA.PERSON_TABLE as `root`\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,A4\n" +
                "John,Firm X,A5\n" +
                "John,Firm X,A6\n" +
                "Anthony,Firm X,A7\n" +
                "Fabrice,Firm A,A8\n" +
                "Oliver,Firm B,A9\n" +
                "David,Firm B,A10\n", h2Result.replace("\r\n", "\n"));
    }

    @Test
    public void testSemiStructuredRelOpWithJoinPropertyAccessFromMultipleBindingMapping()
    {
        String queryFunction = "multiBinding::semiStructuredRelOpWithJoinPropertyAccessFromMultipleBindingMapping__TabularDataSet_1_";
        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Firm Legal Name, String, VARCHAR(65536), \"\"), (Address Name, String, VARCHAR(65536), \"\"), (Manager Firm Legal Name, String, VARCHAR(65536), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Firm Legal Name\", \"\"), (\"Address Name\", \"\"), (\"Manager Firm Legal Name\", \"\")]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name`, `root`.FIRM_DETAILS::$legalName as `Firm Legal Name`, `root`.ADDRESS_DETAILS::$name as `Address Name`, `person_table_1`.FIRM_DETAILS::$legalName as `Manager Firm Legal Name` from PERSON_SCHEMA.PERSON_TABLE as `root` left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_1` on (`root`.MANAGERID = `person_table_1`.ID)\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);

        String h2Result = this.executeFunction(queryFunction, h2Mapping, h2Runtime);
        Assert.assertEquals("Peter,Firm X,A4,Firm X\n" +
                "John,Firm X,A5,Firm X\n" +
                "John,Firm X,A6,Firm X\n" +
                "Anthony,Firm X,A7,\n" +
                "Fabrice,Firm A,A8,\n" +
                "Oliver,Firm B,A9,Firm B\n" +
                "David,Firm B,A10,\n", h2Result.replace("\r\n", "\n"));
    }


    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredMultiBindingMapping.pure";
    }
}
