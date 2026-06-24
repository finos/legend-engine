// Copyright 2023 Goldman Sachs
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
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TestRelationalMappingRunner
{

    String model1 = "###Data\n" +
            "Data data::RelationalData\n" +
            "{\n" +
            "  Relational\n" +
            "  #{\n" +
            "    default.PersonTable:\n" +
            "              'id,firm_id,firstName,lastName\\n' +\n" +
            "              '1,1,John,Doe\\n' +\n" +
            "              '2,1,Nicole,Smith\\n' +\n" +
            "              '3,2,Time,Smith\\n';\n" +
            "\n" +
            "    default.FirmTable:\n" +
            "          'id,legal_name\\n' +\n" +
            "          '1,Finos\\n' +\n" +
            "          '2,Apple';\n" +
            "\n" +
            "  }#\n" +
            "}\n" +
            "\n" +
            "###Relational\n" +
            "Database store::TestDB\n" +
            "(\n" +
            "  Table FirmTable\n" +
            "  (\n" +
            "    id INTEGER PRIMARY KEY,\n" +
            "    legal_name VARCHAR(200)\n" +
            "  )\n" +
            "  Table PersonTable\n" +
            "  (\n" +
            "    id INTEGER PRIMARY KEY,\n" +
            "    firm_id INTEGER,\n" +
            "    firstName VARCHAR(200),\n" +
            "    lastName VARCHAR(200)\n" +
            "  )\n" +
            "\n" +
            "  Join FirmPerson(PersonTable.firm_id = FirmTable.id)\n" +
            ")\n" +
            "\n" +
            "\n" +
            "###Pure\n" +
            "Class model::Person\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "  lastName: String[1];\n" +
            "}\n" +
            "\n" +
            "Class model::Firm\n" +
            "{\n" +
            "  legalName: String[1];\n" +
            "  employees: model::Person[*];\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping execution::RelationalMapping\n" +
            "(\n" +
            "  *model::Person: Relational\n" +
            "  {\n" +
            "    ~primaryKey\n" +
            "    (\n" +
            "      [store::TestDB]PersonTable.id\n" +
            "    )\n" +
            "    ~mainTable [store::TestDB]PersonTable\n" +
            "    firstName: [store::TestDB]PersonTable.firstName,\n" +
            "    lastName: [store::TestDB]PersonTable.lastName\n" +
            "  }\n" +
            "  *model::Firm: Relational\n" +
            "  {\n" +
            "    ~primaryKey\n" +
            "    (\n" +
            "      [store::TestDB]FirmTable.id\n" +
            "    )\n" +
            "    ~mainTable [store::TestDB]FirmTable\n" +
            "    legalName: [store::TestDB]FirmTable.legal_name,\n" +
            "    employees[model_Person]: [store::TestDB]@FirmPerson\n" +
            "  }\n" +
            "  testSuites:\n" +
            "    [\n" +
            "      testSuite1:\n" +
            "      {\n" +
            "        function: |model::Firm.all()->project([x|$x.employees.firstName, x|$x.employees.lastName, x|$x.legalName], ['Employees/First Name', 'Employees/Last Name', 'Legal Name']);\n" +
            "        tests:\n" +
            "        [\n" +
            "          test1:\n" +
            "          {\n" +
            "            data:\n" +
            "            [\n" +
            "              store::TestDB:\n" +
            "                Reference\n" +
            "                #{\n" +
            "                  data::RelationalData\n" +
            "                }#\n" +
            "            ];\n" +
            "            asserts:\n" +
            "            [\n" +
            "              shouldPass:\n" +
            "                EqualToJson\n" +
            "                #{\n" +
            "                  expected :\n" +
            "                    ExternalFormat\n" +
            "                    #{\n" +
            "                      contentType: 'application/json';\n" +
            "                      data: '[{\"Employees/First Name\":\"John\",\"Employees/Last Name\":\"Doe\",\"Legal Name\":\"Finos\"},{\"Employees/First Name\":\"Nicole\",\"Employees/Last Name\":\"Smith\",\"Legal Name\":\"Finos\"},{\"Employees/First Name\":\"Time\",\"Employees/Last Name\":\"Smith\",\"Legal Name\":\"Apple\"}]';\n" +
            "                    }#;\n" +
            "                }#\n" +
            "            ];\n" +
            "          }\n" +
            "        ];\n" +
            "      }\n" +
            "    ]\n" +
            ")\n";

    String model2 = "###Data\n" +
            "Data data::RelationalData\n" +
            "{\n" +
            "  Relational\n" +
            "  #{\n" +
            "    default.PersonTable:\n" +
            "              'id,firm_id,firstName,lastName,employeeType\\n' +\n" +
            "              '1,1,John,Doe,FTO\\n' +\n" +
            "              '2,1,Nicole,Smith,FTC\\n' +\n" +
            "              '3,2,Time,Smith,FTE\\n';\n" +
            "\n" +
            "    default.FirmTable:\n" +
            "          'id,legal_name\\n' +\n" +
            "          '1,Finos\\n' +\n" +
            "          '2,Apple';\n" +
            "\n" +
            "  }#\n" +
            "}\n" +
            "\n" +
            "###Relational\n" +
            "Database store::TestDB\n" +
            "(\n" +
            "  Table FirmTable\n" +
            "  (\n" +
            "    id INTEGER PRIMARY KEY,\n" +
            "    legal_name VARCHAR(200)\n" +
            "  )\n" +
            "  Table PersonTable\n" +
            "  (\n" +
            "    id INTEGER PRIMARY KEY,\n" +
            "    firm_id INTEGER,\n" +
            "    firstName VARCHAR(200),\n" +
            "    lastName VARCHAR(200),\n" +
            "    employeeType VARCHAR(200)\n" +
            "  )\n" +
            "\n" +
            "  Join FirmPerson(PersonTable.firm_id = FirmTable.id)\n" +
            ")\n" +
            "\n" +
            "\n" +
            "###Pure\n" +
            "Class model::Person\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "  lastName: String[1];\n" +
            "  employeeType: model::EmployeeType[1];\n" +
            "}\n" +
            "\n" +
            "Enum model::EmployeeType\n" +
            "{\n" +
            "    CONTRACT,\n" +
            "    FULL_TIME\n" +
            "}\n" +
            "\n" +
            "Class model::Firm\n" +
            "{\n" +
            "  legalName: String[1];\n" +
            "  employees: model::Person[*];\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping execution::RelationalMapping\n" +
            "(\n" +
            "  *model::Person: Relational\n" +
            "  {\n" +
            "    ~primaryKey\n" +
            "    (\n" +
            "      [store::TestDB]PersonTable.id\n" +
            "    )\n" +
            "    ~mainTable [store::TestDB]PersonTable\n" +
            "    firstName: [store::TestDB]PersonTable.firstName,\n" +
            "    lastName: [store::TestDB]PersonTable.lastName,\n" +
            "    employeeType: EnumerationMapping EmployeeTypeMapping: [store::TestDB]PersonTable.employeeType\n" +
            "  }\n" +
            "\n" +
            "  model::EmployeeType: EnumerationMapping EmployeeTypeMapping\n" +
            "  {\n" +
            "    CONTRACT: ['FTC', 'FTO'],\n" +
            "    FULL_TIME: 'FTE'\n" +
            "  }\n" +
            "\n" +
            "  *model::Firm: Relational\n" +
            "  {\n" +
            "    ~primaryKey\n" +
            "    (\n" +
            "      [store::TestDB]FirmTable.id\n" +
            "    )\n" +
            "    ~mainTable [store::TestDB]FirmTable\n" +
            "    legalName: [store::TestDB]FirmTable.legal_name,\n" +
            "    employees[model_Person]: [store::TestDB]@FirmPerson\n" +
            "  }\n" +
            "\n" +
            "\n" +
            "  testSuites:\n" +
            "  [\n" +
            "    testSuite1:\n" +
            "    {\n" +
            "      function: |model::Firm.all()->project([x|$x.employees.firstName], ['Employees/First Name']);\n" +
            "      tests:\n" +
            "      [\n" +
            "        test1:\n" +
            "        {\n" +
            "        data:\n" +
            "        [\n" +
            "           store::TestDB: \n" +
            "                Reference\n" +
            "                #{\n" +
            "                  data::RelationalData\n" +
            "                }#\n" +
            "        ];\n" +
            "          asserts:\n" +
            "          [\n" +
            "            assert1:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected :\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '[{\"Employees/First Name\":\"John\"},{\"Employees/First Name\":\"Nicole\"},{\"Employees/First Name\":\"Time\"}]';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ];\n" +
            "        },\n" +
            "        test2:\n" +
            "        {\n" +
            "          data:\n" +
            "          [\n" +
            "           store::TestDB: \n" +
            "                Reference\n" +
            "                #{\n" +
            "                  data::RelationalData\n" +
            "                }#\n" +
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
            "                    data: '{}';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ];\n" +
            "        }\n" +
            "      ];\n" +
            "    }\n" +
            "  ]\n" +
            "\n" +
            ")\n" +
            "\n" +
            "\n" +
            "###Connection\n" +
            "RelationalDatabaseConnection model::MyConnection\n" +
            "{\n" +
            "  store: store::TestDB;\n" +
            "  type: H2;\n" +
            "  specification: LocalH2\n" +
            "  {\n" +
            "    testDataSetupSqls: [\n" +
            "      ];\n" +
            "  };\n" +
            "  auth: DefaultH2;\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Runtime\n" +
            "Runtime execution::Runtime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    execution::RelationalMapping\n" +
            "  ];\n" +
            "  connections:\n" +
            "  [\n" +
            "    store::TestDB:\n" +
            "    [\n" +
            "      connection_1: model::MyConnection\n" +
            "    ]\n" +
            "  ];\n" +
            "}\n";

    @Test
    public void testRelationalMappingTestSuite()
    {
        MappingTestableRunnerExtension mappingTestableRunnerExtension = new MappingTestableRunnerExtension();
        mappingTestableRunnerExtension.setPureVersion("v1_23_0");
        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(model1);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        Mapping mappingToTest = (Mapping) pureModelWithReferenceData.getPackageableElement("execution::RelationalMapping");
        List<TestResult> mappingTestResults = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModelWithReferenceData, modelDataWithReferenceData);

        Assert.assertEquals(1, mappingTestResults.size());
        Assert.assertTrue(mappingTestResults.get(0) instanceof TestExecuted);
        Assert.assertEquals(TestExecutionStatus.PASS, ((TestExecuted) mappingTestResults.get(0)).testExecutionStatus);
        Assert.assertEquals("execution::RelationalMapping", mappingTestResults.get(0).testable);
        Assert.assertEquals("testSuite1", mappingTestResults.get(0).testSuiteId);
        Assert.assertEquals("test1", mappingTestResults.get(0).atomicTestId);
    }



    @Test
    public void testRelationalMappingTestRunner()
    {
        MappingTestableRunnerExtension mappingTestableRunnerExtension = new MappingTestableRunnerExtension();
        mappingTestableRunnerExtension.setPureVersion(PureClientVersions.production);

        PureModelContextData modelDataWithReferenceData = PureGrammarParser.newInstance().parseModel(model2);
        PureModel pureModelWithReferenceData = Compiler.compile(modelDataWithReferenceData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mappingToTest = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) pureModelWithReferenceData.getPackageableElement("execution::RelationalMapping");
        List<TestResult> mappingTestResults = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModelWithReferenceData, modelDataWithReferenceData);
        Assert.assertEquals(2, mappingTestResults.size());
        // test 1
        TestResult _test1 = mappingTestResults.stream().filter(e -> e.atomicTestId.equals("test1")).findFirst().get();
        Assert.assertTrue(_test1 instanceof  TestExecuted);
        TestExecuted testExecuted = (TestExecuted)_test1;
        Assert.assertEquals("testSuite1", testExecuted.testSuiteId);
        Assert.assertEquals(1, testExecuted.assertStatuses.size());
        Assert.assertEquals(TestExecutionStatus.PASS, testExecuted.testExecutionStatus);
        // test 2
        TestResult _test2 = mappingTestResults.stream().filter(e -> e.atomicTestId.equals("test2")).findFirst().get();
        Assert.assertTrue(_test2 instanceof  TestExecuted);
        TestExecuted testExecuted2 = (TestExecuted)_test2;
        Assert.assertEquals("testSuite1", testExecuted2.testSuiteId);
        Assert.assertEquals(1, testExecuted2.assertStatuses.size());
        Assert.assertEquals(TestExecutionStatus.FAIL, testExecuted2.testExecutionStatus);
        AssertionStatus assertionStatus = testExecuted2.assertStatuses.get(0);
        Assert.assertTrue(assertionStatus instanceof EqualToJsonAssertFail);
        EqualToJsonAssertFail equalToJsonAssertFail1 = (EqualToJsonAssertFail)assertionStatus;
        JsonAssert.assertJsonEquals("{}",equalToJsonAssertFail1.expected);
        String expected = "[ {\n" +
                "  \"Employees/First Name\" : \"John\"\n" +
                "}, {\n" +
                "  \"Employees/First Name\" : \"Nicole\"\n" +

                "}, {\n" +
                "  \"Employees/First Name\" : \"Time\"\n" +
                "} ]";
        JsonAssert.assertJsonEquals(expected, equalToJsonAssertFail1.actual);

    }

    String bitTypeModel = "###Data\n" +
            "Data data::FeatureData\n" +
            "{\n" +
            "  Relational\n" +
            "  #{\n" +
            "    default.FeatureTable:\n" +
            "              'id,name,is_active\\n' +\n" +
            "              '1,FeatureA,true\\n' +\n" +
            "              '2,FeatureB,false\\n';\n" +
            "  }#\n" +
            "}\n" +
            "\n" +
            "###Relational\n" +
            "Database store::FeatureDB\n" +
            "(\n" +
            "  Table FeatureTable\n" +
            "  (\n" +
            "    id INTEGER PRIMARY KEY,\n" +
            "    name VARCHAR(200),\n" +
            "    is_active BIT\n" +
            "  )\n" +
            ")\n" +
            "\n" +
            "###Pure\n" +
            "Class model::Feature\n" +
            "{\n" +
            "  name: String[1];\n" +
            "  isActive: Boolean[1];\n" +
            "}\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping execution::FeatureMapping\n" +
            "(\n" +
            "  *model::Feature: Relational\n" +
            "  {\n" +
            "    ~primaryKey\n" +
            "    (\n" +
            "      [store::FeatureDB]FeatureTable.id\n" +
            "    )\n" +
            "    ~mainTable [store::FeatureDB]FeatureTable\n" +
            "    name: [store::FeatureDB]FeatureTable.name,\n" +
            "    isActive: [store::FeatureDB]FeatureTable.is_active\n" +
            "  }\n" +
            "  testSuites:\n" +
            "    [\n" +
            "      testSuite1:\n" +
            "      {\n" +
            "        function: |model::Feature.all()->project(~[name: x|$x.name, isActive: x|$x.isActive]);\n" +
            "        tests:\n" +
            "        [\n" +
            "          test1:\n" +
            "          {\n" +
            "            data:\n" +
            "            [\n" +
            "              store::FeatureDB:\n" +
            "                Reference\n" +
            "                #{\n" +
            "                  data::FeatureData\n" +
            "                }#\n" +
            "            ];\n" +
            "            asserts:\n" +
            "            [\n" +
            "              shouldPass:\n" +
            "                EqualToJson\n" +
            "                #{\n" +
            "                  expected :\n" +
            "                    ExternalFormat\n" +
            "                    #{\n" +
            "                      contentType: 'application/json';\n" +
            "                      data: '[{\"name\":\"FeatureA\",\"isActive\":true},{\"name\":\"FeatureB\",\"isActive\":false}]';\n" +
            "                    }#;\n" +
            "                }#\n" +
            "            ];\n" +
            "          }\n" +
            "        ];\n" +
            "      }\n" +
            "    ]\n" +
            ")\n";

    @Test
    public void testRelationalMappingWithBitTypeAndRelationReturnType()
    {
        MappingTestableRunnerExtension mappingTestableRunnerExtension = new MappingTestableRunnerExtension();
        mappingTestableRunnerExtension.setPureVersion(PureClientVersions.production);
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(bitTypeModel);
        PureModel pureModel = Compiler.compile(modelData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        Mapping mappingToTest = (Mapping) pureModel.getPackageableElement("execution::FeatureMapping");
        List<TestResult> results = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModel, modelData);

        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.get(0) instanceof TestExecuted);
        if (((TestExecuted) results.get(0)).testExecutionStatus == TestExecutionStatus.FAIL)
        {
            AssertionStatus status = ((TestExecuted) results.get(0)).assertStatuses.get(0);
            if (status instanceof EqualToJsonAssertFail)
            {
                EqualToJsonAssertFail failAssert = (EqualToJsonAssertFail) status;
                Assert.assertEquals(failAssert.expected, failAssert.actual);
            }
        }
        Assert.assertEquals(TestExecutionStatus.PASS, ((TestExecuted) results.get(0)).testExecutionStatus);
        Assert.assertEquals("execution::FeatureMapping", results.get(0).testable);
        Assert.assertEquals("testSuite1", results.get(0).testSuiteId);
        Assert.assertEquals("test1", results.get(0).atomicTestId);
    }

    String relationDataModel =
            "###Relational\n" +
            "Database store::TestDB\n" +
            "(\n" +
            "  Table FirmTable\n" +
            "  (\n" +
            "    id INTEGER PRIMARY KEY,\n" +
            "    legal_name VARCHAR(200)\n" +
            "  )\n" +
            "  Table PersonTable\n" +
            "  (\n" +
            "    id INTEGER PRIMARY KEY,\n" +
            "    firm_id INTEGER,\n" +
            "    firstName VARCHAR(200),\n" +
            "    lastName VARCHAR(200)\n" +
            "  )\n" +
            "\n" +
            "  Join FirmPerson(PersonTable.firm_id = FirmTable.id)\n" +
            ")\n" +
            "\n" +
            "\n" +
            "###Pure\n" +
            "Class model::Person\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "  lastName: String[1];\n" +
            "}\n" +
            "\n" +
            "Class model::Firm\n" +
            "{\n" +
            "  legalName: String[1];\n" +
            "  employees: model::Person[*];\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping execution::RelationalMapping\n" +
            "(\n" +
            "  *model::Person: Relational\n" +
            "  {\n" +
            "    ~primaryKey\n" +
            "    (\n" +
            "      [store::TestDB]PersonTable.id\n" +
            "    )\n" +
            "    ~mainTable [store::TestDB]PersonTable\n" +
            "    firstName: [store::TestDB]PersonTable.firstName,\n" +
            "    lastName: [store::TestDB]PersonTable.lastName\n" +
            "  }\n" +
            "  *model::Firm: Relational\n" +
            "  {\n" +
            "    ~primaryKey\n" +
            "    (\n" +
            "      [store::TestDB]FirmTable.id\n" +
            "    )\n" +
            "    ~mainTable [store::TestDB]FirmTable\n" +
            "    legalName: [store::TestDB]FirmTable.legal_name,\n" +
            "    employees[model_Person]: [store::TestDB]@FirmPerson\n" +
            "  }\n" +
            "  testSuites:\n" +
            "    [\n" +
            "      testSuite1:\n" +
            "      {\n" +
            "        function: |model::Firm.all()->project(~['Employees/First Name': x|$x.employees.firstName, 'Employees/Last Name': x|$x.employees.lastName, 'Legal Name': x|$x.legalName]);\n" +
            "        tests:\n" +
            "        [\n" +
            "          test1:\n" +
            "          {\n" +
            "            data:\n" +
            "            [\n" +
            "              store::TestDB:\n" +
            "  Relation\n" +
            "  #{\n" +
            "    default.PersonTable:\n" +
            "      id,firm_id,firstName,lastName\n" +
            "      1,1,John,Doe\n" +
            "      2,1,Nicole,Smith\n" +
            "      3,2,Time,Smith;\n" +
            "\n" +
            "    default.FirmTable:\n" +
            "      id,legal_name\n" +
            "      1,Finos\n" +
            "      2,Apple;\n" +
            "  }#\n" +
            "            ];\n" +
            "            asserts:\n" +
            "            [\n" +
            "              shouldPass:\n" +
            "                Relation\n" +
            "                #{\n" +
            "                  Employees/First Name, Employees/Last Name, Legal Name\n" +
            "                  John                , Doe                , Finos\n" +
            "                  Nicole              , Smith              , Finos\n" +
            "                  Time                , Smith              , Apple;\n" +
            "                }#\n" +
            "            ];\n" +
            "          }\n" +
            "        ];\n" +
            "      }\n" +
            "    ]\n" +
            ")\n";

    @Test
    public void testRelationalMappingTestSuiteWithRelationReturnTypeRelationDataAndEqualToRelationAssertion()
    {
        MappingTestableRunnerExtension mappingTestableRunnerExtension = new MappingTestableRunnerExtension();
        mappingTestableRunnerExtension.setPureVersion(PureClientVersions.production);
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(relationDataModel);
        PureModel pureModel = Compiler.compile(modelData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        Mapping mappingToTest = (Mapping) pureModel.getPackageableElement("execution::RelationalMapping");
        List<TestResult> results = mappingTestableRunnerExtension.executeAllTest(mappingToTest, pureModel, modelData);

        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.get(0) instanceof TestExecuted);
        TestExecuted executed = (TestExecuted) results.get(0);
        Assert.assertEquals(TestExecutionStatus.PASS, executed.testExecutionStatus);
        Assert.assertEquals("execution::RelationalMapping", executed.testable);
        Assert.assertEquals("testSuite1", executed.testSuiteId);
        Assert.assertEquals("test1", executed.atomicTestId);
    }
}
