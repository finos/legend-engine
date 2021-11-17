// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.test.runner.service;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.SingleExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

public class TestRuntimeGenerationForServiceTests
{
    @Test
    public void testRuntimeGenerationForModelChainConnection()
    {
        String pureGrammarWithModelChainConnection =
                "###Relational\n" +
                        "Database demo::modelChainConnection::relationalDB\n" +
                        "(\n" +
                        "  Table Person\n" +
                        "  (\n" +
                        "    fullname VARCHAR(1000) PRIMARY KEY,\n" +
                        "    firm VARCHAR(200)\n" +
                        "  )\n" +
                        ")\n" +
                        "\n" +
                        "\n" +
                        "###Service\n" +
                        "Service demo::modelChainConnection::testModelChainConnectionService\n" +
                        "{\n" +
                        "  pattern: '/maheha/testModelChainConnection/fromStudio/';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'maheha'\n" +
                        "  ];\n" +
                        "  documentation: '';\n" +
                        "  autoActivateUpdates: false;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |demo::modelChainConnection::dest::Person.all()->project([f|$f.firstName, f|$f.lastName], ['firstName', 'lastName']);\n" +
                        "    mapping: demo::modelChainConnection::simpleModelMappingWithAssociation;\n" +
                        "    runtime:\n" +
                        "    #{\n" +
                        "      mappings:\n" +
                        "      [\n" +
                        "        demo::modelChainConnection::simpleModelMappingWithAssociation\n" +
                        "      ];\n" +
                        "      connections:\n" +
                        "      [\n" +
                        "        ModelStore:\n" +
                        "        [\n" +
                        "          connection_1: demo::modelChainConnection::modelChainConnection\n" +
                        "        ],\n" +
                        "        demo::modelChainConnection::relationalDB:\n" +
                        "        [\n" +
                        "          connection_2: demo::modelChainConnection::mySimpleConnection\n" +
                        "        ]\n" +
                        "      ];\n" +
                        "    }#;\n" +
                        "  }\n" +
                        "  test: Single\n" +
                        "  {\n" +
                        "    data: 'default\\nPerson\\nfullname,firm\\nPierre DeBelen,A\\nA. Only One,A\\n\\n\\n\\n';\n" +
                        "    asserts:\n" +
                        "    [\n" +
                        "    ];\n" +
                        "  }\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "###Pure\n" +
                        "Class demo::modelChainConnection::dest::Address\n" +
                        "{\n" +
                        "  street: String[0..1];\n" +
                        "  extension: demo::modelChainConnection::dest::AddressExtension[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class demo::modelChainConnection::dest::AddressExtension\n" +
                        "{\n" +
                        "  stuff: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class demo::modelChainConnection::dest::Firm\n" +
                        "{\n" +
                        "  legalName: String[1];\n" +
                        "  employees: demo::modelChainConnection::dest::Person[*];\n" +
                        "  addresses: demo::modelChainConnection::dest::Address[*];\n" +
                        "  count: Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class demo::modelChainConnection::dest::Person\n" +
                        "{\n" +
                        "  firstName: String[1];\n" +
                        "  lastName: String[1];\n" +
                        "  addresses: demo::modelChainConnection::dest::Address[*];\n" +
                        "  firm: demo::modelChainConnection::dest::Firm[0..1];\n" +
                        "  description: String[0..1];\n" +
                        "  type: String[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class demo::modelChainConnection::src::_Person\n" +
                        "{\n" +
                        "  fullName: String[1];\n" +
                        "  addresses: demo::modelChainConnection::dest::Address[*];\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping demo::modelChainConnection::relationalMapping\n" +
                        "(\n" +
                        "  demo::modelChainConnection::src::_Person: Relational\n" +
                        "  {\n" +
                        "    ~primaryKey\n" +
                        "    (\n" +
                        "      [demo::modelChainConnection::relationalDB]Person.fullname\n" +
                        "    )\n" +
                        "    ~mainTable [demo::modelChainConnection::relationalDB]Person\n" +
                        "    fullName: [demo::modelChainConnection::relationalDB]Person.fullname\n" +
                        "  }\n" +
                        ")\n" +
                        "\n" +
                        "Mapping demo::modelChainConnection::simpleModelMappingWithAssociation\n" +
                        "(\n" +
                        "  demo::modelChainConnection::dest::Person: Pure\n" +
                        "  {\n" +
                        "    ~src demo::modelChainConnection::src::_Person\n" +
                        "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                        "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                        "  }\n" +
                        ")\n" +
                        "\n" +
                        "\n" +
                        "###Connection\n" +
                        "ModelChainConnection demo::modelChainConnection::modelChainConnection\n" +
                        "{\n" +
                        "  mappings: [\n" +
                        "    demo::modelChainConnection::relationalMapping\n" +
                        "  ];\n" +
                        "}\n" +
                        "\n" +
                        "RelationalDatabaseConnection demo::modelChainConnection::mySimpleConnection\n" +
                        "{\n" +
                        "  store: demo::modelChainConnection::relationalDB;\n" +
                        "  type: H2;\n" +
                        "  specification: LocalH2\n" +
                        "  {\n" +
                        "    testDataSetupCSV: '';\n" +
                        "  };\n" +
                        "  auth: DefaultH2;\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "###Runtime\n" +
                        "Runtime demo::modelChainConnection\n" +
                        "{\n" +
                        "  mappings:\n" +
                        "  [\n" +
                        "    demo::modelChainConnection::simpleModelMappingWithAssociation\n" +
                        "  ];\n" +
                        "  connections:\n" +
                        "  [\n" +
                        "    ModelStore:\n" +
                        "    [\n" +
                        "      connection_1: demo::modelChainConnection::modelChainConnection\n" +
                        "    ],\n" +
                        "    demo::modelChainConnection::relationalDB:\n" +
                        "    [\n" +
                        "      connection_2: demo::modelChainConnection::mySimpleConnection\n" +
                        "    ]\n" +
                        "  ];\n" +
                        "}\n";
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(pureGrammarWithModelChainConnection);
        PureModel pureModel = new PureModel(contextData, null, DeploymentMode.TEST);
        Service service = contextData.getElementsOfType(Service.class).get(0);
        EngineRuntime testRuntime = (EngineRuntime) ServiceTestGenerationHelper.buildSingleExecutionTestRuntime((PureSingleExecution) service.execution, (SingleExecutionTest) service.test, contextData, pureModel);
        Assert.assertEquals(testRuntime.connections.size(), 2);
        Assert.assertNotNull(testRuntime.getStoreConnections("ModelStore"));
        Assert.assertEquals(testRuntime.getStoreConnections("ModelStore").storeConnections.size(), 1);
        Assert.assertFalse((testRuntime.getStoreConnections("ModelStore").storeConnections.get(0).connection instanceof RelationalDatabaseConnection));
    }

