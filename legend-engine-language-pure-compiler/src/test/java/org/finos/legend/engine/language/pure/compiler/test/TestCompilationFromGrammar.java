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
    public void testCompilationPureDuplicateSetIdError()
    {
        TestCompilationFromGrammarTestSuite.test("###Pure\n" +
                "Class simple::Account\n" +
                "{\n" +
                "   id: String[1];   \n" +
                "}\n" +
                "\n" +
                "Class simple::Raw_Account\n" +
                "{\n" +
                "   id: String[1];\n" +
                "}\n" +
                "\n" +
                "Class simple::Another extends simple::Account\n" +
                "{  \n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping simple::gen1::map\n" +
                "(\n" +
                "   simple::Account[id]: Pure\n" +
                "   {\n" +
                "      ~src simple::Raw_Account\n" +
                "      id: $src.id\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping simple::gen2::map\n" +
                "(\n" +
                "   simple::Account[id]: Pure\n" +
                "   {\n" +
                "      ~src simple::Raw_Account\n" +
                "      id: $src.id\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping simple::merged(\n" +
                "   include simple::gen1::map\n" +
                "   include simple::gen2::map\n" +
                "      \n" +
                "   simple::Another extends [id]: Pure\n" +
                "   {      \n" +
                "   }   \n" +
                ")", "COMPILATION error at [35:1-42:1]: Duplicated class mappings found with ID 'id' in mapping 'simple::merged'");
    }


}
