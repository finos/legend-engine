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

package org.finos.legend.engine.pure.dsl.persistence.cloud.compiler.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_external_persistence_aws_metamodel_AwsGluePersistencePlatform;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceContext;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_context_PersistencePlatform;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestPersistenceCloudCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
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
                "  platform: AwsGlue\n" +
                "  #{\n" +
                "    dataProcessingUnits: 10;\n" +
                "  }#;\n" +
                "}\n";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-12:1]: Duplicated element 'test::TestPersistenceContext'";
    }

    @Test
    public void persistencePlatformDataProcessingUnits()
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
                "Mapping test::Mapping (" +
                "  *test::Person: Relational\n" +
                "  {\n" +
                "    ~primaryKey\n" +
                "    (\n" +
                "      [test::TestDatabase] personTable.ID\n" +
                "    )\n" +
                "    ~mainTable [test::TestDatabase] personTable\n" +
                "    name: [test::TestDatabase] personTable.NAME\n" +
                "  }" +
                ")\n" +
                "\n" +
                "###Service\n" +
                "Service test::Service \n" +
                "{\n" +
                "  pattern : 'test';\n" +
                "  documentation : 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: test::Person.all()->project([x | $x.name], ['Name']);\n" +
                "    mapping: test::Mapping;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "      mappings: [test::Mapping];" +
                "      connections: [" +
                "        test::TestDatabase: [connection1: test::ServiceConnection]\n" +
                "      ];\n" +
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
                "Database test::TestDatabase\n" +
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
                "  platform: AwsGlue\n" +
                "  #{\n" +
                "    dataProcessingUnits: 10;\n" +
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
                "      database: test::TestDatabase;" +
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

        Root_meta_pure_persistence_metamodel_context_PersistencePlatform persistencePlatform = context._platform();
        assertNotNull(persistencePlatform);
        assertTrue(persistencePlatform instanceof Root_meta_external_persistence_aws_metamodel_AwsGluePersistencePlatform);

        Root_meta_external_persistence_aws_metamodel_AwsGluePersistencePlatform awsGluePlatform = (Root_meta_external_persistence_aws_metamodel_AwsGluePersistencePlatform) persistencePlatform;
        assertEquals(10, awsGluePlatform._dataProcessingUnits());
    }
}
