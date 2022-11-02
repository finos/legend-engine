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

package org.finos.legend.engine.testable.service;

import net.javacrumbs.jsonunit.JsonMatchers;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.test.AtomicTestId;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertPass;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.EqualToJsonAssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestFailed;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestPassed;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.engine.testable.service.extension.ServiceTestableRunnerExtension;
import org.finos.legend.engine.testable.service.result.MultiExecutionServiceTestResult;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.List;

public class TestServiceTestSuite
{
    @Test
    public void testServiceTestSuiteWithServiceStore()
    {
        ServiceTestableRunnerExtension serviceTestableRunnerExtension = new ServiceTestableRunnerExtension();

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
                ")\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "ServiceStoreConnection testServiceStoreTestSuites::ServiceStoreConnection\n" +
                "{\n" +
                "  store: testServiceStoreTestSuites::ServiceStore;\n" +
                "  baseUrl: 'https://prodUrl.com';\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime testServiceStoreTestSuites::ServiceStoreRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    testServiceStoreTestSuites::ServiceStoreMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    testServiceStoreTestSuites::ServiceStore:\n" +
                "    [\n" +
                "      connection_1: testServiceStoreTestSuites::ServiceStoreConnection\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n";

        // Service Store Service With Reference Data
        String serviceStoreServiceWithReferenceData =
                "###Service\n" +
                        "Service testServiceStoreTestSuites::TestService\n" +
                        "{\n" +
                        "  pattern: '/testServiceStoreTestSuites/testService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'dummy1',\n" +
                        "    'dummy2'\n" +
                        "  ];\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  documentation: 'Service to test Service testSuite';\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |testServiceStoreTestSuites::Employee.all()->graphFetch(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#)->serialize(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#);\n" +
                        "    mapping: testServiceStoreTestSuites::ServiceStoreMapping;\n" +
                        "    runtime: testServiceStoreTestSuites::ServiceStoreRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection_1:\n" +
                        "            Reference \n" +
                        "            #{ \n" +
                        "              testServiceStoreTestSuites::TestData \n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          serializationFormat: PURE;\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualToJson\n" +
                        "              #{\n" +
                        "                expected : \n" +
                        "                  ExternalFormat\n" +
                        "                  #{\n" +
                        "                    contentType: 'application/json';\n" +
                        "                    data: '{ \"kerberos\": \"dummy kerberos\", \"employeeID\": \"dummy id\", \"title\": \"dummy title\", \"firstName\": \"dummy firstName\", \"lastName\": \"dummy lastname\", \"countryCode\": \"dummy countryCode\" }';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n\n";

        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(serviceStoreServiceWithReferenceData + grammar);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithReferenceData = (Root_meta_legend_service_metamodel_Service) pureModelWithReferenceData.getPackageableElement("testServiceStoreTestSuites::TestService");
        List<TestResult> serviceStoreTestResults = serviceTestableRunnerExtension.executeAllTest(serviceWithReferenceData, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(1, serviceStoreTestResults.size());
        Assert.assertTrue(serviceStoreTestResults.get(0) instanceof TestPassed);
        Assert.assertEquals("testServiceStoreTestSuites::TestService", ((TestPassed) serviceStoreTestResults.get(0)).testable);
        Assert.assertEquals("testSuite1", ((TestPassed) serviceStoreTestResults.get(0)).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", ((TestPassed) serviceStoreTestResults.get(0)).atomicTestId.atomicTestId);


        // Service Store Service With Reference Data and default serializationFormat
        String serviceStoreServiceWithDefaultSerializationFormat =
                "###Service\n" +
                        "Service testServiceStoreTestSuites::TestService\n" +
                        "{\n" +
                        "  pattern: '/testServiceStoreTestSuites/testService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'dummy1',\n" +
                        "    'dummy2'\n" +
                        "  ];\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  documentation: 'Service to test Service testSuite';\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |testServiceStoreTestSuites::Employee.all()->graphFetch(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#)->serialize(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#);\n" +
                        "    mapping: testServiceStoreTestSuites::ServiceStoreMapping;\n" +
                        "    runtime: testServiceStoreTestSuites::ServiceStoreRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection_1:\n" +
                        "            Reference \n" +
                        "            #{ \n" +
                        "              testServiceStoreTestSuites::TestData \n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
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
                        "                    data: '{\"builder\" : { \"_type\" : \"json\" }, \"values\" : { \"kerberos\": \"dummy kerberos\", \"employeeID\": \"dummy id\", \"title\": \"dummy title\", \"firstName\": \"dummy firstName\", \"lastName\": \"dummy lastname\", \"countryCode\": \"dummy countryCode\" }}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n\n";

        PureModelContextData modelDataWithDefaultSerializationFormat = PureGrammarParser.newInstance().parseModel(serviceStoreServiceWithDefaultSerializationFormat + grammar);
        PureModel pureModelWithDefaultSerializationFormat = Compiler.compile(modelDataWithDefaultSerializationFormat, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithDefaultSerializationFormat = (Root_meta_legend_service_metamodel_Service) pureModelWithDefaultSerializationFormat.getPackageableElement("testServiceStoreTestSuites::TestService");
        List<TestResult> serviceStoreTestResultsWithDefaultSerializationFormat = serviceTestableRunnerExtension.executeAllTest(serviceWithDefaultSerializationFormat, pureModelWithDefaultSerializationFormat, modelDataWithDefaultSerializationFormat);

        Assert.assertEquals(1, serviceStoreTestResultsWithDefaultSerializationFormat.size());
        Assert.assertTrue(serviceStoreTestResultsWithDefaultSerializationFormat.get(0) instanceof TestPassed);
        Assert.assertEquals("testServiceStoreTestSuites::TestService", ((TestPassed) serviceStoreTestResultsWithDefaultSerializationFormat.get(0)).testable);
        Assert.assertEquals("testSuite1", ((TestPassed) serviceStoreTestResultsWithDefaultSerializationFormat.get(0)).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", ((TestPassed) serviceStoreTestResultsWithDefaultSerializationFormat.get(0)).atomicTestId.atomicTestId);


        // Service Store Service With Embedded Data
        String serviceStoreServiceWithEmbeddedData =
                "###Service\n" +
                        "Service testServiceStoreTestSuites::TestService\n" +
                        "{\n" +
                        "  pattern: '/testServiceStoreTestSuites/testService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'dummy1',\n" +
                        "    'dummy2'\n" +
                        "  ];\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  documentation: 'Service to test Service testSuite';\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |testServiceStoreTestSuites::Employee.all()->graphFetch(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#)->serialize(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#);\n" +
                        "    mapping: testServiceStoreTestSuites::ServiceStoreMapping;\n" +
                        "    runtime: testServiceStoreTestSuites::ServiceStoreRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection_1:\n" +
                        "            ServiceStore\n" +
                        "            #{\n" +
                        "              [\n" +
                        "                {\n" +
                        "                   request:\n" +
                        "                   {\n" +
                        "                       method: GET;\n" +
                        "                       url: '/employees/allEmployees';\n" +
                        "                   };\n" +
                        "                   response:\n" +
                        "                   {\n" +
                        "                       body:\n" +
                        "                           ExternalFormat \n" +
                        "                           #{\n" +
                        "                               contentType: 'application/json';\n" +
                        "                               data: '[ { \"kerberos\": \"dummy kerberos\", \"employeeID\": \"dummy id2\", \"title\": \"dummy title\", \"firstName\": \"dummy firstName\", \"lastName\": \"dummy lastname\", \"countryCode\": \"dummy countryCode\" } ]';\n" +
                        "                           }#;\n" +
                        "                   };\n" +
                        "               }\n" +
                        "             ]\n" +
                        "           }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          serializationFormat: PURE;\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualToJson\n" +
                        "              #{\n" +
                        "                expected : \n" +
                        "                  ExternalFormat\n" +
                        "                  #{\n" +
                        "                    contentType: 'application/json';\n" +
                        "                    data: '{ \"kerberos\": \"dummy kerberos\", \"employeeID\": \"dummy id2\", \"title\": \"dummy title\", \"firstName\": \"dummy firstName\", \"lastName\": \"dummy lastname\", \"countryCode\": \"dummy countryCode\" }';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n\n";

        PureModelContextData modelDataWithEmbeddedData = PureGrammarParser.newInstance().parseModel(serviceStoreServiceWithEmbeddedData + grammar);
        PureModel pureModelWithEmbeddedData = Compiler.compile(modelDataWithEmbeddedData, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithEmbeddedData = (Root_meta_legend_service_metamodel_Service) pureModelWithEmbeddedData.getPackageableElement("testServiceStoreTestSuites::TestService");
        List<TestResult> serviceStoreTestResultsWithEmbeddedData = serviceTestableRunnerExtension.executeAllTest(serviceWithEmbeddedData, pureModelWithEmbeddedData, modelDataWithEmbeddedData);

        Assert.assertEquals(1, serviceStoreTestResultsWithEmbeddedData.size());
        Assert.assertTrue(serviceStoreTestResultsWithEmbeddedData.get(0) instanceof TestPassed);
        Assert.assertEquals("testServiceStoreTestSuites::TestService", ((TestPassed) serviceStoreTestResultsWithEmbeddedData.get(0)).testable);
        Assert.assertEquals("testSuite1", ((TestPassed) serviceStoreTestResultsWithEmbeddedData.get(0)).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", ((TestPassed) serviceStoreTestResultsWithEmbeddedData.get(0)).atomicTestId.atomicTestId);

        // Service Store Service With Failing Test
        String serviceStoreServiceWithFailingTest =
                "###Service\n" +
                        "Service testServiceStoreTestSuites::TestService\n" +
                        "{\n" +
                        "  pattern: '/testServiceStoreTestSuites/testService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'dummy1',\n" +
                        "    'dummy2'\n" +
                        "  ];\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  documentation: 'Service to test Service testSuite';\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |testServiceStoreTestSuites::Employee.all()->graphFetch(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#)->serialize(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#);\n" +
                        "    mapping: testServiceStoreTestSuites::ServiceStoreMapping;\n" +
                        "    runtime: testServiceStoreTestSuites::ServiceStoreRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection_1:\n" +
                        "            Reference \n" +
                        "            #{ \n" +
                        "              testServiceStoreTestSuites::TestData \n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          serializationFormat: PURE;\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualToJson\n" +
                        "              #{\n" +
                        "                expected : \n" +
                        "                  ExternalFormat\n" +
                        "                  #{\n" +
                        "                    contentType: 'application/json';\n" +
                        "                    data: '{ \"kerberos\": \"dummy kerberos\", \"employeeID\": \"dummy id2\", \"title\": \"dummy title\", \"firstName\": \"dummy firstName\", \"lastName\": \"dummy lastname\", \"countryCode\": \"dummy countryCode\" }';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n\n";

        PureModelContextData modelDataWithFailingTest = PureGrammarParser.newInstance().parseModel(serviceStoreServiceWithFailingTest + grammar);
        PureModel pureModelWithFailingTest = Compiler.compile(modelDataWithFailingTest, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithFailingTest = (Root_meta_legend_service_metamodel_Service) pureModelWithFailingTest.getPackageableElement("testServiceStoreTestSuites::TestService");
        List<TestResult> serviceStoreTestResultsWithFailingTest = serviceTestableRunnerExtension.executeAllTest(serviceWithFailingTest, pureModelWithFailingTest, modelDataWithFailingTest);

        Assert.assertEquals(1, serviceStoreTestResultsWithFailingTest.size());
        Assert.assertTrue(serviceStoreTestResultsWithFailingTest.get(0) instanceof TestFailed);
        Assert.assertEquals("testServiceStoreTestSuites::TestService", serviceStoreTestResultsWithFailingTest.get(0).testable);
        Assert.assertEquals("testSuite1", serviceStoreTestResultsWithFailingTest.get(0).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", serviceStoreTestResultsWithFailingTest.get(0).atomicTestId.atomicTestId);
        Assert.assertEquals(1, ((TestFailed) serviceStoreTestResultsWithFailingTest.get(0)).assertStatuses.size());
        Assert.assertTrue(((TestFailed) serviceStoreTestResultsWithFailingTest.get(0)).assertStatuses.get(0) instanceof EqualToJsonAssertFail);
        Assert.assertEquals("assert1", ((EqualToJsonAssertFail) ((TestFailed) serviceStoreTestResultsWithFailingTest.get(0)).assertStatuses.get(0)).id);
        Assert.assertEquals("Actual result does not match Expected result", ((EqualToJsonAssertFail) ((TestFailed) serviceStoreTestResultsWithFailingTest.get(0)).assertStatuses.get(0)).message);
        Assert.assertEquals("{\n" +
                "  \"kerberos\" : \"dummy kerberos\",\n" +
                "  \"employeeID\" : \"dummy id\",\n" +
                "  \"title\" : \"dummy title\",\n" +
                "  \"firstName\" : \"dummy firstName\",\n" +
                "  \"lastName\" : \"dummy lastname\",\n" +
                "  \"countryCode\" : \"dummy countryCode\"\n" +
                "}", ((EqualToJsonAssertFail) ((TestFailed) serviceStoreTestResultsWithFailingTest.get(0)).assertStatuses.get(0)).actual);
        Assert.assertEquals("{\n" +
                "  \"kerberos\" : \"dummy kerberos\",\n" +
                "  \"employeeID\" : \"dummy id2\",\n" +
                "  \"title\" : \"dummy title\",\n" +
                "  \"firstName\" : \"dummy firstName\",\n" +
                "  \"lastName\" : \"dummy lastname\",\n" +
                "  \"countryCode\" : \"dummy countryCode\"\n" +
                "}", ((EqualToJsonAssertFail) ((TestFailed) serviceStoreTestResultsWithFailingTest.get(0)).assertStatuses.get(0)).expected);

        // Service Store Service With Multiple Asserts All Passing
        String serviceStoreServiceWithMultipleAsserts =
                "###Service\n" +
                        "Service testServiceStoreTestSuites::TestService\n" +
                        "{\n" +
                        "  pattern: '/testServiceStoreTestSuites/testService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'dummy1',\n" +
                        "    'dummy2'\n" +
                        "  ];\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  documentation: 'Service to test Service testSuite';\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |testServiceStoreTestSuites::Employee.all()->graphFetch(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#)->serialize(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#);\n" +
                        "    mapping: testServiceStoreTestSuites::ServiceStoreMapping;\n" +
                        "    runtime: testServiceStoreTestSuites::ServiceStoreRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection_1:\n" +
                        "            Reference \n" +
                        "            #{ \n" +
                        "              testServiceStoreTestSuites::TestData \n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          serializationFormat: PURE;\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualTo\n" +
                        "              #{\n" +
                        "                expected : '{\"kerberos\":\"dummy kerberos\",\"employeeID\":\"dummy id\",\"title\":\"dummy title\",\"firstName\":\"dummy firstName\",\"lastName\":\"dummy lastname\",\"countryCode\":\"dummy countryCode\"}';\n" +
                        "              }#,\n" +
                        "            assert2:\n" +
                        "              EqualToJson\n" +
                        "              #{\n" +
                        "                expected : \n" +
                        "                  ExternalFormat\n" +
                        "                  #{\n" +
                        "                    contentType: 'application/json';\n" +
                        "                    data: '{ \"kerberos\": \"dummy kerberos\", \"employeeID\": \"dummy id\", \"title\": \"dummy title\", \"firstName\": \"dummy firstName\", \"lastName\": \"dummy lastname\", \"countryCode\": \"dummy countryCode\" }';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n\n";

        PureModelContextData modelDataWithMultipleAsserts = PureGrammarParser.newInstance().parseModel(serviceStoreServiceWithMultipleAsserts + grammar);
        PureModel pureModelWithMultipleAsserts = Compiler.compile(modelDataWithMultipleAsserts, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithMultipleAsserts = (Root_meta_legend_service_metamodel_Service) pureModelWithMultipleAsserts.getPackageableElement("testServiceStoreTestSuites::TestService");
        List<TestResult> serviceStoreTestResultsWithMultipleAsserts = serviceTestableRunnerExtension.executeAllTest(serviceWithMultipleAsserts, pureModelWithMultipleAsserts, modelDataWithMultipleAsserts);

        Assert.assertEquals(1, serviceStoreTestResultsWithMultipleAsserts.size());
        Assert.assertTrue(serviceStoreTestResultsWithMultipleAsserts.get(0) instanceof TestPassed);
        Assert.assertEquals("testServiceStoreTestSuites::TestService", serviceStoreTestResultsWithMultipleAsserts.get(0).testable);
        Assert.assertEquals("testSuite1", serviceStoreTestResultsWithMultipleAsserts.get(0).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", serviceStoreTestResultsWithMultipleAsserts.get(0).atomicTestId.atomicTestId);

        // Service Store Service With Multiple Asserts Few Failing
        String serviceStoreServiceWithMultipleAsserts2 =
                "###Service\n" +
                        "Service testServiceStoreTestSuites::TestService\n" +
                        "{\n" +
                        "  pattern: '/testServiceStoreTestSuites/testService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'dummy1',\n" +
                        "    'dummy2'\n" +
                        "  ];\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  documentation: 'Service to test Service testSuite';\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |testServiceStoreTestSuites::Employee.all()->graphFetch(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#)->serialize(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#);\n" +
                        "    mapping: testServiceStoreTestSuites::ServiceStoreMapping;\n" +
                        "    runtime: testServiceStoreTestSuites::ServiceStoreRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection_1:\n" +
                        "            Reference \n" +
                        "            #{ \n" +
                        "              testServiceStoreTestSuites::TestData \n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          serializationFormat: PURE;\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualTo\n" +
                        "              #{\n" +
                        "                expected : '{\"kerberos\":\"dummy kerberos\",\"employeeID\":\"dummy id\",\"title\":\"dummy title\",\"firstName\":\"dummy firstName\",\"lastName\":\"dummy lastname\",\"countryCode\":\"dummy countryCode\"}';\n" +
                        "              }#,\n" +
                        "            assert2:\n" +
                        "              EqualToJson\n" +
                        "              #{\n" +
                        "                expected : \n" +
                        "                  ExternalFormat\n" +
                        "                  #{\n" +
                        "                    contentType: 'application/json';\n" +
                        "                    data: '{ \"kerberos\": \"dummy kerberos\", \"employeeID\": \"dummy id2\", \"title\": \"dummy title\", \"firstName\": \"dummy firstName\", \"lastName\": \"dummy lastname\", \"countryCode\": \"dummy countryCode\" }';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n\n";

        PureModelContextData modelDataWithMultipleAsserts2 = PureGrammarParser.newInstance().parseModel(serviceStoreServiceWithMultipleAsserts2 + grammar);
        PureModel pureModelWithMultipleAsserts2 = Compiler.compile(modelDataWithMultipleAsserts2, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithMultipleAsserts2 = (Root_meta_legend_service_metamodel_Service) pureModelWithMultipleAsserts2.getPackageableElement("testServiceStoreTestSuites::TestService");
        List<TestResult> serviceStoreTestResultsWithMultipleAsserts2 = serviceTestableRunnerExtension.executeAllTest(serviceWithMultipleAsserts2, pureModelWithMultipleAsserts2, modelDataWithMultipleAsserts2);

        Assert.assertEquals(1, serviceStoreTestResultsWithMultipleAsserts2.size());
        Assert.assertTrue(serviceStoreTestResultsWithMultipleAsserts2.get(0) instanceof TestFailed);
        Assert.assertEquals("testServiceStoreTestSuites::TestService", serviceStoreTestResultsWithMultipleAsserts2.get(0).testable);
        Assert.assertEquals("testSuite1", serviceStoreTestResultsWithMultipleAsserts2.get(0).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", serviceStoreTestResultsWithMultipleAsserts2.get(0).atomicTestId.atomicTestId);
        Assert.assertEquals(2, ((TestFailed) serviceStoreTestResultsWithMultipleAsserts2.get(0)).assertStatuses.size());
        Assert.assertTrue(((TestFailed) serviceStoreTestResultsWithMultipleAsserts2.get(0)).assertStatuses.get(0) instanceof AssertPass);
        Assert.assertTrue(((TestFailed) serviceStoreTestResultsWithMultipleAsserts2.get(0)).assertStatuses.get(1) instanceof EqualToJsonAssertFail);
        Assert.assertEquals("assert2", ((EqualToJsonAssertFail) ((TestFailed) serviceStoreTestResultsWithMultipleAsserts2.get(0)).assertStatuses.get(1)).id);
        Assert.assertEquals("Actual result does not match Expected result", ((EqualToJsonAssertFail) ((TestFailed) serviceStoreTestResultsWithMultipleAsserts2.get(0)).assertStatuses.get(1)).message);
        Assert.assertEquals("{\n" +
                "  \"kerberos\" : \"dummy kerberos\",\n" +
                "  \"employeeID\" : \"dummy id\",\n" +
                "  \"title\" : \"dummy title\",\n" +
                "  \"firstName\" : \"dummy firstName\",\n" +
                "  \"lastName\" : \"dummy lastname\",\n" +
                "  \"countryCode\" : \"dummy countryCode\"\n" +
                "}", ((EqualToJsonAssertFail) ((TestFailed) serviceStoreTestResultsWithMultipleAsserts2.get(0)).assertStatuses.get(1)).actual);
        Assert.assertEquals("{\n" +
                "  \"kerberos\" : \"dummy kerberos\",\n" +
                "  \"employeeID\" : \"dummy id2\",\n" +
                "  \"title\" : \"dummy title\",\n" +
                "  \"firstName\" : \"dummy firstName\",\n" +
                "  \"lastName\" : \"dummy lastname\",\n" +
                "  \"countryCode\" : \"dummy countryCode\"\n" +
                "}", ((EqualToJsonAssertFail) ((TestFailed) serviceStoreTestResultsWithMultipleAsserts2.get(0)).assertStatuses.get(1)).expected);

        // Service Store Service With Multiple Tests All Passing
        String serviceStoreServiceWithMultipleTests1 =
                "###Service\n" +
                        "Service testServiceStoreTestSuites::TestService\n" +
                        "{\n" +
                        "  pattern: '/testServiceStoreTestSuites/testService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'dummy1',\n" +
                        "    'dummy2'\n" +
                        "  ];\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  documentation: 'Service to test Service testSuite';\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |testServiceStoreTestSuites::Employee.all()->graphFetch(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#)->serialize(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#);\n" +
                        "    mapping: testServiceStoreTestSuites::ServiceStoreMapping;\n" +
                        "    runtime: testServiceStoreTestSuites::ServiceStoreRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection_1:\n" +
                        "            Reference \n" +
                        "            #{ \n" +
                        "              testServiceStoreTestSuites::TestData \n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          serializationFormat: PURE;\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualTo\n" +
                        "              #{\n" +
                        "                expected : '{\"kerberos\":\"dummy kerberos\",\"employeeID\":\"dummy id\",\"title\":\"dummy title\",\"firstName\":\"dummy firstName\",\"lastName\":\"dummy lastname\",\"countryCode\":\"dummy countryCode\"}';\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        },\n" +
                        "        test2:\n" +
                        "        {\n" +
                        "          serializationFormat: PURE;\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualToJson\n" +
                        "              #{\n" +
                        "                expected : \n" +
                        "                  ExternalFormat\n" +
                        "                  #{\n" +
                        "                    contentType: 'application/json';\n" +
                        "                    data: '{ \"kerberos\": \"dummy kerberos\", \"employeeID\": \"dummy id\", \"title\": \"dummy title\", \"firstName\": \"dummy firstName\", \"lastName\": \"dummy lastname\", \"countryCode\": \"dummy countryCode\" }';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n\n";

        PureModelContextData modelDataWithMultipleTests1 = PureGrammarParser.newInstance().parseModel(serviceStoreServiceWithMultipleTests1 + grammar);
        PureModel pureModelWithMultipleTests1 = Compiler.compile(modelDataWithMultipleTests1, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithMultipleTests1 = (Root_meta_legend_service_metamodel_Service) pureModelWithMultipleTests1.getPackageableElement("testServiceStoreTestSuites::TestService");
        List<TestResult> serviceStoreTestResultsWithMultipleTests1 = serviceTestableRunnerExtension.executeAllTest(serviceWithMultipleTests1, pureModelWithMultipleTests1, modelDataWithMultipleTests1);

        Assert.assertEquals(2, serviceStoreTestResultsWithMultipleTests1.size());
        Assert.assertTrue(serviceStoreTestResultsWithMultipleTests1.get(0) instanceof TestPassed);
        Assert.assertEquals("testServiceStoreTestSuites::TestService", serviceStoreTestResultsWithMultipleTests1.get(0).testable);
        Assert.assertEquals("testSuite1", serviceStoreTestResultsWithMultipleTests1.get(0).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", serviceStoreTestResultsWithMultipleTests1.get(0).atomicTestId.atomicTestId);
        Assert.assertTrue(serviceStoreTestResultsWithMultipleTests1.get(1) instanceof TestPassed);
        Assert.assertEquals("testServiceStoreTestSuites::TestService", serviceStoreTestResultsWithMultipleTests1.get(1).testable);
        Assert.assertEquals("testSuite1", serviceStoreTestResultsWithMultipleTests1.get(1).atomicTestId.testSuiteId);
        Assert.assertEquals("test2", serviceStoreTestResultsWithMultipleTests1.get(1).atomicTestId.atomicTestId);

        // Service Store Service With Few Failing
        String serviceStoreServiceWithMultipleTests2 =
                "###Service\n" +
                        "Service testServiceStoreTestSuites::TestService\n" +
                        "{\n" +
                        "  pattern: '/testServiceStoreTestSuites/testService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'dummy1',\n" +
                        "    'dummy2'\n" +
                        "  ];\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  documentation: 'Service to test Service testSuite';\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |testServiceStoreTestSuites::Employee.all()->graphFetch(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#)->serialize(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#);\n" +
                        "    mapping: testServiceStoreTestSuites::ServiceStoreMapping;\n" +
                        "    runtime: testServiceStoreTestSuites::ServiceStoreRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection_1:\n" +
                        "            Reference \n" +
                        "            #{ \n" +
                        "              testServiceStoreTestSuites::TestData \n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          serializationFormat: PURE;\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualTo\n" +
                        "              #{\n" +
                        "                expected : '{\"kerberos\":\"dummy kerberos\",\"employeeID\":\"dummy id2\",\"title\":\"dummy title\",\"firstName\":\"dummy firstName\",\"lastName\":\"dummy lastname\",\"countryCode\":\"dummy countryCode\"}';\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        },\n" +
                        "        test2:\n" +
                        "        {\n" +
                        "          serializationFormat: PURE;\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualToJson\n" +
                        "              #{\n" +
                        "                expected : \n" +
                        "                  ExternalFormat\n" +
                        "                  #{\n" +
                        "                    contentType: 'application/json';\n" +
                        "                    data: '{ \"kerberos\": \"dummy kerberos\", \"employeeID\": \"dummy id\", \"title\": \"dummy title\", \"firstName\": \"dummy firstName\", \"lastName\": \"dummy lastname\", \"countryCode\": \"dummy countryCode\" }';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n\n";

        PureModelContextData modelDataWithMultipleTests2 = PureGrammarParser.newInstance().parseModel(serviceStoreServiceWithMultipleTests2 + grammar);
        PureModel pureModelWithMultipleTests2 = Compiler.compile(modelDataWithMultipleTests2, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithMultipleTests2 = (Root_meta_legend_service_metamodel_Service) pureModelWithMultipleTests2.getPackageableElement("testServiceStoreTestSuites::TestService");
        List<TestResult> serviceStoreTestResultsWithMultipleTests2 = serviceTestableRunnerExtension.executeAllTest(serviceWithMultipleTests2, pureModelWithMultipleTests2, modelDataWithMultipleTests2);

        Assert.assertEquals(2, serviceStoreTestResultsWithMultipleTests2.size());
        Assert.assertTrue(serviceStoreTestResultsWithMultipleTests2.get(0) instanceof TestFailed);
        Assert.assertEquals("testServiceStoreTestSuites::TestService", serviceStoreTestResultsWithMultipleTests2.get(0).testable);
        Assert.assertEquals("testSuite1", serviceStoreTestResultsWithMultipleTests2.get(0).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", serviceStoreTestResultsWithMultipleTests2.get(0).atomicTestId.atomicTestId);
        Assert.assertEquals(1, ((TestFailed) serviceStoreTestResultsWithMultipleTests2.get(0)).assertStatuses.size());
        Assert.assertTrue(((TestFailed) serviceStoreTestResultsWithMultipleTests2.get(0)).assertStatuses.get(0) instanceof AssertFail);
        Assert.assertEquals("assert1", ((AssertFail) ((TestFailed) serviceStoreTestResultsWithMultipleTests2.get(0)).assertStatuses.get(0)).id);
        Assert.assertEquals("Expected : {\"kerberos\":\"dummy kerberos\",\"employeeID\":\"dummy id2\",\"title\":\"dummy title\",\"firstName\":\"dummy firstName\",\"lastName\":\"dummy lastname\",\"countryCode\":\"dummy countryCode\"}, Found : {\"kerberos\":\"dummy kerberos\",\"employeeID\":\"dummy id\",\"title\":\"dummy title\",\"firstName\":\"dummy firstName\",\"lastName\":\"dummy lastname\",\"countryCode\":\"dummy countryCode\"}", ((AssertFail) ((TestFailed) serviceStoreTestResultsWithMultipleTests2.get(0)).assertStatuses.get(0)).message);

        Assert.assertTrue(serviceStoreTestResultsWithMultipleTests2.get(1) instanceof TestPassed);
        Assert.assertEquals("testServiceStoreTestSuites::TestService", serviceStoreTestResultsWithMultipleTests2.get(1).testable);
        Assert.assertEquals("testSuite1", serviceStoreTestResultsWithMultipleTests2.get(1).atomicTestId.testSuiteId);
        Assert.assertEquals("test2", serviceStoreTestResultsWithMultipleTests2.get(1).atomicTestId.atomicTestId);

        // Service Store Service With Multiple Test Suite
        String serviceStoreServiceWithMultipleTestSuites =
                "###Service\n" +
                        "Service testServiceStoreTestSuites::TestService\n" +
                        "{\n" +
                        "  pattern: '/testServiceStoreTestSuites/testService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'dummy1',\n" +
                        "    'dummy2'\n" +
                        "  ];\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  documentation: 'Service to test Service testSuite';\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |testServiceStoreTestSuites::Employee.all()->graphFetch(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#)->serialize(#{testServiceStoreTestSuites::Employee{kerberos,employeeID,title,firstName,lastName,countryCode}}#);\n" +
                        "    mapping: testServiceStoreTestSuites::ServiceStoreMapping;\n" +
                        "    runtime: testServiceStoreTestSuites::ServiceStoreRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection_1:\n" +
                        "            Reference \n" +
                        "            #{ \n" +
                        "              testServiceStoreTestSuites::TestData \n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          serializationFormat: PURE;\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualTo\n" +
                        "              #{\n" +
                        "                expected : '{\"kerberos\":\"dummy kerberos\",\"employeeID\":\"dummy id\",\"title\":\"dummy title\",\"firstName\":\"dummy firstName\",\"lastName\":\"dummy lastname\",\"countryCode\":\"dummy countryCode\"}';\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    testSuite2:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection_1:\n" +
                        "            Reference \n" +
                        "            #{ \n" +
                        "              testServiceStoreTestSuites::TestData \n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          serializationFormat: PURE;\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualToJson\n" +
                        "              #{\n" +
                        "                expected : \n" +
                        "                  ExternalFormat\n" +
                        "                  #{\n" +
                        "                    contentType: 'application/json';\n" +
                        "                    data: '{ \"kerberos\": \"dummy kerberos\", \"employeeID\": \"dummy id\", \"title\": \"dummy title\", \"firstName\": \"dummy firstName\", \"lastName\": \"dummy lastname\", \"countryCode\": \"dummy countryCode\" }';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n\n";

        PureModelContextData modelDataWithMultipleTestSuites = PureGrammarParser.newInstance().parseModel(serviceStoreServiceWithMultipleTestSuites + grammar);
        PureModel pureModelWithMultipleTestSuites = Compiler.compile(modelDataWithMultipleTestSuites, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithMultipleTestSuites = (Root_meta_legend_service_metamodel_Service) pureModelWithMultipleTestSuites.getPackageableElement("testServiceStoreTestSuites::TestService");
        List<TestResult> serviceStoreTestResultsWithMultipleTestSuites = serviceTestableRunnerExtension.executeAllTest(serviceWithMultipleTestSuites, pureModelWithMultipleTestSuites, modelDataWithMultipleTestSuites);

        Assert.assertEquals(2, serviceStoreTestResultsWithMultipleTestSuites.size());
        Assert.assertTrue(serviceStoreTestResultsWithMultipleTestSuites.get(1) instanceof TestPassed);
        Assert.assertEquals("testServiceStoreTestSuites::TestService", serviceStoreTestResultsWithMultipleTestSuites.get(1).testable);
        Assert.assertEquals("testSuite1", serviceStoreTestResultsWithMultipleTestSuites.get(0).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", serviceStoreTestResultsWithMultipleTestSuites.get(0).atomicTestId.atomicTestId);

        Assert.assertTrue(serviceStoreTestResultsWithMultipleTestSuites.get(1) instanceof TestPassed);
        Assert.assertEquals("testServiceStoreTestSuites::TestService", serviceStoreTestResultsWithMultipleTestSuites.get(1).testable);
        Assert.assertEquals("testSuite2", serviceStoreTestResultsWithMultipleTestSuites.get(1).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", serviceStoreTestResultsWithMultipleTestSuites.get(1).atomicTestId.atomicTestId);

        // Service Store Service With Parameters
        String serviceStoreServiceWithParameters =
                "###Service\n" +
                        "Service testServiceStoreTestSuites::TestService\n" +
                        "{\n" +
                        "  pattern: '/testServiceStoreTestSuites/testService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'dummy1',\n" +
                        "    'dummy2'\n" +
                        "  ];\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  documentation: 'Service to test Service testSuite';\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: {stringParam:String[1],integerParam:Integer[1],floatParam:Float[1],booleanParam:Boolean[1]|testServiceStoreTestSuites::Employee2.all()->filter(e | $e.stringParam == $stringParam && $e.integerParam == $integerParam && $e.floatParam == $floatParam && $e.booleanParam == $booleanParam )->graphFetch(#{testServiceStoreTestSuites::Employee2{stringParam,integerParam,floatParam,booleanParam}}#)->serialize(#{testServiceStoreTestSuites::Employee2{stringParam,integerParam,floatParam,booleanParam}}#)};\n" +
                        "    mapping: testServiceStoreTestSuites::ServiceStoreMapping;\n" +
                        "    runtime: testServiceStoreTestSuites::ServiceStoreRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection_1:\n" +
                        "            ServiceStore\n" +
                        "            #{\n" +
                        "              [\n" +
                        "                {\n" +
                        "                   request:\n" +
                        "                   {\n" +
                        "                       method: GET;\n" +
                        "                       urlPath: '/employees/employeesWithParameters';\n" +
                        "                       queryParameters:\n" +
                        "                       {\n" +
                        "                           stringParam:\n" +
                        "                               EqualTo\n" +
                        "                               #{\n" +
                        "                                   expected: 'dummy';\n" +
                        "                               }#,\n" +
                        "                           integerParam:\n" +
                        "                               EqualTo\n" +
                        "                               #{\n" +
                        "                                   expected: '1';\n" +
                        "                               }#,\n" +
                        "                           floatParam:\n" +
                        "                               EqualTo\n" +
                        "                               #{\n" +
                        "                                   expected: '1.123';\n" +
                        "                               }#,\n" +
                        "                           booleanParam:\n" +
                        "                               EqualTo\n" +
                        "                               #{\n" +
                        "                                   expected: 'false';\n" +
                        "                               }#\n" +
                        "                       };\n" +
                        "                   };\n" +
                        "                   response:\n" +
                        "                   {\n" +
                        "                       body:\n" +
                        "                           ExternalFormat \n" +
                        "                           #{\n" +
                        "                               contentType: 'application/json';\n" +
                        "                               data: '[ {  \"stringParam\": \"dummy\",  \"integerParam\": 1,  \"floatParam\": 1.123,  \"booleanParam\": false} ]';" +
                        "                           }#;\n" +
                        "                   };\n" +
                        "               }\n" +
                        "             ]\n" +
                        "           }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          serializationFormat: PURE;\n" +
                        "          parameters:\n" +
                        "          [\n" +
                        "            stringParam = 'dummy',\n" +
                        "            integerParam = 1,\n" +
                        "            floatParam = 1.123,\n" +
                        "            booleanParam = false\n" +
                        "          ]\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualTo\n" +
                        "              #{\n" +
                        "                expected : '{\"stringParam\":\"dummy\",\"integerParam\":1,\"floatParam\":1.123,\"booleanParam\":false}';\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n\n";

        PureModelContextData modelDataWithParameters = PureGrammarParser.newInstance().parseModel(serviceStoreServiceWithParameters + grammar);
        PureModel pureModelWithParameters = Compiler.compile(modelDataWithParameters, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithParameters = (Root_meta_legend_service_metamodel_Service) pureModelWithParameters.getPackageableElement("testServiceStoreTestSuites::TestService");
        List<TestResult> serviceStoreTestResultsWithParameters = serviceTestableRunnerExtension.executeAllTest(serviceWithParameters, pureModelWithParameters, modelDataWithParameters);

        Assert.assertEquals(1, serviceStoreTestResultsWithParameters.size());
        Assert.assertTrue(serviceStoreTestResultsWithParameters.get(0) instanceof TestPassed);
        Assert.assertEquals("testServiceStoreTestSuites::TestService", serviceStoreTestResultsWithParameters.get(0).testable);
        Assert.assertEquals("testSuite1", serviceStoreTestResultsWithParameters.get(0).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", serviceStoreTestResultsWithParameters.get(0).atomicTestId.atomicTestId);

        // Service Store Service With Test Error
        String serviceStoreServiceWithTestError =
                "###Service\n" +
                        "Service testServiceStoreTestSuites::TestService\n" +
                        "{\n" +
                        "  pattern: '/testServiceStoreTestSuites/testService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'dummy1',\n" +
                        "    'dummy2'\n" +
                        "  ];\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  documentation: 'Service to test Service testSuite';\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: {stringParam:String[1],integerParam:Integer[1],floatParam:Float[1],booleanParam:Boolean[1]|testServiceStoreTestSuites::Employee2.all()->filter(e | $e.stringParam == $stringParam && $e.integerParam == $integerParam && $e.floatParam == $floatParam && $e.booleanParam == $booleanParam )->graphFetch(#{testServiceStoreTestSuites::Employee2{stringParam,integerParam,floatParam,booleanParam}}#)->serialize(#{testServiceStoreTestSuites::Employee2{stringParam,integerParam,floatParam,booleanParam}}#)};\n" +
                        "    mapping: testServiceStoreTestSuites::ServiceStoreMapping;\n" +
                        "    runtime: testServiceStoreTestSuites::ServiceStoreRuntime;\n" +
                        "  }\n" +
                        "  testSuites:\n" +
                        "  [\n" +
                        "    testSuite1:\n" +
                        "    {\n" +
                        "      data:\n" +
                        "      [\n" +
                        "        connections:\n" +
                        "        [\n" +
                        "          connection_1:\n" +
                        "            ServiceStore\n" +
                        "            #{\n" +
                        "              [\n" +
                        "                {\n" +
                        "                   request:\n" +
                        "                   {\n" +
                        "                       method: GET;\n" +
                        "                       urlPath: '/employees/employeesWithTestError';\n" +
                        "                       queryParameters:\n" +
                        "                       {\n" +
                        "                           stringParam:\n" +
                        "                               EqualTo\n" +
                        "                               #{\n" +
                        "                                   expected: 'dummy';\n" +
                        "                               }#,\n" +
                        "                           floatParam:\n" +
                        "                               EqualTo\n" +
                        "                               #{\n" +
                        "                                   expected: '1.123';\n" +
                        "                               }#,\n" +
                        "                           booleanParam:\n" +
                        "                               EqualTo\n" +
                        "                               #{\n" +
                        "                                   expected: 'false';\n" +
                        "                               }#\n" +
                        "                       };\n" +
                        "                   };\n" +
                        "                   response:\n" +
                        "                   {\n" +
                        "                       body:\n" +
                        "                           ExternalFormat \n" +
                        "                           #{\n" +
                        "                               contentType: 'application/json';\n" +
                        "                               data: '[ {  \"stringParam\": \"dummy\",  \"integerParam\": 1,  \"floatParam\": 1.123,  \"booleanParam\": false} ]';" +
                        "                           }#;\n" +
                        "                   };\n" +
                        "               }\n" +
                        "             ]\n" +
                        "           }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          serializationFormat: PURE;\n" +
                        "          parameters:\n" +
                        "          [\n" +
                        "            stringParam = 'dummy',\n" +
                        "            integerParam = 1,\n" +
                        "            floatParam = 1.123,\n" +
                        "            booleanParam = false\n" +
                        "          ]\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "            assert1:\n" +
                        "              EqualTo\n" +
                        "              #{\n" +
                        "                expected : '{\"stringParam\":\"dummy\",\"integerParam\":1,\"floatParam\":1.123,\"booleanParam\":false}';\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n\n";

        PureModelContextData modelDataWithTestError = PureGrammarParser.newInstance().parseModel(serviceStoreServiceWithTestError + grammar);
        PureModel pureModelWithTestError = Compiler.compile(modelDataWithTestError, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithTestError = (Root_meta_legend_service_metamodel_Service) pureModelWithTestError.getPackageableElement("testServiceStoreTestSuites::TestService");
        List<TestResult> serviceStoreTestResultsWithTestError = serviceTestableRunnerExtension.executeAllTest(serviceWithTestError, pureModelWithTestError, modelDataWithTestError);

        Assert.assertEquals(1, serviceStoreTestResultsWithTestError.size());
        Assert.assertTrue(serviceStoreTestResultsWithTestError.get(0) instanceof TestError);
        Assert.assertTrue(((TestError) serviceStoreTestResultsWithTestError.get(0)).error.contains(
                "                                               Request was not matched\n" +
                        "                                               =======================\n" +
                        "\n" +
                        "-----------------------------------------------------------------------------------------------------------------------\n" +
                        "| Closest stub                                             | Request                                                  |\n" +
                        "-----------------------------------------------------------------------------------------------------------------------\n" +
                        "                                                           |\n" +
                        "GET                                                        | GET\n" +
                        "[path] /employees/employeesWithTestError                   | /employees/employeesWithParameters?stringParam=dummy&inte<<<<< URL does not match\n" +
                        "                                                           | gerParam=1&floatParam=1.123&booleanParam=false\n" +
                        "                                                           |\n" +
                        "Query: booleanParam = false                                | booleanParam: false\n" +
                        "Query: stringParam = dummy                                 | stringParam: dummy\n" +
                        "Query: floatParam = 1.123                                  | floatParam: 1.123\n" +
                        "                                                           |\n" +
                        "                                                           |\n" +
                        "-----------------------------------------------------------------------------------------------------------------------\n"));
        Assert.assertEquals("testServiceStoreTestSuites::TestService", serviceStoreTestResultsWithTestError.get(0).testable);
        Assert.assertEquals("testSuite1", serviceStoreTestResultsWithTestError.get(0).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", serviceStoreTestResultsWithTestError.get(0).atomicTestId.atomicTestId);
    }

    @Test
    public void testServiceTestSuiteWithModelStore()
    {
        ServiceTestableRunnerExtension serviceTestableRunnerExtension = new ServiceTestableRunnerExtension();

        String grammar = "###Service\n" +
                "Service testModelStoreTestSuites::service::DocM2MService\n" +
                "{\n" +
                "  pattern: '/testModelStoreTestSuites/service';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'dummy',\n" +
                "    'dummy1'\n" +
                "  ];\n" +
                "  documentation: 'Service to test refiner flow';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |testModelStoreTestSuites::model::Doc.all()->graphFetchChecked(#{testModelStoreTestSuites::model::Doc{firm_tbl{addressId,firmId,legalName,ceoId},person_tbl{addressId,age,firmId,firstName,id,lastName}}}#)->serialize(#{testModelStoreTestSuites::model::Doc{firm_tbl{addressId,firmId,legalName,ceoId},person_tbl{addressId,age,firmId,firstName,id,lastName}}}#);\n" +
                "    mapping: testModelStoreTestSuites::mapping::DocM2MMapping;\n" +
                "    runtime: testModelStoreTestSuites::runtime::DocM2MRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection_1:\n" +
                "            Reference \n" +
                "            #{ \n" +
                "              testServiceStoreTestSuites::TestData \n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          serializationFormat: PURE;\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\"defects\":[],\"source\":{\"defects\":[],\"source\":{\"number\":1,\"record\":\"{\\\\\"sFirm_tbl\\\\\":{\\\\\"legalName\\\\\":\\\\\"legalName 18\\\\\",\\\\\"firmId\\\\\":22,\\\\\"ceoId\\\\\":49,\\\\\"addressId\\\\\":88,\\\\\"employees\\\\\":{\\\\\"firstName\\\\\":\\\\\"firstName 69\\\\\",\\\\\"lastName\\\\\":\\\\\"lastName 2\\\\\",\\\\\"age\\\\\":14,\\\\\"id\\\\\":52,\\\\\"addressId\\\\\":83,\\\\\"firmId\\\\\":73}},\\\\\"sPerson_tbl\\\\\":{\\\\\"firstName\\\\\":\\\\\"firstName 69\\\\\",\\\\\"lastName\\\\\":\\\\\"lastName 4\\\\\",\\\\\"age\\\\\":98,\\\\\"id\\\\\":87,\\\\\"addressId\\\\\":46,\\\\\"firmId\\\\\":26}}\"},\"value\":{\"sFirm_tbl\":{\"addressId\":88,\"firmId\":22,\"legalName\":\"legalName 18\",\"ceoId\":49},\"sPerson_tbl\":{\"addressId\":46,\"age\":98,\"firmId\":26,\"firstName\":\"firstName 69\",\"id\":87,\"lastName\":\"lastName 4\"}}},\"value\":{\"firm_tbl\":{\"addressId\":88,\"firmId\":22,\"legalName\":\"legalName 18\",\"ceoId\":49},\"person_tbl\":{\"addressId\":46,\"age\":98,\"firmId\":26,\"firstName\":\"firstName 69\",\"id\":87,\"lastName\":\"lastName 4\"}}}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Data\n" +
                "Data testServiceStoreTestSuites::TestData\n" +
                "{\n" +
                "   ExternalFormat\n" +
                "   #{\n" +
                "       contentType: 'application/json';\n" +
                "       data: '{\\n  \"sFirm_tbl\": {\\n    \"legalName\": \"legalName 18\",\\n    \"firmId\": 22,\\n    \"ceoId\": 49,\\n    \"addressId\": 88,\\n    \"employees\": {\\n      \"firstName\": \"firstName 69\",\\n      \"lastName\": \"lastName 2\",\\n      \"age\": 14,\\n      \"id\": 52,\\n      \"addressId\": 83,\\n      \"firmId\": 73\\n    }\\n  },\\n  \"sPerson_tbl\": {\\n    \"firstName\": \"firstName 69\",\\n    \"lastName\": \"lastName 4\",\\n    \"age\": 98,\\n    \"id\": 87,\\n    \"addressId\": 46,\\n    \"firmId\": 26\\n  }\\n}';\n" +
                "   }#\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "Class testModelStoreTestSuites::model::Doc\n" +
                "{\n" +
                "  firm_tbl: testModelStoreTestSuites::model::Firm_TBL[1];\n" +
                "  person_tbl: testModelStoreTestSuites::model::Person_TBL[1];\n" +
                "}\n" +
                "\n" +
                "Class testModelStoreTestSuites::model::Firm_TBL\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  <<equality.Key>> firmId: Integer[1];\n" +
                "  ceoId: Integer[1];\n" +
                "  addressId: Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class testModelStoreTestSuites::model::Person_TBL\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "  age: Integer[1];\n" +
                "  <<equality.Key>> id: Integer[1];\n" +
                "  addressId: Integer[1];\n" +
                "  firmId: Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class testModelStoreTestSuites::model::sDoc\n" +
                "{\n" +
                "  sFirm_tbl: testModelStoreTestSuites::model::sFirm_TBL[1];\n" +
                "  sPerson_tbl: testModelStoreTestSuites::model::sPerson_TBL[1];\n" +
                "}\n" +
                "\n" +
                "Class testModelStoreTestSuites::model::sFirm_TBL\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  firmId: Integer[1];\n" +
                "  ceoId: Integer[1];\n" +
                "  addressId: Integer[1];\n" +
                "  employees: testModelStoreTestSuites::model::sPerson_TBL[1];\n" +
                "}\n" +
                "\n" +
                "Class testModelStoreTestSuites::model::sPerson_TBL\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "  age: Integer[1];\n" +
                "  id: Integer[1];\n" +
                "  addressId: Integer[1];\n" +
                "  firmId: Integer[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping testModelStoreTestSuites::mapping::DocM2MMapping\n" +
                "(\n" +
                "  *testModelStoreTestSuites::model::Doc: Pure\n" +
                "  {\n" +
                "    ~src testModelStoreTestSuites::model::sDoc\n" +
                "    firm_tbl: $src.sFirm_tbl,\n" +
                "    person_tbl: $src.sPerson_tbl\n" +
                "  }\n" +
                "  *testModelStoreTestSuites::model::Firm_TBL: Pure\n" +
                "  {\n" +
                "    ~src testModelStoreTestSuites::model::sFirm_TBL\n" +
                "    legalName: $src.legalName,\n" +
                "    firmId: $src.firmId,\n" +
                "    ceoId: $src.ceoId,\n" +
                "    addressId: $src.addressId\n" +
                "  }\n" +
                "  *testModelStoreTestSuites::model::Person_TBL: Pure\n" +
                "  {\n" +
                "    ~src testModelStoreTestSuites::model::sPerson_TBL\n" +
                "    firstName: $src.firstName,\n" +
                "    lastName: $src.lastName,\n" +
                "    age: $src.age,\n" +
                "    id: $src.id,\n" +
                "    addressId: $src.addressId,\n" +
                "    firmId: $src.firmId\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime testModelStoreTestSuites::runtime::DocM2MRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    testModelStoreTestSuites::mapping::DocM2MMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_1:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: testModelStoreTestSuites::model::sDoc;\n" +
                "          url: 'executor:default';\n" +
                "        }\n" +
                "      }#\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n";

        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(grammar);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithReferenceData = (Root_meta_legend_service_metamodel_Service) pureModelWithReferenceData.getPackageableElement("testModelStoreTestSuites::service::DocM2MService");
        List<TestResult> serviceStoreTestResults = serviceTestableRunnerExtension.executeAllTest(serviceWithReferenceData, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(1, serviceStoreTestResults.size());
        Assert.assertTrue(serviceStoreTestResults.get(0) instanceof TestPassed);
        Assert.assertEquals("testModelStoreTestSuites::service::DocM2MService", ((TestPassed) serviceStoreTestResults.get(0)).testable);
        Assert.assertEquals("testSuite1", ((TestPassed) serviceStoreTestResults.get(0)).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", ((TestPassed) serviceStoreTestResults.get(0)).atomicTestId.atomicTestId);
    }

    @Test
    public void testServiceTestSuiteWithModelChainConnection()
    {
        ServiceTestableRunnerExtension serviceTestableRunnerExtension = new ServiceTestableRunnerExtension();

        String grammar = "###Service\n" +
                "Service testModelChainConnectionTestSuite::testService\n" +
                "{\n" +
                "  pattern: '/testService';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'dummy',\n" +
                "    'dummy1'\n" +
                "  ];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |testModelChainConnectionTestSuite::TFirm.all()->graphFetch(#{testModelChainConnectionTestSuite::TFirm{legalName}}#)->serialize(#{testModelChainConnectionTestSuite::TFirm{legalName}}#);\n" +
                "    mapping: testModelChainConnectionTestSuite::M2MMapping;\n" +
                "    runtime: testModelChainConnectionTestSuite::testRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection_1:\n" +
                "            Reference \n" +
                "            #{ \n" +
                "              testServiceStoreTestSuites::TestData \n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          serializationFormat: PURE;\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{  \"legalName\" : \"Test Firm_source_target\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Data\n" +
                "Data testServiceStoreTestSuites::TestData\n" +
                "{\n" +
                "   ExternalFormat\n" +
                "   #{\n" +
                "       contentType: 'application/json';\n" +
                "       data: '{\"legalName\":\"Test Firm\"}';\n" +
                "   }#\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "Class testModelChainConnectionTestSuite::TFirm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class testModelChainConnectionTestSuite::Firm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class testModelChainConnectionTestSuite::SFirm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping testModelChainConnectionTestSuite::M2MMapping\n" +
                "(\n" +
                "  *testModelChainConnectionTestSuite::TFirm: Pure\n" +
                "  {\n" +
                "    ~src testModelChainConnectionTestSuite::Firm\n" +
                "    legalName: $src.legalName + '_target'\n" +
                "  }\n" +
                "  *testModelChainConnectionTestSuite::Firm: Pure\n" +
                "  {\n" +
                "    ~src testModelChainConnectionTestSuite::SFirm\n" +
                "    legalName: $src.legalName + '_source'\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection testModelChainConnectionTestSuite::SFirmConnection\n" +
                "{\n" +
                "  class: testModelChainConnectionTestSuite::SFirm;\n" +
                "  url: 'executor:default';\n" +
                "}\n" +
                "\n" +
                "ModelChainConnection testModelChainConnectionTestSuite::ModelChainConnection\n" +
                "{\n" +
                "  mappings: [\n" +
                "    testModelChainConnectionTestSuite::M2MMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime testModelChainConnectionTestSuite::testRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    testModelChainConnectionTestSuite::M2MMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_1: testModelChainConnectionTestSuite::SFirmConnection,\n" +
                "      connection_2: testModelChainConnectionTestSuite::ModelChainConnection\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n";

        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(grammar);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithReferenceData = (Root_meta_legend_service_metamodel_Service) pureModelWithReferenceData.getPackageableElement("testModelChainConnectionTestSuite::testService");
        List<TestResult> serviceStoreTestResults = serviceTestableRunnerExtension.executeAllTest(serviceWithReferenceData, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(1, serviceStoreTestResults.size());
        Assert.assertTrue(serviceStoreTestResults.get(0) instanceof TestPassed);
        Assert.assertEquals("testModelChainConnectionTestSuite::testService", ((TestPassed) serviceStoreTestResults.get(0)).testable);
        Assert.assertEquals("testSuite1", ((TestPassed) serviceStoreTestResults.get(0)).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", ((TestPassed) serviceStoreTestResults.get(0)).atomicTestId.atomicTestId);
    }

    @Test
    public void testServiceTestSuiteWithXStore()
    {
        ServiceTestableRunnerExtension serviceTestableRunnerExtension = new ServiceTestableRunnerExtension();

        String grammar = "###Service\n" +
                "Service testXStoreTestSuites::service::InMemoryCrossStoreService\n" +
                "{\n" +
                "  pattern: '/testXStoreTestSuites/InMemoryCrossStoreService';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'dummy',\n" +
                "    'dummy1'\n" +
                "  ];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |testXStoreTestSuites::models::Trade.all()->graphFetch(#{testXStoreTestSuites::models::Trade{tradeId,trader{kerberos,firstName,lastName},product{productId,productName,description,synonyms{name,type}}}}#)->serialize(#{testXStoreTestSuites::models::Trade{tradeId,trader{kerberos,firstName,lastName},product{productId,productName,description,synonyms{name,type}}}}#);\n" +
                "    mapping: testXStoreTestSuites::mapping::JsonCrossStoreMapping;\n" +
                "    runtime: testXStoreTestSuites::runtime::JsonCrossStoreRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection_1:\n" +
                "            Reference \n" +
                "            #{ \n" +
                "              testXStoreTestSuites::TradeTestData \n" +
                "            }#,\n" +
                "          connection_2:\n" +
                "            Reference \n" +
                "            #{ \n" +
                "              testXStoreTestSuites::ProductTestData \n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          serializationFormat: PURE;\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '[ {\\n  \"tradeId\" : 1,\\n  \"trader\" : {\\n    \"kerberos\" : \"abc\",\\n    \"firstName\" : \"F_Name_1\",\\n    \"lastName\" : \"L_Name_1\"\\n  },\\n  \"product\" : {\\n    \"productId\" : \"30\",\\n    \"productName\" : \"Product 30\",\\n    \"description\" : \"Product 30 description\",\\n    \"synonyms\" : [ {\\n      \"name\" : \"product 30 synonym 1\",\\n      \"type\" : \"ISIN\"\\n    }, {\\n      \"name\" : \"product 30 synonym 2\",\\n      \"type\" : \"CUSIP\"\\n    } ]\\n  }\\n}, {\\n  \"tradeId\" : 2,\\n  \"trader\" : {\\n    \"kerberos\" : \"abc\",\\n    \"firstName\" : \"F_Name_1\",\\n    \"lastName\" : \"L_Name_1\"\\n  },\\n  \"product\" : {\\n    \"productId\" : \"31\",\\n    \"productName\" : \"Product 31\",\\n    \"description\" : \"Product 31 description\",\\n    \"synonyms\" : [ {\\n      \"name\" : \"product 31 synonym 1\",\\n      \"type\" : \"ISIN\"\\n    }, {\\n      \"name\" : \"product 31 synonym 2\",\\n      \"type\" : \"CUSIP\"\\n    } ]\\n  }\\n}, {\\n  \"tradeId\" : 3,\\n  \"trader\" : {\\n    \"kerberos\" : \"xyz\",\\n    \"firstName\" : \"F_Name_2\",\\n    \"lastName\" : \"L_Name_2\"\\n  },\\n  \"product\" : {\\n    \"productId\" : \"30\",\\n    \"productName\" : \"Product 30\",\\n    \"description\" : \"Product 30 description\",\\n    \"synonyms\" : [ {\\n      \"name\" : \"product 30 synonym 1\",\\n      \"type\" : \"ISIN\"\\n    }, {\\n      \"name\" : \"product 30 synonym 2\",\\n      \"type\" : \"CUSIP\"\\n    } ]\\n  }\\n}, {\\n  \"tradeId\" : 4,\\n  \"trader\" : {\\n    \"kerberos\" : \"xyz\",\\n    \"firstName\" : \"F_Name_2\",\\n    \"lastName\" : \"L_Name_2\"\\n  },\\n  \"product\" : {\\n    \"productId\" : \"31\",\\n    \"productName\" : \"Product 31\",\\n    \"description\" : \"Product 31 description\",\\n    \"synonyms\" : [ {\\n      \"name\" : \"product 31 synonym 1\",\\n      \"type\" : \"ISIN\"\\n    }, {\\n      \"name\" : \"product 31 synonym 2\",\\n      \"type\" : \"CUSIP\"\\n    } ]\\n  }\\n} ]';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Data\n" +
                "Data testXStoreTestSuites::TradeTestData\n" +
                "{\n" +
                "   ExternalFormat\n" +
                "   #{\n" +
                "       contentType: 'application/json';\n" +
                "       data: '[{\"s_tradeId\": 1, \"s_tradeDetails\": \"30:100\", \"s_traderDetails\": \"abc:F_Name_1:L_Name_1\"},{\"s_tradeId\": 2, \"s_tradeDetails\": \"31:200\", \"s_traderDetails\": \"abc:F_Name_1:L_Name_1\"},{\"s_tradeId\": 3, \"s_tradeDetails\": \"30:300\", \"s_traderDetails\": \"xyz:F_Name_2:L_Name_2\"},{\"s_tradeId\": 4, \"s_tradeDetails\": \"31:400\", \"s_traderDetails\": \"xyz:F_Name_2:L_Name_2\"}]';\n" +
                "   }#\n" +
                "}\n" +
                "Data testXStoreTestSuites::ProductTestData\n" +
                "{\n" +
                "   ExternalFormat\n" +
                "   #{\n" +
                "       contentType: 'application/json';\n" +
                "       data: '[{\"s_productId\": \"30\", \"s_productName\": \"Product 30\", \"s_description\": \"Product 30 description\", \"s_synonyms\": [{\"s_name\":\"product 30 synonym 1\", \"s_type\":\"isin\"}, {\"s_name\":\"product 30 synonym 2\", \"s_type\":\"cusip\"}]},{\"s_productId\": \"31\", \"s_productName\": \"Product 31\", \"s_description\": \"Product 31 description\", \"s_synonyms\": [{\"s_name\":\"product 31 synonym 1\", \"s_type\":\"isin\"}, {\"s_name\":\"product 31 synonym 2\", \"s_type\":\"cusip\"}]}]';\n" +
                "   }#\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "Enum testXStoreTestSuites::models::SynonymType\n" +
                "{\n" +
                "  CUSIP,\n" +
                "  ISIN\n" +
                "}\n" +
                "\n" +
                "Class testXStoreTestSuites::models::S_Synonym\n" +
                "{\n" +
                "  s_name: String[1];\n" +
                "  s_type: String[1];\n" +
                "}\n" +
                "\n" +
                "Class testXStoreTestSuites::models::Trader\n" +
                "{\n" +
                "  kerberos: String[1];\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "Class testXStoreTestSuites::models::TradeEvent\n" +
                "{\n" +
                "  eventId: String[1];\n" +
                "  description: String[1];\n" +
                "  timestamp: DateTime[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "Class testXStoreTestSuites::models::S_Trade\n" +
                "{\n" +
                "  s_tradeId: Integer[1];\n" +
                "  s_traderDetails: String[1];\n" +
                "  s_tradeDetails: String[1];\n" +
                "}\n" +
                "\n" +
                "Class testXStoreTestSuites::models::Product\n" +
                "{\n" +
                "  productId: String[1];\n" +
                "  productName: String[1];\n" +
                "  description: String[1];\n" +
                "}\n" +
                "\n" +
                "Class testXStoreTestSuites::models::Synonym\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  type: testXStoreTestSuites::models::SynonymType[1];\n" +
                "}\n" +
                "\n" +
                "Class testXStoreTestSuites::models::S_Product\n" +
                "{\n" +
                "  s_productId: String[1];\n" +
                "  s_productName: String[1];\n" +
                "  s_description: String[1];\n" +
                "  s_synonyms: testXStoreTestSuites::models::S_Synonym[*];\n" +
                "}\n" +
                "\n" +
                "Class testXStoreTestSuites::models::Trade\n" +
                "{\n" +
                "  tradeId: Integer[1];\n" +
                "  traderKerb: String[0..1];\n" +
                "  quantity: Integer[1];\n" +
                "  productName() {$this.product.productName}: String[0..1];\n" +
                "  justProduct() {$this.product}: testXStoreTestSuites::models::Product[0..1];\n" +
                "}\n" +
                "\n" +
                "Association testXStoreTestSuites::models::Trade_TradeEvent\n" +
                "{\n" +
                "  tradeEvents: testXStoreTestSuites::models::TradeEvent[*];\n" +
                "  trade: testXStoreTestSuites::models::Trade[1];\n" +
                "}\n" +
                "\n" +
                "Association testXStoreTestSuites::models::Trade_Product\n" +
                "{\n" +
                "  product: testXStoreTestSuites::models::Product[1];\n" +
                "  trades: testXStoreTestSuites::models::Trade[*];\n" +
                "}\n" +
                "\n" +
                "Association testXStoreTestSuites::models::Trade_Trader\n" +
                "{\n" +
                "  trader: testXStoreTestSuites::models::Trader[1];\n" +
                "  trades: testXStoreTestSuites::models::Trade[*];\n" +
                "}\n" +
                "\n" +
                "Association testXStoreTestSuites::models::Product_Synonym\n" +
                "{\n" +
                "  product: testXStoreTestSuites::models::Product[1];\n" +
                "  synonyms: testXStoreTestSuites::models::Synonym[*];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping testXStoreTestSuites::mapping::JsonCrossStoreMapping\n" +
                "(\n" +
                "  testXStoreTestSuites::models::Trade[trade_set]: Pure\n" +
                "  {\n" +
                "    ~src testXStoreTestSuites::models::S_Trade\n" +
                "    tradeId: $src.s_tradeId,\n" +
                "    +prodId: String[1]: $src.s_tradeDetails->split(':')->at(0),\n" +
                "    quantity: $src.s_tradeDetails->split(':')->at(1)->parseInteger(),\n" +
                "    trader[trader_set]: $src\n" +
                "  }\n" +
                "  testXStoreTestSuites::models::Trader[trader_set]: Pure\n" +
                "  {\n" +
                "    ~src testXStoreTestSuites::models::S_Trade\n" +
                "    kerberos: $src.s_traderDetails->split(':')->at(0),\n" +
                "    firstName: $src.s_traderDetails->split(':')->at(1),\n" +
                "    lastName: $src.s_traderDetails->split(':')->at(2)\n" +
                "  }\n" +
                "  testXStoreTestSuites::models::Product[prod_set]: Pure\n" +
                "  {\n" +
                "    ~src testXStoreTestSuites::models::S_Product\n" +
                "    productId: $src.s_productId,\n" +
                "    productName: $src.s_productName,\n" +
                "    description: $src.s_description,\n" +
                "    synonyms[synonym_set]: $src.s_synonyms\n" +
                "  }\n" +
                "  testXStoreTestSuites::models::Synonym[synonym_set]: Pure\n" +
                "  {\n" +
                "    ~src testXStoreTestSuites::models::S_Synonym\n" +
                "    name: $src.s_name,\n" +
                "    type: EnumerationMapping SynonymTypeMapping: $src.s_type\n" +
                "  }\n" +
                "\n" +
                "  testXStoreTestSuites::models::Trade_Product: XStore\n" +
                "  {\n" +
                "    product[trade_set, prod_set]: $this.prodId == $that.productId,\n" +
                "    trades[prod_set, trade_set]: $this.productId == $that.prodId\n" +
                "  }\n" +
                "\n" +
                "  testXStoreTestSuites::models::SynonymType: EnumerationMapping SynonymTypeMapping\n" +
                "  {\n" +
                "    CUSIP: ['cusip', 'CUSIP'],\n" +
                "    ISIN: ['isin', 'ISIN']\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection testXStoreTestSuites::runtime::JsonProductConnection\n" +
                "{\n" +
                "  class: testXStoreTestSuites::models::S_Product;\n" +
                "  url: 'executor:default';\n" +
                "}\n" +
                "\n" +
                "JsonModelConnection testXStoreTestSuites::runtime::JsonTradeConnection\n" +
                "{\n" +
                "  class: testXStoreTestSuites::models::S_Trade;\n" +
                "  url: 'executor:default';\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime testXStoreTestSuites::runtime::JsonCrossStoreRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    testXStoreTestSuites::mapping::JsonCrossStoreMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_1: testXStoreTestSuites::runtime::JsonTradeConnection,\n" +
                "      connection_2: testXStoreTestSuites::runtime::JsonProductConnection\n" +
                "    ]\n" +
                "  ];\n" +
                "}";

        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(grammar);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithReferenceData = (Root_meta_legend_service_metamodel_Service) pureModelWithReferenceData.getPackageableElement("testXStoreTestSuites::service::InMemoryCrossStoreService");
        List<TestResult> serviceStoreTestResults = serviceTestableRunnerExtension.executeAllTest(serviceWithReferenceData, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(1, serviceStoreTestResults.size());
        Assert.assertTrue(serviceStoreTestResults.get(0) instanceof TestPassed);
        Assert.assertEquals("testXStoreTestSuites::service::InMemoryCrossStoreService", ((TestPassed) serviceStoreTestResults.get(0)).testable);
        Assert.assertEquals("testSuite1", ((TestPassed) serviceStoreTestResults.get(0)).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", ((TestPassed) serviceStoreTestResults.get(0)).atomicTestId.atomicTestId);
    }

    @Test
    public void testMultiExecutionServiceTestSuite()
    {
        ServiceTestableRunnerExtension serviceTestableRunnerExtension = new ServiceTestableRunnerExtension();

        String grammar = "###Data\n" +
                "Data testServiceStoreTestSuites::TestData\n" +
                "{\n" +
                "   ExternalFormat\n" +
                "   #{\n" +
                "       contentType: 'application/json';\n" +
                "       data: '{\\n  \"sFirm_tbl\": {\\n    \"legalName\": \"legalName 18\",\\n    \"firmId\": 22,\\n    \"ceoId\": 49,\\n    \"addressId\": 88,\\n    \"employees\": {\\n      \"firstName\": \"firstName 69\",\\n      \"lastName\": \"lastName 2\",\\n      \"age\": 14,\\n      \"id\": 52,\\n      \"addressId\": 83,\\n      \"firmId\": 73\\n    }\\n  },\\n  \"sPerson_tbl\": {\\n    \"firstName\": \"firstName 69\",\\n    \"lastName\": \"lastName 4\",\\n    \"age\": 98,\\n    \"id\": 87,\\n    \"addressId\": 46,\\n    \"firmId\": 26\\n  }\\n}';\n" +
                "   }#\n" +
                "}\n" +
                "Data testServiceStoreTestSuites::TestData2\n" +
                "{\n" +
                "   ExternalFormat\n" +
                "   #{\n" +
                "       contentType: 'application/json';\n" +
                "       data: '{\\n  \"sFirm_tbl\": {\\n    \"legalName\": \"legalName 18\",\\n    \"firmId\": 22,\\n    \"ceoId\": 49,\\n    \"addressId\": 88,\\n    \"employees\": {\\n      \"firstName\": \"firstName 69\",\\n      \"lastName\": \"lastName 2\",\\n      \"age\": 14,\\n      \"id\": 52,\\n      \"addressId\": 83,\\n      \"firmId\": 73\\n    }\\n  },\\n  \"sPerson_tbl\": {\\n    \"firstName\": \"firstName 69\",\\n    \"lastName\": \"lastName 4\",\\n    \"age\": 98,\\n    \"id\": 87,\\n    \"addressId\": 46,\\n    \"firmId\": 26\\n  }\\n}';\n" +
                "   }#\n" +
                "}\n" +
                "Data testServiceStoreTestSuites::TestData3\n" +
                "{\n" +
                "   ExternalFormat\n" +
                "   #{\n" +
                "       contentType: 'application/json';\n" +
                "       data: '{\\n  \"sFirm_tbl\": {\\n    \"legalName\": \"legalName 19\",\\n    \"firmId\": 22,\\n    \"ceoId\": 49,\\n    \"addressId\": 88,\\n    \"employees\": {\\n      \"firstName\": \"firstName 69\",\\n      \"lastName\": \"lastName 2\",\\n      \"age\": 14,\\n      \"id\": 52,\\n      \"addressId\": 83,\\n      \"firmId\": 73\\n    }\\n  },\\n  \"sPerson_tbl\": {\\n    \"firstName\": \"firstName 69\",\\n    \"lastName\": \"lastName 4\",\\n    \"age\": 98,\\n    \"id\": 87,\\n    \"addressId\": 46,\\n    \"firmId\": 26\\n  }\\n}';\n" +
                "   }#\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "Class testModelStoreTestSuites::model::Doc\n" +
                "{\n" +
                "  firm_tbl: testModelStoreTestSuites::model::Firm_TBL[1];\n" +
                "  person_tbl: testModelStoreTestSuites::model::Person_TBL[1];\n" +
                "}\n" +
                "\n" +
                "Class testModelStoreTestSuites::model::Firm_TBL\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  <<equality.Key>> firmId: Integer[1];\n" +
                "  ceoId: Integer[1];\n" +
                "  addressId: Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class testModelStoreTestSuites::model::Person_TBL\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "  age: Integer[1];\n" +
                "  <<equality.Key>> id: Integer[1];\n" +
                "  addressId: Integer[1];\n" +
                "  firmId: Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class testModelStoreTestSuites::model::sDoc\n" +
                "{\n" +
                "  sFirm_tbl: testModelStoreTestSuites::model::sFirm_TBL[1];\n" +
                "  sPerson_tbl: testModelStoreTestSuites::model::sPerson_TBL[1];\n" +
                "}\n" +
                "\n" +
                "Class testModelStoreTestSuites::model::sFirm_TBL\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  firmId: Integer[1];\n" +
                "  ceoId: Integer[1];\n" +
                "  addressId: Integer[1];\n" +
                "  employees: testModelStoreTestSuites::model::sPerson_TBL[1];\n" +
                "}\n" +
                "\n" +
                "Class testModelStoreTestSuites::model::sPerson_TBL\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "  age: Integer[1];\n" +
                "  id: Integer[1];\n" +
                "  addressId: Integer[1];\n" +
                "  firmId: Integer[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping testModelStoreTestSuites::mapping::DocM2MMapping\n" +
                "(\n" +
                "  *testModelStoreTestSuites::model::Doc: Pure\n" +
                "  {\n" +
                "    ~src testModelStoreTestSuites::model::sDoc\n" +
                "    firm_tbl: $src.sFirm_tbl,\n" +
                "    person_tbl: $src.sPerson_tbl\n" +
                "  }\n" +
                "  *testModelStoreTestSuites::model::Firm_TBL: Pure\n" +
                "  {\n" +
                "    ~src testModelStoreTestSuites::model::sFirm_TBL\n" +
                "    legalName: $src.legalName,\n" +
                "    firmId: $src.firmId,\n" +
                "    ceoId: $src.ceoId,\n" +
                "    addressId: $src.addressId\n" +
                "  }\n" +
                "  *testModelStoreTestSuites::model::Person_TBL: Pure\n" +
                "  {\n" +
                "    ~src testModelStoreTestSuites::model::sPerson_TBL\n" +
                "    firstName: $src.firstName,\n" +
                "    lastName: $src.lastName,\n" +
                "    age: $src.age,\n" +
                "    id: $src.id,\n" +
                "    addressId: $src.addressId,\n" +
                "    firmId: $src.firmId\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime testModelStoreTestSuites::runtime::DocM2MRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    testModelStoreTestSuites::mapping::DocM2MMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_1:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: testModelStoreTestSuites::model::sDoc;\n" +
                "          url: 'executor:default';\n" +
                "        }\n" +
                "      }#\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Runtime testModelStoreTestSuites::runtime::DocM2MRuntime2\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    testModelStoreTestSuites::mapping::DocM2MMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_1:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: testModelStoreTestSuites::model::sDoc;\n" +
                "          url: 'executor:default';\n" +
                "        }\n" +
                "      }#\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Runtime testModelStoreTestSuites::runtime::DocM2MRuntime3\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    testModelStoreTestSuites::mapping::DocM2MMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_2:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: testModelStoreTestSuites::model::sDoc;\n" +
                "          url: 'executor:default';\n" +
                "        }\n" +
                "      }#\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n";

        // Service With All Env Test Passing        
        String serviceGrammarWithAllTestPassing = "###Service\n" +
                "Service testModelStoreTestSuites::service::DocM2MService\n" +
                "{\n" +
                "  pattern: '/testModelStoreTestSuites/service';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'dummy',\n" +
                "    'dummy1'\n" +
                "  ];\n" +
                "  documentation: 'Service to test refiner flow';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: |testModelStoreTestSuites::model::Doc.all()->graphFetchChecked(#{testModelStoreTestSuites::model::Doc{firm_tbl{addressId,firmId,legalName,ceoId},person_tbl{addressId,age,firmId,firstName,id,lastName}}}#)->serialize(#{testModelStoreTestSuites::model::Doc{firm_tbl{addressId,firmId,legalName,ceoId},person_tbl{addressId,age,firmId,firstName,id,lastName}}}#);\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: testModelStoreTestSuites::mapping::DocM2MMapping;\n" +
                "      runtime: testModelStoreTestSuites::runtime::DocM2MRuntime;\n" +
                "    }\n" +
                "    executions['UAT']:\n" +
                "    {\n" +
                "      mapping: testModelStoreTestSuites::mapping::DocM2MMapping;\n" +
                "      runtime: testModelStoreTestSuites::runtime::DocM2MRuntime2;\n" +
                "    }\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection_1:\n" +
                "            Reference \n" +
                "            #{ \n" +
                "              testServiceStoreTestSuites::TestData \n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          serializationFormat: PURE;\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\"defects\":[],\"source\":{\"defects\":[],\"source\":{\"number\":1,\"record\":\"{\\\\\"sFirm_tbl\\\\\":{\\\\\"legalName\\\\\":\\\\\"legalName 18\\\\\",\\\\\"firmId\\\\\":22,\\\\\"ceoId\\\\\":49,\\\\\"addressId\\\\\":88,\\\\\"employees\\\\\":{\\\\\"firstName\\\\\":\\\\\"firstName 69\\\\\",\\\\\"lastName\\\\\":\\\\\"lastName 2\\\\\",\\\\\"age\\\\\":14,\\\\\"id\\\\\":52,\\\\\"addressId\\\\\":83,\\\\\"firmId\\\\\":73}},\\\\\"sPerson_tbl\\\\\":{\\\\\"firstName\\\\\":\\\\\"firstName 69\\\\\",\\\\\"lastName\\\\\":\\\\\"lastName 4\\\\\",\\\\\"age\\\\\":98,\\\\\"id\\\\\":87,\\\\\"addressId\\\\\":46,\\\\\"firmId\\\\\":26}}\"},\"value\":{\"sFirm_tbl\":{\"addressId\":88,\"firmId\":22,\"legalName\":\"legalName 18\",\"ceoId\":49},\"sPerson_tbl\":{\"addressId\":46,\"age\":98,\"firmId\":26,\"firstName\":\"firstName 69\",\"id\":87,\"lastName\":\"lastName 4\"}}},\"value\":{\"firm_tbl\":{\"addressId\":88,\"firmId\":22,\"legalName\":\"legalName 18\",\"ceoId\":49},\"person_tbl\":{\"addressId\":46,\"age\":98,\"firmId\":26,\"firstName\":\"firstName 69\",\"id\":87,\"lastName\":\"lastName 4\"}}}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "\n" +
                "\n";

        PureModelContextData modelDataWithAllTestPassing = PureGrammarParser.newInstance().parseModel(serviceGrammarWithAllTestPassing + grammar);
        PureModel pureModelWithAllTestPassing = Compiler.compile(modelDataWithAllTestPassing, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithAllTestPassing = (Root_meta_legend_service_metamodel_Service) pureModelWithAllTestPassing.getPackageableElement("testModelStoreTestSuites::service::DocM2MService");
        List<TestResult> serviceStoreTestResults = serviceTestableRunnerExtension.executeAllTest(serviceWithAllTestPassing, pureModelWithAllTestPassing, modelDataWithAllTestPassing);

        Assert.assertEquals(1, serviceStoreTestResults.size());
        Assert.assertTrue(serviceStoreTestResults.get(0) instanceof MultiExecutionServiceTestResult);
        Assert.assertEquals("testModelStoreTestSuites::service::DocM2MService", serviceStoreTestResults.get(0).testable);
        Assert.assertEquals("testSuite1", serviceStoreTestResults.get(0).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", serviceStoreTestResults.get(0).atomicTestId.atomicTestId);

        TestResult uatTestResult = ((MultiExecutionServiceTestResult) serviceStoreTestResults.get(0)).getKeyIndexedTestResults().get("UAT");
        Assert.assertTrue(uatTestResult instanceof TestPassed);
        Assert.assertEquals("testModelStoreTestSuites::service::DocM2MService", uatTestResult.testable);
        Assert.assertEquals("testSuite1", uatTestResult.atomicTestId.testSuiteId);
        Assert.assertEquals("test1", uatTestResult.atomicTestId.atomicTestId);

        TestResult qaTestResult = ((MultiExecutionServiceTestResult) serviceStoreTestResults.get(0)).getKeyIndexedTestResults().get("QA");
        Assert.assertTrue(qaTestResult instanceof TestPassed);
        Assert.assertEquals("testModelStoreTestSuites::service::DocM2MService", qaTestResult.testable);
        Assert.assertEquals("testSuite1", qaTestResult.atomicTestId.testSuiteId);
        Assert.assertEquals("test1", qaTestResult.atomicTestId.atomicTestId);

        // Multi execution Service with different connections 
        String serviceGrammarWithDifferentConnections = "###Service\n" +
                "Service testModelStoreTestSuites::service::DocM2MService\n" +
                "{\n" +
                "  pattern: '/testModelStoreTestSuites/service';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'dummy',\n" +
                "    'dummy1'\n" +
                "  ];\n" +
                "  documentation: 'Service to test refiner flow';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: |testModelStoreTestSuites::model::Doc.all()->graphFetchChecked(#{testModelStoreTestSuites::model::Doc{firm_tbl{addressId,firmId,legalName,ceoId},person_tbl{addressId,age,firmId,firstName,id,lastName}}}#)->serialize(#{testModelStoreTestSuites::model::Doc{firm_tbl{addressId,firmId,legalName,ceoId},person_tbl{addressId,age,firmId,firstName,id,lastName}}}#);\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: testModelStoreTestSuites::mapping::DocM2MMapping;\n" +
                "      runtime: testModelStoreTestSuites::runtime::DocM2MRuntime;\n" +
                "    }\n" +
                "    executions['UAT']:\n" +
                "    {\n" +
                "      mapping: testModelStoreTestSuites::mapping::DocM2MMapping;\n" +
                "      runtime: testModelStoreTestSuites::runtime::DocM2MRuntime3;\n" +
                "    }\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection_1:\n" +
                "            Reference \n" +
                "            #{ \n" +
                "              testServiceStoreTestSuites::TestData \n" +
                "            }#,\n" +
                "          connection_2:\n" +
                "            Reference \n" +
                "            #{ \n" +
                "              testServiceStoreTestSuites::TestData2 \n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          serializationFormat: PURE;\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\"defects\":[],\"source\":{\"defects\":[],\"source\":{\"number\":1,\"record\":\"{\\\\\"sFirm_tbl\\\\\":{\\\\\"legalName\\\\\":\\\\\"legalName 18\\\\\",\\\\\"firmId\\\\\":22,\\\\\"ceoId\\\\\":49,\\\\\"addressId\\\\\":88,\\\\\"employees\\\\\":{\\\\\"firstName\\\\\":\\\\\"firstName 69\\\\\",\\\\\"lastName\\\\\":\\\\\"lastName 2\\\\\",\\\\\"age\\\\\":14,\\\\\"id\\\\\":52,\\\\\"addressId\\\\\":83,\\\\\"firmId\\\\\":73}},\\\\\"sPerson_tbl\\\\\":{\\\\\"firstName\\\\\":\\\\\"firstName 69\\\\\",\\\\\"lastName\\\\\":\\\\\"lastName 4\\\\\",\\\\\"age\\\\\":98,\\\\\"id\\\\\":87,\\\\\"addressId\\\\\":46,\\\\\"firmId\\\\\":26}}\"},\"value\":{\"sFirm_tbl\":{\"addressId\":88,\"firmId\":22,\"legalName\":\"legalName 18\",\"ceoId\":49},\"sPerson_tbl\":{\"addressId\":46,\"age\":98,\"firmId\":26,\"firstName\":\"firstName 69\",\"id\":87,\"lastName\":\"lastName 4\"}}},\"value\":{\"firm_tbl\":{\"addressId\":88,\"firmId\":22,\"legalName\":\"legalName 18\",\"ceoId\":49},\"person_tbl\":{\"addressId\":46,\"age\":98,\"firmId\":26,\"firstName\":\"firstName 69\",\"id\":87,\"lastName\":\"lastName 4\"}}}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "\n" +
                "\n";

        PureModelContextData modelDataWithDifferentConnections = PureGrammarParser.newInstance().parseModel(serviceGrammarWithDifferentConnections + grammar);
        PureModel pureModelWithDifferentConnections = Compiler.compile(modelDataWithDifferentConnections, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithDifferentConnections = (Root_meta_legend_service_metamodel_Service) pureModelWithDifferentConnections.getPackageableElement("testModelStoreTestSuites::service::DocM2MService");
        List<TestResult> serviceStoreTestResultsWithDifferentConnections = serviceTestableRunnerExtension.executeAllTest(serviceWithDifferentConnections, pureModelWithDifferentConnections, modelDataWithDifferentConnections);

        Assert.assertEquals(1, serviceStoreTestResultsWithDifferentConnections.size());
        Assert.assertTrue(serviceStoreTestResultsWithDifferentConnections.get(0) instanceof MultiExecutionServiceTestResult);
        Assert.assertEquals("testModelStoreTestSuites::service::DocM2MService", serviceStoreTestResultsWithDifferentConnections.get(0).testable);
        Assert.assertEquals("testSuite1", serviceStoreTestResultsWithDifferentConnections.get(0).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", serviceStoreTestResultsWithDifferentConnections.get(0).atomicTestId.atomicTestId);

        TestResult uatTestResultWithDifferentConnections = ((MultiExecutionServiceTestResult) serviceStoreTestResultsWithDifferentConnections.get(0)).getKeyIndexedTestResults().get("UAT");
        Assert.assertTrue(uatTestResultWithDifferentConnections instanceof TestPassed);
        Assert.assertEquals("testModelStoreTestSuites::service::DocM2MService", uatTestResultWithDifferentConnections.testable);
        Assert.assertEquals("testSuite1", uatTestResultWithDifferentConnections.atomicTestId.testSuiteId);
        Assert.assertEquals("test1", uatTestResultWithDifferentConnections.atomicTestId.atomicTestId);

        TestResult qaTestResultWithDifferentConnections = ((MultiExecutionServiceTestResult) serviceStoreTestResultsWithDifferentConnections.get(0)).getKeyIndexedTestResults().get("QA");
        Assert.assertTrue(qaTestResultWithDifferentConnections instanceof TestPassed);
        Assert.assertEquals("testModelStoreTestSuites::service::DocM2MService", qaTestResultWithDifferentConnections.testable);
        Assert.assertEquals("testSuite1", qaTestResultWithDifferentConnections.atomicTestId.testSuiteId);
        Assert.assertEquals("test1", qaTestResultWithDifferentConnections.atomicTestId.atomicTestId);

        String serviceGrammarWithTestFailing = "###Service\n" +
                "Service testModelStoreTestSuites::service::DocM2MService\n" +
                "{\n" +
                "  pattern: '/testModelStoreTestSuites/service';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'dummy',\n" +
                "    'dummy1'\n" +
                "  ];\n" +
                "  documentation: 'Service to test refiner flow';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: |testModelStoreTestSuites::model::Doc.all()->graphFetchChecked(#{testModelStoreTestSuites::model::Doc{firm_tbl{addressId,firmId,legalName,ceoId},person_tbl{addressId,age,firmId,firstName,id,lastName}}}#)->serialize(#{testModelStoreTestSuites::model::Doc{firm_tbl{addressId,firmId,legalName,ceoId},person_tbl{addressId,age,firmId,firstName,id,lastName}}}#);\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: testModelStoreTestSuites::mapping::DocM2MMapping;\n" +
                "      runtime: testModelStoreTestSuites::runtime::DocM2MRuntime;\n" +
                "    }\n" +
                "    executions['UAT']:\n" +
                "    {\n" +
                "      mapping: testModelStoreTestSuites::mapping::DocM2MMapping;\n" +
                "      runtime: testModelStoreTestSuites::runtime::DocM2MRuntime3;\n" +
                "    }\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection_1:\n" +
                "            Reference \n" +
                "            #{ \n" +
                "              testServiceStoreTestSuites::TestData \n" +
                "            }#,\n" +
                "          connection_2:\n" +
                "            Reference \n" +
                "            #{ \n" +
                "              testServiceStoreTestSuites::TestData3 \n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          serializationFormat: PURE;\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\"defects\":[],\"source\":{\"defects\":[],\"source\":{\"number\":1,\"record\":\"{\\\\\"sFirm_tbl\\\\\":{\\\\\"legalName\\\\\":\\\\\"legalName 18\\\\\",\\\\\"firmId\\\\\":22,\\\\\"ceoId\\\\\":49,\\\\\"addressId\\\\\":88,\\\\\"employees\\\\\":{\\\\\"firstName\\\\\":\\\\\"firstName 69\\\\\",\\\\\"lastName\\\\\":\\\\\"lastName 2\\\\\",\\\\\"age\\\\\":14,\\\\\"id\\\\\":52,\\\\\"addressId\\\\\":83,\\\\\"firmId\\\\\":73}},\\\\\"sPerson_tbl\\\\\":{\\\\\"firstName\\\\\":\\\\\"firstName 69\\\\\",\\\\\"lastName\\\\\":\\\\\"lastName 4\\\\\",\\\\\"age\\\\\":98,\\\\\"id\\\\\":87,\\\\\"addressId\\\\\":46,\\\\\"firmId\\\\\":26}}\"},\"value\":{\"sFirm_tbl\":{\"addressId\":88,\"firmId\":22,\"legalName\":\"legalName 18\",\"ceoId\":49},\"sPerson_tbl\":{\"addressId\":46,\"age\":98,\"firmId\":26,\"firstName\":\"firstName 69\",\"id\":87,\"lastName\":\"lastName 4\"}}},\"value\":{\"firm_tbl\":{\"addressId\":88,\"firmId\":22,\"legalName\":\"legalName 18\",\"ceoId\":49},\"person_tbl\":{\"addressId\":46,\"age\":98,\"firmId\":26,\"firstName\":\"firstName 69\",\"id\":87,\"lastName\":\"lastName 4\"}}}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "\n" +
                "\n";

        PureModelContextData modelDataWithTestFailing = PureGrammarParser.newInstance().parseModel(serviceGrammarWithTestFailing + grammar);
        PureModel pureModelWithTestFailing = Compiler.compile(modelDataWithTestFailing, DeploymentMode.TEST, null);

        Root_meta_legend_service_metamodel_Service serviceWithTestFailing = (Root_meta_legend_service_metamodel_Service) pureModelWithTestFailing.getPackageableElement("testModelStoreTestSuites::service::DocM2MService");
        List<TestResult> serviceStoreTestResultsWithTestFailing = serviceTestableRunnerExtension.executeAllTest(serviceWithTestFailing, pureModelWithTestFailing, modelDataWithTestFailing);

        Assert.assertEquals(1, serviceStoreTestResultsWithTestFailing.size());
        Assert.assertTrue(serviceStoreTestResultsWithTestFailing.get(0) instanceof MultiExecutionServiceTestResult);
        Assert.assertEquals("testModelStoreTestSuites::service::DocM2MService", serviceStoreTestResultsWithTestFailing.get(0).testable);
        Assert.assertEquals("testSuite1", serviceStoreTestResultsWithTestFailing.get(0).atomicTestId.testSuiteId);
        Assert.assertEquals("test1", serviceStoreTestResultsWithDifferentConnections.get(0).atomicTestId.atomicTestId);

        TestResult qaTestResultWithTestFailing = ((MultiExecutionServiceTestResult) serviceStoreTestResultsWithTestFailing.get(0)).getKeyIndexedTestResults().get("QA");
        Assert.assertTrue(qaTestResultWithTestFailing instanceof TestPassed);
        Assert.assertEquals("testModelStoreTestSuites::service::DocM2MService", qaTestResultWithTestFailing.testable);
        Assert.assertEquals("testSuite1", qaTestResultWithTestFailing.atomicTestId.testSuiteId);
        Assert.assertEquals("test1", qaTestResultWithTestFailing.atomicTestId.atomicTestId);

        TestResult uatTestResultWithTestFailing = ((MultiExecutionServiceTestResult) serviceStoreTestResultsWithTestFailing.get(0)).getKeyIndexedTestResults().get("UAT");
        Assert.assertTrue(uatTestResultWithTestFailing instanceof TestFailed);
        Assert.assertEquals("testModelStoreTestSuites::service::DocM2MService", uatTestResultWithTestFailing.testable);
        Assert.assertEquals("testSuite1", uatTestResultWithTestFailing.atomicTestId.testSuiteId);
        Assert.assertEquals("test1", uatTestResultWithTestFailing.atomicTestId.atomicTestId);
        Assert.assertEquals(1, ((TestFailed) uatTestResultWithTestFailing).assertStatuses.size());
        Assert.assertTrue(((TestFailed) uatTestResultWithTestFailing).assertStatuses.get(0) instanceof EqualToJsonAssertFail);
        Assert.assertEquals("assert1", ((EqualToJsonAssertFail) ((TestFailed) uatTestResultWithTestFailing).assertStatuses.get(0)).id);
        Assert.assertEquals("Actual result does not match Expected result", ((EqualToJsonAssertFail) ((TestFailed) uatTestResultWithTestFailing).assertStatuses.get(0)).message);
        Assert.assertEquals("{\n" +
                "  \"defects\" : [ ],\n" +
                "  \"source\" : {\n" +
                "    \"defects\" : [ ],\n" +
                "    \"source\" : {\n" +
                "      \"number\" : 1,\n" +
                "      \"record\" : \"{\\\"sFirm_tbl\\\":{\\\"legalName\\\":\\\"legalName 19\\\",\\\"firmId\\\":22,\\\"ceoId\\\":49,\\\"addressId\\\":88,\\\"employees\\\":{\\\"firstName\\\":\\\"firstName 69\\\",\\\"lastName\\\":\\\"lastName 2\\\",\\\"age\\\":14,\\\"id\\\":52,\\\"addressId\\\":83,\\\"firmId\\\":73}},\\\"sPerson_tbl\\\":{\\\"firstName\\\":\\\"firstName 69\\\",\\\"lastName\\\":\\\"lastName 4\\\",\\\"age\\\":98,\\\"id\\\":87,\\\"addressId\\\":46,\\\"firmId\\\":26}}\"\n" +
                "    },\n" +
                "    \"value\" : {\n" +
                "      \"sFirm_tbl\" : {\n" +
                "        \"addressId\" : 88,\n" +
                "        \"ceoId\" : 49,\n" +
                "        \"firmId\" : 22,\n" +
                "        \"legalName\" : \"legalName 19\"\n" +
                "      },\n" +
                "      \"sPerson_tbl\" : {\n" +
                "        \"addressId\" : 46,\n" +
                "        \"age\" : 98,\n" +
                "        \"firmId\" : 26,\n" +
                "        \"firstName\" : \"firstName 69\",\n" +
                "        \"id\" : 87,\n" +
                "        \"lastName\" : \"lastName 4\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"value\" : {\n" +
                "    \"firm_tbl\" : {\n" +
                "      \"addressId\" : 88,\n" +
                "      \"firmId\" : 22,\n" +
                "      \"legalName\" : \"legalName 19\",\n" +
                "      \"ceoId\" : 49\n" +
                "    },\n" +
                "    \"person_tbl\" : {\n" +
                "      \"addressId\" : 46,\n" +
                "      \"age\" : 98,\n" +
                "      \"firmId\" : 26,\n" +
                "      \"firstName\" : \"firstName 69\",\n" +
                "      \"id\" : 87,\n" +
                "      \"lastName\" : \"lastName 4\"\n" +
                "    }\n" +
                "  }\n" +
                "}", ((EqualToJsonAssertFail) ((TestFailed) uatTestResultWithTestFailing).assertStatuses.get(0)).actual);
        Assert.assertEquals("{\n" +
                "  \"defects\" : [ ],\n" +
                "  \"source\" : {\n" +
                "    \"defects\" : [ ],\n" +
                "    \"source\" : {\n" +
                "      \"number\" : 1,\n" +
                "      \"record\" : \"{\\\"sFirm_tbl\\\":{\\\"legalName\\\":\\\"legalName 18\\\",\\\"firmId\\\":22,\\\"ceoId\\\":49,\\\"addressId\\\":88,\\\"employees\\\":{\\\"firstName\\\":\\\"firstName 69\\\",\\\"lastName\\\":\\\"lastName 2\\\",\\\"age\\\":14,\\\"id\\\":52,\\\"addressId\\\":83,\\\"firmId\\\":73}},\\\"sPerson_tbl\\\":{\\\"firstName\\\":\\\"firstName 69\\\",\\\"lastName\\\":\\\"lastName 4\\\",\\\"age\\\":98,\\\"id\\\":87,\\\"addressId\\\":46,\\\"firmId\\\":26}}\"\n" +
                "    },\n" +
                "    \"value\" : {\n" +
                "      \"sFirm_tbl\" : {\n" +
                "        \"addressId\" : 88,\n" +
                "        \"firmId\" : 22,\n" +
                "        \"legalName\" : \"legalName 18\",\n" +
                "        \"ceoId\" : 49\n" +
                "      },\n" +
                "      \"sPerson_tbl\" : {\n" +
                "        \"addressId\" : 46,\n" +
                "        \"age\" : 98,\n" +
                "        \"firmId\" : 26,\n" +
                "        \"firstName\" : \"firstName 69\",\n" +
                "        \"id\" : 87,\n" +
                "        \"lastName\" : \"lastName 4\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"value\" : {\n" +
                "    \"firm_tbl\" : {\n" +
                "      \"addressId\" : 88,\n" +
                "      \"firmId\" : 22,\n" +
                "      \"legalName\" : \"legalName 18\",\n" +
                "      \"ceoId\" : 49\n" +
                "    },\n" +
                "    \"person_tbl\" : {\n" +
                "      \"addressId\" : 46,\n" +
                "      \"age\" : 98,\n" +
                "      \"firmId\" : 26,\n" +
                "      \"firstName\" : \"firstName 69\",\n" +
                "      \"id\" : 87,\n" +
                "      \"lastName\" : \"lastName 4\"\n" +
                "    }\n" +
                "  }\n" +
                "}", ((EqualToJsonAssertFail) ((TestFailed) uatTestResultWithTestFailing).assertStatuses.get(0)).expected);
    }

    @Test
    public void testFailingRelationalServiceSuite()
    {
        // setup
        List<TestResult> relationalTestResult = executeServiceTest("legend-testable-relational-service-simple-fail.pure", "service::SimpleRelationalPassFailing");
        // Assertions
        Assert.assertEquals(relationalTestResult.size(), 1);
        TestResult testResult = relationalTestResult.get(0);
        Assert.assertEquals(testResult.testable, "service::SimpleRelationalPassFailing");
        Assert.assertTrue(testResult instanceof TestFailed);
        TestFailed failedResult = (TestFailed) testResult;
        AtomicTestId atomicTestId = failedResult.atomicTestId;
        Assert.assertEquals(atomicTestId.atomicTestId, "test1");
        Assert.assertEquals(atomicTestId.testSuiteId, "testSuite1");
        List<AssertionStatus> statuses = failedResult.assertStatuses;
        Assert.assertEquals(statuses.size(), 2);
        // pass assertion
        AssertionStatus status1 = statuses.stream().filter(t -> t.id.equals("shouldPass")).findFirst().get();
        Assert.assertEquals(status1.id, "shouldPass");
        Assert.assertTrue(status1 instanceof AssertPass);
        // fail assertion
        AssertionStatus failStatus = statuses.stream().filter(t -> t.id.equals("shouldFail")).findFirst().get();
        Assert.assertTrue(failStatus instanceof EqualToJsonAssertFail);
        EqualToJsonAssertFail jsonAssertFail = (EqualToJsonAssertFail) failStatus;
        Assert.assertEquals("Actual result does not match Expected result", jsonAssertFail.message);
        String expected_Expected =
                "[ {\n" +
                        "  \"Employees/First Name\" : \"JohnDIFF\",\n" +
                        "  \"Employees/Last Name\" : \"Doe\",\n" +
                        "  \"Legal Name\" : \"Finos\"\n" +
                        "}, {\n" +
                        "  \"Employees/First Name\" : \"Nicole\",\n" +
                        "  \"Employees/Last Name\" : \"Smith\",\n" +
                        "  \"Legal Name\" : \"Finos\"\n" +
                        "}, {\n" +
                        "  \"Employees/First Name\" : \"Time\",\n" +
                        "  \"Employees/Last Name\" : \"Smith\",\n" +
                        "  \"Legal Name\" : \"Apple\"\n" +
                        "} ]";
        String expected_Actual = "[ {\n" +
                "  \"Employees/First Name\" : \"John\",\n" +
                "  \"Employees/Last Name\" : \"Doe\",\n" +
                "  \"Legal Name\" : \"Finos\"\n" +
                "}, {\n" +
                "  \"Employees/First Name\" : \"Nicole\",\n" +
                "  \"Employees/Last Name\" : \"Smith\",\n" +
                "  \"Legal Name\" : \"Finos\"\n" +
                "}, {\n" +
                "  \"Employees/First Name\" : \"Time\",\n" +
                "  \"Employees/Last Name\" : \"Smith\",\n" +
                "  \"Legal Name\" : \"Apple\"\n" +
                "} ]";

        MatcherAssert.assertThat(expected_Expected, JsonMatchers.jsonEquals(jsonAssertFail.expected));
        MatcherAssert.assertThat(expected_Actual, JsonMatchers.jsonEquals(jsonAssertFail.actual));
    }

    @Test
    public void testPassingRelationalWithParams()
    {
        // setup
        List<TestResult> relationalTestResult = executeServiceTest( "legend-testable-relational-service-parameters.pure", "service::RelationalServiceWithParams");
        // Assertions
        Assert.assertEquals(relationalTestResult.size(), 1);
        TestResult testResult = relationalTestResult.get(0);
        Assert.assertEquals(testResult.testable, "service::RelationalServiceWithParams");
        Assert.assertTrue(testResult instanceof TestPassed);
        TestPassed passed = (TestPassed) testResult;
        AtomicTestId atomicTestId = passed.atomicTestId;
        Assert.assertEquals(atomicTestId.atomicTestId, "test1");
        Assert.assertEquals(atomicTestId.testSuiteId, "testSuite1");
    }

    @Test
    public void testPassingRelationalWithEnumParams()
    {
        // setup
        List<TestResult> relationalTestResult = executeServiceTest("legend-testable-relational-service-enum-parameters.pure", "service::RelationalServiceWithEnumParams");
        // Assertions
        Assert.assertEquals(relationalTestResult.size(), 1);
        TestResult testResult = relationalTestResult.get(0);
        Assert.assertEquals(testResult.testable, "service::RelationalServiceWithEnumParams");
        Assert.assertTrue(testResult instanceof TestPassed);
    }

    @Test
    public void testPassingRelationalWithSpecialEmbeddedData()
    {
        // setup
        List<TestResult> relationalTestResult = executeServiceTest("legend-testable-relational-service-embeddedData.pure", "service::SimpleRelationalPassWithSpecialEmbeddedData");
        // Assertions
        Assert.assertEquals(relationalTestResult.size(), 1);
        TestResult testResult = relationalTestResult.get(0);
        Assert.assertEquals(testResult.testable, "service::SimpleRelationalPassWithSpecialEmbeddedData");
        if (testResult instanceof TestFailed)
        {
            AssertionStatus status = ((TestFailed) testResult).assertStatuses.get(0);
            if (status instanceof EqualToJsonAssertFail)
            {
                EqualToJsonAssertFail failAssert = (EqualToJsonAssertFail) status;
                Assert.assertEquals(failAssert.expected, failAssert.actual);
            }
        }
        Assert.assertTrue(testResult instanceof TestPassed);
        TestPassed passed = (TestPassed) testResult;
        AtomicTestId atomicTestId = passed.atomicTestId;
        Assert.assertEquals(atomicTestId.atomicTestId, "test1");
        Assert.assertEquals(atomicTestId.testSuiteId, "testSuite1");
    }

    private List<TestResult> executeServiceTest(String service, String fullPath)
    {
        ServiceTestableRunnerExtension serviceTestableRunnerExtension = new ServiceTestableRunnerExtension();
        String pureModelString = getFullPureModelGrammar("testable/relational/", "legend-testable-relational-model.pure", service);
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureModelString);
        PureModel pureModel = Compiler.compile(pureModelContextData, DeploymentMode.TEST, null);
        Root_meta_legend_service_metamodel_Service serviceWithTest = (Root_meta_legend_service_metamodel_Service) pureModel.getPackageableElement(fullPath);
        return serviceTestableRunnerExtension.executeAllTest(serviceWithTest, pureModel, pureModelContextData);
    }

    private String getResourceAsString(String path)
    {
        try
        {
            URL infoURL = DeploymentStateAndVersions.class.getClassLoader().getResource(path);
            if (infoURL != null)
            {
                java.util.Scanner scanner = new java.util.Scanner(infoURL.openStream()).useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : null;
            }
            return null;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private String getFullPureModelGrammar(String basePath, String model, String service)
    {
        return getResourceAsString(basePath + model) + "\n\n" + getResourceAsString(basePath + service);
    }
}
