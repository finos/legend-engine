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

import net.javacrumbs.jsonunit.JsonAssert;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.EqualToJsonAssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.*;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.testable.TestAccessor;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

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
            "      function: |test::changedModel.all()->graphFetch(#{test::changedModel{id,name}}#)->serialize(#{test::changedModel{id,name}}#);\n" +
            "      tests:\n" +
            "      [\n" +
            "        test1:\n" +
            "        {\n" +
            "         data:\n" +
            "         [\n" +
            "           ModelStore: ModelStore\n" +
            "             #{\n" +
            "               test::model:\n" +
            "                Reference\n" +
            "                #{\n" +
            "                  test::data::MyData\n" +
            "                }#\n" +
            "             }#\n" +
            "           ];\n" +
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
            "                    data: '{\"id\" : 77, \"name\" : \"john doe2x\"}';\n" +
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
            "      function: |test::PersonModel.all()->graphFetch(#{test::PersonModel{id,address{streetName,stateInfo{pinCode,stateName}}}}#)->serialize(#{test::PersonModel{id,address{streetName,stateInfo{pinCode,stateName}}}}#);\n" +
            "      tests:\n" +
            "      [\n" +
            "        test1:\n" +
            "        {\n" +
            "          data:\n" +
            "          [\n" +
            "            ModelStore: ModelStore\n" +
            "            #{\n" +
            "             test::changedTestModel:\n" +
            "              Reference \n" +
            "              #{ \n" +
            "                test::data::MyTestData\n" +
            "              }#\n" +
            "            }#\n" +
            "          ];\n" +
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



    String multiInputWithReferenceM2M = "###Data\n" +
            "Data model::PersonData\n" +
            "{\n" +
            "  ExternalFormat\n" +
            "  #{\n" +
            "    contentType: 'application/json';\n" +
            "    data: '{\\n  \"firstName\": \"John\",\\n  \"lastName\": \"Smith\"\\n}';\n" +
            "  }#\n" +
            "}\n" +
            "\n" +
            "Data model::ModelStoreRef\n" +
            "{\n" +
            "  ModelStore\n" +
            "  #{\n" +
            "    model::Person:\n" +
            "      Reference\n" +
            "      #{\n" +
            "        model::PersonData\n" +
            "      }#\n" +
            "  }#\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Pure\n" +
            "Class model::Person\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "  lastName: String[1];\n" +
            "}\n" +
            "\n" +
            "Class model::TargetPerson\n" +
            "{\n" +
            "  fullName: String[1];\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping model::M2MSimpleMapping\n" +
            "(\n" +
            "  *model::TargetPerson: Pure\n" +
            "  {\n" +
            "    ~src model::Person\n" +
            "    fullName: $src.firstName + ' ' + $src.lastName\n" +
            "  }\n" +
            "\n" +
            "  testSuites:\n" +
            "  [\n" +
            "    graphFetchSuite:\n" +
            "    {\n" +
            "      function: |model::TargetPerson.all()->graphFetch(\n" +
            "  #{\n" +
            "    model::TargetPerson{\n" +
            "      fullName\n" +
            "    }\n" +
            "  }#\n" +
            ")->serialize(\n" +
            "  #{\n" +
            "    model::TargetPerson{\n" +
            "      fullName\n" +
            "    }\n" +
            "  }#\n" +
            ");\n" +
            "      tests:\n" +
            "      [\n" +
            "        basicInput:\n" +
            "        {\n" +
            "          doc: 'basic input documentation';\n" +
            "          data:\n" +
            "          [\n" +
            "            ModelStore:\n" +
            "              ModelStore\n" +
            "              #{\n" +
            "                model::Person:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\\n  \"firstName\": \"John\",\\n  \"lastName\": \"Smith\"\\n}';\n" +
            "                  }#\n" +
            "              }#\n" +
            "          ];\n" +
            "          asserts:\n" +
            "          [\n" +
            "            expectedAssertion:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\\n  \"fullName\": \"John Smith\"\\n}';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ];\n" +
            "        },\n" +
            "        differentInput:\n" +
            "        {\n" +
            "          data:\n" +
            "          [\n" +
            "            ModelStore:\n" +
            "              ModelStore\n" +
            "              #{\n" +
            "                model::Person:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\\n  \"firstName\": \"John2\",\\n  \"lastName\": \"Smith2\"\\n}';\n" +
            "                  }#\n" +
            "              }#\n" +
            "          ];\n" +
            "          asserts:\n" +
            "          [\n" +
            "            expectedAssertion:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\\n  \"fullName\": \"John2 Smith2\"\\n}';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ];\n" +
            "        },\n" +
            "        failingDifferentInput:\n" +
            "        {\n" +
            "          data:\n" +
            "          [\n" +
            "            ModelStore:\n" +
            "              ModelStore\n" +
            "              #{\n" +
            "                model::Person:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\\n  \"firstName\": \"John2\",\\n  \"lastName\": \"Smith2\"\\n}';\n" +
            "                  }#\n" +
            "              }#\n" +
            "          ];\n" +
            "          asserts:\n" +
            "          [\n" +
            "            expectedAssertion:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\\n  \"fullName\": \"John2 Smith2 WRONG\"\\n}';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ];\n" +
            "        },\n" +
            "        dataReferenceOnPerson:\n" +
            "        {\n" +
            "          data:\n" +
            "          [\n" +
            "            ModelStore:\n" +
            "              ModelStore\n" +
            "              #{\n" +
            "                model::Person:\n" +
            "                  Reference\n" +
            "                  #{\n" +
            "                    model::PersonData\n" +
            "                  }#\n" +
            "              }#\n" +
            "          ];\n" +
            "          asserts:\n" +
            "          [\n" +
            "            expectedAssertion:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\\n  \"fullName\": \"John Smith\"\\n}';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ];\n" +
            "        },\n" +
            "        dataReferenceFail:\n" +
            "        {\n" +
            "          data:\n" +
            "          [\n" +
            "            ModelStore:\n" +
            "              Reference\n" +
            "              #{\n" +
            "                model::ModelStoreRef\n" +
            "              }#\n" +
            "          ];\n" +
            "          asserts:\n" +
            "          [\n" +
            "            expectedAssertion:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\\n  \"fullName\": \"John Smith2\"\\n}';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ];\n" +
            "        },\n" +
            "        dataReference:\n" +
            "        {\n" +
            "          data:\n" +
            "          [\n" +
            "            ModelStore:\n" +
            "              Reference\n" +
            "              #{\n" +
            "                model::ModelStoreRef\n" +
            "              }#\n" +
            "          ];\n" +
            "          asserts:\n" +
            "          [\n" +
            "            expectedAssertion:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\\n  \"fullName\": \"John Smith\"\\n}';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ];\n" +
            "        }\n" +
            "      ];\n" +
            "    },\n" +
            "    graphFetchChecked:\n" +
            "    {\n" +
            "      function: |model::TargetPerson.all()->graphFetchChecked(\n" +
            "  #{\n" +
            "    model::TargetPerson{\n" +
            "      fullName\n" +
            "    }\n" +
            "  }#\n" +
            ")->serialize(\n" +
            "  #{\n" +
            "    model::TargetPerson{\n" +
            "      fullName\n" +
            "    }\n" +
            "  }#\n" +
            ");\n" +
            "      tests:\n" +
            "      [\n" +
            "        passingTest:\n" +
            "        {\n" +
            "          data:\n" +
            "          [\n" +
            "            ModelStore:\n" +
            "              ModelStore\n" +
            "              #{\n" +
            "                model::Person:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\\n  \"firstName\": \"firstName 5\",\\n  \"lastName\": \"lastName 65\"\\n}';\n" +
            "                  }#\n" +
            "              }#\n" +
            "          ];\n" +
            "          asserts:\n" +
            "          [\n" +
            "            expectedAssertion:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\\n  \"defects\": [],\\n  \"source\": {\\n    \"defects\": [],\\n    \"source\": {\\n      \"number\": 1,\\n      \"record\": \"{\\\\\"firstName\\\\\":\\\\\"firstName 5\\\\\",\\\\\"lastName\\\\\":\\\\\"lastName 65\\\\\"}\"\\n    },\\n    \"value\": {\\n      \"firstName\": \"firstName 5\",\\n      \"lastName\": \"lastName 65\"\\n    }\\n  },\\n  \"value\": {\\n    \"fullName\": \"firstName 5 lastName 65\"\\n  }\\n}';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ];\n" +
            "        },\n" +
            "        failingTest:\n" +
            "        {\n" +
            "          data:\n" +
            "          [\n" +
            "            ModelStore:\n" +
            "              ModelStore\n" +
            "              #{\n" +
            "                model::Person:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\\n  \"firstName\": \"firstName 5\",\\n  \"lastName\": \"lastName 65\"\\n}';\n" +
            "                  }#\n" +
            "              }#\n" +
            "          ];\n" +
            "          asserts:\n" +
            "          [\n" +
            "            expectedAssertion:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{}';\n" +
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
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mappingToTest = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) pureModelWithReferenceData.getPackageableElement("test::modelToModelMapping");
        List<TestResult> mappingTestResults = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(1, mappingTestResults.size());
        Assert.assertTrue(hasTestPassed(mappingTestResults.get(0)));
        Assert.assertEquals("test::modelToModelMapping", mappingTestResults.get(0).testable);
        Assert.assertEquals("testSuite1", mappingTestResults.get(0).testSuiteId);
        Assert.assertEquals("test1", mappingTestResults.get(0).atomicTestId);
    }

    @Test
    public void testDebugMappingTestSuiteForM2MUsecase()
    {
        MappingTestableRunnerExtension mappingTestableRunnerExtension = new MappingTestableRunnerExtension();
        mappingTestableRunnerExtension.setPureVersion(PureClientVersions.production);

        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(TEST_SUITE_1);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mappingToTest = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) pureModelWithReferenceData.getPackageableElement("test::modelToModelMapping");
        TestRunner runner =  mappingTestableRunnerExtension.getTestRunner(mappingToTest);
        List<TestDebug> debugLists = mappingToTest._tests().flatCollect(testSuite ->
        {
            List<String> atomicTestIds = ((Root_meta_pure_test_TestSuite) testSuite)._tests().collect(TestAccessor::_id).toList();
            return runner.debugTestSuite((Root_meta_pure_test_TestSuite) testSuite, atomicTestIds, pureModelWithReferenceData, modelDataWithReferenceData);
        }).toList();
        Assert.assertEquals(1, debugLists.size());
        Assert.assertEquals("test::modelToModelMapping", debugLists.get(0).testable);
        Assert.assertEquals("testSuite1", debugLists.get(0).testSuiteId);
        Assert.assertEquals("test1", debugLists.get(0).atomicTestId);
        TestDebug testDebug = debugLists.get(0);
        Assert.assertTrue(testDebug instanceof TestExecutionPlanDebug);
        TestExecutionPlanDebug testExecutionPlanDebug = (TestExecutionPlanDebug) testDebug;
        Assert.assertNotNull(testExecutionPlanDebug.executionPlan);
        Assert.assertNull(testExecutionPlanDebug.error);
        Assert.assertFalse(testExecutionPlanDebug.debug.isEmpty());
    }

    @Test
    public void testMappingTestSuiteForM2MThreeLevelDeep()
    {
        MappingTestableRunnerExtension mappingTestableRunnerExtension = new MappingTestableRunnerExtension();
        mappingTestableRunnerExtension.setPureVersion(PureClientVersions.production);
        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(grammar2);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mappingToTest = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) pureModelWithReferenceData.getPackageableElement("test::modelToModelTestMapping");
        List<TestResult> mappingTestResults = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(1, mappingTestResults.size());
        Assert.assertTrue(hasTestPassed(mappingTestResults.get(0)));
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
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mappingToTest1 = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) pureModelWithReferenceData.getPackageableElement("test::modelToModelTestMapping");
        List<TestResult> mappingTestResults1 = mappingTestableRunnerExtension.executeAllTest(mappingToTest1, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(1, mappingTestResults1.size());
        Assert.assertTrue(hasTestPassed(mappingTestResults1.get(0)));
        Assert.assertEquals("test::modelToModelTestMapping", mappingTestResults1.get(0).testable);
        Assert.assertEquals("testSuite1", mappingTestResults1.get(0).testSuiteId);
        Assert.assertEquals("test1", mappingTestResults1.get(0).atomicTestId);

        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mappingToTest = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) pureModelWithReferenceData.getPackageableElement("test::modelToModelMapping");
        List<TestResult> mappingTestResults2 = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(1, mappingTestResults2.size());
        Assert.assertTrue(hasTestPassed(mappingTestResults2.get(0)));
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
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mappingToTest = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) pureModelWithReferenceData.getPackageableElement("test::modelToModelMapping");
        List<TestResult> mappingTestResults = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(2, mappingTestResults.size());
        Assert.assertEquals(2, mappingTestResults.stream().filter(e -> e instanceof TestExecuted).count());
        TestExecuted test1 = guaranteedTestExecuted(mappingTestResults.get(0));
        Assert.assertEquals("test::modelToModelMapping", test1.testable);
        Assert.assertEquals("testSuite1", test1.testSuiteId);
        Assert.assertEquals("test1", test1.atomicTestId);
        Assert.assertEquals(1, test1.assertStatuses.size());
        Assert.assertEquals(TestExecutionStatus.PASS, test1.testExecutionStatus);

        TestExecuted test2 = guaranteedTestExecuted(mappingTestResults.get(1));
        Assert.assertEquals("test::modelToModelMapping", test2.testable);
        Assert.assertEquals("testSuite1", test2.testSuiteId);
        Assert.assertEquals("test2", test2.atomicTestId);
        Assert.assertEquals(1, test2.assertStatuses.size());
        Assert.assertEquals(TestExecutionStatus.FAIL, test2.testExecutionStatus);
    }

    @Test
    public void testMultiInputsWithDifferentSerializationAndReferences()
    {
        MappingTestableRunnerExtension mappingTestableRunnerExtension = new MappingTestableRunnerExtension();
        mappingTestableRunnerExtension.setPureVersion(PureClientVersions.production);

        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(multiInputWithReferenceM2M);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mappingToTest = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) pureModelWithReferenceData.getPackageableElement("model::M2MSimpleMapping");
        List<TestResult> mappingTestResults = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModelWithReferenceData, modelDataWithReferenceData);

        // basic assertions 2 suites
        Assert.assertEquals(8, mappingTestResults.size());
        List<TestResult> graphFetchSuite = mappingTestResults.stream().filter(e -> e.testSuiteId.equals("graphFetchSuite")).collect(Collectors.toList());;
        Assert.assertEquals(6, graphFetchSuite.size());
        List<TestResult> graphFetchCheckedSuite = mappingTestResults.stream().filter(e -> e.testSuiteId.equals("graphFetchChecked")).collect(Collectors.toList());;
        Assert.assertEquals(2, graphFetchCheckedSuite.size());

        // graphFetchSuite
        Assert.assertEquals(4,graphFetchSuite.stream().filter(this::hasTestPassed).collect(Collectors.toList()).size());
        Assert.assertEquals(2,graphFetchSuite.stream().filter(e -> !this.hasTestPassed(e)).collect(Collectors.toList()).size());
        Assert.assertTrue(hasTestPassed(findTestById(graphFetchSuite, "basicInput")));
        Assert.assertTrue(hasTestPassed(findTestById(graphFetchSuite, "differentInput")));
        Assert.assertTrue(hasTestPassed(findTestById(graphFetchSuite, "dataReferenceOnPerson")));
        Assert.assertTrue(hasTestPassed(findTestById(graphFetchSuite, "dataReference")));
        // check diffs
        String expected =
                "{\n" +
                        "  \"fullName\": \"John2 Smith2 WRONG\"\n" +
                        "}";
        String actual =
                "{\n" +
                        "  \"fullName\": \"John2 Smith2\"\n" +
                        "}";
        String expected1 = "{\n" +
                "  \"fullName\": \"John Smith2\"\n" +
                "}";
        String actual1 =
                "{\n" +
                        "  \"fullName\": \"John Smith\"\n" +
                        "}";
        testFailingTest(findTestById(graphFetchSuite, "failingDifferentInput"), expected, actual);
        testFailingTest(findTestById(graphFetchSuite, "dataReferenceFail"), expected1, actual1);

        // graphFetchSuite Checked
        Assert.assertEquals(1, graphFetchCheckedSuite.stream().filter(this::hasTestPassed).collect(Collectors.toList()).size());
        Assert.assertEquals(1, graphFetchCheckedSuite.stream().filter(e -> !this.hasTestPassed(e)).collect(Collectors.toList()).size());
        String e = "{}";
        String a = "{\n" +
                "  \"defects\": [],\n" +
                "  \"source\": {\n" +
                "    \"defects\": [],\n" +
                "    \"source\": {\n" +
                "      \"number\": 1,\n" +
                "      \"record\": \"{\\\"firstName\\\":\\\"firstName 5\\\",\\\"lastName\\\":\\\"lastName 65\\\"}\"\n" +
                "    },\n" +
                "    \"value\": {\n" +
                "      \"firstName\": \"firstName 5\",\n" +
                "      \"lastName\": \"lastName 65\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"value\": {\n" +
                "    \"fullName\": \"firstName 5 lastName 65\"\n" +
                "  }\n" +
                "}";
        testFailingTest(findTestById(graphFetchCheckedSuite, "failingTest"), e, a);
    }


    private void testFailingTest(TestResult testResult, String expected, String actual)
    {
        TestExecuted testExecuted = guaranteedTestExecuted(testResult);
        Assert.assertEquals(TestExecutionStatus.FAIL, testExecuted.testExecutionStatus);
        AssertionStatus status = testExecuted.assertStatuses.get(0);
        if (status instanceof EqualToJsonAssertFail)
        {
            EqualToJsonAssertFail equalToJsonAssertFail = (EqualToJsonAssertFail)status;
            JsonAssert.assertJsonEquals(expected, equalToJsonAssertFail.expected);
            JsonAssert.assertJsonEquals(actual, equalToJsonAssertFail.actual);
        }
        else
        {
            throw new RuntimeException("Test Assertion" + status.id + " expected to fail");
        }
    }

    private TestResult findTestById(List<TestResult> results, String id)
    {
        return results.stream().filter(test -> test.atomicTestId.equals(id)).findFirst().orElseThrow(() -> new RuntimeException("Test Id " + id + " not found"));
    }

    private TestExecuted guaranteedTestExecuted(TestResult result)
    {
        if (result instanceof  TestExecuted)
        {
            return (TestExecuted) result;
        }
        throw new RuntimeException("test expected to have been executed");
    }

    private boolean hasTestPassed(TestResult result)
    {
        if (result instanceof  TestExecuted)
        {
            return ((TestExecuted) result).testExecutionStatus.equals(TestExecutionStatus.PASS);
        }
        return false;
    }

}

