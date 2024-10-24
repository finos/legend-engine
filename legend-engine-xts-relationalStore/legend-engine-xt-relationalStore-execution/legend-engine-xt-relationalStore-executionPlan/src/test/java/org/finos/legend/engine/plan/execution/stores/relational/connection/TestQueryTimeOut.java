//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection;

import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestQueryTimeOut extends AlloyTestServer
{

    private static final String TEST_FUNCTION = "###Pure\n" +
            "function test::fetch(): Any[1]\n" +
            "{\n" +
            "  {names:String[*] | test::Person.all()\n" +
            "                        ->project([x | $x.fullName, x | $x.firmName], ['fullName', 'firmName'])\n" +
            "                        ->extend([col(row:TDSRow[1] | $row.getString('fullName'), 'string')])\n" +
            "                        ->olapGroupBy(['fullName'], asc('firmName'), func(y | $y->meta::pure::functions::math::olap::rank()), 'RANK 1')\n" +
            "                        ->olapGroupBy(['fullName'], desc('firmName'), func(y | $y->meta::pure::functions::math::olap::rowNumber()), 'ROW 1')\n" +
            "                        ->olapGroupBy(['fullName'], asc('firmName'), func(y | $y->meta::pure::functions::math::olap::denseRank()), 'DENSE RANK 1')\n" +
            "                        ->olapGroupBy(['fullName'], desc('firmName'), func(y | $y->meta::pure::functions::math::olap::rank()), 'RANK 2')\n" +
            "                        ->olapGroupBy(['firmName'], asc('fullName'), func(y | $y->meta::pure::functions::math::olap::rank()), 'RANK 3')\n" +
            "                        ->olapGroupBy(['firmName'], desc('fullName'), func(y | $y->meta::pure::functions::math::olap::rowNumber()), 'ROW 2')\n" +
            "                        ->olapGroupBy(['firmName'], asc('fullName'), func(y | $y->meta::pure::functions::math::olap::denseRank()), 'DENSE RANK 2')\n" +
            "                        ->olapGroupBy(['firmName'], desc('fullName'), func(y | $y->meta::pure::functions::math::olap::rank()), 'RANK 3')\n" +
            "  }\n" +
            "}";

    private static final String LOGICAL_MODEL = "###Pure\n" +
            "Class test::Person\n" +
            "{\n" +
            "  fullName: String[1];\n" +
            "  addressName: String[1];\n" +
            "  firmName: String[1];\n" +
            "}\n\n\n";

    private static final String STORE_MODEL = "###Relational\n" +
            "Database test::DB\n" +
            "(\n" +
            "  Table PERSON (\n" +
            "    fullName VARCHAR(100) PRIMARY KEY,\n" +
            "    firmName VARCHAR(100),\n" +
            "    addressName VARCHAR(100)\n" +
            "  )\n" +
            ")\n\n\n";

    private static final String MAPPING = "###Mapping\n" +
            "Mapping test::Map\n" +
            "(\n" +
            "  test::Person: Relational\n" +
            "  {\n" +
            "    ~primaryKey\n" +
            "    (\n" +
            "      [test::DB]PERSON.fullName\n" +
            "    )\n" +
            "    ~mainTable [test::DB]PERSON\n" +
            "    fullName:  [test::DB]PERSON.fullName,\n" +
            "    firmName:  [test::DB]PERSON.firmName,\n" +
            "    addressName:  [test::DB]PERSON.addressName\n" +
            "  }\n" +
            ")\n\n\n";

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
            "          queryTimeOutInSeconds: 1;\n" +
            "        }\n" +
            "      }#\n" +
            "    ]\n" +
            "  ];\n" +
            "}\n";

    public static final String TEST_EXECUTION_PLAN = LOGICAL_MODEL + STORE_MODEL + MAPPING + RUNTIME + TEST_FUNCTION;


    @Override
    protected void insertTestData(Statement statement) throws SQLException
    {
        statement.execute("Drop table if exists PERSON;");
        statement.execute("Create Table PERSON(fullName VARCHAR(100) NOT NULL,firmName VARCHAR(100) NULL,addressName VARCHAR(100) NULL,PRIMARY KEY(fullName));");

        Integer personTableLength = 100000;
        Long t1 = System.currentTimeMillis();
        for (int i = 1; i <= personTableLength; i++)
        {
            statement.execute(String.format("insert into PERSON (fullName,firmName,addressName) values ('fullName%d','firmName%d','addressName%d');", personTableLength - i, i, personTableLength - i));
        }
        Long t2 = System.currentTimeMillis();
        System.out.println("Insert took " + (t2 - t1) + " ms");
    }

    @Test
    public void testQueryTimeOutInSeconds()
    {
        try
        {
            SingleExecutionPlan executionPlan = buildPlan(TEST_EXECUTION_PLAN);
            Assert.assertNotNull(executionPlan);
            System.out.println(LocalDateTime.now());
            Assert.assertNotNull(executePlan(executionPlan, Maps.mutable.empty()));
        }
        catch (Exception e)
        {
            System.out.println(LocalDateTime.now());
            Assert.assertEquals("org.h2.jdbc.JdbcSQLTimeoutException: Statement was canceled or the session timed out; SQL statement:", e.getMessage().substring(0, e.getMessage().indexOf('\n')));
            return;
        }
        Assert.fail("Cannot test QueryTimeOut as query runs for less than 1 second");
    }
}
