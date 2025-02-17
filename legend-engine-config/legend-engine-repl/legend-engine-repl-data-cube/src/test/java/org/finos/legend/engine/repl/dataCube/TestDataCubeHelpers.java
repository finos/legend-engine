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

package org.finos.legend.engine.repl.dataCube;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.repl.autocomplete.CompleterExtension;
import org.finos.legend.engine.repl.core.legend.LegendInterface;
import org.finos.legend.engine.repl.core.legend.LocalLegendInterface;
import org.finos.legend.engine.repl.dataCube.server.DataCubeHelpers;
import org.finos.legend.engine.repl.dataCube.server.model.DataCubeExecutionResult;
import org.finos.legend.engine.repl.relational.autocomplete.RelationalCompleterExtension;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.finos.legend.engine.repl.dataCube.server.DataCubeHelpers.executeQuery;
import static org.finos.legend.engine.repl.shared.ExecutionHelper.REPL_RUN_FUNCTION_SIGNATURE;

public class TestDataCubeHelpers
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final PlanExecutor planExecutor = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build();
    private final MutableList<CompleterExtension> completerExtensions = Lists.mutable.with(new RelationalCompleterExtension());
    private final LegendInterface legendInterface = new LocalLegendInterface();
    private final PureModelContextData pureModelContextData = legendInterface.parse(
            "###Relational\n" +
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
                    "   #>{test::TestDatabase.TEST0}#->filter(c | $c.FIRSTNAME != 'Doe')->select(~FIRSTNAME)->from(test::test)\n" +
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
                    "}"
    );

    @Test
    public void testExecuteSort()
    {
        String lambda = "|#>{test::TestDatabase.TEST0}#->filter(c | $c.FIRSTNAME != 'Doe')->sort([~FIRSTNAME->ascending()])->from(test::test)";
        String expectedResult = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"FIRSTNAME\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1024)\"},{\"name\":\"LASTNAME\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1024)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"test0_0\\\".FIRSTNAME as \\\"FIRSTNAME\\\", \\\"test0_0\\\".LASTNAME as \\\"LASTNAME\\\" from TEST0 as \\\"test0_0\\\" where (\\\"test0_0\\\".FIRSTNAME <> 'Doe' OR \\\"test0_0\\\".FIRSTNAME is null) order by \\\"FIRSTNAME\\\"\"}],\"result\":{\"columns\":[\"FIRSTNAME\",\"LASTNAME\"],\"rows\":[{\"values\":[\"John\",\"Doe\"]},{\"values\":[\"Nicole\",\"Doe\"]},{\"values\":[\"Tim\",\"Smith\"]}]}}";
        testExecuteQuery(expectedResult, lambda);
    }


    @Test
    public void testExecuteFilter()
    {
        String lambda = "|#>{test::TestDatabase.TEST0}#->filter(c | ($c.FIRSTNAME != 'Doe' && $c.LASTNAME != 'Doe'))->sort([~FIRSTNAME->ascending()])->from(test::test)";
        String expectedResult = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"FIRSTNAME\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1024)\"},{\"name\":\"LASTNAME\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1024)\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"test0_0\\\".FIRSTNAME as \\\"FIRSTNAME\\\", \\\"test0_0\\\".LASTNAME as \\\"LASTNAME\\\" from TEST0 as \\\"test0_0\\\" where ((\\\"test0_0\\\".FIRSTNAME <> 'Doe' OR \\\"test0_0\\\".FIRSTNAME is null) and (\\\"test0_0\\\".LASTNAME <> 'Doe' OR \\\"test0_0\\\".LASTNAME is null)) order by \\\"FIRSTNAME\\\"\"}],\"result\":{\"columns\":[\"FIRSTNAME\",\"LASTNAME\"],\"rows\":[{\"values\":[\"Tim\",\"Smith\"]}]}}";
        testExecuteQuery(expectedResult, lambda);
    }

    @Test
    public void testExecuteGroupBy()
    {
        String lambda = "|#>{test::TestDatabase.TEST0}#->filter(c | $c.FIRSTNAME != 'Doe')->groupBy(~[FIRSTNAME], ~[count: x | $x.FIRSTNAME : y | $y->count()])->from(test::test)";
        String expectedResult = "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"FIRSTNAME\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1024)\"},{\"name\":\"count\",\"type\":\"Integer\",\"relationalType\":\"INTEGER\"}]},\"activities\":[{\"_type\":\"relational\",\"sql\":\"select \\\"test0_0\\\".FIRSTNAME as \\\"FIRSTNAME\\\", count(\\\"test0_0\\\".FIRSTNAME) as \\\"count\\\" from TEST0 as \\\"test0_0\\\" where (\\\"test0_0\\\".FIRSTNAME <> 'Doe' OR \\\"test0_0\\\".FIRSTNAME is null) group by \\\"FIRSTNAME\\\"\"}],\"result\":{\"columns\":[\"FIRSTNAME\",\"count\"],\"rows\":[{\"values\":[\"John\",1]},{\"values\":[\"Nicole\",1]},{\"values\":[\"Tim\",1]}]}}";
        testExecuteQuery(expectedResult, lambda);
    }

    private void testExecuteQuery(String expectedResult, String code)
    {
        try
        {
            Lambda lambda = (Lambda) DataCubeHelpers.parseQuery(code, false);
            PureModelContextData data = DataCubeHelpers.injectNewFunction(pureModelContextData, lambda).getOne();
            DataCubeExecutionResult result = executeQuery(null, legendInterface, planExecutor, data, false);
            Assert.assertEquals(expectedResult, RelationalResultToJsonDefaultSerializer.removeComment(result.result));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Test
    public void testParseQuerySimple()
    {
        String lambda = "|#>{test::TestDatabase.TEST0}#->filter(c | $c.FIRSTNAME != 'Doe')->groupBy(~[FIRSTNAME], ~[count: x | $x.FIRSTNAME : y | $y->count()])->from(test::test)";
        String expectedQuery = "{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"from\",\"parameters\":[{\"_type\":\"func\",\"function\":\"groupBy\",\"parameters\":[{\"_type\":\"func\",\"function\":\"filter\",\"parameters\":[{\"_type\":\"classInstance\",\"type\":\">\",\"value\":{\"path\":[\"test::TestDatabase\",\"TEST0\"]}},{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"not\",\"parameters\":[{\"_type\":\"func\",\"function\":\"equal\",\"parameters\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"c\"}],\"property\":\"FIRSTNAME\"},{\"_type\":\"string\",\"value\":\"Doe\"}]}]}],\"parameters\":[{\"_type\":\"var\",\"name\":\"c\"}]}]},{\"_type\":\"classInstance\",\"type\":\"colSpecArray\",\"value\":{\"colSpecs\":[{\"name\":\"FIRSTNAME\"}]}},{\"_type\":\"classInstance\",\"type\":\"colSpecArray\",\"value\":{\"colSpecs\":[{\"function1\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}],\"property\":\"FIRSTNAME\"}],\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}]},\"function2\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"count\",\"parameters\":[{\"_type\":\"var\",\"name\":\"y\"}]}],\"parameters\":[{\"_type\":\"var\",\"name\":\"y\"}]},\"name\":\"count\"}]}}]},{\"_type\":\"packageableElementPtr\",\"fullPath\":\"test::test\"}]}],\"parameters\":[]}";
        testParseQuery(expectedQuery, lambda, false);
    }

    @Test
    public void testParseQuerySimpleWithSourceInformationReturned()
    {
        String lambda = "|#>{test::TestDatabase.TEST0}#->filter(c | $c.FIRSTNAME != 'Doe')->groupBy(~[FIRSTNAME], ~[count: x | $x.FIRSTNAME : y | $y->count()])->from(test::test)";
        String expectedQuery = "{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"from\",\"parameters\":[{\"_type\":\"func\",\"function\":\"groupBy\",\"parameters\":[{\"_type\":\"func\",\"function\":\"filter\",\"parameters\":[{\"_type\":\"classInstance\",\"sourceInformation\":{\"endColumn\":30,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":2,\"startLine\":1},\"type\":\">\",\"value\":{\"path\":[\"test::TestDatabase\",\"TEST0\"],\"sourceInformation\":{\"endColumn\":30,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":2,\"startLine\":1}}},{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"not\",\"parameters\":[{\"_type\":\"func\",\"function\":\"equal\",\"parameters\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"c\",\"sourceInformation\":{\"endColumn\":45,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":44,\"startLine\":1}}],\"property\":\"FIRSTNAME\",\"sourceInformation\":{\"endColumn\":55,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":47,\"startLine\":1}},{\"_type\":\"string\",\"sourceInformation\":{\"endColumn\":64,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":60,\"startLine\":1},\"value\":\"Doe\"}],\"sourceInformation\":{\"endColumn\":58,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":57,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":58,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":57,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"name\":\"c\"}],\"sourceInformation\":{\"endColumn\":64,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":42,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":38,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":33,\"startLine\":1}},{\"_type\":\"classInstance\",\"sourceInformation\":{\"endColumn\":87,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":77,\"startLine\":1},\"type\":\"colSpecArray\",\"value\":{\"colSpecs\":[{\"name\":\"FIRSTNAME\",\"sourceInformation\":{\"endColumn\":86,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":78,\"startLine\":1}}]}},{\"_type\":\"classInstance\",\"sourceInformation\":{\"endColumn\":133,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":91,\"startLine\":1},\"type\":\"colSpecArray\",\"value\":{\"colSpecs\":[{\"function1\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"property\",\"parameters\":[{\"_type\":\"var\",\"name\":\"x\",\"sourceInformation\":{\"endColumn\":104,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":103,\"startLine\":1}}],\"property\":\"FIRSTNAME\",\"sourceInformation\":{\"endColumn\":114,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":106,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}],\"sourceInformation\":{\"endColumn\":114,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":101,\"startLine\":1}},\"function2\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"count\",\"parameters\":[{\"_type\":\"var\",\"name\":\"y\",\"sourceInformation\":{\"endColumn\":123,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":122,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":130,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":126,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"name\":\"y\"}],\"sourceInformation\":{\"endColumn\":132,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":120,\"startLine\":1}},\"name\":\"count\",\"sourceInformation\":{\"endColumn\":132,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":92,\"startLine\":1}}]}}],\"sourceInformation\":{\"endColumn\":74,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":68,\"startLine\":1}},{\"_type\":\"packageableElementPtr\",\"fullPath\":\"test::test\",\"sourceInformation\":{\"endColumn\":151,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":142,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":140,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":137,\"startLine\":1}}],\"parameters\":[],\"sourceInformation\":{\"endColumn\":153,\"endLine\":1,\"sourceId\":\"\",\"startColumn\":1,\"startLine\":1}}";
        testParseQuery(expectedQuery, lambda, true);
    }

    private void testParseQuery(String expectedQuery, String code, boolean returnSourceInformation)
    {
        try
        {
            Assert.assertEquals(expectedQuery, objectMapper.writeValueAsString(DataCubeHelpers.parseQuery(code, returnSourceInformation)));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Test
    public void testGetQueryCodeStandard()
    {
        String lambda = "|#>{test::TestDatabase.TEST0}#->filter(c | $c.FIRSTNAME != 'Doe')->groupBy(~[FIRSTNAME], ~[count: x | $x.FIRSTNAME : y | $y->count()])->from(test::test)";
        String expectedQuery = "|#>{test::TestDatabase.TEST0}#->filter(c|$c.FIRSTNAME != 'Doe')->groupBy(~[FIRSTNAME], ~[count:x|$x.FIRSTNAME:y|$y->count()])->from(test::test)";
        testGetQueryCode(expectedQuery, lambda, false);
    }

    @Test
    public void testGetQueryCodePretty()
    {
        String lambda = "|#>{test::TestDatabase.TEST0}#->filter(c | $c.FIRSTNAME != 'Doe')->groupBy(~[FIRSTNAME], ~[count: x | $x.FIRSTNAME : y | $y->count()])->from(test::test)";
        String expectedQuery = "|#>{test::TestDatabase.TEST0}#->filter(\n" +
                "  c|$c.FIRSTNAME != 'Doe'\n" +
                ")->groupBy(\n" +
                "  ~[\n" +
                "     FIRSTNAME\n" +
                "   ],\n" +
                "  ~[\n" +
                "     count: x|$x.FIRSTNAME:y|$y->count()\n" +
                "   ]\n" +
                ")->from(\n" +
                "  test::test\n" +
                ")";
        testGetQueryCode(expectedQuery, lambda, true);
    }

    private void testGetQueryCode(String expectedQuery, String code, boolean pretty)
    {
        ValueSpecification query = DataCubeHelpers.parseQuery(code, false);
        Assert.assertEquals(expectedQuery, DataCubeHelpers.getQueryCode(query, pretty));
    }

    @Test
    public void testTypeaheadPartial()
    {
        String code = "->extend(~[newCol:c|'ok', colX: c|$c.";
        String expectedResult = "{\"completion\":[{\"completion\":\"FIRSTNAME\",\"display\":\"FIRSTNAME\"}]}";
        testTypeahead(expectedResult, code, (Lambda) DataCubeHelpers.parseQuery("|#>{test::TestDatabase.TEST0}#->filter(c | $c.FIRSTNAME != 'Doe')->select(~FIRSTNAME)->from(test::test)", false), pureModelContextData);
    }

    @Test
    public void testTypeaheadPartialWithDummySource()
    {
        // pmcd is empty and the query uses casting hack
        String code = "->extend(~[newCol:c|'ok', colX: c|$c.";
        String expectedResult = "{\"completion\":[{\"completion\":\"FIRSTNAME\",\"display\":\"FIRSTNAME\"}]}";
        PureModelContextData pmcd = PureModelContextData.newBuilder().build();
        testTypeahead(expectedResult, code, (Lambda) DataCubeHelpers.parseQuery("|''->cast(@meta::pure::metamodel::relation::Relation<(FIRSTNAME:String)>)", false), pmcd);

        // pmcd is minimal
        pmcd = legendInterface.parse(
                "###Relational\n" +
                        "Database test::TestDatabase\n" +
                        "(\n" +
                        "    Table TEST0\n" +
                        "    (\n" +
                        "       FIRSTNAME VARCHAR(200)\n" +
                        "     )\n" +
                        ")"
        );
        testTypeahead(expectedResult, code, (Lambda) DataCubeHelpers.parseQuery("|#>{test::TestDatabase.TEST0}#", false), pmcd);
    }

    @Test
    public void testTypeaheadFull()
    {
        String code = "#>{test::TestDatabase.TEST0}#->extend(~[newCol:c|'ok', colX: c|$c.";
        String expectedResult = "{\"completion\":[{\"completion\":\"FIRSTNAME\",\"display\":\"FIRSTNAME\"},{\"completion\":\"LASTNAME\",\"display\":\"LASTNAME\"}]}";
        testTypeahead(expectedResult, code, null, pureModelContextData);
    }

    @Test
    public void testTypeaheadFullWithError()
    {
        String code = "#>{test::TestDatabase.TEST0}#-->extend(~[newCol:c|'ok', colX: c|$c.";
        String expectedResult = "{\"completion\":[]}";
        testTypeahead(expectedResult, code, null, pureModelContextData);
    }

    private void testTypeahead(String expectedResult, String code, Lambda lambda, PureModelContextData data)
    {
        try
        {
            Assert.assertEquals(expectedResult, objectMapper.writeValueAsString(DataCubeHelpers.getCodeTypeahead(code, lambda, data, completerExtensions, legendInterface)));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Test
    public void testExtractRelationReturnTypeGroupBy()
    {
        String lambda = "|#>{test::TestDatabase.TEST0}#->filter(c | $c.FIRSTNAME != 'Doe')->groupBy(~[FIRSTNAME], ~[count: x | $x.FIRSTNAME : y | $y->count()])->from(test::test)";
        String expectedResult = "{\"_type\":\"relationType\",\"columns\":[{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":0,\"upperBound\":1},\"name\":\"FIRSTNAME\"},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"Integer\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"count\"}]}";
        testExtractRelationReturnType(expectedResult, lambda, pureModelContextData);
    }

    @Test
    public void testExtractRelationReturnTypeCast()
    {
        String lambda = "|#>{test::TestDatabase.TEST0}#->filter(c | $c.FIRSTNAME != 'Doe')->from(test::test)->groupBy(~[FIRSTNAME], ~[count: x | $x.FIRSTNAME : y | $y->count()])->cast(@meta::pure::metamodel::relation::Relation<(hai:String,ba:Integer)>)";
        String expectedResult = "{\"_type\":\"relationType\",\"columns\":[{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":0,\"upperBound\":1},\"name\":\"hai\"},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"Integer\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":0,\"upperBound\":1},\"name\":\"ba\"}]}";
        testExtractRelationReturnType(expectedResult, lambda, pureModelContextData);
    }

    @Test
    public void testExtractRelationReturnTypeSimpleExtend()
    {
        String lambda = "|#>{test::TestDatabase.TEST0}#->extend(~newCol:c|'ok')";
        String expectedResult = "{\"_type\":\"relationType\",\"columns\":[{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":0,\"upperBound\":1},\"name\":\"FIRSTNAME\"},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":0,\"upperBound\":1},\"name\":\"LASTNAME\"},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"newCol\"}]}";
        testExtractRelationReturnType(expectedResult, lambda, pureModelContextData);
    }

    @Test
    public void testExtractRelationReturnTypeMultipleExtend()
    {
        String lambda = "|#>{test::TestDatabase.TEST0}#->extend(~[newCol:c|'ok', colX: c|$c.FIRSTNAME])";
        String expectedResult = "{\"_type\":\"relationType\",\"columns\":[{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":0,\"upperBound\":1},\"name\":\"FIRSTNAME\"},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":0,\"upperBound\":1},\"name\":\"LASTNAME\"},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"newCol\"},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"colX\"}]}";
        testExtractRelationReturnType(expectedResult, lambda, pureModelContextData);
    }

    @Test
    public void testExtractRelationReturnTypeWithDummySource()
    {
        // pmcd is empty and the query uses casting hack
        String lambda = "|''->cast(@meta::pure::metamodel::relation::Relation<(FIRSTNAME:String,LASTNAME:String)>)->extend(~[newCol:c|'ok', colX: c|$c.FIRSTNAME])";
        PureModelContextData pmcd = PureModelContextData.newBuilder().build();
        String expectedResult = "{\"_type\":\"relationType\",\"columns\":[{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":0,\"upperBound\":1},\"name\":\"FIRSTNAME\"},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":0,\"upperBound\":1},\"name\":\"LASTNAME\"},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"newCol\"},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"colX\"}]}";
        testExtractRelationReturnType(expectedResult, lambda, pmcd);

        // pmcd is minimal
        lambda = "|#>{test::TestDatabase.TEST0}#->extend(~[newCol:c|'ok', colX: c|$c.FIRSTNAME])";
        pmcd = legendInterface.parse(
                "###Relational\n" +
                        "Database test::TestDatabase\n" +
                        "(\n" +
                        "    Table TEST0\n" +
                        "    (\n" +
                        "       FIRSTNAME VARCHAR(200),\n" +
                        "       LASTNAME   VARCHAR(200)\n" +
                        "     )\n" +
                        ")"
        );
        expectedResult = "{\"_type\":\"relationType\",\"columns\":[{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":0,\"upperBound\":1},\"name\":\"FIRSTNAME\"},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":0,\"upperBound\":1},\"name\":\"LASTNAME\"},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"newCol\"},{\"genericType\":{\"multiplicityArguments\":[],\"rawType\":{\"_type\":\"packageableType\",\"fullPath\":\"String\"},\"typeArguments\":[],\"typeVariableValues\":[]},\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"colX\"}]}";
        testExtractRelationReturnType(expectedResult, lambda, pmcd);
    }

    private void testExtractRelationReturnType(String expectedResult, String code, PureModelContextData data)
    {
        try
        {
            Lambda lambda = (Lambda) DataCubeHelpers.parseQuery(code, false);
            Assert.assertEquals(expectedResult, objectMapper.writeValueAsString(DataCubeHelpers.getRelationReturnType(legendInterface, lambda, data)));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Test
    public void testExtractRelationReturnTypeWithParserError()
    {
        String lambda = "|#>{test::TestDatabase.TEST0}#-->extend(~[newCol:c|'ok', colX: c|$c.FIRSTNAME])";
        testExtractRelationReturnTypeFailure("PARSER error at [1:31]: Unexpected token '-'. Valid alternatives: [';']", lambda, pureModelContextData);
    }

    @Test
    public void testExtractRelationReturnTypeWithCompilationError()
    {
        String lambda = "|#>{test::TestDatabase.TEST0}#->extend(~[newCol:c|'ok', colX: c|$c.FIRSTNAME2])";
        testExtractRelationReturnTypeFailure("COMPILATION error at [1:68-77]: The column 'FIRSTNAME2' can't be found in the relation (FIRSTNAME:String, LASTNAME:String)", lambda, pureModelContextData);

        // with dummy source
        PureModelContextData pmcd = legendInterface.parse(
                "###Relational\n" +
                        "Database test::TestDatabase\n" +
                        "(\n" +
                        "    Table TEST0\n" +
                        "    (\n" +
                        "       FIRSTNAME VARCHAR(200),\n" +
                        "       LASTNAME   VARCHAR(200)\n" +
                        "     )\n" +
                        ")"
        );
        testExtractRelationReturnTypeFailure("COMPILATION error at [1:68-77]: The column 'FIRSTNAME2' can't be found in the relation (FIRSTNAME:String, LASTNAME:String)", lambda, pmcd);
    }

    private void testExtractRelationReturnTypeFailure(String errorMessage, String code, PureModelContextData data)
    {
        EngineException e = Assert.assertThrows(EngineException.class, () ->
        {
            Lambda lambda = (Lambda) DataCubeHelpers.parseQuery(code, true);
            DataCubeHelpers.getRelationReturnType(legendInterface, lambda, data);
        });
        Assert.assertEquals(errorMessage, EngineException.buildPrettyErrorMessage(e.getMessage(), e.getSourceInformation(), e.getErrorType()));
    }
}
