// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.test;

import org.junit.Test;

public class TestDataSpaceCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Test
    public void testFailedRuntimeConnectionStoreDataspace()
    {
        String models = "Class test::A extends test::B {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::S_A {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "  currency: String[*];\n" +
                "}\n" +
                "\n";
        test(models +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "Mapping test::M1 (\n" +
                "   test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "   connectionStores:\n" +
                "   [\n" +
                "       model::connection:\n" +
                "       [\n" +
                "           (dataspace) model::nonExistantDataspace\n" +
                "       ]\n" +
                "   ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection model::connection\n" +
                "{\n" +
                "  class: test::B;\n" +
                "  url: 'executor:default';\n" +
                "}\n", "COMPILATION error at [38:12-50]: Dataspace model::nonExistantDataspace cannot be found.");
    }

    @Test
    public void testSuccessfulMappingIncludeDataspace()
    {
        String models = "Class test::A extends test::B {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::S_A {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "  currency: String[*];\n" +
                "}\n" +
                "\n";
        test(models +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "Mapping test::M1 (\n" +
                "   test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::M1\n" +
                "  ];\n" +
                "   connectionStores:\n" +
                "   [\n" +
                "       model::connection:\n" +
                "       [\n" +
                "           (dataspace) model::dataSpace\n" +
                "       ]\n" +
                "   ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection model::connection\n" +
                "{\n" +
                "  class: test::B;\n" +
                "  url: 'executor:default';\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Data\n" +
                "Data com::model::someDataElement\n" +
                "{\n" +
                "  ExternalFormat\n" +
                "  #{\n" +
                "    contentType: 'test';\n" +
                "    data: 'test';\n" +
                "  }#\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: test::M1;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "      testData:\n" +
                "        Reference\n" +
                "        #{\n" +
                "          com::model::someDataElement\n" +
                "        }#;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n");
    }

    @Test
    public void testRuntimeIncludeDataspace()
    {
        String models = "Class test::A extends test::B {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::S_A {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "  currency: String[*];\n" +
                "}\n" +
                "\n";
        test(models +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "Mapping test::M1 (\n" +
                "   include dataspace model::dataSpace\n" +
                "   test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "   connectionStores:\n" +
                "   [\n" +
                "       model::connection:\n" +
                "       [\n" +
                "           ModelStore\n" +
                "       ]\n" +
                "   ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection model::connection\n" +
                "{\n" +
                "  class: test::B;\n" +
                "  url: 'executor:default';\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Data\n" +
                "Data com::model::someDataElement\n" +
                "{\n" +
                "  ExternalFormat\n" +
                "  #{\n" +
                "    contentType: 'test';\n" +
                "    data: 'test';\n" +
                "  }#\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "      testData:\n" +
                "        Reference\n" +
                "        #{\n" +
                "          com::model::someDataElement\n" +
                "        }#;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n");
    }

    @Test
    public void testDataspaceIncludesDataspaceWithoutTestData()
    {
        String models = "Class test::A extends test::B {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::S_A {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "  currency: String[*];\n" +
                "}\n" +
                "\n";
        test(models +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "Mapping test::M1 (\n" +
                "   include dataspace model::dataSpace\n" +
                "   test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "   connectionStores:\n" +
                "   [\n" +
                "       model::connection:\n" +
                "       [\n" +
                "           ModelStore\n" +
                "       ]\n" +
                "   ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection model::connection\n" +
                "{\n" +
                "  class: test::B;\n" +
                "  url: 'executor:default';\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace2" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "      testData:\n" +
                "        DataspaceTestData\n" +
                "        #{\n" +
                "          model::dataSpace\n" +
                "        }#;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n", "COMPILATION error at [76:9-79:10]: Dataspace model::dataSpace does not have test data in its default execution context.");
    }

    @Test
    public void testDataspaceDataELementNotFound()
    {
        String models = "Class test::A extends test::B {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::S_A {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "  currency: String[*];\n" +
                "}\n" +
                "\n";
        test(models +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "Mapping test::M1 (\n" +
                "   include dataspace model::dataSpace\n" +
                "   test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "   connectionStores:\n" +
                "   [\n" +
                "       model::connection:\n" +
                "       [\n" +
                "           ModelStore\n" +
                "       ]\n" +
                "   ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection model::connection\n" +
                "{\n" +
                "  class: test::B;\n" +
                "  url: 'executor:default';\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "      testData:\n" +
                "        DataspaceTestData\n" +
                "        #{\n" +
                "          model::nonexistantDataspace\n" +
                "        }#;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n", "COMPILATION error at [63:9-66:10]: Dataspace model::nonexistantDataspace cannot be found.");
    }

    @Test
    public void testDuplicateMappingIncludeDataspaceWithImport()
    {
        String models = "Class test::A extends test::B {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::S_A {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "  currency: String[*];\n" +
                "}\n" +
                "\n";
        test(models +
                "###Mapping\n" +
                "import model::mapping::*;\n" +
                "Mapping model::mapping::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "Mapping test::M1 (\n" +
                "   include mapping dummyMapping" +
                "   include dataspace model::dataspace::dataSpace\n" +
                "   test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataspace::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::mapping::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n", "COMPILATION error at [20:1-26:1]: Duplicated mapping include " +
                "'model::mapping::dummyMapping' in mapping 'test::M1'");
    }

    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class model::element {}\n" +
                "###DataSpace\n" +
                "DataSpace model::element" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [3:1-14:1]: Duplicated element 'model::element'";
    }

    @Test
    public void testFaultyAnnotations()
    {
        // Faulty stereotype
        test("###DataSpace\n" +
                "DataSpace <<NoProfile.NoKey>> model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n", "COMPILATION error at [2:13-21]: Can't find the profile 'NoProfile'");

        // Faulty tagged value
        test("###DataSpace\n" +
                "DataSpace { NoProfile.NoKey = 'something' } model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n", "COMPILATION error at [2:13-21]: Can't find the profile 'NoProfile'");
    }


    @Test
    public void testProblemWithExecutionContext()
    {
        // No execution context provided
        test("###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n", "COMPILATION error at [2:1-7:1]: Data space must have at least one execution context");

        // Duplicated execution contexts
        test("###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    },\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n", "COMPILATION error at [27:5-32:5]: Data space execution context, Context 1, is not unique");


        // Unknown default execution context
        test("###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 2';\n" +
                "}\n", "COMPILATION error at [18:1-29:1]: Default execution context 'Context 2' does not match any existing execution contexts");

        // reference resolution
        test("###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 2';\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping2\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping model::dummyMapping2\n" +
                "(\n" +
                ")\n", "COMPILATION error at [8:7-35]: Can't find mapping 'model::dummyMapping'");

        test("###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n", "COMPILATION error at [15:7-42]: Can't find packageable runtime 'model::dummyRuntime'");

        // Default runtime is not compatible with execution context mapping
        test("###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "Mapping model::dummyMapping2\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping2\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n", "COMPILATION error at [22:1-33:1]: Execution context 'Context 1' default runtime is not compatible with mapping");
    }

    @Test
    public void testDataSpaceWithElements()
    {
        test("Class model::element {}\n" +
                "Class model::sub::element {}\n" +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  elements: [model::element, model, -model::sub];\n" +
                "}\n");

        test("Class model::element {}\n" +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                // no error should occur since, although the specified element is a mapping, it is excluded
                "  elements: [model::element, model::dummyMapping, -model::dummyMapping];\n" +
                "}\n");
    }

    @Test
    public void testDataSpaceWithTemplateExecutableWithInlineQuery()
    {
        test("Class model::element {}\n" +
                "Class model::sub::element {}\n" +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  elements: [model::element, model, -model::sub];\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      id: 1;\n" +
                "      title: 'Template 1';\n" +
                "      query: src: model::sub::element[1]|$src;\n" +
                "      executionContextKey: 'Context 1';\n" +
                "    },\n" +
                "    {\n" +
                "      id: 2;\n" +
                "      title: 'Template 2';\n" +
                "      query: src: model::sub::element[1]|$src;\n" +
                "    },\n" +
                "    {\n" +
                "      id: 3;\n" +
                "      title: 'Template 3';\n" +
                "      description: 'an example of a template query';\n" +
                "      query: src: model::sub::element[1]|$src;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n");

        test("Class model::element {}\n" +
                "Class model::sub::element {}\n" +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  elements: [model::element, model, -model::sub];\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      id: 1;\n" +
                "      title: 'Template 1';\n" +
                "      query: src: model::sub::element[1]|$src;\n" +
                "      executionContextKey: 'Context 1';\n" +
                "    },\n" +
                "    {\n" +
                "      id: 2;\n" +
                "      title: 'Template 2';\n" +
                "      query: src: model::sub::element[1]|$srssc;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "COMPILATION error at [43:42-47]: Can't find variable class for variable 'srssc' in the graph");

        test("Class model::element {}\n" +
                "Class model::sub::element {}\n" +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  elements: [model::element, model, -model::sub];\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      id: 1;\n" +
                "      title: 'Template 1';\n" +
                "      query: src: model::sub::element[1]|$src;\n" +
                "      executionContextKey: 'Context 1';\n" +
                "    },\n" +
                "    {\n" +
                "      id: 1;\n" +
                "      title: 'Template 1';\n" +
                "      query: src: model::sub::element[1]|$srssc;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "COMPILATION error at [20:1-46:1]: Data space executable id, 1, is not unique");

        test("Class model::element {}\n" +
                "Class model::sub::element {}\n" +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  elements: [model::element, model, -model::sub];\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      id: 1;\n" +
                "      title: 'Template 1';\n" +
                "      query: src: model::sub::element[1]|$src;\n" +
                "      executionContextKey: 'random 1';\n" +
                "    },\n" +
                "    {\n" +
                "      id: 2;\n" +
                "      title: 'Template 2';\n" +
                "      query: src: model::sub::element[1]|$srssc;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "COMPILATION error at [20:1-46:1]: Data space template executable's executionContextKey, random 1, is not valid. Please specify one from [Context 1]");
    }

    @Test
    public void testDataSpaceWithExecutableWithFunctionPointer()
    {
        String model = "Class model::element {}\n" +
                "Class model::sub::element\n" +
                "{" +
                " name: String[1];\n" +
                "}\n" +
                "function model::templateFunc(): meta::pure::tds::TabularDataSet[1]\n" +
                "{\n" +
                "   model::sub::element.all()->project([x | $x.name],['Name'])->from(model::dummyMapping, model::dummyRuntime)\n" +
                "}\n" +
                "function model::templateFunc1(): meta::pure::tds::TabularDataSet[1]\n" +
                "{\n" +
                "   model::sub::element.all()->project([x | $x.name],['Name'])->from(model::dummyMapping1, model::dummyRuntime)\n" +
                "}\n" +
                "function model::templateFunc2(): meta::pure::tds::TabularDataSet[1]\n" +
                "{\n" +
                "   model::sub::element.all()->project([x | $x.name],['Name'])->from(model::dummyMapping, model::dummyRuntime1)\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "Mapping model::dummyMapping1\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "Runtime model::dummyRuntime1\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n";

        test(model +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  elements: [model::element, model, model::sub];\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      id: 1;\n" +
                "      title: 'Template 1';\n" +
                "      executable: model::templateFunc():TabularDataSet[1];\n" +
                "      executionContextKey: 'Context 1';\n" +
                "    },\n" +
                "    {\n" +
                "      id: 2;\n" +
                "      title: 'Template 2';\n" +
                "      executable: model::templateFunc():TabularDataSet[1];\n" +
                "    },\n" +
                "    {\n" +
                "      id: 3;\n" +
                "      title: 'Template 3';\n" +
                "      description: 'an example of a template query';\n" +
                "      executable: model::templateFunc():TabularDataSet[1];\n" +
                "    }\n" +
                "  ];\n" +
                "}\n");

        test(model +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  elements: [model::element, model, model::sub];\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      id: 1;\n" +
                "      title: 'Template 1';\n" +
                "      executable: model::templateFunc():TabularDataSet[1];\n" +
                "      executionContextKey: 'Context 1';\n" +
                "    },\n" +
                "    {\n" +
                "      id: 1;\n" +
                "      title: 'Template 2';\n" +
                "      executable: model::templateFunc():TabularDataSet[1];\n" +
                "    }\n" +
                "  ];\n" +
                "}\n","COMPILATION error at [44:1-70:1]: Data space executable id, 1, is not unique");

        test(model +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  elements: [model::element, model, model::sub];\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      id: 1;\n" +
                "      title: 'Template 1';\n" +
                "      executable: model::templateFunc1():TabularDataSet[1];\n" +
                "      executionContextKey: 'Context 1';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", " at [44:1-65:1]: Error in 'model::dataSpace': The mapping utilized in the function within the curated template query does not align with the mapping applied in the execution context `Context 1`.");

        test(model +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  elements: [model::element, model, model::sub];\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      id: 1;\n" +
                "      title: 'Template 1';\n" +
                "      executable: model::templateFunc2():TabularDataSet[1];\n" +
                "      executionContextKey: 'Context 1';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", " at [44:1-65:1]: Error in 'model::dataSpace': The runtime utilized in the function within the curated template query does not align with the runtime applied in the execution context `Context 1`.");
    }

    @Test
    public void testDataSpaceWithUnsupportedElement()
    {
        test("Class model::element {}\n" +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  elements: [model::element, model::dummyMapping];\n" +
                "}\n", "COMPILATION error at [30:30-48]: Included element is not of supported types (only packages, classes, enumerations, and associations are supported)");
    }

    @Test
    public void testDataSpaceDiagramsCompilation()
    {
        // no diagrams
        test("###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n");

        // backward compatible
        test("###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  featuredDiagrams: [model::SomeDiagram];\n" +
                "}\n", "COMPILATION error at [29:22-39]: Can't find diagram 'model::SomeDiagram'");

        test("###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  diagrams: [{ title: 'MyDiag'; diagram: model::SomeDiagram; }];\n" +
                "}\n", "COMPILATION error at [29:33-60]: Can't find diagram 'model::SomeDiagram'");
    }

    @Test
    public void testDataSpaceExecutablesCompilation()
    {
        test("###Service\n" +
                "Service model::MyService\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['test'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: '';\n" +
                "    mapping: model::dummyMapping;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "     connections: [];\n" +
                "    }#;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "    ];\n" +
                "  }\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  executables: [{ title: 'MyExec'; executable: model::MyService; }];\n" +
                "}\n");

        // not found executable
        test("###Service\n" +
                "Service model::MyService\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['test'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: '';\n" +
                "    mapping: model::dummyMapping;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "     connections: [];\n" +
                "    }#;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "    ];\n" +
                "  }\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::dummyMapping;\n" +
                "      defaultRuntime: model::dummyRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  executables: [{ title: 'MyExec'; executable: model::MyService; }, { title: 'MyExec1'; executable: model::Mine; }];\n" +
                "}\n", "COMPILATION error at [42:1-54:1]: Error in 'model::dataSpace': Can't find the packageable element 'model::Mine'");
    }
}
