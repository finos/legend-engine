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

package org.finos.legend.engine.language.pure.dsl.persistence.compiler.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_RelationalDatabaseConnection;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_Persistence;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceContext;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_service_ServiceParameter;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestPersistenceContextCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    protected String getDuplicatedElementTestCode()
    {
        return "Class test::TestPersistenceContext {}\n" +
                "\n" +
                "###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "}\n";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-8:1]: Duplicated element 'test::TestPersistenceContext'";
    }

    @Test
    public void persistenceUndefined()
    {
        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "}\n", "COMPILATION error at [3:1-6:1]: Persistence 'test::TestPersistence' is not defined");
    }

    @Test
    public void serviceParameterConnectionUndefined()
    {
        test("Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::ServiceResult\n" +
                "{\n" +
                "   deleted: String[1];\n" +
                "   dateTimeIn: String[1];\n" +
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
                "    query: src: test::Person[1]|$src.name;\n" +
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
                "\n" +
                "PersistenceContext test::TestPersistenceContext \n" +
                "\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  serviceParameters:\n" +
                "  [\n" +
                "    con=test::TestConnection\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: Relational\n" +
                "    {\n" +
                "      database: test::Database;" +
                "    }\n" +
                "    ingestMode: UnitemporalDelta\n" +
                "    {\n" +
                "      mergeStrategy: NoDeletes;\n" +
                "      transactionMilestoning: DateTime\n" +
                "      {\n" +
                "        dateTimeInName: 'IN_Z';\n" +
                "        dateTimeOutName: 'OUT_Z';\n" +
                "        derivation: SourceSpecifiesInDateTime\n" +
                "        {\n" +
                "          sourceDateTimeInField: 'dateTimeIn';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ServiceResult;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [54:9-28]: Can't find connection 'test::TestConnection'");
    }

    @Test
    public void sinkConnectionUndefined()
    {
        test("Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::ServiceResult\n" +
                "{\n" +
                "   deleted: String[1];\n" +
                "   dateTimeIn: String[1];\n" +
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
                "    query: src: test::Person[1]|$src.name;\n" +
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
                "\n" +
                "PersistenceContext test::TestPersistenceContext \n" +
                "\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  sinkConnection: test::TestConnection;\n" +
                "}\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: Relational\n" +
                "    {\n" +
                "      database: test::Database;" +
                "    }\n" +
                "    ingestMode: UnitemporalDelta\n" +
                "    {\n" +
                "      mergeStrategy: NoDeletes;\n" +
                "      transactionMilestoning: DateTime\n" +
                "      {\n" +
                "        dateTimeInName: 'IN_Z';\n" +
                "        dateTimeOutName: 'OUT_Z';\n" +
                "        derivation: SourceSpecifiesInDateTime\n" +
                "        {\n" +
                "          sourceDateTimeInField: 'dateTimeIn';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ServiceResult;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [52:19-38]: Can't find connection 'test::TestConnection'");
    }

    @Test
    public void persistenceContextSinkConnectionPointer()
    {
        Pair<PureModelContextData, PureModel> result = test("Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::ServiceResult\n" +
                "{\n" +
                "   deleted: String[1];\n" +
                "   dateTimeIn: String[1];\n" +
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
                "    query: src: test::Person[1]|$src.name;\n" +
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
                "###Connection" +
                "\n" +
                "RelationalDatabaseConnection test::ServiceConnection\n" +
                "{\n" +
                "  store: test::TestDatabase;\n" +
                "  type: Snowflake;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "  };\n" +
                "  auth: Test;\n" +
                "}\n" +
                "\n" +
                "RelationalDatabaseConnection test::SinkConnection\n" +
                "{\n" +
                "  store: test::TestDatabase;\n" +
                "  type: MemSQL;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "  };\n" +
                "  auth: Test;\n" +
                "}\n" +
                "\n" +
                "###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext \n" +
                "\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  serviceParameters:\n" +
                "  [\n" +
                "    foo='hello',\n" +
                "    bar=1,\n" +
                "    con1=test::ServiceConnection,\n" +
                "    con2=\n" +
                "    #{\n" +
                "      RelationalDatabaseConnection\n" +
                "      {\n" +
                "        store: test::TestDatabase;\n" +
                "        type: H2;\n" +
                "        specification: LocalH2\n" +
                "        {\n" +
                "        };\n" +
                "        auth: Test;\n" +
                "      }\n" +
                "    }#\n" +
                "  ];\n" +
                "  sinkConnection: test::SinkConnection;\n" +
                "}\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: Relational\n" +
                "    {\n" +
                "      database: test::Database;" +
                "    }\n" +
                "    ingestMode: UnitemporalDelta\n" +
                "    {\n" +
                "      mergeStrategy: NoDeletes;\n" +
                "      transactionMilestoning: DateTime\n" +
                "      {\n" +
                "        dateTimeInName: 'IN_Z';\n" +
                "        dateTimeOutName: 'OUT_Z';\n" +
                "        derivation: SourceSpecifiesInDateTime\n" +
                "        {\n" +
                "          sourceDateTimeInField: 'dateTimeIn';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ServiceResult;\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        PureModel model = result.getTwo();

        // persistence context
        PackageableElement packageableElement = model.getPackageableElement("test::TestPersistenceContext");
        assertNotNull(packageableElement);
        assertTrue(packageableElement instanceof Root_meta_pure_persistence_metamodel_PersistenceContext);

        Root_meta_pure_persistence_metamodel_PersistenceContext context = (Root_meta_pure_persistence_metamodel_PersistenceContext) packageableElement;

        Root_meta_pure_persistence_metamodel_Persistence persistence = context._persistence();
        assertNotNull(persistence);
        assertEquals("test", persistence._package()._name());
        assertEquals("TestPersistence", persistence._name());

        List<? extends Root_meta_pure_persistence_metamodel_service_ServiceParameter> serviceParameters = context._serviceParameters().toList();
        assertEquals(4, serviceParameters.size());

        Root_meta_pure_persistence_metamodel_service_ServiceParameter serviceParameter1 = serviceParameters.get(0);
        assertEquals("foo", serviceParameter1._name());
        List<?> values1 = serviceParameter1._value().toList();
        assertEquals(1, values1.size());
        assertEquals("hello", values1.get(0));

        Root_meta_pure_persistence_metamodel_service_ServiceParameter serviceParameter2 = serviceParameters.get(1);
        assertEquals("bar", serviceParameter2._name());
        List<?> values2 = serviceParameter2._value().toList();
        assertEquals(1, values2.size());
        assertEquals(1L, values2.get(0));

        Root_meta_pure_persistence_metamodel_service_ServiceParameter serviceParameter3 = serviceParameters.get(2);
        assertEquals("con1", serviceParameter3._name());
        List<?> values3 = serviceParameter3._value().toList();
        assertEquals(1, values3.size());
        Object o3 = values3.get(0);
        assertTrue(o3 instanceof Root_meta_pure_alloy_connections_RelationalDatabaseConnection);
        Root_meta_pure_alloy_connections_RelationalDatabaseConnection con1 = (Root_meta_pure_alloy_connections_RelationalDatabaseConnection) o3;
        assertEquals("Snowflake", con1._type()._name());

        Root_meta_pure_persistence_metamodel_service_ServiceParameter serviceParameter4 = serviceParameters.get(3);
        assertEquals("con2", serviceParameter4._name());
        List<?> values4 = serviceParameter4._value().toList();
        assertEquals(1, values4.size());
        Object o4 = values4.get(0);
        assertTrue(o4 instanceof Root_meta_pure_alloy_connections_RelationalDatabaseConnection);
        Root_meta_pure_alloy_connections_RelationalDatabaseConnection con2 = (Root_meta_pure_alloy_connections_RelationalDatabaseConnection) o4;
        assertEquals("H2", con2._type()._name());

        Connection connection = context._sinkConnection();
        assertNotNull(connection);
        assertTrue(connection instanceof Root_meta_pure_alloy_connections_RelationalDatabaseConnection);
        Root_meta_pure_alloy_connections_RelationalDatabaseConnection sinkConnection = (Root_meta_pure_alloy_connections_RelationalDatabaseConnection) connection;
        assertEquals("MemSQL", sinkConnection._type()._name());
    }

    @Test
    public void persistenceContextSinkConnectionEmbedded()
    {
        Pair<PureModelContextData, PureModel> result = test("Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::ServiceResult\n" +
                "{\n" +
                "   deleted: String[1];\n" +
                "   dateTimeIn: String[1];\n" +
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
                "    query: src: test::Person[1]|$src.name;\n" +
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
                "###Connection" +
                "\n" +
                "RelationalDatabaseConnection test::TestConnection\n" +
                "{\n" +
                "  store: test::TestDatabase;\n" +
                "  type: Snowflake;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "  };\n" +
                "  auth: Test;\n" +
                "}\n" +
                "\n" +
                "###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext \n" +
                "\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  serviceParameters:\n" +
                "  [\n" +
                "    foo='hello',\n" +
                "    bar=1,\n" +
                "    con1=test::TestConnection,\n" +
                "    con2=\n" +
                "    #{\n" +
                "      RelationalDatabaseConnection\n" +
                "      {\n" +
                "        store: test::TestDatabase;\n" +
                "        type: H2;\n" +
                "        specification: LocalH2\n" +
                "        {\n" +
                "        };\n" +
                "        auth: Test;\n" +
                "      }\n" +
                "    }#\n" +
                "  ];\n" +
                "  sinkConnection:\n" +
                "  #{\n" +
                "    RelationalDatabaseConnection\n" +
                "    {\n" +
                "      store: test::TestDatabase;\n" +
                "      type: MemSQL;\n" +
                "      specification: LocalH2\n" +
                "      {\n" +
                "      };\n" +
                "      auth: Test;\n" +
                "    }\n" +
                "  }#;\n" +
                "}\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: Relational\n" +
                "    {\n" +
                "      database: test::Database;" +
                "    }\n" +
                "    ingestMode: UnitemporalDelta\n" +
                "    {\n" +
                "      mergeStrategy: NoDeletes;\n" +
                "      transactionMilestoning: DateTime\n" +
                "      {\n" +
                "        dateTimeInName: 'IN_Z';\n" +
                "        dateTimeOutName: 'OUT_Z';\n" +
                "        derivation: SourceSpecifiesInDateTime\n" +
                "        {\n" +
                "          sourceDateTimeInField: 'dateTimeIn';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ServiceResult;\n" +
                "    }\n" +
                "  }\n" +
                "}\n");

        PureModel model = result.getTwo();

        // persistence context
        PackageableElement packageableElement = model.getPackageableElement("test::TestPersistenceContext");
        assertNotNull(packageableElement);
        assertTrue(packageableElement instanceof Root_meta_pure_persistence_metamodel_PersistenceContext);

        Root_meta_pure_persistence_metamodel_PersistenceContext context = (Root_meta_pure_persistence_metamodel_PersistenceContext) packageableElement;

        Connection connection = context._sinkConnection();
        assertNotNull(connection);
        assertTrue(connection instanceof Root_meta_pure_alloy_connections_RelationalDatabaseConnection);
        Root_meta_pure_alloy_connections_RelationalDatabaseConnection sinkConnection = (Root_meta_pure_alloy_connections_RelationalDatabaseConnection) connection;
        assertEquals("MemSQL", sinkConnection._type()._name());
    }
}
