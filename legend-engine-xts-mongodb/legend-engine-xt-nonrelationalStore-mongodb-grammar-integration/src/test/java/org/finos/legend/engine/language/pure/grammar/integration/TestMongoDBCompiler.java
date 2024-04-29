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

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestMongoDBCompiler extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{


    @Override
    protected String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping test::mongodb::mapping ()\n" +
                "###MongoDB\n" +
                "Database test::mongodb::mapping\n" +
                "(\n" +
                ")";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'test::mongodb::mapping'";
    }

    @Test
    public void testMongoDBStoreDefinitionv1()
    {
        test(TestMongoDBCompilerUtil.MODEL_PLUS_BINDING +
                "###MongoDB\n" +
                "Database meta::external::store::mongodb::showcase::store::PersonDatabase\n" +
                "(\n" +
                "  Collection PersonCollection\n" +
                "  (\n" +
                "    validationLevel: strict;\n" +
                "    validationAction: warn;\n" +
                "    jsonSchema: {\n" +
                "      \"bsonType\": \"object\",\n" +
                "      \"title\": \"Person\",\n" +
                "      \"properties\": {\n" +
                "        \"firstName\": {\n" +
                "          \"bsonType\": \"string\"\n" +
                "        },\n" +
                "        \"lastName\": {\n" +
                "          \"bsonType\": \"string\"\n" +
                "        },\n" +
                "        \"middleName\": {\n" +
                "          \"bsonType\": \"string\"\n" +
                "        },\n" +
                "        \"firm\": {\n" +
                "          \"bsonType\": \"object\",\n" +
                "          \"properties\": {\n" +
                "            \"firmName\": {\n" +
                "              \"bsonType\": \"string\"\n" +
                "            },\n" +
                "            \"firmId\": {\n" +
                "              \"bsonType\": \"int\"\n" +
                "            },\n" +
                "            \"address\": {\n" +
                "              \"bsonType\": \"array\",\n" +
                "              \"items\": {\n" +
                "                \"bsonType\": \"object\",\n" +
                "                \"properties\": {\n" +
                "                  \"street\": {\n" +
                "                    \"bsonType\": \"string\"\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    };" +
                "   )\n" +
                "\n" +
                ")\n" +
                "\n");
    }
}
