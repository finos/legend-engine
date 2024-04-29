// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.grammar.integration;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MongoDBConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.schema.MongoDBSchemaParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;

import java.util.List;

public class TestMongoDBConnectionGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return MongoDBConnectionParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Connection\n" +
                "MongoDBConnection " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  database: legend_db;\n" +
                "  serverURLs: [localhost:27071];\n" +
                "  authentication: # UserPassword {\n" +
                "    username: 'mongo_ro';\n" +
                "    password: SystemPropertiesSecret\n" +
                "    {\n" +
                "      systemPropertyName: 'sys.prop.name';\n" +
                "    };\n" +
                "  }#;\n" +
                "}\n";
    }


}

