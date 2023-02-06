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
        String grammar = "###ServiceStore\n" +
                "ServiceStore testServiceStoreTestSuites::ServiceStore\n" +
                "(\n" +
                "  ServiceGroup EmployeeServices\n" +
                "  (\n" +
                "    path : '/employees';\n" +
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
                "          stringParam : String (location = query),\n" +
                "          integerParam : Integer (location = query),\n" +
                "          floatParam : Float (location = query),\n" +
                "          booleanParam : Boolean (location = query)\n" +
                "      );\n" +
                "      response : [testServiceStoreTestSuites::Employee2 <- testServiceStoreTestSuites::employeeServiceStoreSchemaBinding];\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Data\n" +
                "Data testServiceStoreTestSuites::TestData\n" +
                "{\n" +
                "  ServiceStore #{\n" +
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
                "            ExternalFormat \n" +
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
                "###ExternalFormat\n" +
                "Binding testServiceStoreTestSuites::employeeServiceStoreSchemaBinding\n" +
                "{\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: \n" +
                "  [\n" +
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
                "Class testServiceStoreTestSuites::Employee2\n" +
                "{\n" +
                "  stringParam : String[1];\n" +
                "  integerParam :  Integer[1];\n" +
                "  floatParam :  Float[1];\n" +
                "  booleanParam :  Boolean[1];\n" +
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
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        testServiceStoreTestSuites::ServiceStore:\n" +
                "            Reference \n" +
                "            #{ \n" +
                "              testServiceStoreTestSuites::TestData \n" +
                "            }#\n" +
                "      ];\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          query: |testServiceStoreTestSuites::Employee.all()->graphFetch(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#)->serialize(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#);\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\"kerberos\" : \"dummy kerberos\",\"employeeID\" : \"dummy id\",\"title\" : \"dummy title\",\"firstName\" : \"dummy firstName\",\"lastName\" : \"dummy lastname\",\"countryCode\" : \"dummy countryCode\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ];\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                ")\n" +
                "\n";

        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(grammar);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, null);
        Mapping mappingToTest = (Mapping) pureModelWithReferenceData.getPackageableElement("testServiceStoreTestSuites::ServiceStoreMapping");
        List<TestResult> mappingTestResults = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(1, mappingTestResults.size());
        Assert.assertTrue(mappingTestResults.get(0) instanceof TestExecuted); //gets truncated
        Assert.assertEquals(TestExecutionStatus.PASS, ((TestExecuted) mappingTestResults.get(0)).testExecutionStatus);
        Assert.assertEquals("testServiceStoreTestSuites::ServiceStoreMapping", mappingTestResults.get(0).testable);
        Assert.assertEquals("testSuite1", mappingTestResults.get(0).testSuiteId);
        Assert.assertEquals("test1", mappingTestResults.get(0).atomicTestId);
    }
}
