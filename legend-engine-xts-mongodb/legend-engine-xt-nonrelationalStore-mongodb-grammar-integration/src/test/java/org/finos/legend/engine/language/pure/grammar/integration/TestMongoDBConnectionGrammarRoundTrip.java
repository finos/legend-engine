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

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestMongoDBConnectionGrammarRoundTrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    public void testMongoDBConnectionGrammarWithDifferentHostnames(String hostname1, String hostname2)
    {
        test("###Connection\n" +
                "MongoDBConnection test::testConnection\n" +
                "{\n" +
                "  database: legend_db;\n" +
                "  store: mongo::test::db;\n" +
                "  serverURLs: [" + hostname1 + ":27071, " + hostname2 + ":27071" + "];\n" +
                "  authentication: # UserPassword {\n" +
                "    username: 'mongo_ro';\n" +
                "    password: SystemPropertiesSecret\n" +
                "    {\n" +
                "      systemPropertyName: 'sys.prop.name';\n" +
                "    };\n" +
                "  }#;\n" +
                "}\n");
    }

    @Test
    public void testMongoDBConnectionGrammarLocalhost()
    {
        testMongoDBConnectionGrammarWithDifferentHostnames("localhost", "myhost.example.com");
    }

    @Test
    public void testMongoDBConnectionGrammarWithHyphenAndDots()
    {
        testMongoDBConnectionGrammarWithDifferentHostnames("f12345-001.ab.com", "j982345-001.ab.com");
    }
}
