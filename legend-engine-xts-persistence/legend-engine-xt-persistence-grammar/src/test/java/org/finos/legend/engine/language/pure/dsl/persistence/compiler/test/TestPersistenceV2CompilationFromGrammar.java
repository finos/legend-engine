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

package org.finos.legend.engine.language.pure.dsl.persistence.compiler.test;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_path_Path_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_Persistence;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceTest;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceTestBatch;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_DatasetType;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_Delta;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_Snapshot;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_actionindicator_ActionIndicatorFields;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_actionindicator_DeleteIndicatorForGraphFetch;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_actionindicator_DeleteIndicatorForTds;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_deduplication_AnyVersion;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_deduplication_Deduplication;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_deduplication_MaxVersionForGraphFetch;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_deduplication_MaxVersionForTds;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_deduplication_NoDeduplication;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_emptyhandling_DeleteTargetData;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_partitioning_FieldBasedForTds;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_partitioning_NoPartitioning;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_dataset_partitioning_Partitioning;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_notifier_Notifier;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_notifier_Notifyee;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_service_GraphFetchServiceOutput;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_service_ServiceOutput;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_service_TdsServiceOutput;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_target_PersistenceTarget;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_trigger_ManualTrigger;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_trigger_Trigger;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestPersistenceV2CompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
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
            "    TDS\n" +
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
            "    {\n" +
            "    }\n" +
            "  ];\n" +
            "}\n";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [4:1-36:1]: Duplicated element 'test::MyPersistence'";
    }

    @Test
    public void serviceUndefined()
    {
        test("###Persistence\n" +
            "Persistence test::TestPersistence\n" +
            "{\n" +
            "  doc: 'This is test documentation.';\n" +
            "  trigger: Manual;\n" +
            "  service: test::Service;\n" +
            "  serviceOutputTargets:\n" +
            "  [\n" +
            "    TDS\n" +
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
            "    {\n" +
            "    }\n" +
            "  ];" +
            "}\n", "COMPILATION error at [2:1-33:5]: Service 'test::Service' is not defined");
    }

    @Test
    public void snapshotTds()
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
            "###Persistence\n" +
            "Persistence test::TestPersistence\n" +
            "{\n" +
            "  doc: 'This is test documentation.';\n" +
            "  trigger: Manual;\n" +
            "  service: test::Service;\n" +
            "  serviceOutputTargets:\n" +
            "  [\n" +
            "    TDS\n" +
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
            "    {\n" +
            "    }\n" +
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

        // persistence
        PackageableElement packageableElement = model.getPackageableElement("test::TestPersistence");
        assertNotNull(packageableElement);
        assertTrue(packageableElement instanceof Root_meta_pure_persistence_metamodel_Persistence);

        // documentation
        Root_meta_pure_persistence_metamodel_Persistence persistence = (Root_meta_pure_persistence_metamodel_Persistence) packageableElement;
        assertEquals("This is test documentation.", persistence._documentation());

        // trigger
        Root_meta_pure_persistence_metamodel_trigger_Trigger trigger = persistence._trigger();
        assertNotNull(trigger);
        assertTrue(trigger instanceof Root_meta_pure_persistence_metamodel_trigger_ManualTrigger);

        // service
        Root_meta_legend_service_metamodel_Service service = persistence._service();
        assertNotNull(service);
        assertEquals("Service", service._name());

        // serviceOutputTarget
        RichIterable<? extends Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget> serviceServiceOutputTargets = persistence._serviceOutputTargets();
        assertNotNull(serviceServiceOutputTargets);
        assertFalse(serviceServiceOutputTargets.isEmpty());

        // serviceOutput
        Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget serviceServiceOutputTarget = serviceServiceOutputTargets.getAny();
        Root_meta_pure_persistence_metamodel_service_ServiceOutput serviceOutput = serviceServiceOutputTarget._serviceOutput();
        assertTrue(serviceOutput instanceof Root_meta_pure_persistence_metamodel_service_TdsServiceOutput);
        Root_meta_pure_persistence_metamodel_service_TdsServiceOutput tdsServiceOutput = (Root_meta_pure_persistence_metamodel_service_TdsServiceOutput) serviceOutput;

        // target
        Root_meta_pure_persistence_metamodel_target_PersistenceTarget target = serviceServiceOutputTarget._target();
        assertNull(target);

        // datasetKeys
        assertArrayEquals(Lists.mutable.of("foo", "bar").toArray(), tdsServiceOutput._keys().toArray());

        // deduplication
        Root_meta_pure_persistence_metamodel_dataset_deduplication_Deduplication deduplication = tdsServiceOutput._deduplication();
        assertTrue(deduplication instanceof Root_meta_pure_persistence_metamodel_dataset_deduplication_MaxVersionForTds);
        Root_meta_pure_persistence_metamodel_dataset_deduplication_MaxVersionForTds maxVersionForTds = (Root_meta_pure_persistence_metamodel_dataset_deduplication_MaxVersionForTds) deduplication;
        assertEquals("version", maxVersionForTds._versionField());

        // datasetType
        Root_meta_pure_persistence_metamodel_dataset_DatasetType datasetType = tdsServiceOutput._datasetType();
        assertTrue(datasetType instanceof Root_meta_pure_persistence_metamodel_dataset_Snapshot);
        Root_meta_pure_persistence_metamodel_dataset_Snapshot snapshot = (Root_meta_pure_persistence_metamodel_dataset_Snapshot) datasetType;

        // partitioning
        Root_meta_pure_persistence_metamodel_dataset_partitioning_Partitioning partitioning = snapshot._partitioning();
        assertTrue(partitioning instanceof Root_meta_pure_persistence_metamodel_dataset_partitioning_FieldBasedForTds);
        Root_meta_pure_persistence_metamodel_dataset_partitioning_FieldBasedForTds fieldBasedForTds = (Root_meta_pure_persistence_metamodel_dataset_partitioning_FieldBasedForTds) partitioning;
        assertArrayEquals(Lists.mutable.of("foo1", "bar2").toArray(), fieldBasedForTds._partitionFields().toArray());

        // notifier
        Root_meta_pure_persistence_metamodel_notifier_Notifier notifier = persistence._notifier();
        assertNotNull(notifier);
        List<? extends Root_meta_pure_persistence_metamodel_notifier_Notifyee> notifyees = notifier._notifyees().toList();
        assertEquals(0, notifyees.size());

        // tests
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test> persistenceTests = persistence._tests();
        assertNotNull(persistenceTests);
        assertFalse(persistenceTests.isEmpty());
        assertTrue(persistenceTests.getAny() instanceof Root_meta_pure_persistence_metamodel_PersistenceTest);
        Root_meta_pure_persistence_metamodel_PersistenceTest test = (Root_meta_pure_persistence_metamodel_PersistenceTest) persistenceTests.getAny();
        assertNotNull(test._isTestDataFromServiceOutput());
        assertFalse(test._isTestDataFromServiceOutput());
        assertFalse(test._testBatches().isEmpty());
        assertTrue(test._testBatches().getAny() instanceof Root_meta_pure_persistence_metamodel_PersistenceTestBatch);
        Root_meta_pure_persistence_metamodel_PersistenceTestBatch testBatch = test._testBatches().getAny();
        assertNotNull(testBatch._testData());
        assertNotNull(testBatch._assertions());
        assertFalse(testBatch._assertions().isEmpty());
    }

    @Test
    public void deltaTds()
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
                "###Persistence\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  serviceOutputTargets:\n" +
                "  [\n" +
                "    TDS\n" +
                "    {\n" +
                "      keys:\n" +
                "      [\n" +
                "        foo, bar\n" +
                "      ]\n" +
                "      datasetType: Delta\n" +
                "      {\n" +
                "        actionIndicator: DeleteIndicator\n" +
                "        {\n" +
                "          deleteField: toBeDeleted;\n" +
                "          deleteValues: ['Yes', 'true'];\n" +
                "        }\n" +
                "      }\n" +
                "      deduplication: AnyVersion;\n" +
                "    }\n" +
                "    ->\n" +
                "    {\n" +
                "    }\n" +
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

        // persistence
        PackageableElement packageableElement = model.getPackageableElement("test::TestPersistence");
        assertNotNull(packageableElement);
        assertTrue(packageableElement instanceof Root_meta_pure_persistence_metamodel_Persistence);

        // documentation
        Root_meta_pure_persistence_metamodel_Persistence persistence = (Root_meta_pure_persistence_metamodel_Persistence) packageableElement;
        assertEquals("This is test documentation.", persistence._documentation());

        // trigger
        Root_meta_pure_persistence_metamodel_trigger_Trigger trigger = persistence._trigger();
        assertNotNull(trigger);
        assertTrue(trigger instanceof Root_meta_pure_persistence_metamodel_trigger_ManualTrigger);

        // service
        Root_meta_legend_service_metamodel_Service service = persistence._service();
        assertNotNull(service);
        assertEquals("Service", service._name());

        // serviceOutputTarget
        RichIterable<? extends Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget> serviceServiceOutputTargets = persistence._serviceOutputTargets();
        assertNotNull(serviceServiceOutputTargets);
        assertFalse(serviceServiceOutputTargets.isEmpty());

        // serviceOutput
        Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget serviceServiceOutputTarget = serviceServiceOutputTargets.getAny();
        Root_meta_pure_persistence_metamodel_service_ServiceOutput serviceOutput = serviceServiceOutputTarget._serviceOutput();
        assertTrue(serviceOutput instanceof Root_meta_pure_persistence_metamodel_service_TdsServiceOutput);
        Root_meta_pure_persistence_metamodel_service_TdsServiceOutput tdsServiceOutput = (Root_meta_pure_persistence_metamodel_service_TdsServiceOutput) serviceOutput;

        // target
        Root_meta_pure_persistence_metamodel_target_PersistenceTarget target = serviceServiceOutputTarget._target();
        assertNull(target);

        // datasetKeys
        assertArrayEquals(Lists.mutable.of("foo", "bar").toArray(), tdsServiceOutput._keys().toArray());

        // deduplication
        Root_meta_pure_persistence_metamodel_dataset_deduplication_Deduplication deduplication = tdsServiceOutput._deduplication();
        assertTrue(deduplication instanceof Root_meta_pure_persistence_metamodel_dataset_deduplication_AnyVersion);

        // datasetType
        Root_meta_pure_persistence_metamodel_dataset_DatasetType datasetType = tdsServiceOutput._datasetType();
        assertTrue(datasetType instanceof Root_meta_pure_persistence_metamodel_dataset_Delta);
        Root_meta_pure_persistence_metamodel_dataset_Delta delta = (Root_meta_pure_persistence_metamodel_dataset_Delta) datasetType;

        // action indicator
        Root_meta_pure_persistence_metamodel_dataset_actionindicator_ActionIndicatorFields actionIndicator = delta._actionIndicator();
        assertTrue(actionIndicator instanceof Root_meta_pure_persistence_metamodel_dataset_actionindicator_DeleteIndicatorForTds);
        Root_meta_pure_persistence_metamodel_dataset_actionindicator_DeleteIndicatorForTds deleteIndicatorForTds = (Root_meta_pure_persistence_metamodel_dataset_actionindicator_DeleteIndicatorForTds) actionIndicator;
        assertEquals("toBeDeleted", deleteIndicatorForTds._deleteField());
        assertArrayEquals(Lists.mutable.of("Yes", "true").toArray(), deleteIndicatorForTds._deleteValues().toArray());

        // notifier
        Root_meta_pure_persistence_metamodel_notifier_Notifier notifier = persistence._notifier();
        assertNotNull(notifier);
        List<? extends Root_meta_pure_persistence_metamodel_notifier_Notifyee> notifyees = notifier._notifyees().toList();
        assertEquals(0, notifyees.size());

        // tests
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test> persistenceTests = persistence._tests();
        assertNotNull(persistenceTests);
        assertFalse(persistenceTests.isEmpty());
        assertTrue(persistenceTests.getAny() instanceof Root_meta_pure_persistence_metamodel_PersistenceTest);
        Root_meta_pure_persistence_metamodel_PersistenceTest test = (Root_meta_pure_persistence_metamodel_PersistenceTest) persistenceTests.getAny();
        assertNotNull(test._isTestDataFromServiceOutput());
        assertFalse(test._isTestDataFromServiceOutput());
        assertFalse(test._testBatches().isEmpty());
        assertTrue(test._testBatches().getAny() instanceof Root_meta_pure_persistence_metamodel_PersistenceTestBatch);
        Root_meta_pure_persistence_metamodel_PersistenceTestBatch testBatch = test._testBatches().getAny();
        assertNotNull(testBatch._testData());
        assertNotNull(testBatch._assertions());
        assertFalse(testBatch._assertions().isEmpty());
    }

    @Test
    public void snapshotGraphFetch()
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
                "  version: Integer[1];\n" +
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
                "        #/test::model::Firm/employees/firstName#, #/test::model::Firm/employees/lastName#\n" +
                "      ]\n" +
                "      datasetType: Snapshot\n" +
                "      {\n" +
                "        partitioning: None\n" +
                "        {\n" +
                "          emptyDatasetHandling: DeleteTargetData;\n" +
                "        }\n" +
                "      }\n" +
                "      deduplication: MaxVersion\n" +
                "      {\n" +
                "        versionField: #/test::model::Firm/employees/version#;\n" +
                "      }\n" +
                "    }\n" +
                "    ->\n" +
                "    {\n" +
                "    }\n" +
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

        // persistence
        PackageableElement packageableElement = model.getPackageableElement("test::TestPersistence");
        assertNotNull(packageableElement);
        assertTrue(packageableElement instanceof Root_meta_pure_persistence_metamodel_Persistence);

        // documentation
        Root_meta_pure_persistence_metamodel_Persistence persistence = (Root_meta_pure_persistence_metamodel_Persistence) packageableElement;
        assertEquals("This is test documentation.", persistence._documentation());

        // trigger
        Root_meta_pure_persistence_metamodel_trigger_Trigger trigger = persistence._trigger();
        assertNotNull(trigger);
        assertTrue(trigger instanceof Root_meta_pure_persistence_metamodel_trigger_ManualTrigger);

        // service
        Root_meta_legend_service_metamodel_Service service = persistence._service();
        assertNotNull(service);
        assertEquals("FirmService", service._name());

        // serviceOutputTarget
        RichIterable<? extends Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget> serviceServiceOutputTargets = persistence._serviceOutputTargets();
        assertNotNull(serviceServiceOutputTargets);
        assertFalse(serviceServiceOutputTargets.isEmpty());

        // serviceOutput
        Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget serviceServiceOutputTarget = serviceServiceOutputTargets.getAny();
        Root_meta_pure_persistence_metamodel_service_ServiceOutput serviceOutput = serviceServiceOutputTarget._serviceOutput();
        assertTrue(serviceOutput instanceof Root_meta_pure_persistence_metamodel_service_GraphFetchServiceOutput);
        Root_meta_pure_persistence_metamodel_service_GraphFetchServiceOutput graphFetchServiceOutput = (Root_meta_pure_persistence_metamodel_service_GraphFetchServiceOutput) serviceOutput;

        // target
        Root_meta_pure_persistence_metamodel_target_PersistenceTarget target = serviceServiceOutputTarget._target();
        assertNull(target);

        // path
        assertNotNull(graphFetchServiceOutput._path());
        assertTrue(graphFetchServiceOutput._path() instanceof Root_meta_pure_metamodel_path_Path_Impl);

        // datasetKeys
        assertNotNull(graphFetchServiceOutput._keys());
        assertEquals(2, graphFetchServiceOutput._keys().size());
        assertTrue(graphFetchServiceOutput._keys().getAny() instanceof Root_meta_pure_metamodel_path_Path_Impl);

        // deduplication
        Root_meta_pure_persistence_metamodel_dataset_deduplication_Deduplication deduplication = graphFetchServiceOutput._deduplication();
        assertTrue(deduplication instanceof Root_meta_pure_persistence_metamodel_dataset_deduplication_MaxVersionForGraphFetch);
        Root_meta_pure_persistence_metamodel_dataset_deduplication_MaxVersionForGraphFetch maxVersionForGraphFetch = (Root_meta_pure_persistence_metamodel_dataset_deduplication_MaxVersionForGraphFetch) deduplication;
        assertNotNull(maxVersionForGraphFetch._versionFieldPath());
        assertTrue(maxVersionForGraphFetch._versionFieldPath() instanceof Root_meta_pure_metamodel_path_Path_Impl);

        // datasetType
        Root_meta_pure_persistence_metamodel_dataset_DatasetType datasetType = graphFetchServiceOutput._datasetType();
        assertTrue(datasetType instanceof Root_meta_pure_persistence_metamodel_dataset_Snapshot);
        Root_meta_pure_persistence_metamodel_dataset_Snapshot snapshot = (Root_meta_pure_persistence_metamodel_dataset_Snapshot) datasetType;

        // partitioning
        Root_meta_pure_persistence_metamodel_dataset_partitioning_Partitioning partitioning = snapshot._partitioning();
        assertTrue(partitioning instanceof Root_meta_pure_persistence_metamodel_dataset_partitioning_NoPartitioning);
        Root_meta_pure_persistence_metamodel_dataset_partitioning_NoPartitioning noPartitioning = (Root_meta_pure_persistence_metamodel_dataset_partitioning_NoPartitioning) partitioning;
        assertTrue(noPartitioning._emptyDatasetHandling() instanceof Root_meta_pure_persistence_metamodel_dataset_emptyhandling_DeleteTargetData);

        // notifier
        Root_meta_pure_persistence_metamodel_notifier_Notifier notifier = persistence._notifier();
        assertNotNull(notifier);
        List<? extends Root_meta_pure_persistence_metamodel_notifier_Notifyee> notifyees = notifier._notifyees().toList();
        assertEquals(0, notifyees.size());

        // tests
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test> persistenceTests = persistence._tests();
        assertNotNull(persistenceTests);
        assertFalse(persistenceTests.isEmpty());
        assertTrue(persistenceTests.getAny() instanceof Root_meta_pure_persistence_metamodel_PersistenceTest);
        Root_meta_pure_persistence_metamodel_PersistenceTest test = (Root_meta_pure_persistence_metamodel_PersistenceTest) persistenceTests.getAny();
        assertNotNull(test._isTestDataFromServiceOutput());
        assertFalse(test._isTestDataFromServiceOutput());
        assertFalse(test._testBatches().isEmpty());
        assertTrue(test._testBatches().getAny() instanceof Root_meta_pure_persistence_metamodel_PersistenceTestBatch);
        Root_meta_pure_persistence_metamodel_PersistenceTestBatch testBatch = test._testBatches().getAny();
        assertNotNull(testBatch._testData());
        assertNotNull(testBatch._assertions());
        assertFalse(testBatch._assertions().isEmpty());
    }

    @Test
    public void deltaGraphFetch()
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
                "    {\n" +
                "    }\n" +
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

        // persistence
        PackageableElement packageableElement = model.getPackageableElement("test::TestPersistence");
        assertNotNull(packageableElement);
        assertTrue(packageableElement instanceof Root_meta_pure_persistence_metamodel_Persistence);

        // documentation
        Root_meta_pure_persistence_metamodel_Persistence persistence = (Root_meta_pure_persistence_metamodel_Persistence) packageableElement;
        assertEquals("This is test documentation.", persistence._documentation());

        // trigger
        Root_meta_pure_persistence_metamodel_trigger_Trigger trigger = persistence._trigger();
        assertNotNull(trigger);
        assertTrue(trigger instanceof Root_meta_pure_persistence_metamodel_trigger_ManualTrigger);

        // service
        Root_meta_legend_service_metamodel_Service service = persistence._service();
        assertNotNull(service);
        assertEquals("FirmService", service._name());

        // serviceOutputTarget
        RichIterable<? extends Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget> serviceServiceOutputTargets = persistence._serviceOutputTargets();
        assertNotNull(serviceServiceOutputTargets);
        assertFalse(serviceServiceOutputTargets.isEmpty());

        // serviceOutput
        Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget serviceServiceOutputTarget = serviceServiceOutputTargets.getAny();
        Root_meta_pure_persistence_metamodel_service_ServiceOutput serviceOutput = serviceServiceOutputTarget._serviceOutput();
        assertTrue(serviceOutput instanceof Root_meta_pure_persistence_metamodel_service_GraphFetchServiceOutput);
        Root_meta_pure_persistence_metamodel_service_GraphFetchServiceOutput graphFetchServiceOutput = (Root_meta_pure_persistence_metamodel_service_GraphFetchServiceOutput) serviceOutput;

        // target
        Root_meta_pure_persistence_metamodel_target_PersistenceTarget target = serviceServiceOutputTarget._target();
        assertNull(target);

        // path
        assertNotNull(graphFetchServiceOutput._path());
        assertTrue(graphFetchServiceOutput._path() instanceof Root_meta_pure_metamodel_path_Path_Impl);

        // datasetKeys
        assertNotNull(graphFetchServiceOutput._keys());
        assertEquals(1, graphFetchServiceOutput._keys().size());
        assertTrue(graphFetchServiceOutput._keys().getAny() instanceof Root_meta_pure_metamodel_path_Path_Impl);

        // deduplication
        Root_meta_pure_persistence_metamodel_dataset_deduplication_Deduplication deduplication = graphFetchServiceOutput._deduplication();
        assertTrue(deduplication instanceof Root_meta_pure_persistence_metamodel_dataset_deduplication_NoDeduplication);

        // datasetType
        Root_meta_pure_persistence_metamodel_dataset_DatasetType datasetType = graphFetchServiceOutput._datasetType();
        assertTrue(datasetType instanceof Root_meta_pure_persistence_metamodel_dataset_Delta);
        Root_meta_pure_persistence_metamodel_dataset_Delta delta = (Root_meta_pure_persistence_metamodel_dataset_Delta) datasetType;

        // action indicator
        Root_meta_pure_persistence_metamodel_dataset_actionindicator_ActionIndicatorFields actionIndicator = delta._actionIndicator();
        assertTrue(actionIndicator instanceof Root_meta_pure_persistence_metamodel_dataset_actionindicator_DeleteIndicatorForGraphFetch);
        Root_meta_pure_persistence_metamodel_dataset_actionindicator_DeleteIndicatorForGraphFetch deleteIndicatorForGraphFetch = (Root_meta_pure_persistence_metamodel_dataset_actionindicator_DeleteIndicatorForGraphFetch) actionIndicator;
        assertNotNull(deleteIndicatorForGraphFetch._deleteFieldPath());
        assertTrue(deleteIndicatorForGraphFetch._deleteFieldPath() instanceof Root_meta_pure_metamodel_path_Path_Impl);
        assertArrayEquals(Lists.mutable.of("Yes", "true", "1").toArray(), deleteIndicatorForGraphFetch._deleteValues().toArray());

        // notifier
        Root_meta_pure_persistence_metamodel_notifier_Notifier notifier = persistence._notifier();
        assertNotNull(notifier);
        List<? extends Root_meta_pure_persistence_metamodel_notifier_Notifyee> notifyees = notifier._notifyees().toList();
        assertEquals(0, notifyees.size());

        // tests
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test> persistenceTests = persistence._tests();
        assertNotNull(persistenceTests);
        assertFalse(persistenceTests.isEmpty());
        assertTrue(persistenceTests.getAny() instanceof Root_meta_pure_persistence_metamodel_PersistenceTest);
        Root_meta_pure_persistence_metamodel_PersistenceTest test = (Root_meta_pure_persistence_metamodel_PersistenceTest) persistenceTests.getAny();
        assertNotNull(test._isTestDataFromServiceOutput());
        assertFalse(test._isTestDataFromServiceOutput());
        assertFalse(test._testBatches().isEmpty());
        assertTrue(test._testBatches().getAny() instanceof Root_meta_pure_persistence_metamodel_PersistenceTestBatch);
        Root_meta_pure_persistence_metamodel_PersistenceTestBatch testBatch = test._testBatches().getAny();
        assertNotNull(testBatch._testData());
        assertNotNull(testBatch._assertions());
        assertFalse(testBatch._assertions().isEmpty());
    }
}
