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
                "  groupId: 'test.group';\n" +
                "  artifactId: 'test-data-space';\n" +
                "  versionId: '1.0.0';\n" +
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
        return "COMPILATION error at [3:1-17:1]: Duplicated element 'model::element'";
    }

    @Test
    public void testFaultyAnnotations()
    {
        // Faulty stereotype
        test("###DataSpace\n" +
                "DataSpace <<NoProfile.NoKey>> model::dataSpace" +
                "{\n" +
                "  groupId: 'test.group';\n" +
                "  artifactId: 'test-data-space';\n" +
                "  versionId: '1.0.0';\n" +
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
                "  groupId: 'test.group';\n" +
                "  artifactId: 'test-data-space';\n" +
                "  versionId: '1.0.0';\n" +
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
                "  groupId: 'test.group';\n" +
                "  artifactId: 'test-data-space';\n" +
                "  versionId: '1.0.0';\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n", "COMPILATION error at [2:1-10:1]: Data space must have at least one execution context");

        // Unknown default execution context
        test("###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  groupId: 'test.group';\n" +
                "  artifactId: 'test-data-space';\n" +
                "  versionId: '1.0.0';\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 2';\n" +
                "}\n", "COMPILATION error at [2:1-16:1]: Default execution context does not match any existing execution contexts");
    }

    @Test
    public void testDiagramCompilation()
    {
        test("###DataSpace\n" +
                "DataSpace model::dataSpace" +
                "{\n" +
                "  groupId: 'test.group';\n" +
                "  artifactId: 'test-data-space';\n" +
                "  versionId: '1.0.0';\n" +
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
                "}\n");
    }
}
