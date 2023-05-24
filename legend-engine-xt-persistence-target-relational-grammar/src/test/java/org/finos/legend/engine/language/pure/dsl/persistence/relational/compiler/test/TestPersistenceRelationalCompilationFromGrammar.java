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

package org.finos.legend.engine.language.pure.dsl.persistence.relational.compiler.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.junit.Test;

public class TestPersistenceRelationalCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{

    @Override
    protected String getDuplicatedElementTestCode()
    {
        return "Class test::MyPersistence {}\n" +
            "\n" +
            "###Persistence\n" +
            "Persistence test::MyPersistence\n" +
            "{\n" +
            "  doc: 'This is test documentation.';\n" +
            "  trigger: Manual;\n" +
            "  service: test::Service;\n" +
            "  serviceOutputTargets:\n" +
            "  [\n" +
            "    ROOT\n" +
            "    {\n" +
            "      keys:\n" +
            "      [\n" +
            "        foo, bar\n" +
            "      ]\n" +
            "      datasetType: Delta\n" +
            "      {\n" +
            "        actionIndicator: DeleteIndicator\n" +
            "        {\n" +
            "          deleteField: isDeleted;\n" +
            "          deleteValues: ['Y', '1', 'true'];\n" +
            "        }\n" +
            "      }\n" +
            "      deduplication: AnyVersion;\n" +
            "    }\n" +
            "    ->\n" +
            "    Relational\n" +
            "    #{\n" +
            "      table: TableA;\n" +
            "      database: test::Database;\n" +
            "      temporality: None;\n" +
            "    }#\n" +
            "  ];\n" +
            "}\n";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [4:1-35:1]: Duplicated element 'test::MyPersistence'";
    }

    @Test
    public void nontemporal()
    {
        Pair<PureModelContextData, PureModel> result = test(
            "Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::Mapping ()\n" +
                "\n" +
                "###Service\n" +
                "Service test::Service \n" +
                "{\n" +
                "  pattern : 'test';\n" +
                "  documentation : 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |test::Person.all()->project([p|$p.name],['name']);\n" +
                "    mapping: test::Mapping;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "      connections: [];\n" +
                "    }#;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'test';\n" +
                "    asserts: [];\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database test::Database\n" +
                "(\n" +
                "  Table personTable\n" +
                "  (\n" +
                "    ID INTEGER PRIMARY KEY,\n" +
                "    NAME VARCHAR(100)\n" +
                "  )\n" +
                ")" +
                "\n" +
                "###Persistence\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  serviceOutputTargets:\n" +
                "  [\n" +
                "    ROOT\n" +
                "    {\n" +
                "      keys:\n" +
                "      [\n" +
                "        foo, bar\n" +
                "      ]\n" +
                "      datasetType: Snapshot\n" +
                "      {\n" +
                "        partitioning: FieldBased\n" +
                "        {\n" +
                "          partitionFields:\n" +
                "          [\n" +
                "            foo1, bar2\n" +
                "          ];\n" +
                "        }\n" +
                "      }\n" +
                "      deduplication: MaxVersion\n" +
                "      {\n" +
                "        versionField: version;\n" +
                "      }\n" +
                "    }\n" +
                "    ->\n" +
                "    Relational\n" +
                "    #{\n" +
                "      table: personTable;\n" +
                "      database: test::Database;\n" +
                "      temporality: None;\n" +
                "    }#\n" +
                "  ];\n" +
                "  tests:\n" +
                "  [\n" +
                "    test1:\n" +
                "    {\n" +
                "      testBatches:\n" +
                "      [\n" +
                "        testBatch1:\n" +
                "        {\n" +
                "         data:\n" +
                "         {\n" +
                "           connection:\n" +
                "           {\n" +
                "              ExternalFormat\n" +
                "              #{\n" +
                "                contentType: 'application/x.flatdata';\n" +
                "                data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "              }#\n" +
                "           }\n" +
                "         }\n" +
                "         asserts:\n" +
                "         [\n" +
                "           assert1:\n" +
                "             EqualToJson\n" +
                "             #{\n" +
                "               expected: \n" +
                "                 ExternalFormat\n" +
                "                 #{\n" +
                "                   contentType: 'application/json';\n" +
                "                   data: '{\"Age\":12, \"Name\":\"dummy\"}';\n" +
                "                 }#;\n" +
                "             }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "      isTestDataFromServiceOutput: false;\n" +
                "    }\n" +
                "  ]\n" +
                "}\n");

