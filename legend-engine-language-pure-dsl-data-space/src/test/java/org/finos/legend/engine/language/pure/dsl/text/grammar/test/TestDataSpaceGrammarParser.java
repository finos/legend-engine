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

package org.finos.legend.engine.language.pure.dsl.text.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DataSpaceParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestDataSpaceGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return DataSpaceParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###DataSpace\n" +
            "DataSpace " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts: [];\n" +
            "  defaultExecutionContext: '';\n" +
            "}\n";
    }

    @Test
    public void testMissingFields()
    {
        test("###DataSpace\n" +
            "DataSpace model::dataSpace" +
            "{\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts: [];\n" +
            "  defaultExecutionContext: '';\n" +
            "}\n", "PARSER error at [2:1-7:1]: Field 'groupId' is required");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts: [];\n" +
            "  defaultExecutionContext: '';\n" +
            "}\n", "PARSER error at [2:1-7:1]: Field 'artifactId' is required");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  executionContexts: [];\n" +
            "  defaultExecutionContext: '';\n" +
            "}\n", "PARSER error at [2:1-7:1]: Field 'versionId' is required");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  defaultExecutionContext: '';\n" +
            "}\n", "PARSER error at [2:1-7:1]: Field 'executionContexts' is required");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts: [];\n" +
            "}\n", "PARSER error at [2:1-7:1]: Field 'defaultExecutionContext' is required");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "}\n", "PARSER error at [2:1-14:1]: Field 'name' is required");
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
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "}\n", "PARSER error at [2:1-14:1]: Field 'mapping' is required");
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
            "      mapping: model::String;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "}\n", "PARSER error at [2:1-14:1]: Field 'defaultRuntime' is required");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  supportInfo: Email {\n" +
            "  };\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "}\n", "PARSER error at [2:1-17:1]: Field 'address' is required");
    }

    @Test
    public void testDuplicatedFields()
    {
        test("###DataSpace\n" +
            "DataSpace model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "}\n", "PARSER error at [2:1-17:1]: Field 'groupId' should be specified only once");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "}\n", "PARSER error at [2:1-17:1]: Field 'artifactId' should be specified only once");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "}\n", "PARSER error at [2:1-17:1]: Field 'versionId' should be specified only once");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "}\n", "PARSER error at [2:1-17:1]: Field 'defaultExecutionContext' should be specified only once");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "}\n", "PARSER error at [2:1-24:1]: Field 'executionContexts' should be specified only once");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "}\n", "PARSER error at [2:1-24:1]: Field 'executionContexts' should be specified only once");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "}\n", "PARSER error at [2:1-17:1]: Field 'name' should be specified only once");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "}\n", "PARSER error at [2:1-17:1]: Field 'mapping' should be specified only once");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "}\n", "PARSER error at [2:1-17:1]: Field 'defaultRuntime' should be specified only once");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "  description: 'some description';\n" +
            "  description: 'some description';\n" +
            "}\n", "PARSER error at [2:1-18:1]: Field 'description' should be specified only once");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "  featuredDiagrams:[model::Diagram];\n" +
            "  featuredDiagrams:[model::Diagram];\n" +
            "}\n", "PARSER error at [2:1-18:1]: Field 'featuredDiagrams' should be specified only once");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "  supportInfo: Email {\n" +
            "    address: 'someEmail@test.org';\n" +
            "  };\n" +
            "  supportInfo: Email {\n" +
            "    address: 'someEmail@test.org';\n" +
            "  };\n" +
            "}\n", "PARSER error at [2:1-22:1]: Field 'supportInfo' should be specified only once");
        test("###DataSpace\n" +
            "DataSpace model::dataSpace\n" +
            "{\n" +
            "  groupId: 'test.group';\n" +
            "  artifactId: 'test-data-space';\n" +
            "  versionId: '1.0.0';\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Context 1';\n" +
            "      mapping: model::String;\n" +
            "      defaultRuntime: model::Runtime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Context 1';\n" +
            "  supportInfo: Email {\n" +
            "    address: 'someEmail@test.org';\n" +
            "    address: 'someEmail@test.org';\n" +
            "  };\n" +
            "}\n", "PARSER error at [2:1-20:1]: Field 'address' should be specified only once");
    }
}
