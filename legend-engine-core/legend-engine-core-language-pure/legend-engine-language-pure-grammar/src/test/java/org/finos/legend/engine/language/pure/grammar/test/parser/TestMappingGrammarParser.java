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

package org.finos.legend.engine.language.pure.grammar.test.parser;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.enumerationMapping.EnumerationMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.operationClassMapping.OperationClassMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.pureInstanceClassMapping.PureInstanceClassMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.xStoreAssociationMapping.XStoreAssociationMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CLatestDate;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.junit.Test;

import java.util.List;

public class TestMappingGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return MappingParserGrammar.VOCABULARY;
    }

    @Override
    public List<Vocabulary> getDelegatedParserGrammarVocabulary()
    {
        return FastList.newListWith(
                EnumerationMappingParserGrammar.VOCABULARY,
                OperationClassMappingParserGrammar.VOCABULARY,
                PureInstanceClassMappingParserGrammar.VOCABULARY,
                XStoreAssociationMappingParserGrammar.VOCABULARY
        );
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Mapping\n" +
                "Mapping " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "(\n" +
                // enumeration mapping
                "  anything::else::goes: EnumerationMapping TargetTradeTypeMapping2\n" +
                "  {\n" +
                "    TT3: [anything::else::goes::" + ListAdapter.adapt(keywords).makeString("::") + ".MT21]\n" +
                "  }\n" +
                // operation class mapping
                "  *anything::goes[anything_goes]: Operation\n" +
                "  {\n" +
                "    meta::pure::router::" + ListAdapter.adapt(keywords).makeString("::") + "::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(p1,p2)\n" +
                "  }\n" +
                // pure instance class mapping
                "  *anything::goes[anything_goes]: Pure\n" +
                "  {\n" +
                "    ~src anything::goes::" + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    incType : EnumerationMapping a : 1" +
                "  }\n" +
                "\n" +
                ")\n";
    }

    @Test
    public void testEmptyGraphFetchTree()
    {
        test("###Mapping\n" +
                "Mapping testing::mapping\n" +
                "(\n" +
                "  *testing::Person[testing_Person]: Pure\n" +
                "  {\n" +
                "    ~src testing::other::NPerson\n" +
                "    fullName: $src.firstName + ' ' + $src.lastName\n" +
                "  }\n" +
                "\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test_1\n" +
                "    (\n" +
                "      query: |testing::Person.all()->graphFetchChecked(#{}#)->serialize(#{}#);\n" +
                "      data:\n" +
                "      [\n" +
                "        <Object, JSON, testing::other::NPerson, '{\"firstName\":\"firstName 7\",\"lastName\":\"lastName 55\"}'>\n" +
                "      ];\n" +
                "      assert: '{}';\n" +
                "    )\n" +
                "  ]\n" +
                ")\n", "PARSER error at [14:56-59]: Graph fetch tree must not be empty");
        test("###Mapping\n" +
                "Mapping testing::mapping\n" +
                "(\n" +
                "  *testing::Person[testing_Person]: Pure\n" +
                "  {\n" +
                "    ~src testing::other::NPerson\n" +
                "    fullName: $src.firstName + ' ' + $src.lastName\n" +
                "  }\n" +
                "\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test_1\n" +
                "    (\n" +
                "      query: |testing::Person.all()->graphFetchChecked(#{testing::Person{}}#)->serialize(#{testing::Person{}}#);\n" +
                "      data:\n" +
                "      [\n" +
                "        <Object, JSON, testing::other::NPerson, '{\"firstName\":\"firstName 7\",\"lastName\":\"lastName 55\"}'>\n" +
                "      ];\n" +
                "      assert: '{}';\n" +
                "    )\n" +
                "  ]\n" +
                ")\n", "PARSER error at [14:74]: Unexpected token '}'");
    }

    @Test
    public void testLambdaWithFunctionAllWithoutFunctionCaller()
    {
        test("###Mapping\n" +
                "Mapping testing::mapping\n" +
                "(\n" +
                "  *testing::Person[testing_Person]: Pure\n" +
                "  {\n" +
                "    ~src testing::other::NPerson\n" +
                "    fullName: ::.all()\n" +
                "  }\n" +
                ")\n", "PARSER error at [7:15-22]: Expected a non-empty function caller");
    }

    @Test
    public void testSpecialMappingElementId()
    {
        test("###Mapping\n" +
                "Mapping mapping\n" +
                "(\n" +
                "  Person[true]: Pure\n" +
                "  {\n" +
                "  }\n" +
                "  Person[false]: Pure\n" +
                "  {\n" +
                "  }\n" +
                "  Person[1]: Pure\n" +
                "  {\n" +
                "  }\n" +
                "  Person[true]: EnumerationMapping 1\n" +
                "  {\n" +
                "      a : ['a']\n" +
                "  }\n" +
                "  Person[true]: EnumerationMapping true\n" +
                "  {\n" +
                "      a : ['a']\n" +
                "  }\n" +
                "  Person[true]: EnumerationMapping false\n" +
                "  {\n" +
                "      a : ['a']\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testMappingTest()
    {
        // Missing fields
        test("###Mapping\n" +
                "Mapping testing::mapping\n" +
                "(\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test_1\n" +
                "    (\n" +
                "      assert: '{}';\n" +
                "      data:[<Object, JSON, testing::other::NPerson, '{\"firstName\":\"firstName 7\",\"lastName\":\"lastName 55\"}'>];\n" +
                "    )\n" +
                "  ]\n" +
                ")\n", "PARSER error at [6:5-10:5]: Field 'query' is required");
        test("###Mapping\n" +
                "Mapping testing::mapping\n" +
                "(\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test_1\n" +
                "    (\n" +
                "      assert: '{}';\n" +
                "      query: |testing::Person.all();\n" +
                "    )\n" +
                "  ]\n" +
                ")\n", "PARSER error at [6:5-10:5]: Field 'data' is required");
        test("###Mapping\n" +
                "Mapping testing::mapping\n" +
                "(\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test_1\n" +
                "    (\n" +
                "      query: |testing::Person.all();\n" +
                "      data:[<Object, JSON, testing::other::NPerson, '{\"firstName\":\"firstName 7\",\"lastName\":\"lastName 55\"}'>];\n" +
                "    )\n" +
                "  ]\n" +
                ")\n", "PARSER error at [6:5-10:5]: Field 'assert' is required");
        // Duplicated fields
        test("###Mapping\n" +
                "Mapping testing::mapping\n" +
                "(\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test_1\n" +
                "    (\n" +
                "      query: |testing::Person.all();\n" +
                "      query: |testing::Person.all();\n" +
                "      data:[<Object, JSON, testing::other::NPerson, '{\"firstName\":\"firstName 7\",\"lastName\":\"lastName 55\"}'>];\n" +
                "      assert: '{}';\n" +
                "    )\n" +
                "  ]\n" +
                ")\n", "PARSER error at [6:5-12:5]: Field 'query' should be specified only once");
        test("###Mapping\n" +
                "Mapping testing::mapping\n" +
                "(\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test_1\n" +
                "    (\n" +
                "      data:[<Object, JSON, testing::other::NPerson, '{\"firstName\":\"firstName 7\",\"lastName\":\"lastName 55\"}'>];\n" +
                "      query: |testing::Person.all();\n" +
                "      data:[<Object, JSON, testing::other::NPerson, '{\"firstName\":\"firstName 7\",\"lastName\":\"lastName 55\"}'>];\n" +
                "      assert: '{}';\n" +
                "    )\n" +
                "  ]\n" +
                ")\n", "PARSER error at [6:5-12:5]: Field 'data' should be specified only once");
        test("###Mapping\n" +
                "Mapping testing::mapping\n" +
                "(\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test_1\n" +
                "    (\n" +
                "      assert: '{}';\n" +
                "      query: |testing::Person.all();\n" +
                "      data:[<Object, JSON, testing::other::NPerson, '{\"firstName\":\"firstName 7\",\"lastName\":\"lastName 55\"}'>];\n" +
                "      assert: '{}';\n" +
                "    )\n" +
                "  ]\n" +
                ")\n", "PARSER error at [6:5-12:5]: Field 'assert' should be specified only once");
        // check data format type
        test("Class model::domain::Source {}\n" +
                "###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "   MappingTests\n" +
                "   [\n" +
                "      test2\n" +
                "      (\n" +
                "         query: |model::domain::Target.all()->graphFetchChecked(#{model::domain::Target{name}}#)->serialize(#{model::domain::Target{name}}#);\n" +
                "         data: [<Object, FUNK_UNKNOWN_FORMAT, model::domain::Source, '{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}'>" +
                "                   ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                "      )\n" +
                "   ]" +
                "\n" +
                ")\n", "PARSER error at [10:26-44]: Mapping test object input data does not support format 'FUNK_UNKNOWN_FORMAT'. Possible values: JSON, XML");
        // object input data with no format type
        test("Class model::domain::Source {}\n" +
                "###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "   MappingTests\n" +
                "   [\n" +
                "      test2\n" +
                "      (\n" +
                "         query: |model::domain::Target.all()->graphFetchChecked(#{model::domain::Target{name}}#)->serialize(#{model::domain::Target{name}}#);\n" +
                "         data: [<Object, model::domain::Source, '{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}'>" +
                "                   ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                "      )\n" +
                "   ]" +
                "\n" +
                ")\n", "PARSER error at [10:17-189]: Mapping test object 'input type' is missing. Possible values: JSON, XML");
        // check data input type
        test("Class model::domain::Source {}\n" +
                "###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "   MappingTests\n" +
                "   [\n" +
                "      test2\n" +
                "      (\n" +
                "         query: |model::domain::Target.all()->graphFetchChecked(#{model::domain::Target{name}}#)->serialize(#{model::domain::Target{name}}#);\n" +
                "         data: [               <Objected, FUNK_UNKNOWN_FORMAT, model::domain::Source, '{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}'>" +
                "                   ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                "      )\n" +
                "   ]" +
                "\n" +
                ")\n", "PARSER error at [10:32-227]: Unsupported mapping test input data type 'Objected'");
    }

    @Test
    public void testMappingTestSuites()
    {
        test("###Mapping\n" +
                "Mapping testing::mapping\n" +
                "(\n" +
                "  *testing::Person[testing_Person]: Pure\n" +
                "  {\n" +
                "    ~src testing::other::NPerson\n" +
                "    fullName: $src.firstName + ' ' + $src.lastName\n" +
                "  }\n" +
                "\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      doc: 'myDoc';\n" +
                "      function: |testing::Person.all()->graphFetch(#{testing::Person{fullName}}#);\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          doc: 'my test';\n" +
                "      data:\n" +
                "      [\n" +
                "        ModelStore: ModelStore\n" +
                "        #{\n" +
                "           test::example::model:\n" +
                "              Reference \n" +
                "              #{ \n" +
                "                testMapping::TestData \n" +
                "              }#\n" +
                "         }#\n" +
                "      ];\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\"fullName\":[\"john doe\"]';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ];\n" +
                "        }\n" +
                "      ];\n" +
                "    },\n" +
                "    testSuite2:\n" +
                "    {\n" +
               "       function: |testing::Person.all()->graphFetch(#{testing::Person{fullName}}#);\n" +
                "      doc: 'myDoc';\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          doc: 'my test';\n" +
                "          data:\n" +
                "          [\n" +
                "            ModelStore: ModelStore\n" +
                "            #{\n" +
                "               test::example::model:\n" +
                "               Reference \n" +
                "               #{ \n" +
                "                testMapping::TestData \n" +
                "               }#\n" +
                "            }#\n" +
                "          ];\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "            EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\"fullName\":[\"john doe\"]';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ];\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                ")");
        test("###Mapping\n" +
                "Mapping test::modelToModelMapping\n" +
                "(\n" +
                "    *test::changedModel: Pure\n" +
                "{\n" +
                "    ~src test::model\n" +
                "    name: $src.name,\n" +
                "    id: $src.id->parseInteger()\n" +
                "}\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      function: |test::changedModel.all()->graphFetch(#{test::changedModel{id,name}}#)->serialize(#{test::changedModel{id,name}}#);\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          data:\n" +
                "          [\n" +
                "            ModelStore: ModelStore\n" +
                "            #{\n" +
                "               test::model:\n" +
                "                Reference \n" +
                "                #{ \n" +
                "                  test::data::MyData\n" +
                "                }#\n" +
                "            }#\n" +
                "          ];\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                ")","PARSER error at [17:9-30:9]: Field 'asserts' is required");
        test("###Mapping\n" +
                "Mapping testing::mapping\n" +
                "(\n" +
                "  *testing::Person[testing_Person]: Pure\n" +
                "  {\n" +
                "    ~src testing::other::NPerson\n" +
                "    fullName: $src.firstName + ' ' + $src.lastName\n" +
                "  }\n" +
                "\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\"fullName\":[\"john doe\"]';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ];\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                ")","PARSER error at [12:5-33:5]: Mapping Test Suite requires a query function");
    }

    @Test
    public void testPureInstanceClassMapping()
    {
        // Duplicated fields
        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  *anything::goes[anything_goes]: Pure\n" +
                "  {\n" +
                "    ~src anything::goes\n" +
                "    ~src anything::goes\n" +
                "    ~filter $src.fullName->substring(0, $src.fullName->indexOf(' '))\n" +
                "  }\n" +
                ")\n", "PARSER error at [4:3-9:3]: Field '~src' should be specified only once");
        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  *anything::goes[anything_goes]: Pure\n" +
                "  {\n" +
                "    ~filter $src.fullName->substring(0, $src.fullName->indexOf(' '))\n" +
                "    ~src anything::goes\n" +
                "    ~filter $src.fullName->substring(0, $src.fullName->indexOf(' '))\n" +
                "  }\n" +
                ")\n", "PARSER error at [4:3-9:3]: Field '~filter' should be specified only once");
        // Ensure property mappings must be separated by commas
        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  *anything::goes[anything_goes]: Pure\n" +
                "  {\n" +
                "    ~src anything::goes\n" +
                "    ~filter $src.fullName->substring(0, $src.fullName->indexOf(' '))\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' '))\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' '))\n" +
                "  }\n" +
                ")\n", "PARSER error at [9:5-13]: Unexpected token 'firstName'");
    }

    @Test
    public void testPropertyMappingWithoutEndingComma()
    {
        // Ensure property mappings must be separated by commas
        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  *anything::goes[anything_goes]: Pure\n" +
                "  {\n" +
                "    ~src anything::goes\n" +
                "    ~filter $src.fullName->substring(0, $src.fullName->indexOf(' '))\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' '))\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' '))\n" +
                "  }\n" +
                ")\n", "PARSER error at [9:5-13]: Unexpected token 'firstName'");
    }

    @Test
    public void testUnknownClassMappingType()
    {
        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  *anything::goes[anything_goes]: Unknown\n" +
                "  {\n" +
                "  }\n" +
                ")\n", "PARSER error at [4:3-6:3]: No parser for Unknown");
    }

    @Test
    public void testModelMappingWithLocalProperties()
    {
        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  *anything::goes[anything_goes]: Pure\n" +
                "  {\n" +
                "    ~src anything::goes\n" +
                "    + firstName : [1] : $src.fullName->substring(0, $src.fullName->indexOf(' '))\n" +
                "  }\n" +
                ")\n", "PARSER error at [7:19]: Unexpected token '['");

        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  *anything::goes[anything_goes]: Pure\n" +
                "  {\n" +
                "    ~src anything::goes\n" +
                "    + firstName : String : $src.fullName->substring(0, $src.fullName->indexOf(' '))\n" +
                "  }\n" +
                ")\n", "PARSER error at [7:26]: Unexpected token ':'");

        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  *anything::goes[anything_goes]: Pure\n" +
                "  {\n" +
                "    ~src anything::goes\n" +
                "    + firstName : $src.fullName->substring(0, $src.fullName->indexOf(' '))\n" +
                "  }\n" +
                ")\n", "PARSER error at [7:19]: Unexpected token");

        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  *anything::goes[anything_goes]: Pure\n" +
                "  {\n" +
                "    ~src anything::goes\n" +
                "    + firstName : : $src.fullName->substring(0, $src.fullName->indexOf(' '))\n" +
                "  }\n" +
                ")\n", "PARSER error at [7:19]: Unexpected token ':'");

        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  *anything::goes[anything_goes]: Pure\n" +
                "  {\n" +
                "    ~src anything::goes\n" +
                "    + firstName : String[1] : $src.fullName->substring(0, $src.fullName->indexOf(' '))\n" +
                "  }\n" +
                ")\n");

        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  *anything::goes[anything_goes]: Pure\n" +
                "  {\n" +
                "    ~src anything::goes\n" +
                "    + firstName : String[1] : $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    + lastName : String[1] : $src.lastName->substring(0, $src.fullName->indexOf(' '))\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testCrossStoreAssociationMapping()
    {
        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  mapping::SomeClass: XStore\n" +
                "  {\n" +
                "    ~src Something\n" +
                "  }\n" +
                ")", "PARSER error at [6:5]: Unexpected token");

        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  mapping::SomeClass: XStore\n" +
                "  {\n" +
                "    +p: String[1]: 'a'\n" +
                "  }\n" +
                ")", "PARSER error at [6:5]: Unexpected token");

        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  mapping::SomeClass: XStore\n" +
                "  {\n" +
                "    p: true\n" +
                "  }\n" +
                ")");

        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  mapping::SomeClass: XStore\n" +
                "  {\n" +
                "    p1: true,\n" +
                "    p2: false\n" +
                "  }\n" +
                ")");

        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  mapping::SomeClass: XStore\n" +
                "  {\n" +
                "    p1[x, y]: true\n" +
                "    p2[y, x]: false\n" +
                "  }\n" +
                ")", "PARSER error at [7:5-6]: Unexpected token 'p2'");

        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  mapping::SomeClass: XStore\n" +
                "  {\n" +
                "    p1[x]: true\n" +
                "  }\n" +
                ")", "PARSER error at [6:9]: Unexpected token ']'");

        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  mapping::SomeClass: XStore\n" +
                "  {\n" +
                "    p1[x, ]: true\n" +
                "  }\n" +
                ")", "PARSER error at [6:11]: Unexpected token ']'");

        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  mapping::SomeClass: XStore\n" +
                "  {\n" +
                "    p1[x, y]: true,\n" +
                "    p2[y, x]: false\n" +
                "  }\n" +
                ")");

        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  mapping::SomeClass: XStore\n" +
                "  {\n" +
                "    p1[x, y]: true,\n" +
                "    p2: false\n" +
                "  }\n" +
                ")");

        test("###Mapping\n" +
                "Mapping mapping::test\n" +
                "(\n" +
                "  mapping::SomeClass: XStore\n" +
                "  {\n" +
                "    p1[x1, y2]: true,\n" +
                "    p1[x2, y2]: false\n" +
                "  }\n" +
                ")");
    }

    @Test
    public void testLatestMultiplicity()
    {
        PureModelContextData model = PureGrammarParser.newInstance().parseModel("import test::*;\n" +
                "Class test::A\n" +
                "{\n" +
                "  stringProperty: String[1];\n" +
                "  latestProduct(){\n" +
                "         $this.stringProperty(%latest)\n" +
                "   }:String[*];\n" +
                "}\n"
        );
        Multiplicity latest = ((CLatestDate) ((AppliedProperty) model.getElementsOfType(Class.class).get(0).qualifiedProperties.get(0).body.get(0)).parameters.get(1)).multiplicity;
        Assert.assertTrue(latest.isUpperBoundEqualTo(1), () -> "CLatestDate must have multiplicity set to 1");
    }
}