        PureModel model = result.getTwo();
    }

    @Test
    public void unitemporal()
    {
        Pair<PureModelContextData, PureModel> result = test(
            "Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::Mapping ()\n" +
                "\n" +
                "###Service\n" +
                "Service test::Service \n" +
                "{\n" +
                "  pattern : 'test';\n" +
                "  documentation : 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |test::Person.all()->project([p|$p.name],['name']);\n" +
                "    mapping: test::Mapping;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "      connections: [];\n" +
                "    }#;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'test';\n" +
                "    asserts: [];\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "###Relational\n" +
                "Database test::Database\n" +
                "(\n" +
                "  Table personTable\n" +
                "  (\n" +
                "    ID INTEGER PRIMARY KEY,\n" +
                "    NAME VARCHAR(100)," +
                "    time_in TIMESTAMP," +
                "    time_out TIMESTAMP\n" +
                "  )\n" +
                ")" +
                "\n" +
                "###Persistence\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  serviceOutputTargets:\n" +
                "  [\n" +
                "    ROOT\n" +
                "    {\n" +
                "      keys:\n" +
                "      [\n" +
                "        foo, bar\n" +
                "      ]\n" +
                "      datasetType: Snapshot\n" +
                "      {\n" +
                "        partitioning: FieldBased\n" +
                "        {\n" +
                "          partitionFields:\n" +
                "          [\n" +
                "            foo1, bar2\n" +
                "          ];\n" +
                "        }\n" +
                "      }\n" +
                "      deduplication: MaxVersion\n" +
                "      {\n" +
                "        versionField: version;\n" +
                "      }\n" +
                "    }\n" +
                "    ->\n" +
                "    Relational\n" +
                "    #{\n" +
                "      database: test::Database;\n" +
                "      table: personTable;\n" +
                "      temporality: Unitemporal\n" +
                "      {\n" +
                "        processingDimension: DateTime\n" +
                "        {\n" +
                "          dateTimeIn: time_in;\n" +
                "          dateTimeOut: time_out;\n" +
                "        }\n" +
                "      }\n" +
                "    }#\n" +
                "  ];\n" +
                "  tests:\n" +
                "  [\n" +
                "    test1:\n" +
                "    {\n" +
                "      testBatches:\n" +
                "      [\n" +
                "        testBatch1:\n" +
                "        {\n" +
                "         data:\n" +
                "         {\n" +
                "           connection:\n" +
                "           {\n" +
                "              ExternalFormat\n" +
                "              #{\n" +
                "                contentType: 'application/x.flatdata';\n" +
                "                data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "              }#\n" +
                "           }\n" +
                "         }\n" +
                "         asserts:\n" +
                "         [\n" +
                "           assert1:\n" +
                "             EqualToJson\n" +
                "             #{\n" +
                "               expected: \n" +
                "                 ExternalFormat\n" +
                "                 #{\n" +
                "                   contentType: 'application/json';\n" +
                "                   data: '{\"Age\":12, \"Name\":\"dummy\"}';\n" +
                "                 }#;\n" +
                "             }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "      isTestDataFromServiceOutput: false;\n" +
                "    }\n" +
                "  ]\n" +
                "}\n");

        PureModel model = result.getTwo();
    }

    @Test
    public void bitemporal()
    {
        Pair<PureModelContextData, PureModel> result = test(
            "Class test::model::Firm\n" +
                "{\n" +
                "  employees: test::model::Person[1..*];\n" +
                "  legalName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::model::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "  isDeleted: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::store::S_Firm\n" +
                "{\n" +
                "  employees: test::store::S_Person[1..*];\n" +
                "  legalName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::store::S_Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::mapping::FirmMapping\n" +
                "(\n" +
                "  *test::model::Firm: Pure\n" +
                "  {\n" +
                "    ~src test::store::S_Firm\n" +
                "    legalName: $src.legalName,\n" +
                "    employees[test_model_Person]: $src.employees\n" +
                "  }\n" +
                "  *test::model::Person: Pure\n" +
                "  {\n" +
                "    ~src test::store::S_Person\n" +
                "    firstName: $src.firstName,\n" +
                "    lastName: $src.lastName\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection test::runtime::SFirmConnection\n" +
                "{\n" +
                "  class: test::store::S_Firm;\n" +
                "  url: 'executor:default';\n" +
                "}\n" +
                "JsonModelConnection test::runtime::SPersonConnection\n" +
                "{\n" +
                "  class: test::store::S_Person;\n" +
                "  url: 'executor:default';\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime test::runtime::SFirmRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::FirmMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection1: test::runtime::SFirmConnection\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n" +
                "Runtime test::runtime::SFirmAndSPersonRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::FirmMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection1: test::runtime::SFirmConnection,\n" +
                "      connection2: test::runtime::SPersonConnection\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Service\n" +
                "Service test::service::FirmService\n" +
                "{\n" +
                "  pattern: '/testFirmService';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'owner1',\n" +
                "    'owner2'\n" +
                "  ];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: name:String[1]|test::model::Firm.all()->filter(f | $f.legalName == $name )->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                "    mapping: test::mapping::FirmMapping;\n" +
                "    runtime: test::runtime::SFirmRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "  ]\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Relational\n" +
                "Database test::Database\n" +
                "(\n" +
                "  Table personTable\n" +
                "  (\n" +
                "    ID INTEGER PRIMARY KEY,\n" +
                "    NAME VARCHAR(100)," +
                "    startTime TIMESTAMP," +
                "    endTime TIMESTAMP," +
                "    batchIdIn INTEGER," +
                "    batchIdOut INTEGER\n" +
                "  )\n" +
                ")" +
                "\n" +
                "###Persistence\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::service::FirmService;\n" +
                "  serviceOutputTargets:\n" +
                "  [\n" +
                "    #/test::model::Firm/employees#\n" +
                "    {\n" +
                "      keys:\n" +
                "      [\n" +
                "        #/test::model::Firm/employees/lastName#\n" +
                "      ]\n" +
                "      datasetType: Delta\n" +
                "      {\n" +
                "        actionIndicator: DeleteIndicator\n" +
                "        {\n" +
                "          deleteField: #/test::model::Firm/employees/isDeleted#;\n" +
                "          deleteValues: ['Yes', 'true', '1'];\n" +
                "        }\n" +
                "      }\n" +
                "      deduplication: None;\n" +
                "    }\n" +
                "    ->\n" +
                "    Relational\n" +
                "    #{\n" +
                "      table: personTable;\n" +
                "      database: test::Database;\n" +
                "      temporality: Bitemporal\n" +
                "      {\n" +
                "        processingDimension: BatchId\n" +
                "        {\n" +
                "          batchIdIn: batchIdIn;\n" +
                "          batchIdOut: batchIdOut;\n" +
                "        }\n" +
                "        sourceDerivedDimension: DateTime\n" +
                "        {\n" +
                "          dateTimeStart: startTime;\n" +
                "          dateTimeEnd: endTime;\n" +
                "          sourceFields: StartAndEnd\n" +
                "          {\n" +
                "            startField: timeFrom;\n" +
                "            endField: timeThru;\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#\n" +
                "  ];\n" +
                "  tests:\n" +
                "  [\n" +
                "    test1:\n" +
                "    {\n" +
                "      testBatches:\n" +
                "      [\n" +
                "        testBatch1:\n" +
                "        {\n" +
                "         data:\n" +
                "         {\n" +
                "           connection:\n" +
                "           {\n" +
                "              ExternalFormat\n" +
                "              #{\n" +
                "                contentType: 'application/x.flatdata';\n" +
                "                data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "              }#\n" +
                "           }\n" +
                "         }\n" +
                "         asserts:\n" +
                "         [\n" +
                "           assert1:\n" +
                "             EqualToJson\n" +
                "             #{\n" +
                "               expected: \n" +
                "                 ExternalFormat\n" +
                "                 #{\n" +
                "                   contentType: 'application/json';\n" +
                "                   data: '{\"Age\":12, \"Name\":\"dummy\"}';\n" +
                "                 }#;\n" +
                "             }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "      isTestDataFromServiceOutput: false;\n" +
                "    }\n" +
                "  ]\n" +
                "}\n");

        PureModel model = result.getTwo();
    }
}
