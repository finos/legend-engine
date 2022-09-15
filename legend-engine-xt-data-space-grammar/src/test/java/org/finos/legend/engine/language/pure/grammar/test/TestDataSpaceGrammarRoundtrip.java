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

package org.finos.legend.engine.language.pure.grammar.test;

import org.junit.Test;

public class TestDataSpaceGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testDataSpace()
    {
        test("###DataSpace\n" +
                "DataSpace <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'bla'} model::dataSpace\n" +
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
                "  title: 'some title';\n" +
                "  description: 'some description';\n" +
                "  featuredDiagrams:\n" +
                "  [\n" +
                "    model::Diagram,\n" +
                "    model::Diagram2\n" +
                "  ];\n" +
                "  supportInfo: Email {\n" +
                "    address: 'someEmail@test.org';\n" +
                "  };\n" +
                "}\n");
    }

    @Test
    public void testDataSpaceParserBackwardCompatibility()
    {
        testFormat("###DataSpace\n" +
                "DataSpace <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'bla'} model::dataSpace\n" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    },\n" +
                "    {\n" +
                "      name: 'Context 2';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  description: 'some description';\n" +
                "  featuredDiagrams:\n" +
                "  [\n" +
                "    model::Diagram,\n" +
                "    model::Diagram2\n" +
                "  ];\n" +
                "  supportInfo: Email {\n" +
                "    address: 'someEmail@test.org';\n" +
                "  };\n" +
                "}\n", "###DataSpace\n" +
                "DataSpace <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'bla'} model::dataSpace\n" +
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
                "    },\n" +
                "    {\n" +
                "      name: 'Context 2';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  description: 'some description';\n" +
                "  featuredDiagrams:\n" +
                "  [\n" +
                "    model::Diagram,\n" +
                "    model::Diagram2\n" +
                "  ];\n" +
                "  supportInfo: Email {\n" +
                "    address: 'someEmail@test.org';\n" +
                "  };\n" +
                "}\n");
    }
}
