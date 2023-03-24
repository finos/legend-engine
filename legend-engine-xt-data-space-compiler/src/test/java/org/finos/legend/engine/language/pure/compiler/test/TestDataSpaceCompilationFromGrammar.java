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
                "}\n", "COMPILATION error at [8:7-35]: Can't find mapping 'model::dummyMapping'");

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
                "  defaultExecutionContext: 'Context 2';\n" +
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
                "  featuredDiagrams: [];\n" +
                "  elements: [model::element, model::dummyMapping];\n" +
                "}\n", "COMPILATION error at [31:30-48]: Element is not of supported types (only classes, enumerations, and associations are supported)");
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
                "  featuredDiagrams: [];\n" +
                "}\n");

        test("###Diagram\n" +
                "Diagram model::SomeDiagram\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "\n" +
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
                "  featuredDiagrams: [model::SomeDiagram];\n" +
                "}\n");

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
                "  executables: [{ title: 'MyExec'; executable: model::MyService; }, { title: 'MyExec'; executable: model::Mine; }];\n" +
                "}\n", "COMPILATION error at [53:88-111]: Can't find the packageable element 'model::Mine'");
    }
}
