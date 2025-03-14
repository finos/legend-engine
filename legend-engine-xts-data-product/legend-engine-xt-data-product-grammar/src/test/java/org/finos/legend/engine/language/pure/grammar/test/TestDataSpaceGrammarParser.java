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

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DataProductParserGrammar;
import org.junit.Test;

import java.util.List;

public class TestDataProductGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return DataProductParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###DataProduct\n" +
                "DataProduct " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  executionContexts: [];\n" +
                "  defaultExecutionContext: '';\n" +
                "}\n";
    }

    @Test
    public void testMissingFields()
    {
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  defaultExecutionContext: '';\n" +
                "}\n", "PARSER error at [2:1-4:1]: Field 'executionContexts' is required");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts: [];\n" +
                "}\n", "PARSER error at [2:1-4:1]: Field 'defaultExecutionContext' is required");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n", "PARSER error at [5:5-8:5]: Field 'name' is required");

        // Execution Context
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n", "PARSER error at [5:5-8:5]: Field 'mapping' is required");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "}\n", "PARSER error at [5:5-8:5]: Field 'defaultRuntime' is required");

        // Executables
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      executable: model::MyExecutable;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "PARSER error at [14:5-16:5]: Field 'title' is required");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Exec 1';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "PARSER error at [2:8-20:2]: Can't parse unsupported dataProduct executable. please specify token 'execute' or 'query'.");

        // Diagrams
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  diagrams:\n" +
                "  [\n" +
                "    {\n" +
                "      diagram: model::MyDiagra;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "PARSER error at [14:5-16:5]: Field 'title' is required");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  diagrams:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Diag 1';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "PARSER error at [14:5-16:5]: Field 'diagram' is required");

        // Support
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
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
                "}\n", "PARSER error at [3:16-4:3]: Field 'address' is required");
    }

    @Test
    public void testDuplicatedFields()
    {
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
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
                "}\n", "PARSER error at [2:1-14:1]: Field 'defaultExecutionContext' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  title: 'some title';\n" +
                "  title: 'some title';\n" +
                "}\n", "PARSER error at [2:1-15:1]: Field 'title' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
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
                "}\n", "PARSER error at [2:1-15:1]: Field 'description' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  elements:[model::MyClass];\n" +
                "  elements:[model::MyClass];\n" +
                "}\n", "PARSER error at [2:1-15:1]: Field 'elements' should be specified only once");

        // Execution Context
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
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
                "}\n", "PARSER error at [2:1-21:1]: Field 'executionContexts' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
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
                "}\n", "PARSER error at [2:1-21:1]: Field 'executionContexts' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
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
                "}\n", "PARSER error at [6:5-11:5]: Field 'name' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
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
                "}\n", "PARSER error at [6:5-11:5]: Field 'mapping' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
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
                "}\n", "PARSER error at [6:5-11:5]: Field 'defaultRuntime' should be specified only once");

        // Executables
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Exec 1';\n" +
                "      executable: model::MyExecutable;\n" +
                "    }\n" +
                "  ];\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Exec 1';\n" +
                "      executable: model::MyExecutable;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "PARSER error at [2:1-26:1]: Field 'executables' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Exec 1';\n" +
                "      title: 'Exec 1';\n" +
                "      executable: model::MyExecutable;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "PARSER error at [14:5-18:5]: Field 'title' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Exec 1';\n" +
                "      description: 'de 1';\n" +
                "      description: 'de 1';\n" +
                "      executable: model::MyExecutable;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "PARSER error at [14:5-19:5]: Field 'description' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Exec 1';\n" +
                "      executable: model::MyExecutable;\n" +
                "      executable: model::MyExecutable;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "PARSER error at [14:5-18:5]: Field 'executable' should be specified only once");


        // Support Info
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
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
                "}\n", "PARSER error at [2:1-19:1]: Field 'supportInfo' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
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
                "}\n", "PARSER error at [13:16-16:3]: Field 'address' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
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
                "    documentationUrl: 'https://example.com';\n" +
                "    documentationUrl: 'https://example.com';\n" +
                "  };\n" +
                "}\n", "PARSER error at [13:16-17:3]: Field 'documentationUrl' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  supportInfo: Combined {\n" +
                "    documentationUrl: 'https://example.com';\n" +
                "    documentationUrl: 'https://example.com';\n" +
                "  };\n" +
                "}\n", "PARSER error at [13:16-16:3]: Field 'documentationUrl' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  supportInfo: Combined {\n" +
                "    website: 'https://example.com';\n" +
                "    website: 'https://example.com';\n" +
                "  };\n" +
                "}\n", "PARSER error at [13:16-16:3]: Field 'website' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  supportInfo: Combined {\n" +
                "    faqUrl: 'https://example.com';\n" +
                "    faqUrl: 'https://example.com';\n" +
                "  };\n" +
                "}\n", "PARSER error at [13:16-16:3]: Field 'faqUrl' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  supportInfo: Combined {\n" +
                "    supportUrl: 'https://example.com';\n" +
                "    supportUrl: 'https://example.com';\n" +
                "  };\n" +
                "}\n", "PARSER error at [13:16-16:3]: Field 'supportUrl' should be specified only once");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  supportInfo: Combined {\n" +
                "    emails: [];\n" +
                "    emails: [];\n" +
                "  };\n" +
                "}\n", "PARSER error at [13:16-16:3]: Field 'emails' should be specified only once");
    }

    @Test
    public void testDataProductExecutableTemplate()
    {
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Exec 1';\n" +
                "      executable: model::MyExecutable;\n" +
                "    },\n" +
                "    {\n" +
                "      title: 'Template 1';\n" +
                "      query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "      executionContextKey: 'Context 1';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "PARSER error at [18:5-22:5]: Field 'id' is required");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Exec 1';\n" +
                "      executable: model::MyExecutable;\n" +
                "    },\n" +
                "    {\n" +
                "      id: 1234;\n" +
                "      title: 'Exec 2';\n" +
                "      executable: model::MyExecutable;\n" +
                "      executionContextKey: 'Context 1';\n" +
                "    },\n" +
                "    {\n" +
                "      id: 123;\n" +
                "      title: 'Template 1';\n" +
                "      query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "      executionContextKey: 'Context 1';\n" +
                "    },\n" +
                "    {\n" +
                "      id: my_id;\n" +
                "      title: 'Template 2';\n" +
                "      query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "      executionContextKey: 'Context 1';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Exec 1';\n" +
                "      executable: model::MyExecutable;\n" +
                "    },\n" +
                "    {\n" +
                "      id: 1;\n" +
                "      title: 'Template 1';\n" +
                "      query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "      executionContextKey: 'Context 1';\n" +
                "    },\n" +
                "    {\n" +
                "      id: 2Id;\n" +
                "      title: 'Template 1';\n" +
                "      query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n");
        test("###DataProduct\n" +
                "DataProduct model::dataProduct" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Exec 1';\n" +
                "      executable: model::MyExecutable;\n" +
                "    },\n" +
                "    {\n" +
                "      id: 1;\n" +
                "      title: 'Template 1';\n" +
                "      query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "      executionContextKey: 'Context 1';\n" +
                "    },\n" +
                "    {\n" +
                "      id: 2d;\n" +
                "      title: 'Template 2';\n" +
                "      query: domain::COVIDData.all()->project([x|$x.cases], ['Cases']);\n" +
                "    }\n" +
                "  ];\n" +
                "}\n");
    }
}
