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

import com.fasterxml.jackson.core.JsonProcessingException;
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
                "    },\n" +
                "    {\n" +
                "      name: 'Context 2';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  title: 'some title';\n" +
                "  description: 'some description';\n" +
                "  diagrams:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Diag 1';\n" +
                "      description: 'some information about the context';\n" +
                "      diagram: model::SomeDiag1;\n" +
                "    },\n" +
                "    {\n" +
                "      title: 'Diag 2';\n" +
                "      diagram: model::SomeDiag2;\n" +
                "    }\n" +
                "  ];\n" +
                "  elements:\n" +
                "  [\n" +
                "    model::Class1,\n" +
                "    model::Class2,\n" +
                "    -model,\n" +
                "    -model::Enum2,\n" +
                "    -model::Assoc\n" +
                "  ];\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Exec 1';\n" +
                "      description: 'some information about the context';\n" +
                "      executable: model::SomeExec1;\n" +
                "    },\n" +
                "    {\n" +
                "      title: 'Exec 2';\n" +
                "      executable: model::SomeExec2;\n" +
                "    }\n" +
                "  ];\n" +
                "  supportInfo: Combined {\n" +
                "    documentationUrl: 'https://example.org';\n" +
                "    website: 'https://example.org/website';\n" +
                "    faqUrl: 'https://example.org/faq';\n" +
                "    supportUrl: 'https://example.org/support';\n" +
                "    emails:\n" +
                "    [\n" +
                "      'someEmail@test.org',\n" +
                "      'someEmail2@test.org'\n" +
                "    ];\n" +
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
                "  diagrams:\n" +
                "  [\n" +
                "    {\n" +
                "      title: '';\n" +
                "      diagram: model::Diagram;\n" +
                "    },\n" +
                "    {\n" +
                "      title: '';\n" +
                "      diagram: model::Diagram2;\n" +
                "    }\n" +
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

    @Test
    public void backwardCompatibility_parseFeaturedDiagrams() throws JsonProcessingException
    {
        testParsedProtocol("###DataSpace\n" +
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
                "  featuredDiagrams:\n" +
                "  [\n" +
                "    model::Diagram,\n" +
                "    model::Diagram2\n" +
                "  ];\n" +
                "  diagrams:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Diag 1';\n" +
                "      description: 'some information about the context';\n" +
                "      diagram: model::SomeDiag1;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "{\"_type\":\"data\",\"elements\":[{\"_type\":\"dataSpace\",\"defaultExecutionContext\":\"Context 1\",\"diagrams\":[{\"description\":\"some information about the context\",\"diagram\":{\"path\":\"model::SomeDiag1\"},\"title\":\"Diag 1\"},{\"diagram\":{\"path\":\"model::Diagram\"},\"title\":\"\"},{\"diagram\":{\"path\":\"model::Diagram2\"},\"title\":\"\"}],\"executionContexts\":[{\"defaultRuntime\":{\"path\":\"model::Runtime\",\"type\":\"RUNTIME\"},\"description\":\"some information about the context\",\"mapping\":{\"path\":\"model::String\",\"type\":\"MAPPING\"},\"name\":\"Context 1\"}],\"name\":\"dataSpace\",\"package\":\"model\",\"stereotypes\":[{\"profile\":\"meta::pure::profiles::typemodifiers\",\"value\":\"abstract\"}],\"taggedValues\":[{\"tag\":{\"profile\":\"doc\",\"value\":\"doc\"},\"value\":\"bla\"}],\"title\":\"some title\"},{\"_type\":\"sectionIndex\",\"name\":\"SectionIndex\",\"package\":\"__internal__\",\"sections\":[{\"_type\":\"importAware\",\"elements\":[],\"imports\":[],\"parserName\":\"Pure\"},{\"_type\":\"default\",\"elements\":[\"model::dataSpace\"],\"parserName\":\"DataSpace\"}]}]}");

        testParsedProtocol("###DataSpace\n" +
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
                "  featuredDiagrams:\n" +
                "  [\n" +
                "    model::Diagram,\n" +
                "    model::Diagram2\n" +
                "  ];\n" +
                "}\n", "{\"_type\":\"data\",\"elements\":[{\"_type\":\"dataSpace\",\"defaultExecutionContext\":\"Context 1\",\"diagrams\":[{\"diagram\":{\"path\":\"model::Diagram\"},\"title\":\"\"},{\"diagram\":{\"path\":\"model::Diagram2\"},\"title\":\"\"}],\"executionContexts\":[{\"defaultRuntime\":{\"path\":\"model::Runtime\",\"type\":\"RUNTIME\"},\"description\":\"some information about the context\",\"mapping\":{\"path\":\"model::String\",\"type\":\"MAPPING\"},\"name\":\"Context 1\"}],\"name\":\"dataSpace\",\"package\":\"model\",\"stereotypes\":[{\"profile\":\"meta::pure::profiles::typemodifiers\",\"value\":\"abstract\"}],\"taggedValues\":[{\"tag\":{\"profile\":\"doc\",\"value\":\"doc\"},\"value\":\"bla\"}],\"title\":\"some title\"},{\"_type\":\"sectionIndex\",\"name\":\"SectionIndex\",\"package\":\"__internal__\",\"sections\":[{\"_type\":\"importAware\",\"elements\":[],\"imports\":[],\"parserName\":\"Pure\"},{\"_type\":\"default\",\"elements\":[\"model::dataSpace\"],\"parserName\":\"DataSpace\"}]}]}");
    }

    @Test
    public void backwardCompatibility_composeFeaturedDiagrams() throws JsonProcessingException
    {
        testComposedGrammar("{\n" +
                "  \"_type\": \"data\",\n" +
                "  \"version\": \"v1_0_0\",\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"_type\": \"dataSpace\",\n" +
                "      \"name\": \"TestDataSpace\",\n" +
                "      \"package\": \"test::model\",\n" +
                "      \"executionContexts\": [\n" +
                "        {\n" +
                "          \"name\": \"INT\",\n" +
                "          \"description\": \"some description 1\",\n" +
                "          \"mapping\": {\n" +
                "            \"type\": \"MAPPING\",\n" +
                "            \"path\": \"test::model::TestMapping\"\n" +
                "          },\n" +
                "          \"defaultRuntime\": {\n" +
                "            \"type\": \"RUNTIME\",\n" +
                "            \"path\": \"test::model::TestRuntime\"\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"defaultExecutionContext\": \"INT\",\n" +
                "      \"description\": \"some description 2\",\n" +
                "      \"featuredDiagrams\": [\n" +
                "        {\n" +
                "          \"type\": \"DIAGRAM\",\n" +
                "          \"path\": \"test::model::TestDiagram1\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"type\": \"DIAGRAM\",\n" +
                "          \"path\": \"test::model::TestDiagram2\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n", "###DataSpace\n" +
                "DataSpace test::model::TestDataSpace\n" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'INT';\n" +
                "      description: 'some description 1';\n" +
                "      mapping: test::model::TestMapping;\n" +
                "      defaultRuntime: test::model::TestRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'INT';\n" +
                "  description: 'some description 2';\n" +
                "  diagrams:\n" +
                "  [\n" +
                "    {\n" +
                "      title: '';\n" +
                "      diagram: test::model::TestDiagram1;\n" +
                "    },\n" +
                "    {\n" +
                "      title: '';\n" +
                "      diagram: test::model::TestDiagram2;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n");

        testComposedGrammar("{\n" +
                "  \"_type\": \"data\",\n" +
                "  \"version\": \"v1_0_0\",\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"_type\": \"dataSpace\",\n" +
                "      \"name\": \"TestDataSpace\",\n" +
                "      \"package\": \"test::model\",\n" +
                "      \"executionContexts\": [\n" +
                "        {\n" +
                "          \"name\": \"INT\",\n" +
                "          \"description\": \"some description 1\",\n" +
                "          \"mapping\": {\n" +
                "            \"type\": \"MAPPING\",\n" +
                "            \"path\": \"test::model::TestMapping\"\n" +
                "          },\n" +
                "          \"defaultRuntime\": {\n" +
                "            \"type\": \"RUNTIME\",\n" +
                "            \"path\": \"test::model::TestRuntime\"\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"defaultExecutionContext\": \"INT\",\n" +
                "      \"description\": \"some description 2\",\n" +
                "      \"featuredDiagrams\": [\n" +
                "        {\n" +
                "          \"type\": \"DIAGRAM\",\n" +
                "          \"path\": \"test::model::TestDiagram1\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"type\": \"DIAGRAM\",\n" +
                "          \"path\": \"test::model::TestDiagram2\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"diagrams\": [\n" +
                "        {\n" +
                "          \"title\": \"Diagram 1\",\n" +
                "          \"description\": \"description\",\n" +
                "          \"diagram\": { \"path\": \"model::MyDiagram\" }\n" +
                "        },\n" +
                "        {\n" +
                "          \"title\": \"Diagram 2\",\n" +
                "          \"description\": \"description\",\n" +
                "          \"diagram\": { \"path\": \"model::MyDiagram\" }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n", "###DataSpace\n" +
                "DataSpace test::model::TestDataSpace\n" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'INT';\n" +
                "      description: 'some description 1';\n" +
                "      mapping: test::model::TestMapping;\n" +
                "      defaultRuntime: test::model::TestRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'INT';\n" +
                "  description: 'some description 2';\n" +
                "  diagrams:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Diagram 1';\n" +
                "      description: 'description';\n" +
                "      diagram: model::MyDiagram;\n" +
                "    },\n" +
                "    {\n" +
                "      title: 'Diagram 2';\n" +
                "      description: 'description';\n" +
                "      diagram: model::MyDiagram;\n" +
                "    },\n" +
                "    {\n" +
                "      title: '';\n" +
                "      diagram: test::model::TestDiagram1;\n" +
                "    },\n" +
                "    {\n" +
                "      title: '';\n" +
                "      diagram: test::model::TestDiagram2;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n");
    }
}
