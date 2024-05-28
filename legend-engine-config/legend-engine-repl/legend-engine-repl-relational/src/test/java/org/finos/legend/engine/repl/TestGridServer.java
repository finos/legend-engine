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

package org.finos.legend.engine.repl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.repl.core.legend.LegendInterface;
import org.finos.legend.engine.repl.core.legend.LocalLegendInterface;
import org.finos.legend.engine.repl.relational.httpServer.ReplGridServer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.repl.core.Helpers.REPL_RUN_FUNCTION_QUALIFIED_PATH;
import static org.finos.legend.engine.repl.core.Helpers.REPL_RUN_FUNCTION_SIGNATURE;

public class TestGridServer
{
    private String pmcd = "###Relational\n" +
            "Database test::TestDatabase\n" +
            "(\n" +
            "    Table TEST0\n" +
            "    (\n" +
            "       FIRSTNAME VARCHAR(200),\n" +
            "       LASTNAME   VARCHAR(200)\n" +
            "     )\n" +
            ")\n" +
            "\n" +
            "###Pure\n" +
            "function " + REPL_RUN_FUNCTION_SIGNATURE + "\n" +
            "{\n" +
            "   #>{test::TestDatabase.TEST0}#->filter(c | $c.FIRSTNAME != 'Doe')->from(^meta::pure::mapping::Mapping(), test::test)\n" +
            "}\n" +
            "\n" +
            "###Runtime\n" +
            "Runtime test::test\n" +
            "{\n" +
            "    mappings: [];\n" +
            "    connections:\n" +
            "    [\n" +
            "       test::TestDatabase:\n" +
            "       [ \n" +
            "         connection: test::TestConnection\n" +
            "       ]\n" +
            "\n" +
            "    ];\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Connection\n" +
            "RelationalDatabaseConnection test::TestConnection\n" +
            "{\n" +
            "  store: test::TestDatabase;\n" +
            "  type: H2;\n" +
            "  specification: LocalH2\n" +
            "  {\n" +
            "    testDataSetupSqls: [\n" +
            "      '\\nDrop table if exists TEST0;\\nCreate Table TEST0(FIRSTNAME VARCHAR(200), LASTNAME VARCHAR(200));\\nInsert into TEST0 (FIRSTNAME, LASTNAME) values (\\'John\\', \\'Doe\\');\\nInsert into TEST0 (FIRSTNAME, LASTNAME) values (\\'Tim\\', \\'Smith\\');\\nInsert into TEST0 (FIRSTNAME, LASTNAME) values (\\'Nicole\\', \\'Doe\\');\\n\\n'\n" +
            "      ];\n" +
            "  };\n" +
            "  auth: DefaultH2;\n" +
            "}";
    private LegendInterface legendInterface = new LocalLegendInterface();
    private ObjectMapper objectMapper = new ObjectMapper();
    private PureModelContextData pureModelContextData = legendInterface.parse(pmcd);

    @Test
    public void testSort()
    {
        String lambda = "#>{test::TestDatabase.TEST0}#->filter(c | $c.FIRSTNAME != 'Doe')->from(^meta::pure::mapping::Mapping(), test::test)->sort([~FIRSTNAME->ascending()])";
        String expectedResult = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"FIRSTNAME\",\"type\":\"String\"},{\"name\":\"LASTNAME\",\"type\":\"String\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"test0_0\\\".FIRSTNAME as \\\"FIRSTNAME\\\", \\\"test0_0\\\".LASTNAME as \\\"LASTNAME\\\" from TEST0 as \\\"test0_0\\\" where (\\\"test0_0\\\".FIRSTNAME <> 'Doe' OR \\\"test0_0\\\".FIRSTNAME is null) order by \\\"FIRSTNAME\\\"\"}],\"result\":{\"columns\":[\"FIRSTNAME\",\"LASTNAME\"],\"rows\":[{\"values\":[\"John\",\"Doe\"]},{\"values\":[\"Nicole\",\"Doe\"]},{\"values\":[\"Tim\",\"Smith\"]}]}}";
        test(expectedResult, lambda);
    }

