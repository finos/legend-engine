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

package org.finos.legend.engine.plan.execution.stores.service.testable.mapping;

import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.testable.mapping.extension.MappingTestableRunnerExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TestMappingTestRunner
{
    @Test
    public void testServiceTestSuiteWithServiceStore()
    {
        MappingTestableRunnerExtension mappingTestableRunnerExtension = new MappingTestableRunnerExtension();
        mappingTestableRunnerExtension.setPureVersion("vX_X_X");
        String grammar = "###Data\n" +
                "Data testServiceStoreTestSuites::TestData\n" +
                "{\n" +
                "  ServiceStore\n" +
                "  #{\n" +
                "    [\n" +
                "      {\n" +
                "        request:\n" +
                "        {\n" +
                "          method: GET;\n" +
                "          url: '/employees/allEmployees';\n" +
                "        };\n" +
                "        response:\n" +
                "        {\n" +
                "          body:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[ { \"kerberos\": \"dummy kerberos\", \"employeeID\": \"dummy id\", \"title\": \"dummy title\", \"firstName\": \"dummy firstName\", \"lastName\": \"dummy lastname\", \"countryCode\": \"dummy countryCode\" } ]';\n" +
                "            }#;\n" +
                "        };\n" +
                "      }\n" +
                "    ]\n" +
                "  }#\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###ServiceStore\n" +
                "ServiceStore testServiceStoreTestSuites::ServiceStore\n" +
                "(\n" +
                "  ServiceGroup EmployeeServices\n" +
                "  (\n" +
                "    path : '/employees';\n" +
                "\n" +
                "    Service EmployeeService\n" +
                "    (\n" +
                "      path : '/allEmployees';\n" +
                "      method : GET;\n" +
                "      response : [testServiceStoreTestSuites::Employee <- testServiceStoreTestSuites::employeeServiceStoreSchemaBinding];\n" +
                "      security : [];\n" +
                "    )\n" +
                "    Service EmployeeServiceWithParameters\n" +
                "    (\n" +
                "      path : '/employeesWithParameters';\n" +
                "      method : GET;\n" +
                "      parameters :\n" +
                "      (\n" +
                "        stringParam : String ( location = query ),\n" +
                "        integerParam : Integer ( location = query ),\n" +
                "        floatParam : Float ( location = query ),\n" +
                "        booleanParam : Boolean ( location = query )\n" +
                "      );\n" +
                "      response : [testServiceStoreTestSuites::Employee2 <- testServiceStoreTestSuites::employeeServiceStoreSchemaBinding];\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###ExternalFormat\n" +
                "Binding testServiceStoreTestSuites::employeeServiceStoreSchemaBinding\n" +
                "{\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [\n" +
                "    testServiceStoreTestSuites::Employee,\n" +
                "    testServiceStoreTestSuites::Employee2\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "Class testServiceStoreTestSuites::Employee\n" +
                "{\n" +
                "  kerberos: String[1];\n" +
                "  employeeID: String[1];\n" +
                "  title: String[0..1];\n" +
                "  firstName: String[0..1];\n" +
                "  lastName: String[0..1];\n" +
                "  countryCode: String[0..1];\n" +
                "}\n" +
                "\n" +
                "Class testServiceStoreTestSuites::Employee2\n" +
                "{\n" +
                "  stringParam: String[1];\n" +
                "  integerParam: Integer[1];\n" +
                "  floatParam: Float[1];\n" +
                "  booleanParam: Boolean[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping testServiceStoreTestSuites::ServiceStoreMapping\n" +
                "(\n" +
                "  *testServiceStoreTestSuites::Employee[employee_set]: ServiceStore\n" +
                "  {\n" +
                "    ~service [testServiceStoreTestSuites::ServiceStore] EmployeeServices.EmployeeService\n" +
                "  }\n" +
                "  *testServiceStoreTestSuites::Employee2[employee2_set]: ServiceStore\n" +
                "  {\n" +
                "    ~service [testServiceStoreTestSuites::ServiceStore] EmployeeServices.EmployeeServiceWithParameters\n" +
                "    (\n" +
                "      ~request\n" +
                "      (\n" +
                "        parameters\n" +
                "        (\n" +
                "          stringParam = $this.stringParam,\n" +
                "          integerParam = $this.integerParam,\n" +
                "          floatParam = $this.floatParam,\n" +
                "          booleanParam = $this.booleanParam\n" +
                "        )\n" +
                "      )\n" +
                "    )\n" +
                "  }\n" +
                "\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      function: |testServiceStoreTestSuites::Employee.all()->graphFetch(\n" +
                "  #{\n" +
                "    testServiceStoreTestSuites::Employee{\n" +
                "      kerberos,\n" +
                "      employeeID,\n" +
                "      title,\n" +
                "      firstName,\n" +
                "      lastName,\n" +
                "      countryCode\n" +
                "    }\n" +
                "  }#\n" +
                ")->serialize(\n" +
                "  #{\n" +
                "    testServiceStoreTestSuites::Employee{\n" +
                "      kerberos,\n" +
                "      employeeID,\n" +
                "      title,\n" +
                "      firstName,\n" +
                "      lastName,\n" +
                "      countryCode\n" +
                "    }\n" +
                "  }#\n" +
                ");\n" +
                "      tests:\n" +
                "      [\n" +
                "        passingTest:\n" +
                "        {\n" +
                "          data:\n" +
                "          [\n" +
                "            testServiceStoreTestSuites::ServiceStore:\n" +
                "              Reference\n" +
                "              #{\n" +
                "                testServiceStoreTestSuites::TestData\n" +
                "              }#\n" +
                "          ];\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected:\n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\\n  \"kerberos\": \"dummy kerberos\",\\n  \"employeeID\": \"dummy id\",\\n  \"title\": \"dummy title\",\\n  \"firstName\": \"dummy firstName\",\\n  \"lastName\": \"dummy lastname\",\\n  \"countryCode\": \"dummy countryCode\"\\n}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ];\n" +
                "        },\n" +
                "        failingTest:\n" +
                "        {\n" +
                "          data:\n" +
                "          [\n" +
                "            testServiceStoreTestSuites::ServiceStore:\n" +
                "              Reference\n" +
                "              #{\n" +
                "                testServiceStoreTestSuites::TestData\n" +
                "              }#\n" +
                "          ];\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected:\n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\\n  \"kerberos\": \"dummy kerberos\",\\n  \"employeeID\": \"Whoops\",\\n  \"title\": \"dummy title\",\\n  \"firstName\": \"dummy firstName\",\\n  \"lastName\": \"dummy lastname\",\\n  \"countryCode\": \"dummy countryCode\"\\n}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ];\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                ")\n";

        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(grammar);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, Identity.getAnonymousIdentity());
        Mapping mappingToTest = (Mapping) pureModelWithReferenceData.getPackageableElement("testServiceStoreTestSuites::ServiceStoreMapping");
        List<TestResult> mappingTestResults = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModelWithReferenceData, modelDataWithReferenceData);
        Assert.assertEquals(2, mappingTestResults.size());

        // passing test
        TestExecuted passingTest = guaranteedTestExecuted(findTestById(mappingTestResults, "passingTest"));
        Assert.assertEquals(TestExecutionStatus.PASS, passingTest.testExecutionStatus);
        Assert.assertEquals("testServiceStoreTestSuites::ServiceStoreMapping", passingTest.testable);
        Assert.assertEquals("testSuite1", passingTest.testSuiteId);
        Assert.assertEquals("passingTest", passingTest.atomicTestId);

        // failing test
        TestExecuted failingTest = guaranteedTestExecuted(findTestById(mappingTestResults, "failingTest"));
        Assert.assertEquals(TestExecutionStatus.FAIL, failingTest.testExecutionStatus);
        Assert.assertEquals("testServiceStoreTestSuites::ServiceStoreMapping", failingTest.testable);
        Assert.assertEquals("testSuite1", failingTest.testSuiteId);
        Assert.assertEquals("failingTest", failingTest.atomicTestId);
        Assert.assertEquals(1, failingTest.assertStatuses.size());
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


}
