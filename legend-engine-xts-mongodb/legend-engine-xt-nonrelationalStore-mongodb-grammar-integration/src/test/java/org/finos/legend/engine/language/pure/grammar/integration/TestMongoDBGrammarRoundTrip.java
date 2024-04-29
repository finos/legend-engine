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

public class TestMongoDBGrammarRoundTrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{

    @Test
    public void testEmptyMongoDBStoreGrammar()
    {
        test("###MongoDB\n" +
                "Database test::testEmptyDatabase\n" +
                "(\n" +
                ")\n");
    }

    @Test
    public void testSingleCollectionMongoDBStoreGrammar()
    {
        test("###MongoDB\n" +
                "Database test::testEmptyDatabase\n" +
                "(\n" +
                "  Collection Person\n" +
                "  (\n" +
                "    validationLevel: strict;\n" +
                "    validationAction: error;\n" +
                "    jsonSchema: {\n" +
                "      \"bsonType\": \"object\",\n" +
                "      \"title\": \"Record of Firm\",\n" +
                "      \"properties\": {\n" +
                "        \"name\": {\n" +
                "          \"bsonType\": \"string\",\n" +
                "          \"description\": \"name of the firm\",\n" +
                "          \"minLength\": 2\n" +
                "        }\n" +
                "      },\n" +
                "      \"additionalProperties\": false\n" +
                "    };\n" +
                "  )\n" +
                ")\n");
    }
}
