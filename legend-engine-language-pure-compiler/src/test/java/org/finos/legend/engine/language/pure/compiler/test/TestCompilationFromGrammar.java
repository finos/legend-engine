// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.Test;

public class TestCompilationFromGrammar
{
    public abstract static class TestCompilationFromGrammarTestSuite
    {
        protected abstract String getDuplicatedElementTestCode();

        protected abstract String getDuplicatedElementTestExpectedErrorMessage();

        @Test
        public void testDuplicatedElement()
        {
            test(this.getDuplicatedElementTestCode(), this.getDuplicatedElementTestExpectedErrorMessage());
        }

        public static Pair<PureModelContextData, PureModel> test(String str)
        {
            return test(str, null);
        }

        public static Pair<PureModelContextData, PureModel> test(String str, String expectedErrorMsg)
        {
            try
            {
                // do a full re-serialization after parsing to make sure the protocol produced is proper
                PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(str);
                ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
                String json = objectMapper.writeValueAsString(modelData);
                modelData = objectMapper.readValue(json, PureModelContextData.class);
                PureModel pureModel = Compiler.compile(modelData, DeploymentMode.TEST, null);
                if (expectedErrorMsg != null)
                {
                    Assert.fail("Expected compilation error with message: " + expectedErrorMsg + "; but no error occurred");
                }
                return Tuples.pair(modelData, pureModel);
            }
            catch (EngineException e)
            {
                if (expectedErrorMsg == null)
                {
                    throw e;
                }

                Assert.assertNotNull("No source information provided in error", e.getSourceInformation());
                Assert.assertEquals(expectedErrorMsg, EngineException.buildPrettyErrorMessage(e.getMessage(), e.getSourceInformation(), e.getErrorType()));
                return null;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void testCompilationFromGrammarWithParsingError()
    {
        TestCompilationFromGrammarTestSuite.test("Class example::MyTest\n" +
                "{\n" +
                "p:String[1];\n" +
                "}\n" +
                "###Pure\n" +
                "importd example::*;\n" +
                "function example::testMatch(test:MyTest[1]): MyTest[1]\n" +
                "{\n" +
                "  $test->match([ a:MyTest[1]|$a ]);\n" +
                "}", "PARSER error at [6:1-7]: Unexpected token 'importd'");
    }

    @Test
    public void testCompilePathVariable()
    {
        TestCompilationFromGrammarTestSuite.test("Class test::Person\n" +
                "{\n" +
                "  firstName:String[1];\n" +
                "  age: Integer[1];\n" +
                "}\n" +
                "\n" +
                "function test::usePath(p:test::Person[1]):Boolean[1]\n" +
                "{\n" +
                "  let path = #/test::Person/firstName#;\n" +
                "  test::Person.all()->project($path)->filter(r|$r.getInteger('age') > 30 && $r.isNotNull('age'))->isNotEmpty();\n" +
                "}", null);
    }

    @Test
    public void testCompilePathVariableMultipleAssignments()
    {
        TestCompilationFromGrammarTestSuite.test("Class test::Person\n" +
                "{\n" +
                "  firstName:String[1];\n" +
                "  age: Integer[1];\n" +
                "}\n" +
                "\n" +
                "function test::usePath(p:test::Person[1]):Boolean[1]\n" +
                "{\n" +
                "  let path = #/test::Person/firstName#;\n" +
                "  let x = $path;\n" +
                "  test::Person.all()->project($x)->filter(r|$r.getInteger('age') > 30 && $r.isNotNull('age'))->isNotEmpty();\n" +
                "}", null);
    }

    @Test
    public void testCompileLambdaVariable()
    {
        TestCompilationFromGrammarTestSuite.test("function test::func(): Any[*]\n" +
                "{\n" +
                "  let s = {| 'sample string'}; $s->eval();\n" +
                "} ", null);
    }

    @Test
    public void testCompilationFromGrammarWithMergeOperation()
    {
        TestCompilationFromGrammarTestSuite.test("Class  example::SourcePersonWithFirstName\n" +
                "{\n" +
                "   id:Integer[1];\n" +
                "   firstName:String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "Class example::SourcePersonWithLastName\n" +
                "{\n" +
                "   id:Integer[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +
                "Class example::Person\n" +
                "{\n" +
                "   firstName:String[1];\n" +
                "   lastName:String[1];\n" +
                "}\n" +

                "\n" +

                "###Mapping\n" +
                "Mapping  example::MergeModelMappingSourceWithMatch\n" +
                "(\n" +
                "   *example::Person : Operation\n" +
                "           {\n" +
                "             meta::pure::router::operations::merge_OperationSetImplementation_1__SetImplementation_MANY_([p1,p2,p3],{p1:example::SourcePersonWithFirstName[1], p2:example::SourcePersonWithLastName[1],p4:example::SourcePersonWithLastName[1] | $p1.id ==  $p2.id })\n" +

                "           }\n" +
                "\n" +
                "   example::Person[p1] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithFirstName\n" +
                "               firstName : $src.firstName\n" +
                "            }\n" +
                "\n" +
                "   example::Person[p2] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithLastName\n" +
                "        lastName :  $src.lastName\n" +
                "            }\n" +
                "   example::Person[p3] : Pure\n" +
                "            {\n" +
                "               ~src example::SourcePersonWithLastName\n" +
                "        lastName :  $src.lastName\n" +
                "            }\n" +

                "\n" +
                ")");
    }

    @Test
    public void testFunctionTestSimplePrimitiveValueParametersWithName()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function trial2::hello(name: String[1]): String[1]\n" +
                        "<\n" +
                        "test1: {[name = 'Sharvani'], 'Hello, Sharvani'}\n" +
                        ">\n" +
                        "{\n" +
                        "   let result = 'Hello, ' + $name;\n" +
                        "   $result;\n" +
                        "}\n", null);
    }

