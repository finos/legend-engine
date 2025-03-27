// Copyright 2024 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.relational.result;

import java.sql.SQLException;
import java.sql.Statement;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.stores.relational.connection.AlloyTestServer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.Test;

public class TestRelationalResultGridPrintUtility extends AlloyTestServer
{
    @Test
    public void prettyPrintRelationalResult()
    {
        Assert.assertEquals(
                "+----+----+\n" +
                        "|name|i...|\n" +
                        "|V...|V...|\n" +
                        "+----+----+\n" +
                        "|R...|RBH |\n" +
                        "| .  | .  |\n" +
                        "| .  | .  |\n" +
                        "| .  | .  |\n" +
                        "|E...|ERP |\n" +
                        "+----+----+\n" +
                        "4 rows (2 shown) -- 2 columns",
                RelationalResultGridPrintUtility.prettyGridPrint(execute(), 2, 4)
        );
        Assert.assertEquals(
                "+------+------+\n" +
                        "| name |ini...|\n" +
                        "|VAR...|VAR...|\n" +
                        "+------+------+\n" +
                        "|Rafael| RBH  |\n" +
                        "| Jose | JJS  |\n" +
                        "| Juan | JHS  |\n" +
                        "|Enr...| ERP  |\n" +
                        "+------+------+\n" +
                        "4 rows -- 2 columns",
                RelationalResultGridPrintUtility.prettyGridPrint(execute(), 10, 6)
        );
    }

    @Test
    public void prettyPrintRealizeRelationalResult()
    {
        RelationalResult result = execute();
        RealizedRelationalResult realizeInMemory = (RealizedRelationalResult) result.realizeInMemory();

        Assert.assertEquals(
                "+----+----+\n" +
                        "|name|i...|\n" +
                        "|V...|V...|\n" +
                        "+----+----+\n" +
                        "|R...|RBH |\n" +
                        "| .  | .  |\n" +
                        "| .  | .  |\n" +
                        "| .  | .  |\n" +
                        "|E...|ERP |\n" +
                        "+----+----+\n" +
                        "4 rows (2 shown) -- 2 columns",
                RelationalResultGridPrintUtility.prettyGridPrint(realizeInMemory, 2, 4)
        );
        Assert.assertEquals(
                "+------+------+\n" +
                        "| name |ini...|\n" +
                        "|VAR...|VAR...|\n" +
                        "+------+------+\n" +
                        "|Rafael| RBH  |\n" +
                        "| Jose | JJS  |\n" +
                        "| Juan | JHS  |\n" +
                        "|Enr...| ERP  |\n" +
                        "+------+------+\n" +
                        "4 rows -- 2 columns",
                RelationalResultGridPrintUtility.prettyGridPrint(realizeInMemory, 10, 6)
        );
    }

    private RelationalResult execute()
    {
        String func = "function local::func(): Any[*]{ |#>{local::db.person}#->select()->from(test::Runtime) }";
        String db = "###Relational\n" +
                "Database local::db" +
                "(" +
                "  Table person" +
                "  (" +
                "     name varchar(100)," +
                "     initials varchar(100)" +
                "  )" +
                ")";

        String mapping = "###Mapping\n" +
                "Mapping test::Map()";

        String runtime = "###Runtime\n" +
                "Runtime test::Runtime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    local::db:\n" +
                "    [\n" +
                "      c1: #{\n" +
                "        RelationalDatabaseConnection\n" +
                "        {\n" +
                "          type: H2;\n" +
                "          specification: LocalH2 {};\n" +
                "          auth: DefaultH2;\n" +
                "        }\n" +
                "      }#\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n";

        SingleExecutionPlan singleExecutionPlan = this.buildPlan(func + "\n" + db + "\n" + runtime + "\n" + mapping);
        RelationalResult result = (RelationalResult) planExecutor.execute(singleExecutionPlan, Maps.mutable.empty(), null);
        return result;
    }

    @Override
    protected void insertTestData(Statement s) throws SQLException
    {
        s.execute("Create Schema default;");
        s.execute("Drop table if exists person;");
        s.execute("Create Table person(name VARCHAR(100) NOT NULL, INITIALS VARCHAR(10));");
        s.execute("insert into person (name, initials) values ('Rafael', 'RBH');");
        s.execute("insert into person (name, initials) values ('Jose', 'JJS');");
        s.execute("insert into person (name, initials) values ('Juan', 'JHS');");
        s.execute("insert into person (name, initials) values ('Enrique', 'ERP');");
    }
}
