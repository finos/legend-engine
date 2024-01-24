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

public class TestSemiStructuredJoinChainMapping extends AbstractTestSemiStructured
{
    private static final String memSQLMapping = "joinChain::mapping::MemSQLMapping";
    private static final String memSQLRuntime = "joinChain::runtime::MemSQLRuntime";

    @Test
    public void testSingleJoinInChain()
    {
        String queryFunction = "joinChain::singleJoinInChain__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Manager Firm Legal Name, String, VARCHAR(65536), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Manager Firm Legal Name\", \"\")]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name`, `person_table_1`.FIRM_DETAILS::$legalName as `Manager Firm Legal Name` from PERSON_SCHEMA.PERSON_TABLE as `root` left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_1` on (`root`.MANAGERID = `person_table_1`.ID)\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);
    }

    @Test
    public void testMultipleJoinsInChain()
    {
        String queryFunction = "joinChain::multipleJoinsInChain__TabularDataSet_1_";

        String memSQLPlan = this.buildExecutionPlanString(queryFunction, memSQLMapping, memSQLRuntime);
        String memSQLExpected =
                "Relational\n" +
                "(\n" +
                "  type = TDS[(First Name, String, VARCHAR(100), \"\"), (Manager Firm Legal Name, String, VARCHAR(65536), \"\"), (Manager Manager Firm Legal Name, String, VARCHAR(65536), \"\"), (Manager Manager Firm Legal Name Dup1, String, VARCHAR(65536), \"\"), (Manager Manager Firm Legal Name Dup2, String, VARCHAR(65536), \"\")]\n" +
                "  resultColumns = [(\"First Name\", VARCHAR(100)), (\"Manager Firm Legal Name\", \"\"), (\"Manager Manager Firm Legal Name\", \"\"), (\"Manager Manager Firm Legal Name Dup1\", \"\"), (\"Manager Manager Firm Legal Name Dup2\", \"\")]\n" +
                "  sql = select `root`.FIRSTNAME as `First Name`, `person_table_1`.FIRM_DETAILS::$legalName as `Manager Firm Legal Name`, `person_table_2`.FIRM_DETAILS::$legalName as `Manager Manager Firm Legal Name`, `person_table_3`.FIRM_DETAILS::$legalName as `Manager Manager Firm Legal Name Dup1`, `person_table_5`.FIRM_DETAILS::$legalName as `Manager Manager Firm Legal Name Dup2` from PERSON_SCHEMA.PERSON_TABLE as `root` left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_1` on (`root`.MANAGERID = `person_table_1`.ID) left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_2` on (`person_table_1`.MANAGERID = `person_table_2`.ID) left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_3` on (`person_table_1`.MANAGERID = `person_table_3`.ID) left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_4` on (`root`.MANAGERID = `person_table_4`.ID) left outer join PERSON_SCHEMA.PERSON_TABLE as `person_table_5` on (`person_table_4`.MANAGERID = `person_table_5`.ID)\n" +
                "  connection = RelationalDatabaseConnection(type = \"MemSQL\")\n" +
                ")\n";
        Assert.assertEquals(memSQLExpected, memSQLPlan);
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/semiStructured/semiStructuredJoinChainMapping.pure";
    }
}