    @Test
    public void testRuntimeGenerationForServiceWithRuntimePointer()
    {
        String pureGrammarWithModelChainConnection =
                        "###Relational\n" +
                        "Database test::relationalDB\n" +
                        "(\n" +
                        "  Table Person\n" +
                        "  (\n" +
                        "    fullname VARCHAR(1000) PRIMARY KEY\n" +
                        "  )\n" +
                        ")\n" +
                        "\n" +
                        "\n" +
                        "###Service\n" +
                        "Service test::serviceWithRuntimePointer\n" +
                        "{\n" +
                        "  pattern: '/garprat/test/fromStudio/';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'garprat'\n" +
                        "  ];\n" +
                        "  documentation: '';\n" +
                        "  autoActivateUpdates: false;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |test::Person.all()->project([f|$f.fullName], ['fullName']);\n" +
                        "    mapping: test::relationalMapping;\n" +
                        "    runtime: test::runtimePointer;\n" +
                        "  }\n" +
                        "  test: Single\n" +
                        "  {\n" +
                        "    data: 'default\\nPerson\\nfullname\\nPierre DeBelen\\n';\n" +
                        "    asserts:\n" +
                        "    [\n" +
                        "    ];\n" +
                        "  }\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "###Pure\n" +
                        "Class test::Person\n" +
                        "{\n" +
                        "  fullName: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping test::relationalMapping\n" +
                        "(\n" +
                        "  test::Person: Relational\n" +
                        "  {\n" +
                        "    ~primaryKey\n" +
                        "    (\n" +
                        "      [test::relationalDB]Person.fullname\n" +
                        "    )\n" +
                        "    ~mainTable [test::relationalDB]Person\n" +
                        "    fullName: [test::relationalDB]Person.fullname\n" +
                        "  }\n" +
                        ")\n" +
                        "\n" +
                        "###Connection\n" +
                        "RelationalDatabaseConnection test::mySimpleConnection\n" +
                        "{\n" +
                        "  store: test::relationalDB;\n" +
                        "  type: MemSQL;\n" +
                        "  specification: Static\n" +
                        "  {\n" +
                        "    name: 'memsql_person_data';\n" +
                        "    host: 'myserver_url';\n" +
                        "    port: 80;\n" +
                        "  };\n" +
                        "  auth: DefaultH2;\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "###Runtime\n" +
                        "Runtime test::runtimePointer\n" +
                        "{\n" +
                        "  mappings:\n" +
                        "  [\n" +
                        "     test::relationalMapping\n" +
                        "  ];\n" +
                        "  connections:\n" +
                        "  [\n" +
                        "    test::relationalDB:\n" +
                        "    [\n" +
                        "      connection_1: test::mySimpleConnection\n" +
                        "    ]\n" +
                        "  ];\n" +
                        "}\n";
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(pureGrammarWithModelChainConnection);
        PureModel pureModel = new PureModel(contextData, null, DeploymentMode.TEST);
        Service service = contextData.getElementsOfType(Service.class).get(0);
        EngineRuntime testRuntime = (EngineRuntime) ServiceTestGenerationHelper.buildSingleExecutionTestRuntime((PureSingleExecution) service.execution, (SingleExecutionTest) service.test, contextData, pureModel);
        Assert.assertEquals(testRuntime.connections.size(), 1);
    }
}