    @Test
    public void testFunctionTestSimplePrimitiveValueParametersWithoutName()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function trial2::hello(name: String[1]): String[1]\n" +
                        "<\n" +
                        "test1: {['Sharvani'], 'Hello, Sharvani'}\n" +
                        ">\n" +
                        "{\n" +
                        "   let result = 'Hello, ' + $name;\n" +
                        "   $result;\n" +
                        "}\n", null);
    }

    @Test
    public void testFunctionTestBasicComplexValueAssert()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class my::Firm\n" +
                        "{\n" +
                        "  firmName: String[1];\n" +
                        "}\n\n" +
                        "function trial2::world(name: String[1]): my::Firm[1]\n" +
                        "<\n" +
                        "   test1: { [name = 'GS'], #{{\"firmName\":\"GS\"}}# }\n" +
                        ">\n" +
                        "{\n" +
                        "   ^my::Firm(firmName=$name)\n" +
                        "}\n"
        );
    }

    @Test
    public void testFunctionTestBasicComplexValueAssertMultipleLine()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class my::Firm\n" +
                        "{\n" +
                        "  firmName: String[1];\n" +
                        "}\n\n" +
                        "function trial2::world(name: String[1]): my::Firm[1]\n" +
                        "<\n" +
                        "   test1: {[name = 'GS'], #{\n" +
                        "{\n" +
                        "\"firmName\":\n \"GS\"" +
                        "\n}\n" +
                        "}#}\n" +
                        ">\n" +
                        "{\n" +
                        "   ^my::Firm(firmName=$name)\n" +
                        "}\n"
        );
    }

    @Test
    public void testFunctionTestBasicComplexValueParameterAndAssert()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class my::Firm\n" +
                        "{\n" +
                        "  firmName: String[1];\n" +
                        "}\n\n" +
                        "function trial2::world(firm: my::Firm[1]): my::Firm[1]\n" +
                        "<\n" +
                        "   test1: {[#{{ \"firmName\": \"\"} }# ], #{ {\"firmName\": \"GS\"}}#}\n" +
                        ">\n" +
                        "{\n" +
                        "   ^my::Firm(firmName=$firm)\n" +
                        "}\n"
        );
    }

    @Test
    public void testFunctionTestBasicComplexValueParameterAndAssertInvalidJSON()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "Class my::Firm\n" +
                        "{\n" +
                        "  firmName: String[1];\n" +
                        "}\n\n" +
                        "function trial2::world(firm: my::Firm[1]): my::Firm[1]\n" +
                        "<\n" +
                        "   test1: {[ #{ \"firmName\": \"\"} }# ], #{ {\"firmName\": \"GS\"} }#}\n" +
                        ">\n" +
                        "{\n" +
                        "   ^my::Firm(firmName=$firm)\n" +
                        "}\n", "COMPILATION error at [9:4-63]: Invalid JSON format for parameter: 'firm'"
        );
    }

    @Test
    public void testFunctionTestParameterInvalidOrder()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                        "<\n" +
                        "    test1: {[place='Alice', name='Wonderland'], 'Alice in Wonderland'}\n" +
                        ">\n" +
                        "{\n" +
                        "    let result = $name + ' in ' + $place;\n" +
                        "    $result;\n" +
                        "}\n", "COMPILATION error at [4:5-70]: Function test parameter name does not match the input parameter name defined in the function"
        );
    }

    @Test
    public void testFunctionTestParameterInvalidType()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                        "<\n" +
                        "    test1: {[name='Alice', place=9.9], 'Alice in Wonderland'}\n" +
                        ">\n" +
                        "{\n" +
                        "    let result = $name + ' in ' + $place;\n" +
                        "    $result;\n" +
                        "}\n", "COMPILATION error at [4:5-61]: Parameter value type does not match with parameter type for parameter: 'place'"
        );
    }

    @Test
    public void testFunctionTestParameterOptional()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function trial2::hello(name: String[1], place: String[0..1]):String[1]\n" +
                        "<\n" +
                        "    test1: {[name='Alice', place=[]], 'Alice in Wonderland'}\n" +
                        ">\n" +
                        "{\n" +
                        "    let result = $name + ' in ';\n" +
                        "    $result;\n" +
                        "}\n"
        );
    }

    @Test
    public void testFunctionTestOptionalParameterOptionalAssertString()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function trial2::hello(name: String[0..1], place: String[0..1]):String[0..1]\n" +
                        "<\n" +
                        "    test1: {[name=[], place=[]], []}\n" +
                        ">\n" +
                        "{\n" +
                        "    $name;\n" +
                        "}\n"
        );
    }

    @Test
    public void testFunctionTestOptionalReturnOptionalInputInteger()
    {
        TestCompilationFromGrammarTestSuite.test(
                "function trial::optionalParameter(var1: Integer[0..1]): Integer[0..1]\n" +
                        " <\n" +
                        " test1: {[ [] ], []}\n" +
                        ">\n" +
                        "{\n" +
                        "  if ($var1->isEmpty(), \n" +
                        "  | [],\n" +
                        "  | $var1->max()\n" +
                        "  )\n" +
                        "}"
        );
    }

    @Test
    public void testFunctionTestParameterInvalidNumberOfArguments()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                        "<\n" +
                        "    test1: {[name='Alice', place='Wonderland', extraParam = '!!'], 'Alice in Wonderland'}\n" +
                        ">\n" +
                        "{\n" +
                        "    let result = $name + ' in ' + $place;\n" +
                        "    $result;\n" +
                        "}\n", "COMPILATION error at [4:5-89]: Number of parameters passed in the function test do not match the number of input parameters defined in the function."
        );
    }

    @Test
    public void testFunctionTestParameterInvalidComplexParameterMissingJSONValue()
    {
            TestCompilationFromGrammarTestSuite.test(
                    "###Pure\n" +
                            "Class my::Firm\n" +
                            "{\n" +
                            "  firmName: String[1];\n" +
                            "}\n\n" +
                            "function trial2::world(firm: my::Firm[1]): my::Firm[1]\n" +
                            "<\n" +
                            "   test1: {[ #{ {\"firmName\":  } }# ], #{ {\"firmName\": \"GS\"} }#}\n" +
                            ">\n" +
                            "{\n" +
                            "   ^my::Firm(firmName=$firm)\n" +
                            "}\n","COMPILATION error at [9:4-63]: Invalid JSON format for parameter: 'firm'"
            );
    }

    @Test
    public void testFunctionTestDuplicateTestId()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function trial2::hello(name: String[1], place: String[1]):String[1]\n" +
                        "<\n" +
                        "    test1: {[name='Alice', place='Wonderland'], 'Alice in Wonderland'},\n" +
                        "    test1: {[name='Alice', place='Wonderland'], 'Alice in Wonderland'}\n" +
                        ">\n" +
                        "{\n" +
                        "    let result = $name + ' in ' + $place;\n" +
                        "    $result;\n" +
                        "}\n", "COMPILATION error at [2:1-10:1]: Function Test Ids should be unique"
        );
    }

    @Test
    public void testFunctionTestZeroReturnMultiplicity()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function trial2::hello(name: String[1], place: String[1]):String[0]\n" +
                        "<\n" +
                        "    test1: {[name='Alice', place='Wonderland'], 'Alice in Wonderland'}\n" +
                        ">\n" +
                        "{\n" +
                        "    let result = $name + ' in ' + $place;\n" +
                        "    $result;\n" +
                        "}\n", "COMPILATION error at [2:1-9:1]: Return multiplicity should not be Nil for a function with function test"
        );
    }

    @Test
    public void testFunctionTestAssertMismatchingType()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function trial2::hello(name: String[1], place: String[1]):Integer[3]\n" +
                        "<\n" +
                        "    test1: {[name='Alice', place='Wonderland'], 'String'}\n" +
                        ">\n" +
                        "{\n" +
                        "    let result = [1,2,3];\n" +
                        "    $result;\n" +
                        "}\n", "COMPILATION error at [4:5-57]: Function Test assert value type does not match with return type for function"
        );
    }

    @Test
    public void testFunctionTestAssertMismatchingMultiplicity()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function trial2::hello(name: String[1], place: String[1]):Integer[3]\n" +
                        "<\n" +
                        "    test1: {[name='Alice', place='Wonderland'], [1,2]}\n" +
                        ">\n" +
                        "{\n" +
                        "    let result = [1,2,3];\n" +
                        "    $result;\n" +
                        "}\n", "COMPILATION error at [4:5-54]: Function Test assert value type does not match with return type for function"
        );
    }

    @Test
    public void testFunctionTestAssertOptionalZeroReturn()
    {
        TestCompilationFromGrammarTestSuite.test(
                "###Pure\n" +
                        "function trial2::hello(name: String[1], place: String[1]):Integer[0..1]\n" +
                        "<\n" +
                        "    test1: {[name='Alice', place='Wonderland'], []}\n" +
                        ">\n" +
                        "{\n" +
                        "    let result = [];\n" +
                        "    $result;\n" +
                        "}\n"
        );
    }

    @Test
    public void testFunctionTestOptionalReturnListInput()
    {
        TestCompilationFromGrammarTestSuite.test(
                "function trial::ListParameter(var1: Integer[*]): Integer[0..1]\n" +
                        " <\n" +
                        " test1: {[ [] ], []}\n" +
                        ">\n" +
                        "{\n" +
                        "  if ($var1->isEmpty(), \n" +
                        "  | [],\n" +
                        "  | $var1->max()\n" +
                        "  )\n" +
                        "}"
        );
    }

    @Test
    public void testFunctionTestListReturnListInput()
    {
        TestCompilationFromGrammarTestSuite.test(
                "function trial::ListParameter(var1: Integer[*]): Integer[*]\n" +
                        "<\n" +
                        "test1: {[ [1,2] ], [1,2]}\n" +
                        ">\n" +
                        "{\n" +
                        "  if ($var1->isEmpty(), \n" +
                        "  | [],\n" +
                        "  | $var1\n" +
                        "  )\n" +
                        "}"
        );
    }

    @Test
    public void testFunctionTestEmptyListReturnEmptyListInput()
    {
        TestCompilationFromGrammarTestSuite.test(
                "function trial::ListParameter(var1: Integer[*]): Integer[*]\n" +
                        " <\n" +
                        " test1: {[ [] ], []}\n" +
                        ">\n" +
                        "{\n" +
                        "  if ($var1->isEmpty(), \n" +
                        "  | [],\n" +
                        "  | $var1\n" +
                        "  )\n" +
                        "}"
        );
    }

}
