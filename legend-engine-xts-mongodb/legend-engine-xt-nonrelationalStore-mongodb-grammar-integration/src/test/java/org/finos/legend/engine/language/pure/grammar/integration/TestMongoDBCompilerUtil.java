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

public class TestMongoDBCompilerUtil
{
    public static final String JSON_BINDING =
            "###Pure\n" +
                    "import meta::external::store::mongodb::showcase::domain::*;\n" +
                    "\n" +
                    "Class meta::external::store::mongodb::showcase::domain::Person\n" +
                    "{\n" +
                    "    firstName  : String[1];\n" +
                    "    lastName   : String[1];\n" +
                    "    middleName : String[0..1];\n" +
                    "    firm       : Firm[0..1];\n" +
                    "}\n" +
                    "\n" +
                    "Class meta::external::store::mongodb::showcase::domain::Firm\n" +
                    "{\n" +
                    "    firmName : String[1];\n" +
                    "    firmId   : Integer[1];\n" +
                    "    address  : Address[*];\n" +
                    "}\n" +
                    "\n" +
                    "Class meta::external::store::mongodb::showcase::domain::Address\n" +
                    "{\n" +
                    "    street : String[1];\n" +
                    "}\n" +
                    "\n" +
                    "###MongoDB\n" +
                    "Database meta::external::store::mongodb::showcase::store::PersonDatabase\n" +
                    "(\n" +
                    "  Collection PersonCollection\n" +
                    "  (\n" +
                    "    validationLevel: strict;\n" +
                    "    validationAction: warn'\n" +
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
                    "              \"bsonType\": \"integer\"\n" +
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
                    "\n" +
                    "###ExternalFormat\n" +
                    "Binding meta::external::store::mongodb::showcase::store::PersonCollectionBinding\n" +
                    "{\n" +
                    "  contentType   : 'application/json';\n" +
                    "  modelIncludes : [\n" +
                    "                    meta::external::store::mongodb::showcase::domain::Person,\n" +
                    "                    meta::external::store::mongodb::showcase::domain::Firm,\n" +
                    "                    meta::external::store::mongodb::showcase::domain::Address,\n" +
                    "                  ];\n" +
                    "}\n\n";


    static final String MODEL_PLUS_BINDING =
            "###Pure\n" +
                    "import meta::external::store::mongodb::showcase::domain::*;\n" +
                    "\n" +
                    "Class meta::external::store::mongodb::showcase::domain::Person\n" +
                    "{\n" +
                    "    firstName  : String[1];\n" +
                    "    lastName   : String[1];\n" +
                    "    middleName : String[0..1];\n" +
                    "    firm       : Firm[0..1];\n" +
                    "}\n" +
                    "\n" +
                    "Class meta::external::store::mongodb::showcase::domain::Firm\n" +
                    "{\n" +
                    "    firmName : String[1];\n" +
                    "    firmId   : Integer[1];\n" +
                    "    address  : Address[*];\n" +
                    "}\n" +
                    "\n" +
                    "Class meta::external::store::mongodb::showcase::domain::Address\n" +
                    "{\n" +
                    "    street : String[1];\n" +
                    "}\n" +
                    "\n" +
                    "###ExternalFormat\n" +
                    "Binding meta::external::store::mongodb::showcase::store::PersonCollectionBinding\n" +
                    "{\n" +
                    "  contentType   : 'application/json';\n" +
                    "  modelIncludes : [\n" +
                    "                    meta::external::store::mongodb::showcase::domain::Person,\n" +
                    "                    meta::external::store::mongodb::showcase::domain::Firm,\n" +
                    "                    meta::external::store::mongodb::showcase::domain::Address\n" +
                    "                  ];\n" +
                    "}\n\n";

    static final String SAMPLE_STORE =
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
                    "\n";

}
