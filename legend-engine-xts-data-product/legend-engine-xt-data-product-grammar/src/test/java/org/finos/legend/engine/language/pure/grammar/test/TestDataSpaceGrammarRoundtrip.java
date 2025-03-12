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

public class TestDataProductGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testDataProduct()
    {
        test("###DataProduct\n" +
                "DataProduct <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'bla'} model::dataProduct\n" +
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
                "      testData:\n" +
                "        Reference\n" +
                "        #{\n" +
                "          com::model::someDataElement\n" +
                "        }#;\n" +
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

        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "      testData:\n" +
                "        DataspaceTestData\n" +
                "        #{\n" +
                "          com::test::aDifferentDataspace\n" +
                "        }#;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  title: 'some title';\n" +
                "  description: 'some description';\n" +
                "}\n");

        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'Context 1';\n" +
                "      description: 'some information about the context';\n" +
                "      mapping: model::String;\n" +
                "      defaultRuntime: model::Runtime;\n" +
                "      testData:\n" +
                "        DataspaceTestData\n" +
                "        #{\n" +
                "          com::test::aDifferentDataspace\n" +
                "        }#;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'Context 1';\n" +
                "  title: 'some title';\n" +
                "  description: 'some description';\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Executable 1';\n" +
                "      description: 'description';\n" +
                "      executable: model::MyExecutable;\n" +
                "    },\n" +
                "    {\n" +
                "      id: 1;\n" +
                "      title: 'Template 1';\n" +
                "      description: 'description';\n" +
                "      query: |model::Firm.all()->project([x|$x.id, x|$x.employees.firstName], ['Id', 'Employees/First Name']);\n" +
                "      executionContextKey: 'Context 1';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n");
    }

    @Test
    public void testSuccessfulMappingIncludeDataspace()
    {
        String models = "###Pure\n" +
                "Class test::A extends test::B\n" +
                "{\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::S_A\n" +
                "{\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "  currency: String[*];\n" +
                "}\n" +
                "\n" +
                "\n";
        test("###DataProduct\n" +
                "DataProduct model::dataProduct\n" +
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
                "\n" +
                "\n" + models +
                "###Mapping\n" +
                "Mapping model::dummyMapping\n" +
                "(\n" +
                ")\n" +
                "\n" +
                "Mapping test::M1\n" +
                "(\n" +
                "  include dataspace model::dataProduct\n" +
                "\n" +
                "  test::A[1]: Pure\n" +
                "  {\n" +
                "    ~src test::S_A\n" +
                "    prop1: $src.prop1\n" +
                "  }\n" +
                ")\n" +
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
                "###Runtime\n" +
                "Runtime model::dummyRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    model::dummyMapping\n" +
                "  ];\n" +
                "  connectionStores:\n" +
                "  [\n" +
                "    model::connection:\n" +
                "    [\n" +
                "      (dataspace) model::dataProduct\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n"
                );
    }

    @Test
    public void testDataProductParserBackwardCompatibility()
    {
        testFormat("###DataProduct\n" +
                "DataProduct <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'bla'} model::dataProduct\n" +
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
                "}\n", "###DataProduct\n" +
                "DataProduct <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'bla'} model::dataProduct\n" +
                "{\n" +
                "  groupId: 'test.group';\n" +
                "  artifactId: 'test-data-product';\n" +
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
        testParsedProtocol("###DataProduct\n" +
                "DataProduct <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'bla'} model::dataProduct\n" +
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
                "}\n", "{\"_type\":\"data\",\"elements\":[{\"_type\":\"dataProduct\",\"defaultExecutionContext\":\"Context 1\",\"diagrams\":[{\"description\":\"some information about the context\",\"diagram\":{\"path\":\"model::SomeDiag1\"},\"title\":\"Diag 1\"},{\"diagram\":{\"path\":\"model::Diagram\"},\"title\":\"\"},{\"diagram\":{\"path\":\"model::Diagram2\"},\"title\":\"\"}],\"executionContexts\":[{\"defaultRuntime\":{\"path\":\"model::Runtime\",\"type\":\"RUNTIME\"},\"description\":\"some information about the context\",\"mapping\":{\"path\":\"model::String\",\"type\":\"MAPPING\"},\"name\":\"Context 1\"}],\"name\":\"dataProduct\",\"package\":\"model\",\"stereotypes\":[{\"profile\":\"meta::pure::profiles::typemodifiers\",\"value\":\"abstract\"}],\"taggedValues\":[{\"tag\":{\"profile\":\"doc\",\"value\":\"doc\"},\"value\":\"bla\"}],\"title\":\"some title\"},{\"_type\":\"sectionIndex\",\"name\":\"SectionIndex\",\"package\":\"__internal__\",\"sections\":[{\"_type\":\"importAware\",\"elements\":[],\"imports\":[],\"parserName\":\"Pure\"},{\"_type\":\"default\",\"elements\":[\"model::dataProduct\"],\"parserName\":\"DataProduct\"}]}]}");

        testParsedProtocol("###DataProduct\n" +
                "DataProduct <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'bla'} model::dataProduct\n" +
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
                "}\n", "{\"_type\":\"data\",\"elements\":[{\"_type\":\"dataProduct\",\"defaultExecutionContext\":\"Context 1\",\"diagrams\":[{\"diagram\":{\"path\":\"model::Diagram\"},\"title\":\"\"},{\"diagram\":{\"path\":\"model::Diagram2\"},\"title\":\"\"}],\"executionContexts\":[{\"defaultRuntime\":{\"path\":\"model::Runtime\",\"type\":\"RUNTIME\"},\"description\":\"some information about the context\",\"mapping\":{\"path\":\"model::String\",\"type\":\"MAPPING\"},\"name\":\"Context 1\"}],\"name\":\"dataProduct\",\"package\":\"model\",\"stereotypes\":[{\"profile\":\"meta::pure::profiles::typemodifiers\",\"value\":\"abstract\"}],\"taggedValues\":[{\"tag\":{\"profile\":\"doc\",\"value\":\"doc\"},\"value\":\"bla\"}],\"title\":\"some title\"},{\"_type\":\"sectionIndex\",\"name\":\"SectionIndex\",\"package\":\"__internal__\",\"sections\":[{\"_type\":\"importAware\",\"elements\":[],\"imports\":[],\"parserName\":\"Pure\"},{\"_type\":\"default\",\"elements\":[\"model::dataProduct\"],\"parserName\":\"DataProduct\"}]}]}");
    }

    @Test
    public void backwardCompatibility_composeFeaturedDiagrams() throws JsonProcessingException
    {
        testComposedGrammar("{\n" +
                "  \"_type\": \"data\",\n" +
                "  \"version\": \"v1_0_0\",\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"_type\": \"dataProduct\",\n" +
                "      \"name\": \"TestDataProduct\",\n" +
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
                "}\n", "###DataProduct\n" +
                "DataProduct test::model::TestDataProduct\n" +
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
                "      \"_type\": \"dataProduct\",\n" +
                "      \"name\": \"TestDataProduct\",\n" +
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
                "}\n", "###DataProduct\n" +
                "DataProduct test::model::TestDataProduct\n" +
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

    @Test
    public void testDataspaceExecutableTemplate() throws JsonProcessingException
    {
        testComposedGrammar("{\n" +
                "  \"_type\": \"data\",\n" +
                "  \"version\": \"v1_0_0\",\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"_type\": \"dataProduct\",\n" +
                "      \"name\": \"TestDataProduct\",\n" +
                "      \"package\": \"test::model\",\n" +
                "      \"title\": \"My Data Space\",\n" +
                "      \"executionContexts\": [\n" +
                "        {\n" +
                "          \"name\": \"INT\",\n" +
                "          \"title\": \"Integration\",\n" +
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
                "      ],\n" +
                "      \"elements\": [\n" +
                "        { \"path\": \"test::model\" },\n" +
                "        { \"path\": \"test::model::Class1\" },\n" +
                "        { \"path\": \"test::model::Class2\" },\n" +
                "        { \"path\": \"test::model::Assoc1\" },\n" +
                "        { \"path\": \"test::model::MyEnum\", \"exclude\": true }\n" +
                "      ],\n" +
                "      \"executables\": [\n" +
                "        {\n" +
                "          \"_type\": \"dataProductPackageableElementExecutable\",\n" +
                "          \"title\": \"Executable 1\",\n" +
                "          \"description\": \"description\",\n" +
                "          \"executable\": { \"path\": \"model::MyExecutable\" }\n" +
                "        },\n" +
                "        {\n" +
                "          \"_type\": \"dataProductPackageableElementExecutable\",\n" +
                "          \"title\": \"Executable 2\",\n" +
                "          \"description\": \"description\",\n" +
                "          \"executable\": { \"path\": \"model::MyExecutable\" }\n" +
                "        },\n" +
                "        {\n" +
                "          \"_type\": \"dataProductPackageableElementExecutable\",\n" +
                "          \"title\": \"Executable 3\",\n" +
                "          \"id\": \"2\",\n" +
                "          \"description\": \"description\",\n" +
                "          \"executable\": { \"path\": \"domain::COVIDData_QueryFunction():TabularDataSet[1]\" },\n" +
                "          \"executionContextKey\": \"INT\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"_type\": \"dataProductTemplateExecutable\",\n" +
                "          \"id\": \"1\",\n" +
                "          \"title\": \"Template 1\",\n" +
                "          \"description\": \"description\",\n" +
                "          \"query\": {\n" +
                "            \"_type\": \"lambda\",\n" +
                "            \"body\": [\n" +
                "              {\n" +
                "                \"_type\": \"func\",\n" +
                "                \"function\": \"project\",\n" +
                "                \"parameters\": [\n" +
                "                  {\n" +
                "                    \"_type\": \"func\",\n" +
                "                    \"function\": \"getAll\",\n" +
                "                    \"parameters\": [\n" +
                "                      {\n" +
                "                        \"_type\": \"packageableElementPtr\",\n" +
                "                        \"fullPath\": \"model::Firm\"\n" +
                "                      }\n" +
                "                    ]\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"_type\": \"collection\",\n" +
                "                    \"multiplicity\": {\n" +
                "                      \"lowerBound\": 2,\n" +
                "                      \"upperBound\": 2\n" +
                "                    },\n" +
                "                    \"values\": [\n" +
                "                      {\n" +
                "                        \"_type\": \"lambda\",\n" +
                "                        \"body\": [\n" +
                "                          {\n" +
                "                            \"_type\": \"property\",\n" +
                "                            \"parameters\": [\n" +
                "                              {\n" +
                "                                \"_type\": \"var\",\n" +
                "                                \"name\": \"x\"\n" +
                "                              }\n" +
                "                            ],\n" +
                "                            \"property\": \"id\"\n" +
                "                          }\n" +
                "                        ],\n" +
                "                        \"parameters\": [\n" +
                "                          {\n" +
                "                            \"_type\": \"var\",\n" +
                "                            \"name\": \"x\"\n" +
                "                          }\n" +
                "                        ]\n" +
                "                      },\n" +
                "                      {\n" +
                "                        \"_type\": \"lambda\",\n" +
                "                        \"body\": [\n" +
                "                          {\n" +
                "                            \"_type\": \"property\",\n" +
                "                            \"parameters\": [\n" +
                "                              {\n" +
                "                                \"_type\": \"property\",\n" +
                "                                \"parameters\": [\n" +
                "                                  {\n" +
                "                                    \"_type\": \"var\",\n" +
                "                                    \"name\": \"x\"\n" +
                "                                  }\n" +
                "                                ],\n" +
                "                                \"property\": \"employees\"\n" +
                "                              }\n" +
                "                            ],\n" +
                "                            \"property\": \"firstName\"\n" +
                "                          }\n" +
                "                        ],\n" +
                "                        \"parameters\": [\n" +
                "                          {\n" +
                "                            \"_type\": \"var\",\n" +
                "                            \"name\": \"x\"\n" +
                "                          }\n" +
                "                        ]\n" +
                "                      }\n" +
                "                    ]\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"_type\": \"collection\",\n" +
                "                    \"multiplicity\": {\n" +
                "                      \"lowerBound\": 2,\n" +
                "                      \"upperBound\": 2\n" +
                "                    },\n" +
                "                    \"values\": [\n" +
                "                      {\n" +
                "                        \"_type\": \"string\",\n" +
                "                        \"value\": \"Id\"\n" +
                "                      },\n" +
                "                      {\n" +
                "                        \"_type\": \"string\",\n" +
                "                        \"value\": \"Employees/First Name\"\n" +
                "                      }\n" +
                "                    ]\n" +
                "                  }\n" +
                "                ]\n" +
                "              }\n" +
                "            ],\n" +
                "            \"parameters\": []\n" +
                "          },\n" +
                "          \"executionContextKey\": \"INT\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"supportInfo\": {\n" +
                "        \"_type\": \"combined\",\n" +
                "        \"documentationUrl\": \"https://www.example.org\",\n" +
                "        \"emails\": [\"testEmail@test.org\"],\n" +
                "        \"website\": \"https://www.example.org\",\n" +
                "        \"faqUrl\": \"https://www.example.org\",\n" +
                "        \"supportUrl\": \"https://www.example.org\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n", "###DataProduct\n" +
                "DataProduct test::model::TestDataProduct\n" +
                "{\n" +
                "  executionContexts:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'INT';\n" +
                "      title: 'Integration';\n" +
                "      description: 'some description 1';\n" +
                "      mapping: test::model::TestMapping;\n" +
                "      defaultRuntime: test::model::TestRuntime;\n" +
                "    }\n" +
                "  ];\n" +
                "  defaultExecutionContext: 'INT';\n" +
                "  title: 'My Data Space';\n" +
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
                "    }\n" +
                "  ];\n" +
                "  elements:\n" +
                "  [\n" +
                "    test::model,\n" +
                "    test::model::Class1,\n" +
                "    test::model::Class2,\n" +
                "    test::model::Assoc1,\n" +
                "    -test::model::MyEnum\n" +
                "  ];\n" +
                "  executables:\n" +
                "  [\n" +
                "    {\n" +
                "      title: 'Executable 1';\n" +
                "      description: 'description';\n" +
                "      executable: model::MyExecutable;\n" +
                "    },\n" +
                "    {\n" +
                "      title: 'Executable 2';\n" +
                "      description: 'description';\n" +
                "      executable: model::MyExecutable;\n" +
                "    },\n" +
                "    {\n" +
                "      id: 2;\n" +
                "      title: 'Executable 3';\n" +
                "      description: 'description';\n" +
                "      executable: domain::COVIDData_QueryFunction():TabularDataSet[1];\n" +
                "      executionContextKey: 'INT';\n" +
                "    },\n" +
                "    {\n" +
                "      id: 1;\n" +
                "      title: 'Template 1';\n" +
                "      description: 'description';\n" +
                "      query: |model::Firm.all()->project([x|$x.id, x|$x.employees.firstName], ['Id', 'Employees/First Name']);\n" +
                "      executionContextKey: 'INT';\n" +
                "    }\n" +
                "  ];\n" +
                "  supportInfo: Combined {\n" +
                "    documentationUrl: 'https://www.example.org';\n" +
                "    website: 'https://www.example.org';\n" +
                "    faqUrl: 'https://www.example.org';\n" +
                "    supportUrl: 'https://www.example.org';\n" +
                "    emails:\n" +
                "    [\n" +
                "      'testEmail@test.org'\n" +
                "    ];\n" +
                "  };\n" +
                "}\n");
    }
}