    @Test
    public void testFilter()
    {
        String lambda = "#>{test::TestDatabase.TEST0}#->filter(c | ($c.FIRSTNAME != 'Doe' && $c.LASTNAME != 'Doe'))->from(^meta::pure::mapping::Mapping(), test::test)->sort([~FIRSTNAME->ascending()])";
        String expectedResult = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"FIRSTNAME\",\"type\":\"String\"},{\"name\":\"LASTNAME\",\"type\":\"String\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"test0_0\\\".FIRSTNAME as \\\"FIRSTNAME\\\", \\\"test0_0\\\".LASTNAME as \\\"LASTNAME\\\" from TEST0 as \\\"test0_0\\\" where ((\\\"test0_0\\\".FIRSTNAME <> 'Doe' OR \\\"test0_0\\\".FIRSTNAME is null) and (\\\"test0_0\\\".LASTNAME <> 'Doe' OR \\\"test0_0\\\".LASTNAME is null)) order by \\\"FIRSTNAME\\\"\"}],\"result\":{\"columns\":[\"FIRSTNAME\",\"LASTNAME\"],\"rows\":[{\"values\":[\"Tim\",\"Smith\"]}]}}";
        test(expectedResult, lambda);
    }

    @Test
    public void testGroupBy()
    {
        String lambda = "#>{test::TestDatabase.TEST0}#->filter(c | $c.FIRSTNAME != 'Doe')->from(^meta::pure::mapping::Mapping(), test::test)->groupBy(~[FIRSTNAME], ~[count: x | $x.FIRSTNAME : y | $y->count()])";
        String expectedResult = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"FIRSTNAME\",\"type\":\"String\"},{\"name\":\"count\",\"type\":\"Integer\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"test0_0\\\".FIRSTNAME as \\\"FIRSTNAME\\\", count(\\\"test0_0\\\".FIRSTNAME) as \\\"count\\\" from TEST0 as \\\"test0_0\\\" where (\\\"test0_0\\\".FIRSTNAME <> 'Doe' OR \\\"test0_0\\\".FIRSTNAME is null) group by \\\"FIRSTNAME\\\"\"}],\"result\":{\"columns\":[\"FIRSTNAME\",\"count\"],\"rows\":[{\"values\":[\"John\",1]},{\"values\":[\"Nicole\",1]},{\"values\":[\"Tim\",1]}]}}";
        test(expectedResult, lambda);
    }

    @Test
    public void testSlice()
    {
        String lambda = "#>{test::TestDatabase.TEST0}#->filter(c | ($c.FIRSTNAME != 'Doe' && $c.LASTNAME != 'Doe'))->from(^meta::pure::mapping::Mapping(), test::test)";
        String expectedResult = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"FIRSTNAME\",\"type\":\"String\"},{\"name\":\"LASTNAME\",\"type\":\"String\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select top 100 \\\"test0_0\\\".FIRSTNAME as \\\"FIRSTNAME\\\", \\\"test0_0\\\".LASTNAME as \\\"LASTNAME\\\" from TEST0 as \\\"test0_0\\\" where ((\\\"test0_0\\\".FIRSTNAME <> 'Doe' OR \\\"test0_0\\\".FIRSTNAME is null) and (\\\"test0_0\\\".LASTNAME <> 'Doe' OR \\\"test0_0\\\".LASTNAME is null))\"}],\"result\":{\"columns\":[\"FIRSTNAME\",\"LASTNAME\"],\"rows\":[{\"values\":[\"Tim\",\"Smith\"]}]}}";
        test(expectedResult, lambda, true);
    }

    private void test(String expectedResult, String function)
    {
        test(expectedResult, function, false);
    }

    private void test(String expectedResult, String function, boolean isPaginationEnabled)
    {
        try
        {
            Function originalFunction = (Function) pureModelContextData.getElements().stream().filter(e -> e.getPath().equals(REPL_RUN_FUNCTION_QUALIFIED_PATH)).collect(Collectors.toList()).get(0);
            ValueSpecification originalFunctionBody = originalFunction.body.get(0);
            AppliedFunction currentFunction = (AppliedFunction) PureGrammarParser.newInstance().parseValueSpecification(function, null, 0, 0, true);
            List<ValueSpecification> newBody = Lists.mutable.of(currentFunction);
            if (isPaginationEnabled)
            {
                ReplGridServer.applySliceFunction(newBody);
            }
            originalFunction.body = newBody;
            String response = ReplGridServer.executeLambda(legendInterface, pureModelContextData, originalFunction, originalFunctionBody);
            ReplGridServer.GridServerResult result = objectMapper.readValue(response, ReplGridServer.GridServerResult.class);
            Assert.assertEquals(expectedResult, RelationalResultToJsonDefaultSerializer.removeComment(result.getResult()));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }
}
