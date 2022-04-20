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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TestPersistenceCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    protected String getDuplicatedElementTestCode()
    {
        return "Class test::TestPersistence {}\n" +
                "\n" +
                "Class test::ModelClass {}\n" +
                "\n" +
                "###Persistence\n" +
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
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "    }\n" +
                "    ingestMode: AppendOnly\n" +
                "    {\n" +
                "      auditing: None;\n" +
                "      filterDuplicates: false;\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [7:1-28:1]: Duplicated element 'test::TestPersistence'";
    }

    @Test
    public void serviceUndefined()
    {
        test("Class test::ModelClass {}\n" +
                "\n" +
                "###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: ObjectStorage\n" +
                "    {\n" +
                "      binding: test::Binding;\n" +
                "      connection:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: test::Person;\n" +
                "          url: 'my_url2';\n" +
                "        }\n" +
                "      }#\n" +
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "    }\n" +
                "    ingestMode: AppendOnly\n" +
                "    {\n" +
                "      auditing: None;\n" +
                "      filterDuplicates: false;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [5:1-35:1]: Persistence refers to a service 'test::Service' that is not defined");
    }

    @Test
    public void bindingUndefined()
    {
        test("Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::ServiceResult {}\n" +
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
                "###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: ObjectStorage\n" +
                "    {\n" +
                "      binding: test::Binding;\n" +
                "      connection:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: test::ServiceResult;\n" +
                "          url: 'my_url2';\n" +
                "        }\n" +
                "      }#\n" +
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ServiceResult;\n" +
                "    }\n" +
                "    ingestMode: AppendOnly\n" +
                "    {\n" +
                "      auditing: None;\n" +
                "      filterDuplicates: false;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [41:11-52:5]: Binding 'test::Binding' is not defined");
    }

    @Test
    public void flatModelClassUndefined()
    {
        test("Class test::Person\n" +
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
                "###Persistence\n" +
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
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ServiceResult;\n" +
                "    }\n" +
                "    ingestMode: AppendOnly\n" +
                "    {\n" +
                "      auditing: None;\n" +
                "      filterDuplicates: false;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [32:1-53:1]: Error in 'test::TestPersistence': Can't find class 'test::ServiceResult'");
    }

    @Test
    public void multiFlatModelClassUndefined()
    {
        test("Class test::Person\n" +
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
                "###ExternalFormat\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [\n" +
                "    test::Person\n" +
                "  ];\n" +
                "}\n" +
                "###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: ObjectStorage\n" +
                "    {\n" +
                "      binding: test::Binding;\n" +
                "      connection:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: test::Person;\n" +
                "          url: 'my_url2';\n" +
                "        }\n" +
                "      }#\n" +
                "    }\n" +
                "    targetShape: MultiFlat\n" +
                "    {\n" +
                "      modelClass: test::ServiceResult;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      parts:\n" +
                "      [\n" +
                "        {\n" +
                "          modelProperty: property1;\n" +
                "          targetName: 'TestDataset1';\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "    ingestMode: AppendOnly\n" +
                "    {\n" +
                "      auditing: None;\n" +
                "      filterDuplicates: false;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [40:1-77:1]: Error in 'test::TestPersistence': Can't find class 'test::ServiceResult'");
    }

    @Test
    public void multiFlatModelPropertyUndefined()
    {
        test("Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::ServiceResult {}\n" +
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
                "###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: Relational\n" +
                "    {\n" +
                "    }\n" +
                "    targetShape: MultiFlat\n" +
                "    {\n" +
                "      modelClass: test::ServiceResult;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      parts:\n" +
                "      [\n" +
                "        {\n" +
                "          modelProperty: property1;\n" +
                "          targetName: 'TestDataset1';\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "    ingestMode: AppendOnly\n" +
                "    {\n" +
                "      auditing: None;\n" +
                "      filterDuplicates: false;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [34:1-62:1]: Error in 'test::TestPersistence': Can't find property 'property1' in class 'test::ServiceResult'");
    }

    @Test
    public void multiFlatModelPropertyInvalidType()
    {
        test("Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::ServiceResult\n" +
                "{\n" +
                "  property1: String[1];\n" +
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
                "###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: Relational\n" +
                "    {\n" +
                "      connection:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: test::Person;\n" +
                "          url: 'my_url2';\n" +
                "        }\n" +
                "      }#\n" +
                "    }\n" +
                "    targetShape: MultiFlat\n" +
                "    {\n" +
                "      modelClass: test::ServiceResult;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      parts:\n" +
                "      [\n" +
                "        {\n" +
                "          modelProperty: property1;\n" +
                "          targetName: 'TestDataset1';\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "    ingestMode: AppendOnly\n" +
                "    {\n" +
                "      auditing: None;\n" +
                "      filterDuplicates: false;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [61:9-64:9]: Target shape modelProperty 'property1' must refer to a class.");
    }

    @Test
    public void multiFlatModelPropertyUndefinedType()
    {
        test("Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::ServiceResult\n" +
                "{\n" +
                "  property1: Animal[1];\n" +
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
                "###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: Relational\n" +
                "    {\n" +
                "    }\n" +
                "    targetShape: MultiFlat\n" +
                "    {\n" +
                "      modelClass: test::ServiceResult;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      parts:\n" +
                "      [\n" +
                "        {\n" +
                "          modelProperty: property1;\n" +
                "          targetName: 'TestDataset1';\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "    ingestMode: AppendOnly\n" +
                "    {\n" +
                "      auditing: None;\n" +
                "      filterDuplicates: false;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [8:14-19]: Can't find type 'Animal'");
    }

    @Test
    public void flatDeleteIndicatorDeletePropertyUndefined()
    {
        test("Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::ServiceResult {}\n" +
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
                "###ExternalFormat\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [\n" +
                "    test::Person\n" +
                "  ];\n" +
                "}\n" +
                "###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: ObjectStorage\n" +
                "    {\n" +
                "      binding: test::Binding;\n" +
                "      connection:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: test::Person;\n" +
                "          url: 'my_url2';\n" +
                "        }\n" +
                "      }#\n" +
                "    }\n" +
                "    ingestMode: NontemporalDelta\n" +
                "    {\n" +
                "      mergeStrategy: DeleteIndicator\n" +
                "      {\n" +
                "        deleteField: deleted;\n" +
                "        deleteValues: ['Y', 'true'];\n" +
                "      }\n" +
                "      auditing: None;\n" +
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ServiceResult;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [42:1-76:1]: Error in 'test::TestPersistence': Property 'deleted' must exist in class 'test::ServiceResult'");
    }

    @Test
    public void multiFlatDeleteIndicatorDeletePropertyUndefined()
    {
        test("import org::dxl::*;\n" +
                "\n" +
                "Class org::dxl::Zoo\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  zookeeper: Person[1];\n" +
                "  owner: Person[1];\n" +
                "}\n" +
                "\n" +
                "Class org::dxl::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  effectiveFrom: DateTime[1];\n" +
                "  effectiveThru: DateTime[1];\n" +
                "}\n" +
                "\n" +
                "Class org::dxl::Animal\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Association org::dxl::ZooAnimal\n" +
                "{\n" +
                "  zoo: Zoo[1];\n" +
                "  animals: Animal[*];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping org::dxl::Mapping ()\n" +
                "\n" +
                "###Service\n" +
                "Service org::dxl::ZooService\n" +
                "{\n" +
                "  pattern : 'test';\n" +
                "  documentation : 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: org::dxl::Zoo[1]|$src.name;\n" +
                "    mapping: org::dxl::Mapping;\n" +
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
                "###ExternalFormat\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [\n" +
                "    org::dxl::Person\n" +
                "  ];\n" +
                "}\n" +
                "###Connection\n" +
                "JsonModelConnection test::TestConnection\n" +
                "{\n" +
                "  class : org::dxl::Person;" +
                "  url : 'my_url1';\n" +
                "}\n" +
                "###Persistence\n" +
                "Persistence org::dxl::ZooPersistence\n" +
                "{\n" +
                "  doc: 'A persistence specification for Zoos.';\n" +
                "  trigger: Manual;\n" +
                "  service: org::dxl::ZooService;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: ObjectStorage\n" +
                "    {\n" +
                "      binding: test::Binding;\n" +
                "      connection: test::TestConnection;\n" +
                "    }\n" +
                "    ingestMode: BitemporalDelta\n" +
                "    {\n" +
                "      mergeStrategy: DeleteIndicator\n" +
                "      {\n" +
                "        deleteField: deleted;\n" +
                "        deleteValues: ['Y', 'true'];\n" +
                "      }\n" +
                "      transactionMilestoning: BatchIdAndDateTime\n" +
                "      {\n" +
                "        batchIdInName: 'batchIdIn';\n" +
                "        batchIdOutName: 'batchIdOut';\n" +
                "        dateTimeInName: 'IN_Z';\n" +
                "        dateTimeOutName: 'OUT_Z';\n" +
                "      }\n" +
                "      validityMilestoning: DateTime\n" +
                "      {\n" +
                "        dateTimeFromName: 'FROM_Z';\n" +
                "        dateTimeThruName: 'THRU_Z';\n" +
                "        derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "        {\n" +
                "          sourceDateTimeFromField: 'effectiveFrom';\n" +
                "          sourceDateTimeThruField: 'effectiveThru';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    targetShape: MultiFlat\n" +
                "    {\n" +
                "      modelClass: org::dxl::Zoo;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      parts:\n" +
                "      [\n" +
                "        {\n" +
                "          modelProperty: 'zookeeper';\n" +
                "          targetName: 'PersonDataset1';\n" +
                "        },\n" +
                "        {\n" +
                "          modelProperty: 'owner';\n" +
                "          targetName: 'PersonDataset2';\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "  notifier:\n" +
                "  {\n" +
                "    notifyees:\n" +
                "    [\n" +
                "      Email\n" +
                "      {\n" +
                "        address: 'x.y@z.com';\n" +
                "      },\n" +
                "      PagerDuty\n" +
                "      {\n" +
                "        url: 'https://x.com';\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}", "COMPILATION error at [67:1-135:1]: Error in 'org::dxl::ZooPersistence': Property 'deleted' must exist in class 'org::dxl::Person'");
    }

    @Test
    public void objectStorageSinkNoBinding()
    {
        test("Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::ServiceResult\n" +
                "{\n" +
                "   deleted: String[1];\n" +
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
                "###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: ObjectStorage\n" +
                "    {\n" +
                "      connection:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: test::Person;\n" +
                "          url: 'my_url2';\n" +
                "        }\n" +
                "      }#\n" +
                "    }\n" +
                "    ingestMode: NontemporalSnapshot\n" +
                "    {\n" +
                "      auditing: None;\n" +
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ServiceResult;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [44:11-54:5]: Field 'binding' is required");
    }

    @Test
    public void objectStorageSinkNoConnection()
    {
        test("Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::ServiceResult\n" +
                "{\n" +
                "   deleted: String[1];\n" +
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
                "###ExternalFormat\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [\n" +
                "    test::Person\n" +
                "  ];\n" +
                "}\n" +
                "###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: ObjectStorage\n" +
                "    {\n" +
                "      binding: test::Binding;\n" +
                "    }\n" +
                "    ingestMode: NontemporalSnapshot\n" +
                "    {\n" +
                "      auditing: None;\n" +
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ServiceResult;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [52:11-55:5]: Field 'connection' is required");
    }

    @Test
    public void relationalSinkNoConnection()
    {
        test("Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::ServiceResult\n" +
                "{\n" +
                "   deleted: String[1];\n" +
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
                "###Persistence\n" +
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
                "    }\n" +
                "    ingestMode: NontemporalSnapshot\n" +
                "    {\n" +
                "      auditing: None;\n" +
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ServiceResult;\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void flatShape()
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
                "###Persistence\n" +
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
                "      connection:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: test::Person;\n" +
                "          url: 'my_url2';\n" +
                "        }\n" +
                "      }#\n" +
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

        // persistence
        PackageableElement packageableElement = model.getPackageableElement("test::TestPersistence");
        assertNotNull(packageableElement);
        assertTrue(packageableElement instanceof Root_meta_pure_persistence_metamodel_Persistence);

        Root_meta_pure_persistence_metamodel_Persistence persistence = (Root_meta_pure_persistence_metamodel_Persistence) packageableElement;
        assertEquals("This is test documentation.", persistence._documentation());

        // trigger
        Root_meta_pure_persistence_metamodel_trigger_Trigger trigger = persistence._trigger();
        assertNotNull(trigger);
        assertTrue(trigger instanceof Root_meta_pure_persistence_metamodel_trigger_ManualTrigger);

        // notifier
        Root_meta_pure_persistence_metamodel_notifier_Notifier notifier = persistence._notifier();
        assertNotNull(notifier);
        List<? extends Root_meta_pure_persistence_metamodel_notifier_Notifyee> notifyees = notifier._notifyees().toList();
        assertEquals(0, notifyees.size());

        // persister
        Root_meta_pure_persistence_metamodel_persister_Persister persister = persistence._persister();
        assertNotNull(persister);
        assertTrue(persister instanceof Root_meta_pure_persistence_metamodel_persister_BatchPersister);
        Root_meta_pure_persistence_metamodel_persister_BatchPersister batchPersister = (Root_meta_pure_persistence_metamodel_persister_BatchPersister) persister;

        // sink
        Root_meta_pure_persistence_metamodel_persister_sink_Sink sink = persister._sink();
        assertNotNull(sink);
        assertTrue(sink instanceof Root_meta_pure_persistence_metamodel_persister_sink_RelationalSink);
        Root_meta_pure_persistence_metamodel_persister_sink_RelationalSink relationalSink = (Root_meta_pure_persistence_metamodel_persister_sink_RelationalSink) sink;

        //TODO: ledav -- use a connection applicable for a real use case
        // connection
        Connection connection = relationalSink._connection();
        assertNotNull(connection);
        assertTrue(connection instanceof Root_meta_pure_mapping_modelToModel_JsonModelConnection);

        // ingest mode
        Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode ingestMode = batchPersister._ingestMode();
        assertNotNull(ingestMode);
        assertTrue(ingestMode instanceof Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_UnitemporalDelta);
        Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_UnitemporalDelta unitemporalDelta = (Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_UnitemporalDelta) ingestMode;

        // merge strategy
        Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy mergeStrategy = unitemporalDelta._mergeStrategy();
        assertNotNull(mergeStrategy);
        assertTrue(mergeStrategy instanceof Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_NoDeletesMergeStrategy);

        // transaction milestoning
        Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning txnMilestoning = unitemporalDelta._transactionMilestoning();
        assertNotNull(txnMilestoning);
        assertTrue(txnMilestoning instanceof Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_DateTimeTransactionMilestoning);
        Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_DateTimeTransactionMilestoning dateTimeTxnMilestoning = (Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_DateTimeTransactionMilestoning) txnMilestoning;
        assertEquals("IN_Z", dateTimeTxnMilestoning._dateTimeInName());
        assertEquals("OUT_Z", dateTimeTxnMilestoning._dateTimeOutName());

        // transaction derivation
        Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_TransactionDerivation txnDerivation = dateTimeTxnMilestoning._derivation();
        assertNotNull(txnDerivation);
        assertTrue(txnDerivation instanceof Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_SourceSpecifiesTransactionInDate);
        Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_SourceSpecifiesTransactionInDate sourceSpecifiesIn = (Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_SourceSpecifiesTransactionInDate) txnDerivation;
        assertEquals("dateTimeIn", sourceSpecifiesIn._sourceDateTimeInField());

        // target shape
        Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape targetShape = batchPersister._targetShape();
        assertNotNull(targetShape);
        assertTrue(targetShape instanceof Root_meta_pure_persistence_metamodel_persister_targetshape_FlatTarget);
        Root_meta_pure_persistence_metamodel_persister_targetshape_FlatTarget flatTarget = (Root_meta_pure_persistence_metamodel_persister_targetshape_FlatTarget) targetShape;

        Class<?> modelClass = flatTarget._modelClass();
        assertNotNull(modelClass);
        assertEquals("ServiceResult", modelClass._name());
        assertEquals("TestDataset1", flatTarget._targetName());
        Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy dedupStrategy = flatTarget._deduplicationStrategy();
        assertNotNull(dedupStrategy);
        assertTrue(dedupStrategy instanceof Root_meta_pure_persistence_metamodel_persister_deduplication_NoDeduplicationStrategy);
    }

    @Test
    public void multiFlatShape()
    {
        Pair<PureModelContextData, PureModel> result = test(
                "import org::dxl::*;\n" +
                "\n" +
                "Class org::dxl::Zoo\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  zookeeper: Person[1];\n" +
                "  owner: Person[1];\n" +
                "}\n" +
                "\n" +
                "Class org::dxl::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  stripe1: Integer[1];\n" +
                "  stripe2: Integer[1];\n" +
                "  effectiveFrom: DateTime[1];\n" +
                "  effectiveThru: DateTime[1];\n" +
                "  dateTimeIn: DateTime[1];\n" +
                "  dateTimeOut: DateTime[1];\n" +
                "  deleted: String[1];\n" +
                "  version: Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class org::dxl::Animal\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Association org::dxl::ZooAnimal\n" +
                "{\n" +
                "  zoo: Zoo[1];\n" +
                "  animals: Animal[*];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping org::dxl::Mapping ()\n" +
                "\n" +
                "###Service\n" +
                "Service org::dxl::ZooService\n" +
                "{\n" +
                "  pattern : 'test';\n" +
                "  documentation : 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: org::dxl::Zoo[1]|$src.name;\n" +
                "    mapping: org::dxl::Mapping;\n" +
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
                "###ExternalFormat\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [\n" +
                "    org::dxl::Person\n" +
                "  ];\n" +
                "}\n" +
                "###Connection\n" +
                "JsonModelConnection test::TestConnection\n" +
                "{\n" +
                "  class : org::dxl::Person;" +
                "  url : 'my_url1';\n" +
                "}\n" +
                "###Persistence\n" +
                "Persistence org::dxl::ZooPersistence\n" +
                "{\n" +
                "  doc: 'A persistence specification for Zoos.';\n" +
                "  trigger: Manual;\n" +
                "  service: org::dxl::ZooService;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: ObjectStorage\n" +
                "    {\n" +
                "      binding: test::Binding;\n" +
                "      connection: test::TestConnection;\n" +
                "    }\n" +
                "    ingestMode: BitemporalDelta\n" +
                "    {\n" +
                "      mergeStrategy: DeleteIndicator\n" +
                "      {\n" +
                "        deleteField: deleted;\n" +
                "        deleteValues: ['Y', 'true'];\n" +
                "      }\n" +
                "      transactionMilestoning: BatchIdAndDateTime\n" +
                "      {\n" +
                "        batchIdInName: 'batchIdIn';\n" +
                "        batchIdOutName: 'batchIdOut';\n" +
                "        dateTimeInName: 'IN_Z';\n" +
                "        dateTimeOutName: 'OUT_Z';\n" +
                "        derivation: SourceSpecifiesInAndOutDateTime\n" +
                "        {\n" +
                "          sourceDateTimeInField: 'dateTimeIn';\n" +
                "          sourceDateTimeOutField: 'dateTimeOut';\n" +
                "        }\n" +
                "      }\n" +
                "      validityMilestoning: DateTime\n" +
                "      {\n" +
                "        dateTimeFromName: 'FROM_Z';\n" +
                "        dateTimeThruName: 'THRU_Z';\n" +
                "        derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "        {\n" +
                "          sourceDateTimeFromField: 'effectiveFrom';\n" +
                "          sourceDateTimeThruField: 'effectiveThru';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    targetShape: MultiFlat\n" +
                "    {\n" +
                "      modelClass: org::dxl::Zoo;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      parts:\n" +
                "      [\n" +
                "        {\n" +
                "          modelProperty: 'zookeeper';\n" +
                "          targetName: 'PersonDataset1';\n" +
                "          partitionFields: ['stripe1', 'stripe2'];\n" +
                "          deduplicationStrategy: MaxVersion\n" +
                "          {\n" +
                "            versionField: version;\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          modelProperty: 'owner';\n" +
                "          targetName: 'PersonDataset2';\n" +
                "          deduplicationStrategy: DuplicateCount\n" +
                "          {\n" +
                "            duplicateCountName: 'duplicateCount';\n" +
                "          }\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "  notifier:\n" +
                "  {\n" +
                "    notifyees:\n" +
                "    [\n" +
                "      Email\n" +
                "      {\n" +
                "        address: 'x.y@z.com';\n" +
                "      },\n" +
                "      PagerDuty\n" +
                "      {\n" +
                "        url: 'https://x.com';\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}");

        PureModel model = result.getTwo();

        // persistence
        PackageableElement packageableElement = model.getPackageableElement("org::dxl::ZooPersistence");
        assertNotNull(packageableElement);
        assertTrue(packageableElement instanceof Root_meta_pure_persistence_metamodel_Persistence);

        Root_meta_pure_persistence_metamodel_Persistence persistence = (Root_meta_pure_persistence_metamodel_Persistence) packageableElement;
        assertEquals("A persistence specification for Zoos.", persistence._documentation());

        // trigger
        Root_meta_pure_persistence_metamodel_trigger_Trigger trigger = persistence._trigger();
        assertNotNull(trigger);
        assertTrue(trigger instanceof Root_meta_pure_persistence_metamodel_trigger_ManualTrigger);

        // notifier
        Root_meta_pure_persistence_metamodel_notifier_Notifier notifier = persistence._notifier();
        assertNotNull(notifier);
        List<? extends Root_meta_pure_persistence_metamodel_notifier_Notifyee> notifyees = notifier._notifyees().toList();
        assertEquals(2, notifyees.size());

        // email notifyee
        Root_meta_pure_persistence_metamodel_notifier_Notifyee notifyee1 = notifyees.get(0);
        assertTrue(notifyee1 instanceof Root_meta_pure_persistence_metamodel_notifier_EmailNotifyee);
        Root_meta_pure_persistence_metamodel_notifier_EmailNotifyee emailNotifyee = (Root_meta_pure_persistence_metamodel_notifier_EmailNotifyee) notifyee1;
        assertEquals("x.y@z.com", emailNotifyee._emailAddress());

        // pager duty notifyee
        Root_meta_pure_persistence_metamodel_notifier_Notifyee notifyee2 = notifyees.get(1);
        assertTrue(notifyee2 instanceof Root_meta_pure_persistence_metamodel_notifier_PagerDutyNotifyee);
        Root_meta_pure_persistence_metamodel_notifier_PagerDutyNotifyee pagerDutyNotifyee = (Root_meta_pure_persistence_metamodel_notifier_PagerDutyNotifyee) notifyee2;
        assertEquals("https://x.com", pagerDutyNotifyee._url());

        // persister
        Root_meta_pure_persistence_metamodel_persister_Persister persister = persistence._persister();
        assertNotNull(persister);
        assertTrue(persister instanceof Root_meta_pure_persistence_metamodel_persister_BatchPersister);
        Root_meta_pure_persistence_metamodel_persister_BatchPersister batchPersister = (Root_meta_pure_persistence_metamodel_persister_BatchPersister) persister;

        // sink
        Root_meta_pure_persistence_metamodel_persister_sink_Sink sink = persister._sink();
        assertNotNull(sink);
        assertTrue(sink instanceof Root_meta_pure_persistence_metamodel_persister_sink_ObjectStorageSink);
        Root_meta_pure_persistence_metamodel_persister_sink_ObjectStorageSink objectStorageSink = (Root_meta_pure_persistence_metamodel_persister_sink_ObjectStorageSink) sink;

        // binding
        Root_meta_external_shared_format_binding_Binding binding = objectStorageSink._binding();
        assertNotNull(binding);
        assertEquals("application/json", binding._contentType());

        //TODO: ledav -- use a connection applicable for a real use case
        // connection
        Connection connection = objectStorageSink._connection();
        assertNotNull(connection);
        assertTrue(connection instanceof Root_meta_pure_mapping_modelToModel_JsonModelConnection);

        // ingest mode
        Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode ingestMode = batchPersister._ingestMode();
        assertNotNull(ingestMode);
        assertTrue(ingestMode instanceof Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_BitemporalDelta);
        Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_BitemporalDelta bitemporalDelta = (Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_BitemporalDelta) ingestMode;

        // merge strategy
        Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy mergeStrategy = bitemporalDelta._mergeStrategy();
        assertNotNull(mergeStrategy);
        assertTrue(mergeStrategy instanceof Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_DeleteIndicatorMergeStrategy);
        Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_DeleteIndicatorMergeStrategy deleteIndicator = (Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_DeleteIndicatorMergeStrategy) mergeStrategy;
        assertEquals("deleted", deleteIndicator._deleteField());
        List<? extends String> deleteValues = deleteIndicator._deleteValues().toList();
        assertEquals(2, deleteValues.size());
        assertEquals("Y", deleteValues.get(0));
        assertEquals("true", deleteValues.get(1));

        // transaction milestoning
        Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning txnMilestoning = bitemporalDelta._transactionMilestoning();
        assertNotNull(txnMilestoning);
        assertTrue(txnMilestoning instanceof Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_BatchIdAndDateTimeTransactionMilestoning);
        Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_BatchIdAndDateTimeTransactionMilestoning batchIdAndDateTimeTxnMilestoning = (Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_BatchIdAndDateTimeTransactionMilestoning) txnMilestoning;
        assertEquals("batchIdIn", batchIdAndDateTimeTxnMilestoning._batchIdInName());
        assertEquals("batchIdOut", batchIdAndDateTimeTxnMilestoning._batchIdOutName());
        assertEquals("IN_Z", batchIdAndDateTimeTxnMilestoning._dateTimeInName());
        assertEquals("OUT_Z", batchIdAndDateTimeTxnMilestoning._dateTimeOutName());

        // transaction derivation
        Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_TransactionDerivation txnDerivation = batchIdAndDateTimeTxnMilestoning._derivation();
        assertNotNull(txnDerivation);
        assertTrue(txnDerivation instanceof Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_SourceSpecifiesTransactionInAndOutDate);
        Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_SourceSpecifiesTransactionInAndOutDate sourceSpecifiesInAndOut = (Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_SourceSpecifiesTransactionInAndOutDate) txnDerivation;
        assertEquals("dateTimeIn", sourceSpecifiesInAndOut._sourceDateTimeInField());
        assertEquals("dateTimeOut", sourceSpecifiesInAndOut._sourceDateTimeOutField());

        // validity milestoning
        Root_meta_pure_persistence_metamodel_persister_validitymilestoning_ValidityMilestoning validMilestoning = bitemporalDelta._validityMilestoning();
        assertNotNull(validMilestoning);
        assertTrue(validMilestoning instanceof Root_meta_pure_persistence_metamodel_persister_validitymilestoning_DateTimeValidityMilestoning);
        Root_meta_pure_persistence_metamodel_persister_validitymilestoning_DateTimeValidityMilestoning dateTimeValidMilestoning = (Root_meta_pure_persistence_metamodel_persister_validitymilestoning_DateTimeValidityMilestoning) validMilestoning;
        assertEquals("FROM_Z", dateTimeValidMilestoning._dateTimeFromName());
        assertEquals("THRU_Z", dateTimeValidMilestoning._dateTimeThruName());

        // validity derivation
        Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_ValidityDerivation validityDerivation = dateTimeValidMilestoning._derivation();
        assertNotNull(validityDerivation);
        assertTrue(validityDerivation instanceof Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_SourceSpecifiesValidFromAndThruDate);
        Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_SourceSpecifiesValidFromAndThruDate sourceSpecifiesFromAndThru = (Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_SourceSpecifiesValidFromAndThruDate) validityDerivation;
        assertEquals("effectiveFrom", sourceSpecifiesFromAndThru._sourceDateTimeFromField());
        assertEquals("effectiveThru", sourceSpecifiesFromAndThru._sourceDateTimeThruField());

        // target shape
        Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape targetShape = batchPersister._targetShape();
        assertNotNull(targetShape);
        assertTrue(targetShape instanceof Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTarget);
        Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTarget multiFlatTarget = (Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTarget) targetShape;
        Class<?> modelClass = multiFlatTarget._modelClass();
        assertNotNull(modelClass);
        assertEquals("Zoo", modelClass._name());

        // multiflat parts
        List<? extends Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTargetPart> parts = multiFlatTarget._parts().toList();
        assertEquals(2, parts.size());

        Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTargetPart part1 = parts.get(0);
        assertEquals("zookeeper", part1._modelProperty()._name());
        assertEquals("PersonDataset1", part1._targetName());

        List<? extends String> partitionFields1 = part1._partitionFields().toList();
        assertEquals(2, partitionFields1.size());
        assertEquals("stripe1", partitionFields1.get(0));
        assertEquals("stripe2", partitionFields1.get(1));

        Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy dedupStrategy1 = part1._deduplicationStrategy();
        assertNotNull(dedupStrategy1);
        assertTrue(dedupStrategy1 instanceof Root_meta_pure_persistence_metamodel_persister_deduplication_MaxVersionDeduplicationStrategy);
        Root_meta_pure_persistence_metamodel_persister_deduplication_MaxVersionDeduplicationStrategy maxVersion = (Root_meta_pure_persistence_metamodel_persister_deduplication_MaxVersionDeduplicationStrategy) dedupStrategy1;
        assertEquals("version", maxVersion._versionField());

        Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTargetPart part2 = parts.get(1);
        assertEquals("owner", part2._modelProperty()._name());
        assertEquals("PersonDataset2", part2._targetName());
        Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy dedupStrategy2 = part2._deduplicationStrategy();
        assertNotNull(dedupStrategy2);
        assertTrue(dedupStrategy2 instanceof Root_meta_pure_persistence_metamodel_persister_deduplication_DuplicateCountDeduplicationStrategy);
        Root_meta_pure_persistence_metamodel_persister_deduplication_DuplicateCountDeduplicationStrategy duplicateCount = (Root_meta_pure_persistence_metamodel_persister_deduplication_DuplicateCountDeduplicationStrategy) dedupStrategy2;
        assertEquals("duplicateCount", duplicateCount._duplicateCountName());
    }
}
