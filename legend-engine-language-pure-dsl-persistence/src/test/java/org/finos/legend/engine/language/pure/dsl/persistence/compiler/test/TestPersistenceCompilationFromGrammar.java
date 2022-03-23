package org.finos.legend.engine.language.pure.dsl.persistence.compiler.test;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

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
                "    binding: test::Binding;\n" +
                "    connection:\n" +
                "    #{\n" +
                "      JsonModelConnection\n" +
                "      {\n" +
                "        class: test::Person;\n" +
                "        url: 'my_url2';\n" +
                "      }\n" +
                "    }#\n" +
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
        return "COMPILATION error at [7:1-34:1]: Duplicated element 'test::TestPersistence'";
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
                "    binding: test::Binding;\n" +
                "    connection:\n" +
                "    #{\n" +
                "      JsonModelConnection\n" +
                "      {\n" +
                "        class: test::Person;\n" +
                "        url: 'my_url2';\n" +
                "      }\n" +
                "    }#\n" +
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
                "}\n", "COMPILATION error at [5:1-32:1]: Persistence refers to a service 'test::Service' that is not defined");
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
                "    binding: test::Binding;\n" +
                "    connection:\n" +
                "    #{\n" +
                "      JsonModelConnection\n" +
                "      {\n" +
                "        class: test::ServiceResult;\n" +
                "        url: 'my_url2';\n" +
                "      }\n" +
                "    }#\n" +
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
                "}\n", "COMPILATION error at [44:16-34]: Can't find class 'test::ServiceResult'");
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
                "###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    binding: test::Binding;\n" +
                "    connection:\n" +
                "    #{\n" +
                "      JsonModelConnection\n" +
                "      {\n" +
                "        class: test::Person;\n" +
                "        url: 'my_url2';\n" +
                "      }\n" +
                "    }#\n" +
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
                "}\n", "COMPILATION error at [48:18-59:5]: Can't find class 'test::ServiceResult'");
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
                "    binding: test::Binding;\n" +
                "    connection:\n" +
                "    #{\n" +
                "      JsonModelConnection\n" +
                "      {\n" +
                "        class: test::Person;\n" +
                "        url: 'my_url2';\n" +
                "      }\n" +
                "    }#\n" +
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
                "}\n", "COMPILATION error at [56:9-59:9]: Property 'property1' must exist in class 'test::ServiceResult'");
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
                "    binding: test::Binding;\n" +
                "    connection:\n" +
                "    #{\n" +
                "      JsonModelConnection\n" +
                "      {\n" +
                "        class: test::Person;\n" +
                "        url: 'my_url2';\n" +
                "      }\n" +
                "    }#\n" +
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
                "}\n", "COMPILATION error at [59:9-62:9]: Target shape modelProperty 'property1' must refer to a class.");
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
                "    binding: test::Binding;\n" +
                "    connection:\n" +
                "    #{\n" +
                "      JsonModelConnection\n" +
                "      {\n" +
                "        class: test::Person;\n" +
                "        url: 'my_url2';\n" +
                "      }\n" +
                "    }#\n" +
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
    public void flatShape()
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
                "    binding: test::Binding;\n" +
                "    connection:\n" +
                "    #{\n" +
                "      JsonModelConnection\n" +
                "      {\n" +
                "        class: test::Person;\n" +
                "        url: 'my_url2';\n" +
                "      }\n" +
                "    }#\n" +
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
                "}\n");
    }

    @Test
    public void success()
    {
        test("###Pure\n" +
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
                "    binding: test::Binding;\n" +
                "    connection:\n" +
                "    #{\n" +
                "      JsonModelConnection\n" +
                "      {\n" +
                "        class: org::dxl::Animal;\n" +
                "        url: 'my_url2';\n" +
                "      }\n" +
                "    }#\n" +
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
                "    ingestMode: BitemporalSnapshot\n" +
                "    {\n" +
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
    }
}
