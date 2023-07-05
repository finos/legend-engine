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

package org.finos.legend.engine.plan.execution.stores.relational.test.full.graphFetch.concurrent;

import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToPureFormatSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.stores.relational.connection.AlloyTestServer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

public class TestConcurrentRelationalGraphFetchExecution extends AlloyTestServer
{
    private static final String LOGICAL_MODEL = "###Pure\n" +
            "Class <<meta::pure::profiles::temporal.businesstemporal>> test::Firm\n" +
            "{\n" +
            "  legalName: String[1];\n" +
            "  employees: test::Person[*];\n" +
            "}\n\n" +
            "Class <<meta::pure::profiles::temporal.businesstemporal>> test::Person\n" +
            "{\n" +
            "  name: String[1];\n" +
            "}\n\n";

    private static final String STORE_MODEL = "###Relational\n" +
            "Database test::DB\n" +
            "(\n" +
            "  Table firm\n" +
            "  (\n" +
            "    milestoning\n" +
            "    (\n" +
            "      business(BUS_FROM = BUS_FROM, BUS_THRU = BUS_THRU)\n" +
            "    )\n" +
            "    ID INTEGER PRIMARY KEY,\n" +
            "    LEGAL_NAME VARCHAR(100),\n" +
            "    BUS_FROM TIMESTAMP PRIMARY KEY,\n" +
            "    BUS_THRU TIMESTAMP\n" +
            "  )\n\n" +
            "  Table person\n" +
            "  (\n" +
            "    milestoning\n" +
            "    (\n" +
            "      business(BUS_FROM = BUS_FROM, BUS_THRU = BUS_THRU)\n" +
            "    )\n" +
            "    ID INTEGER PRIMARY KEY,\n" +
            "    NAME VARCHAR(100),\n" +
            "    FIRM_ID INTEGER,\n" +
            "    BUS_FROM TIMESTAMP PRIMARY KEY,\n" +
            "    BUS_THRU TIMESTAMP\n" +
            "  )\n" +
            "\n" +
            "  Join firm_person(firm.ID = person.FIRM_ID)\n" +
            ")\n\n";

    private static final String MAPPING = "###Mapping\n" +
            "Mapping test::Map\n" +
            "(\n" +
            "  test::Firm: Relational\n" +
            "  {\n" +
            "    legalName: [test::DB]firm.LEGAL_NAME,\n" +
            "    employees: [test::DB]@firm_person\n" +
            "  }\n" +
            "  test::Person: Relational\n" +
            "  {\n" +
            "    name: [test::DB]person.NAME\n" +
            "  }\n" +
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
            "      connection_1:\n" +
            "      #{\n" +
            "        RelationalDatabaseConnection\n" +
            "        {\n" +
            "          store: test::DB;\n" +
            "          type: H2;\n" +
            "          specification: LocalH2\n" +
            "          {\n" +
            "          };\n" +
            "          auth: DefaultH2;\n" +
            "        }\n" +
            "      }#\n" +
            "    ]\n" +
            "  ];\n" +
            "}\n\n";

    @Test
    public void testConcurrentRelationalGraphFetchExecutionWithParams() throws Exception
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[*]\n" +
                "{\n" +
                "  {businessDate: DateTime[1] | \n" +
                "    test::Firm.all($businessDate)->graphFetch(\n" +
                "      #{\n" +
                "        test::Firm{\n" +
                "          legalName,\n" +
                "          'employees':employees($businessDate){\n" +
                "            name\n" +
                "          }\n" +
                "        }\n" +
                "      }#\n" +
                "    )->serialize(\n" +
                "      #{\n" +
                "        test::Firm{\n" +
                "          legalName,\n" +
                "          'employees':employees($businessDate){\n" +
                "            name\n" +
                "          }\n" +
                "        }\n" +
                "      }#\n" +
                "    )\n" +
                "  }\n" +
                "}";

        SingleExecutionPlan plan = buildPlan(LOGICAL_MODEL + STORE_MODEL + MAPPING + RUNTIME + fetchFunction);

        Function<String, Callable<String>> generator = (businessDate) -> () ->
        {
            JsonStreamingResult res = (JsonStreamingResult) this.planExecutor.execute(plan, Maps.mutable.of("businessDate", businessDate));
            return res.flush(new JsonStreamToPureFormatSerializer(res));
        };

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Future<String>> results2023 = new ArrayList<>();
        List<Future<String>> results2024 = new ArrayList<>();
        for (int i = 0; i < 100; ++i)
        {
            results2023.add(executorService.submit(generator.apply("2023-01-01T00:00:00")));
            results2024.add(executorService.submit(generator.apply("2024-01-01T00:00:00")));
        }
        executorService.shutdown();
        Assert.assertTrue(executorService.awaitTermination(10, TimeUnit.MINUTES));

        String expected = "{\"legalName\":\"F1\",\"employees\":[{\"name\":\"P1\"},{\"name\":\"P2\"}]}";
        for (int i = 0; i < 100; ++i)
        {
            Assert.assertEquals(expected, objectMapper.readTree(results2023.get(i).get()).toString());
            Assert.assertEquals(expected, objectMapper.readTree(results2024.get(i).get()).toString());
        }
    }

    @Override
    protected void insertTestData(Statement s) throws SQLException
    {
        s.execute("Drop table if exists firm;");
        s.execute("Create Table firm(ID INT, LEGAL_NAME VARCHAR(100), BUS_FROM TIMESTAMP, BUS_THRU TIMESTAMP, PRIMARY KEY(ID, BUS_FROM));");
        s.execute("insert into firm (ID, LEGAL_NAME, BUS_FROM, BUS_THRU) VALUES (1, 'F1', '2022-01-01T00:00:00', '8888-12-31T00:00:00')");

        s.execute("Drop table if exists person;");
        s.execute("Create Table person(ID INT, NAME VARCHAR(100), FIRM_ID INT, BUS_FROM TIMESTAMP, BUS_THRU TIMESTAMP, PRIMARY KEY(ID, BUS_FROM));");
        s.execute("insert into person (ID, NAME, FIRM_ID, BUS_FROM, BUS_THRU) VALUES (1, 'P1', 1, '2022-01-01T00:00:00', '8888-12-31T00:00:00')");
        s.execute("insert into person (ID, NAME, FIRM_ID, BUS_FROM, BUS_THRU) VALUES (2, 'P2', 1, '2022-01-01T00:00:00', '8888-12-31T00:00:00')");
    }
}
