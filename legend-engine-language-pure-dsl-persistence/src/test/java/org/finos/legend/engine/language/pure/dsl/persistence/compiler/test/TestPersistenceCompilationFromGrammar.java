package org.finos.legend.engine.language.pure.dsl.persistence.compiler.test;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestPersistenceCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    protected String getDuplicatedElementTestCode()
    {
        return "Class test::TestPipe {}\n" +
                "\n" +
                "Class test::ModelClass {}\n" +
                "\n" +
                "###Persistence\n" +
                "\n" +
                "PersistencePipe test::TestPipe \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      batchMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: NoAuditing;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [7:1-28:1]: Duplicated element 'test::TestPipe'";
    }

    @Test
    public void undefinedModelClass()
    {
        test("###Service\n" +
                "Service test::Service \n" +
                "{\n" +
                "  pattern : 'test';\n" +
                "  documentation : 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
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
                "PersistencePipe test::TestPipe \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      batchMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: NoAuditing;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [36:7-42:7]: Can't find class 'test::ModelClass'");
    }

    @Test
    public void undefinedService()
    {
        test("Class test::ModelClass {}\n" +
                "\n" +
                "###Persistence\n" +
                "\n" +
                "PersistencePipe test::TestPipe \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      batchMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: NoAuditing;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [9:11-12:3]: PersistencePipe refers to a service 'test::Service' that is not defined.");
    }

    @Test
    public void success()
    {
        test("Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "Class test::ServiceResult {}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::Mapping ()\n" +
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
                "PersistencePipe test::TestPipe \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ServiceResult;\n" +
                "      batchMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: NoAuditing;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
    }
}
