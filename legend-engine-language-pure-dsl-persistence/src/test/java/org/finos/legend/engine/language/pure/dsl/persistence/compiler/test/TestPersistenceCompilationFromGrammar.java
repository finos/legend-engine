package org.finos.legend.engine.language.pure.dsl.persistence.compiler.test;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public abstract class TestPersistenceCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    protected abstract String targetFlat();
    protected abstract String targetMulti();
    protected abstract String targetOpaque();
    protected abstract String ingestMode();
    protected abstract String flatTarget();
    protected abstract String parts();

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
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      ingestMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [7:1-25:1]: Duplicated element 'test::TestPersistence'";
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
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      ingestMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [5:1-23:1]: Persistence refers to a service 'test::Service' that is not defined");
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
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ServiceResult;\n" +
                "      ingestMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [39:13-48:5]: Can't find class 'test::ServiceResult'");
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
                "    target: " + targetMulti() + "\n" +
                "    {\n" +
                "      modelClass: test::ServiceResult;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      " + parts() + ":\n" +
                "      [\n" +
                "        {\n" +
                "          property: property1;\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'TestDataset1';\n" +
                "            ingestMode: AppendOnly\n" +
                "            {\n" +
                "              auditing: None;\n" +
                "              filterDuplicates: false;\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [39:13-58:5]: Can't find class 'test::ServiceResult'");
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
                "    target: " + targetMulti() + "\n" +
                "    {\n" +
                "      modelClass: test::ServiceResult;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      " + parts() + ":\n" +
                "      [\n" +
                "        {\n" +
                "          property: property1;\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'TestDataset1';\n" +
                "            ingestMode: AppendOnly\n" +
                "            {\n" +
                "              auditing: None;\n" +
                "              filterDuplicates: false;\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [47:9-58:9]: Property 'property1' must exist in class 'test::ServiceResult'");
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
                "    target: " + targetMulti() + "\n" +
                "    {\n" +
                "      modelClass: test::ServiceResult;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      " + parts() + ":\n" +
                "      [\n" +
                "        {\n" +
                "          property: property1;\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'TestDataset1';\n" +
                "            ingestMode: AppendOnly\n" +
                "            {\n" +
                "              auditing: None;\n" +
                "              filterDuplicates: false;\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [50:9-61:9]: Target " + parts().substring(0, parts().length() - 1) + " property must refer to a Class. The property 'property1' refers to a String");
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
                "    target: " + targetMulti() + "\n" +
                "    {\n" +
                "      modelClass: test::ServiceResult;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      " + parts() + ":\n" +
                "      [\n" +
                "        {\n" +
                "          property: property1;\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'TestDataset1';\n" +
                "            ingestMode: AppendOnly\n" +
                "            {\n" +
                "              auditing: None;\n" +
                "              filterDuplicates: false;\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ];\n" +
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
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ServiceResult;\n" +
                "      ingestMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
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
                "    connections:\n" +
                "    [\n" +
                "      id1: test::TestConnection,\n" +
                "      id2:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: org::dxl::Animal;\n" +
                "          url: 'my_url2';\n" +
                "        }\n" +
                "      }#\n" +
                "    ];\n" +
                "    target: " + targetMulti() + "\n" +
                "    {\n" +
                "      modelClass: org::dxl::Zoo;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      " + parts() + ":\n" +
                "      [\n" +
                "        {\n" +
                "          property: 'zookeeper';\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'PersonDataset1';\n" +
                "            ingestMode: AppendOnly\n" +
                "            {\n" +
                "              auditing: None;\n" +
                "              filterDuplicates: false;\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          property: 'owner';\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'PersonDataset2';\n" +
                "            ingestMode: BitemporalSnapshot\n" +
                "            {\n" +
                "              transactionMilestoning: BatchIdAndDateTime\n" +
                "              {\n" +
                "                batchIdInFieldName: 'batchIdIn';\n" +
                "                batchIdOutFieldName: 'batchIdOut';\n" +
                "                dateTimeInFieldName: 'IN_Z';\n" +
                "                dateTimeOutFieldName: 'OUT_Z';\n" +
                "              }\n" +
                "              validityMilestoning: DateTime\n" +
                "              {\n" +
                "                dateTimeFromFieldName: 'FROM_Z';\n" +
                "                dateTimeThruFieldName: 'THRU_Z';\n" +
                "                derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "                {\n" +
                "                  sourceDateTimeFromProperty: 'effectiveFrom';\n" +
                "                  sourceDateTimeThruProperty: 'effectiveThru';\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
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
    }
}
