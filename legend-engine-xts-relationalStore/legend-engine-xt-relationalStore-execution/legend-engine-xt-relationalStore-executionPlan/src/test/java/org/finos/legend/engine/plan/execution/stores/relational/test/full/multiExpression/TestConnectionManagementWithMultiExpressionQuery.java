// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.full.multiExpression;

import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.stores.relational.connection.AlloyTestServer;
import org.finos.legend.engine.plan.execution.stores.relational.connection.TestRelationalExecutionStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToPureTDSToObjectSerializer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;

public class TestConnectionManagementWithMultiExpressionQuery extends AlloyTestServer
{
    private static final String LOGICAL_MODEL = "###Pure\n" +
            "Class test::Person {name: String[1]; age: Integer[1];}\n\n";

    private static final String STORE_MODEL = "###Relational\n" +
            "Database test::DB\n" +
            "(\n" +
            "    Table dataTable (personName VARCHAR(100) PRIMARY KEY, personAge INT)\n" +
            ")\n\n";

    private static final String MAPPING = "###Mapping\n" +
            "Mapping test::Map\n" +
            "(\n" +
            "    test::Person: Relational\n" +
            "    {\n" +
            "       scope([test::DB] dataTable)\n" +
            "       (\n" +
            "          name: personName,\n" +
            "          age: personAge\n" +
            "       )\n" +
            "    }\n" +
            ")\n\n";

    private static final String RUNTIME = "###Runtime\n" +
            "Runtime test::Runtime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    test::Map\n" +
            "  ];\n" +
            "  connections:\n" +
            "  [\n" +
            "    test::DB:\n" +
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
            "}\n\n";


    @Test
    public void testGraphFetchDataTypes() throws Exception
    {
        String function = "###Pure\n" +
                "function test::function(): Any[*]\n" +
                "{\n" +
                "  {|\n" +
                "    let pName = test::Person.all()->filter(p | $p.name == 'Peter')->toOne().name;\n" +
                "    test::Person.all()->filter(p | $p.name == $pName)->project(col(x | $x.age, 'age'));\n" +
                "  };\n" +
                "}";

        SingleExecutionPlan plan = buildPlan(LOGICAL_MODEL + STORE_MODEL + MAPPING + RUNTIME + function);
        RelationalResult relationalResult = (RelationalResult) planExecutor.execute(plan, Maps.mutable.empty(), null, Identity.getAnonymousIdentity());
        Assert.assertEquals("[{\"age\":23}]", relationalResult.flush(new RelationalResultToPureTDSToObjectSerializer(relationalResult)));
        Assert.assertEquals(0L, (long) ConnectionStateManager.getInstance().get(TestRelationalExecutionStatistics.getPoolName()).getActiveConnections());
    }

    @Override
    protected void insertTestData(Statement s) throws SQLException
    {
        s.execute("Drop table if exists dataTable;");
        s.execute("Create Table dataTable (personName VARCHAR(100), personAge INT, PRIMARY KEY(personName));");
        s.execute("insert into dataTable (personName, personAge) values ('Peter', 23), ('John', 30);");
    }
}
