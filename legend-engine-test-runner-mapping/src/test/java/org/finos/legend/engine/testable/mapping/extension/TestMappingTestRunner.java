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

package org.finos.legend.engine.testable.mapping.extension;

import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TestMappingTestRunner
{

    private String getGrammar1MappingSuite(String TEST_SUITE_GRAMMAR)
    {
        return "###Pure\n" +
                "Class test::model\n" +
                "{\n" +
                "    name: String[1];\n" +
                "    id: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::changedModel{    name: String[1];    id: Integer[1];}\n" +
                "###Data\n" +
                "Data test::data::MyData\n" +
                "{\n" +
                "  ExternalFormat\n" +
                "  #{\n" +
                "    contentType: 'application/json';\n" +
                "    data: '{\"name\":\"john doe\",\"id\":\"77\"}';\n" +
                "  }#\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::modelToModelMapping\n" +
                "(\n" +
                "    *test::changedModel: Pure\n" +
                "{\n" +
                "    ~src test::model\n" +
                "    name: $src.name,\n" +
                "    id: $src.id->parseInteger()\n" +
                "}\n" +
                "\n" + TEST_SUITE_GRAMMAR + '\n' +
                ")\n";
    }

    String TEST_SUITE_1 = getGrammar1MappingSuite("  testSuites:\n" +
            "  [\n" +
            "    testSuite1:\n" +
            "    {\n" +
            "      data:\n" +
            "      [\n" +
            "       ModelStore: ModelStore\n" +
            "        #{\n" +
            "           test::model:\n" +
            "            Reference\n" +
            "            #{\n" +
            "              test::data::MyData\n" +
            "            }#\n" +
            "        }#\n" +
            "      ];\n" +
            "      tests:\n" +
            "      [\n" +
            "        test1:\n" +
            "        {\n" +
            "          function: |test::changedModel.all()->graphFetch(#{test::changedModel{id,name}}#)->serialize(#{test::changedModel{id,name}}#);\n" +
            "          asserts:\n" +
            "          [\n" +
            "            assert1:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected :\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\"id\" : 77, \"name\" : \"john doe\"}';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ];\n" +
            "        }\n" +
            "      ];\n" +
            "    }\n" +
            "  ]\n");


    String TEST_SUITE_2 = getGrammar1MappingSuite("  testSuites:\n" +
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
            "           ModelStore: ModelStore\n" +
            "            #{\n" +
            "               test::model:\n" +
            "                Reference\n" +
            "                #{\n" +
            "                  test::data::MyData\n" +
            "                }#\n" +
            "            }#\n" +
            "          ];\n" +
            "          asserts:\n" +
            "          [\n" +
            "            assert1:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected :\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\"id\" : 77, \"name\" : \"john doe\"}';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ];\n" +
            "        },\n" +
            "        test2:\n" +
            "        {\n" +
            "          data:\n" +
            "          [\n" +
            "           ModelStore: ModelStore\n" +
            "            #{\n" +
            "               test::model:\n" +
            "               ExternalFormat\n" +
            "               #{\n" +
            "                 contentType: 'application/json';\n" +
            "                 data: '{\"name\":\"john doe2\",\"id\":\"77\"}';\n" +
            "               }#\n" +
            "            }#\n" +
            "          ];\n" +
            "          asserts:\n" +
            "          [\n" +
            "            assert1:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected :\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\"id\" : 77, \"name\" : \"john doe2\"}';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ];\n" +
            "        }\n" +
            "      ];\n" +
            "    }\n" +
            "  ]");


    String grammar2 = "###Data\n" +
            "Data test::data::MyTestData\n" +
            "{\n" +
            "  ExternalFormat\n" +
            "  #{\n" +
            "    contentType: 'application/json';\n" +
            "    data: '{\\n  \"name\": \"name 35\",\\n  \"id\": 82,\\n  \"streetNumber\": 86,\\n  \"streetName\": \"streetName 99\",\\n  \"pinCode\": 94,\\n  \"stateName\": \"stateName 21\"\\n}';\n" +
            "  }#\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Pure\n" +
            "Class test::PersonModel\n" +
            "{\n" +
            "  name: String[1];\n" +
            "  id: String[1];\n" +
            "  address: test::Address[1];\n" +
            "}\n" +
            "\n" +
            "Class test::Address\n" +
            "{\n" +
            "  streetNumber: Integer[1];\n" +
            "  streetName: String[1];\n" +
            "  stateInfo: test::StateInfo[1];\n" +
            "}\n" +
            "\n" +
            "Class test::StateInfo\n" +
            "{\n" +
            "  stateName: String[1];\n" +
            "  pinCode: Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class test::changedTestModel\n" +
            "{\n" +
            "  name: String[1];\n" +
            "  id: Integer[1];\n" +
            "  streetNumber: Integer[1];\n" +
            "  streetName: String[1];\n" +
            "  pinCode: Integer[1];\n" +
            "  stateName: String[1];\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping test::modelToModelTestMapping\n" +
            "(\n" +
            "  *test::PersonModel: Pure\n" +
            "  {\n" +
            "    ~src test::changedTestModel\n" +
            "    name: $src.name,\n" +
            "    id: $src.id->toString(),\n" +
            "    address[test_Address]: $src\n" +
            "  }\n" +
            "  *test::Address: Pure\n" +
            "  {\n" +
            "    ~src test::changedTestModel\n" +
            "    streetNumber: $src.streetNumber,\n" +
            "    streetName: $src.streetName,\n" +
            "    stateInfo[test_StateInfo]: $src\n" +
            "  }\n" +
            "  *test::StateInfo: Pure\n" +
            "  {\n" +
            "    ~src test::changedTestModel\n" +
            "    stateName: $src.stateName,\n" +
            "    pinCode: $src.pinCode\n" +
            "  }\n" +
            "\n" +
            "    testSuites:\n" +
            "  [\n" +
            "    testSuite1:\n" +
            "    {\n" +
            "      data:\n" +
            "      [\n" +
            "       ModelStore: ModelStore\n" +
            "        #{\n" +
            "           test::changedTestModel:\n" +
            "            Reference \n" +
            "            #{ \n" +
            "              test::data::MyTestData\n" +
            "            }#\n" +
            "        }#\n" +
            "      ];\n" +
            "      tests:\n" +
            "      [\n" +
            "        test1:\n" +
            "        {\n" +
            "          function: |test::PersonModel.all()->graphFetch(#{test::PersonModel{id,address{streetName,stateInfo{pinCode,stateName}}}}#)->serialize(#{test::PersonModel{id,address{streetName,stateInfo{pinCode,stateName}}}}#);\n" +
            "          asserts:\n" +
            "          [\n" +
            "            assert1:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected : \n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\"id\":\"82\",\"address\":{\"streetName\":\"streetName 99\",\"stateInfo\":{\"pinCode\":94,\"stateName\":\"stateName 21\"}}}';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ];\n" +
            "        }\n" +
            "      ];\n" +
            "    }\n" +
            "  ]\n" +
            ")\n";




    @Test
    public void testMappingTestSuiteForM2MUsecase()
    {
        MappingTestableRunnerExtension mappingTestableRunnerExtension = new MappingTestableRunnerExtension();
        mappingTestableRunnerExtension.setPureVersion(PureClientVersions.production);

        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(TEST_SUITE_1);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, null);
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mappingToTest = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) pureModelWithReferenceData.getPackageableElement("test::modelToModelMapping");
        List<TestResult> mappingTestResults = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(1, mappingTestResults.size());
        Assert.assertTrue(mappingTestResults.get(0) instanceof TestExecuted);
        Assert.assertEquals(TestExecutionStatus.PASS, ((TestExecuted) mappingTestResults.get(0)).testExecutionStatus);
        Assert.assertEquals("test::modelToModelMapping", mappingTestResults.get(0).testable);
        Assert.assertEquals("testSuite1", mappingTestResults.get(0).testSuiteId);
        Assert.assertEquals("test1", mappingTestResults.get(0).atomicTestId);
    }

    @Test
    public void testMappingTestSuiteForM2MThreeLevelDeep()
    {
        MappingTestableRunnerExtension mappingTestableRunnerExtension = new MappingTestableRunnerExtension();
        mappingTestableRunnerExtension.setPureVersion(PureClientVersions.production);
        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(grammar2);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, null);
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mappingToTest = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) pureModelWithReferenceData.getPackageableElement("test::modelToModelTestMapping");
        List<TestResult> mappingTestResults = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(1, mappingTestResults.size());
        Assert.assertTrue(mappingTestResults.get(0) instanceof TestExecuted);
        Assert.assertEquals(TestExecutionStatus.PASS, ((TestExecuted) mappingTestResults.get(0)).testExecutionStatus);
        Assert.assertEquals("test::modelToModelTestMapping", mappingTestResults.get(0).testable);
        Assert.assertEquals("testSuite1", mappingTestResults.get(0).testSuiteId);
        Assert.assertEquals("test1", mappingTestResults.get(0).atomicTestId);
    }

    @Test
    public void testMappingTestSuiteWithTwoMappings()
    {
        MappingTestableRunnerExtension mappingTestableRunnerExtension = new MappingTestableRunnerExtension();
        mappingTestableRunnerExtension.setPureVersion(PureClientVersions.production);
        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(TEST_SUITE_1 + grammar2);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, null);
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mappingToTest1 = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) pureModelWithReferenceData.getPackageableElement("test::modelToModelTestMapping");
        List<TestResult> mappingTestResults1 = mappingTestableRunnerExtension.executeAllTest(mappingToTest1, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(1, mappingTestResults1.size());
        Assert.assertTrue(mappingTestResults1.get(0) instanceof TestExecuted);
        Assert.assertEquals(TestExecutionStatus.PASS, ((TestExecuted) mappingTestResults1.get(0)).testExecutionStatus);
        Assert.assertEquals("test::modelToModelTestMapping", mappingTestResults1.get(0).testable);
        Assert.assertEquals("testSuite1", mappingTestResults1.get(0).testSuiteId);
        Assert.assertEquals("test1", mappingTestResults1.get(0).atomicTestId);

        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mappingToTest = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) pureModelWithReferenceData.getPackageableElement("test::modelToModelMapping");
        List<TestResult> mappingTestResults2 = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(1, mappingTestResults2.size());
        Assert.assertTrue(mappingTestResults2.get(0) instanceof TestExecuted);
        Assert.assertEquals(TestExecutionStatus.PASS, ((TestExecuted) mappingTestResults2.get(0)).testExecutionStatus);
        Assert.assertEquals("test::modelToModelMapping", mappingTestResults2.get(0).testable);
        Assert.assertEquals("testSuite1", mappingTestResults2.get(0).testSuiteId);
        Assert.assertEquals("test1", mappingTestResults2.get(0).atomicTestId);
    }

    @Test
    public void testM2MMappingSuiteWithMultiInputData()
    {
        MappingTestableRunnerExtension mappingTestableRunnerExtension = new MappingTestableRunnerExtension();
        mappingTestableRunnerExtension.setPureVersion(PureClientVersions.production);

        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(TEST_SUITE_2);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, null);
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mappingToTest = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) pureModelWithReferenceData.getPackageableElement("test::modelToModelMapping");
        List<TestResult> mappingTestResults = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(2, mappingTestResults.size());
        Assert.assertEquals(2, mappingTestResults.stream().filter(e -> e instanceof TestExecuted).count());
        TestExecuted test1 = (TestExecuted) mappingTestResults.get(0);
        Assert.assertEquals("test::modelToModelMapping", test1.testable);
        Assert.assertEquals("testSuite1", test1.testSuiteId);
        Assert.assertEquals("test1", test1.atomicTestId);
        Assert.assertEquals(1, test1.assertStatuses.size());

        TestExecuted test2 = (TestExecuted) mappingTestResults.get(1);
        Assert.assertEquals("test::modelToModelMapping", test2.testable);
        Assert.assertEquals("testSuite1", test2.testSuiteId);
        Assert.assertEquals("test2", test2.atomicTestId);
        Assert.assertEquals(1, test2.assertStatuses.size());

    }


}

