// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.language.stores.elasticsearch.v7.from.connection;

import java.util.List;
import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authentication.AuthenticationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ElasticsearchConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

public class TestElasticsearchConnectionGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return ConnectionParserGrammar.VOCABULARY;
    }

    @Override
    public List<Vocabulary> getDelegatedParserGrammarVocabulary()
    {
        return Lists.fixedSize.of(ElasticsearchConnectionParserGrammar.VOCABULARY, AuthenticationParserGrammar.VOCABULARY);
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Connection\n" +
                "Elasticsearch7ClusterConnection " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  store: test::esStore;\n" +
                "  clusterDetails: # URL { http://dummyurl.com:1234/api }#;\n" +
                "  authentication: # UserPassword {\n" +
                "    username: 'hello_user';\n" +
                "    password: SystemPropertiesSecret\n" +
                "    {\n" +
                "      systemPropertyName: 'sys.prop.name';\n" +
                "    };\n" +
                "  }#;\n" +
                "}\n\n";
    }

    @Test
    public void testRequiresStore()
    {
        test("###Connection\n" +
                        "Elasticsearch7ClusterConnection abc::abc::Connection\n" +
                        "{\n" +
                        "  clusterDetails: # URL { http://dummyurl.com:1234/api }#;\n" +
                        "  authentication: # UserPassword {\n" +
                        "    username: 'hello_user';\n" +
                        "    password: SystemPropertiesSecret\n" +
                        "    {\n" +
                        "      systemPropertyName: 'sys.prop.name';\n" +
                        "    };\n" +
                        "  }#;\n" +
                        "}",
                "PARSER error at [4:3-12:5]: Field ''store'' is required");
    }

    @Test
    public void testDuplicateRequiresStore()
    {
        test("###Connection\n" +
                        "Elasticsearch7ClusterConnection abc::abc::Connection\n" +
                        "{\n" +
                        "  store: test::esStore;\n" +
                        "  store: test::esStore2;\n" +
                        "  clusterDetails: # URL { http://dummyurl.com:1234/api }#;\n" +
                        "  authentication: # UserPassword {\n" +
                        "    username: 'hello_user';\n" +
                        "    password: SystemPropertiesSecret\n" +
                        "    {\n" +
                        "      systemPropertyName: 'sys.prop.name';\n" +
                        "    };\n" +
                        "  }#;\n" +
                        "}",
                "PARSER error at [4:3-14:5]: Field ''store'' should be specified only once");
    }

    @Test
    public void testRequiresClusterDetails()
    {
        test("###Connection\n" +
                        "Elasticsearch7ClusterConnection abc::abc::Connection\n" +
                        "{\n" +
                        "  store: test::esStore;\n" +
                        "  authentication: # UserPassword {\n" +
                        "    username: 'hello_user';\n" +
                        "    password: SystemPropertiesSecret\n" +
                        "    {\n" +
                        "      systemPropertyName: 'sys.prop.name';\n" +
                        "    };\n" +
                        "  }#;\n" +
                        "}",
                "PARSER error at [4:3-12:5]: Field ''clusterDetails'' is required");
    }

    @Test
    public void testDuplicateClusterDetails()
    {
        test("###Connection\n" +
                        "Elasticsearch7ClusterConnection abc::abc::Connection\n" +
                        "{\n" +
                        "  store: test::esStore;\n" +
                        "  clusterDetails: # URL { http://dummyurl.com:1234/api }#;\n" +
                        "  clusterDetails: # URL { http://dummyurl2.com:1234/api }#;\n" +
                        "  authentication: # UserPassword {\n" +
                        "    username: 'hello_user';\n" +
                        "    password: SystemPropertiesSecret\n" +
                        "    {\n" +
                        "      systemPropertyName: 'sys.prop.name';\n" +
                        "    };\n" +
                        "  }#;\n" +
                        "}",
                "PARSER error at [4:3-14:5]: Field ''clusterDetails'' should be specified only once");
    }

    @Test
    public void testRequiredAuthentication()
    {
        test("###Connection\n" +
                        "Elasticsearch7ClusterConnection abc::abc::Connection\n" +
                        "{\n" +
                        "  store: test::esStore;\n" +
                        "  clusterDetails: # URL { http://dummyurl.com:1234/api }#;\n" +
                        "}",
                "PARSER error at [4:3-6:5]: Field ''authentication'' is required");
    }

    @Test
    public void testDuplicateAuthentication()
    {
        test("###Connection\n" +
                        "Elasticsearch7ClusterConnection abc::abc::Connection\n" +
                        "{\n" +
                        "  store: test::esStore;\n" +
                        "  clusterDetails: # URL { http://dummyurl.com:1234/api }#;\n" +
                        "  authentication: # UserPassword {\n" +
                        "    username: 'hello_user';\n" +
                        "    password: SystemPropertiesSecret\n" +
                        "    {\n" +
                        "      systemPropertyName: 'sys.prop.name';\n" +
                        "    };\n" +
                        "  }#;\n" +
                        "  authentication: # UserPassword {\n" +
                        "    username: 'hello_user';\n" +
                        "    password: SystemPropertiesSecret\n" +
                        "    {\n" +
                        "      systemPropertyName: 'sys.prop.name';\n" +
                        "    };\n" +
                        "  }#;\n" +
                        "}",
                "PARSER error at [4:3-20:5]: Field ''authentication'' should be specified only once");
    }

    @Test
    public void testInvalidClusterDetailsType()
    {
        test("###Connection\n" +
                        "Elasticsearch7ClusterConnection abc::abc::Connection\n" +
                        "{\n" +
                        "  store: test::esStore;\n" +
                        "  clusterDetails: # URI { http://dummyurl.com:1234/api }#;\n" +
                        "  authentication: # UserPassword {\n" +
                        "    username: 'hello_user';\n" +
                        "    password: SystemPropertiesSecret\n" +
                        "    {\n" +
                        "      systemPropertyName: 'sys.prop.name';\n" +
                        "    };\n" +
                        "  }#;\n" +
                        "}",
                "PARSER error at [5:19-25]: Unsupported cluster details type: URI.  Supported: URL");
    }

    @Test
    public void testInvalidClusterDetailsURL()
    {
        test("###Connection\n" +
                        "Elasticsearch7ClusterConnection abc::abc::Connection\n" +
                        "{\n" +
                        "  store: test::esStore;\n" +
                        "  clusterDetails: # URL { wrong url }#;\n" +
                        "  authentication: # UserPassword {\n" +
                        "    username: 'hello_user';\n" +
                        "    password: SystemPropertiesSecret\n" +
                        "    {\n" +
                        "      systemPropertyName: 'sys.prop.name';\n" +
                        "    };\n" +
                        "  }#;\n" +
                        "}",
                "PARSER error at [5:26-38]: URL is not valid");
    }
}