//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.test;

import org.finos.legend.engine.external.format.json.toModel.JsonSchemaToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.test.SchemaToModelGenerationTest;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.junit.Assert;
import org.junit.Test;

public class TestJsonSchemaToModelGeneration extends SchemaToModelGenerationTest
{
    @Test
    public void testSimpleJsonSchema()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test1", "Test.json",
                        "  {\n" +
                                "    \"type\": \"object\",\n" +
                                "    \"description\": \"A simple description\",\n" +
                                "    \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
                                "    \"allOf\": [\n" +
                                "      {\n" +
                                "        \"$ref\": \"SuperType.json\"\n" +
                                "      }\n" +
                                "    ],\n" +
                                "    \"properties\": {\n" +
                                "      \"simpleString\": {\n" +
                                "        \"type\": \"string\",\n" +
                                "        \"nullable\": true\n" +
                                "      },         \"arrayMaxItemOne\": {\n" +
                                "        \"type\": \"array\",\n" +
                                "        \"description\": \"description\",\n" +
                                "        \"items\": {\n" +
                                "          \"type\": \"string\",\n" +
                                "          \"nullable\": true,\n" +
                                "          \"description\": \"A combined\"\n" +
                                "        },\n" +
                                "        \"maxItems\": 10\n" +
                                "      }\n" +
                                "    }\n" +
                                "  }")
                .withSchemaText("Test2", "SuperType.json",
                        "{\n" +
                                "  \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
                                "  \"description\": \"a Super Type Object\",\n" +
                                "  \"type\": \"object\"\n" +
                                "}\n")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

        String expected = ">>>example::jsonSchema::SuperType\n" +
                "Class {meta::pure::profiles::doc.doc = 'a Super Type Object'} example::jsonSchema::SuperType extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::Test\n" +
                "Class {meta::pure::profiles::doc.doc = 'A simple description'} example::jsonSchema::Test extends example::jsonSchema::SuperType\n" +
                "{\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaTypeExtension.null>> simpleString: String[0..1];\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaTypeExtension.null>> {meta::pure::profiles::doc.doc = 'A combined description'} arrayMaxItemOne: String[0..10];\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testDiscriminator()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Cat.json",
                        "{\n" +
                                "    \"title\": \"Cat\",\n" +
                                "    \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
                                "    \"allOf\": [\n" +
                                "        {\n" +
                                "            \"$ref\": \"/shared/Pet.json\"\n" +
                                "        }\n" +
                                "    ],\n" +
                                "    \"type\": \"object\",\n" +
                                "    \"properties\": {\n" +
                                "        \"hasMeow\": {\n" +
                                "            \"type\": \"boolean\"\n" +
                                "        },\n" +
                                "\"friend\": {\n" +
                                "      \"$ref\": \"friends.json\"\n" +
                                "    }\n" +
                                "    }\n" +
                                "}\n")
                .withSchemaText("Test2", "BigCat.json",
                        "{\n" +
                                "    \"title\": \"Big Cat\",\n" +
                                "    \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
                                "    \"allOf\": [\n" +
                                "        {\n" +
                                "            \"$ref\": \"Cat.json\"\n" +
                                "        }\n" +
                                "    ],\n" +
                                "    \"type\": \"object\",\n" +
                                "    \"properties\": {\n" +
                                "        \"harRoar\": {\n" +
                                "            \"type\": \"boolean\"\n" +
                                "        }    }\n" +
                                "}\n")
                .withSchemaText("Test3", "Dog.json",
                        "{\n" +
                                "    \"title\": \"Dog\",\n" +
                                "    \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
                                "    \"allOf\": [\n" +
                                "        {\n" +
                                "            \"$ref\": \"/shared/Pet.json\"\n" +
                                "        }\n" +
                                "    ],\n" +
                                "    \"type\": \"object\",\n" +
                                "    \"properties\": {\n" +
                                "        \"hasBark\": {\n" +
                                "            \"type\": \"boolean\"\n" +
                                "        },\n" +
                                "        \"friends\": {\n" +
                                "          \"type\": \"array\",\n" +
                                "          \"uniqueItems\": true,\n" +
                                "          \"items\": {\n" +
                                "      \"$ref\": \"friendsNoMap.json\"\n" +
                                "          }\n" +
                                "        },\n" +
                                "\"friendsMaxThree\": {\n" +
                                "          \"type\": \"array\",\n" +
                                "\"maxItems\":3,\"minItems\":1,          \"items\": {\n" +
                                "      \"$ref\": \"friendsNoMap.json\"\n" +
                                "          }\n" +
                                "        },\n" +
                                "        \"bestFriend\": {\n" +
                                "      \"$ref\": \"friendsNoMap.json\"\n" +
                                "        },\n" +
                                "        \"siblings\": {\n" +
                                "          \"type\": \"array\",\n" +
                                "          \"uniqueItems\": true,\n" +
                                "          \"items\": {\n" +
                                "      \"$ref\": \"Dog.json\"\n" +
                                "          }\n" +
                                "        }\n" +
                                "    }\n" +
                                "}\n")
                .withSchemaText("Test4", "/shared/Pet.json",
                        "{\n" +
                                "    \"title\": \"Pet\",\n" +
                                "    \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
                                "    \"type\": \"object\",\n" +
                                "    \"properties\": {\n" +
                                "        \"petType\": {\n" +
                                "            \"type\": \"string\",\n" +
                                "            \"enum\": [\n" +
                                "                \"Feline\",\n" +
                                "                \"Canine\",\n" +
                                "                \"woof\"\n" +
                                "            ]\n" +
                                "        }\n" +
                                "    },\n" +
                                "    \"discriminator\": {  \n" +
                                "        \"propertyName\": \"petType\",\n" +
                                "        \"mapping\": {\n" +
                                "            \"Feline\": \"Cat.json\",\n" +
                                "            \"Canine\": \"Dog.json\",\n" +
                                "              \"woof\": \"Dog.json\",\n" +
                                "             \"bigCat\": \"BigCat.json\"\n" +
                                "        }\n" +
                                "    },\n" +
                                "\"required\": [\n" +
                                "    \"petType\"\n" +
                                "  ]}")
                .withSchemaText("Test5", "friends.json",
                        "{\n" +
                                "    \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
                                "    \"oneOf\": [\n" +
                                "        {\n" +
                                "            \"$ref\": \"Cat.json\"\n" +
                                "        },\n" +
                                "        {\n" +
                                "            \"$ref\": \"Dog.json\"\n" +
                                "        }\n" +
                                "    ],\n" +
                                "    \"discriminator\": {  \n" +
                                "        \"propertyName\": \"petType\",\n" +
                                "        \"mapping\": {\n" +
                                "            \"Feline\": \"Cat.json\",\n" +
                                "            \"Canine\": \"Dog.json\",\n" +
                                "              \"woof\": \"Dog.json\",\n" +
                                "             \"bigCat\": \"BigCat.json\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}")
                .withSchemaText("Test6", "friendsNoMap.json",
                        "{\n" +
                                "    \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
                                "    \"oneOf\": [\n" +
                                "        {\n" +
                                "            \"$ref\": \"Cat.json\"\n" +
                                "        },\n" +
                                "        {\n" +
                                "            \"$ref\": \"Dog.json\"\n" +
                                "        }\n" +
                                "    ],\n" +
                                "    \"discriminator\": {  \n" +
                                "        \"propertyName\": \"petType\"\n" +
                                "    }\n" +
                                "}")

                .withSchemaText("Test7", "anyAnimal.json",
                        " {\n" +
                                "    \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
                                "  \"anyOf\": [\n" +
                                "    {\n" +
                                "      \"$ref\": \"Cat.json\"\n" +
                                "    },\n" +
                                "    {\n" +
                                "      \"$ref\": \"Dog.json\"\n" +
                                "    },\n" +
                                "    {\n" +
                                "      \"$ref\": \"BigCat.json\"\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

        String expected = ">>>example::jsonSchema::BigCat\n" +
                "Class {meta::external::format::json::binding::toPure::JSONSchemaGeneration.title = 'Big Cat', meta::external::format::json::binding::toPure::JSONSchemaOpenAPIExtension.discriminatorName = 'bigCat'} example::jsonSchema::BigCat extends example::jsonSchema::Cat\n" +
                "{\n" +
                "  harRoar: Boolean[0..1];\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::Cat\n" +
                "Class {meta::external::format::json::binding::toPure::JSONSchemaGeneration.title = 'Cat', meta::external::format::json::binding::toPure::JSONSchemaOpenAPIExtension.discriminatorName = 'Feline'} example::jsonSchema::Cat extends example::jsonSchema::shared::Pet\n" +
                "[\n" +
                "  friend_inLine: if($this.friend->isNotEmpty(), |$this.friend->toOne()->example::jsonSchema::friends(), |true)\n" +
                "]\n" +
                "{\n" +
                "  friend: example::jsonSchema::shared::Pet[0..1];\n" +
                "  hasMeow: Boolean[0..1];\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::Dog\n" +
                "Class {meta::external::format::json::binding::toPure::JSONSchemaGeneration.title = 'Dog', meta::external::format::json::binding::toPure::JSONSchemaOpenAPIExtension.discriminatorName = 'woof', meta::external::format::json::binding::toPure::JSONSchemaOpenAPIExtension.discriminatorName = 'Canine'} example::jsonSchema::Dog extends example::jsonSchema::shared::Pet\n" +
                "[\n" +
                "  bestFriend_inLine: if($this.bestFriend->isNotEmpty(), |$this.bestFriend->toOne()->example::jsonSchema::friendsNoMap(), |true),\n" +
                "  siblings_inLine: $this.siblings->isDistinct(),\n" +
                "  friendsMaxThree_inLine: $this.friendsMaxThree->forAll(value: example::jsonSchema::shared::Pet[1]|$value->example::jsonSchema::friendsNoMap()),\n" +
                "  friends_inLine: if($this.friends->isNotEmpty(), |$this.friends->forAll(value: example::jsonSchema::shared::Pet[1]|$value->example::jsonSchema::friendsNoMap())->toOne() && $this.friends->isDistinct()->toOne(), |true)\n" +
                "]\n" +
                "{\n" +
                "  hasBark: Boolean[0..1];\n" +
                "  bestFriend: example::jsonSchema::shared::Pet[0..1];\n" +
                "  siblings: example::jsonSchema::Dog[*];\n" +
                "  friendsMaxThree: example::jsonSchema::shared::Pet[1..3];\n" +
                "  friends: example::jsonSchema::shared::Pet[*];\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::anyAnimal_Any_$0_1$__Boolean_1_\n" +
                "function example::jsonSchema::anyAnimal(value: meta::pure::metamodel::type::Any[0..1]): Boolean[1]\n" +
                "{\n" +
                "   if($value->isNotEmpty(), |[$value->toOne()->instanceOf(example::jsonSchema::Cat), $value->toOne()->instanceOf(example::jsonSchema::Dog), $value->toOne()->instanceOf(example::jsonSchema::BigCat)]->or(), |true)\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::friendsNoMap_Pet_1__Boolean_1_\n" +
                "function example::jsonSchema::friendsNoMap(value: example::jsonSchema::shared::Pet[1]): Boolean[1]\n" +
                "{\n" +
                "   $value->discriminateOneOf($value.petType, [example::jsonSchema::Cat, example::jsonSchema::Dog], [])\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::friends_Pet_1__Boolean_1_\n" +
                "function example::jsonSchema::friends(value: example::jsonSchema::shared::Pet[1]): Boolean[1]\n" +
                "{\n" +
                "   $value->discriminateOneOf($value.petType, [example::jsonSchema::Cat, example::jsonSchema::Dog], ['bigCat'->mapSchema(example::jsonSchema::BigCat), 'Feline'->mapSchema(example::jsonSchema::Cat), 'woof'->mapSchema(example::jsonSchema::Dog), 'Canine'->mapSchema(example::jsonSchema::Dog)])\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::shared::Pet\n" +
                "Class {meta::external::format::json::binding::toPure::JSONSchemaGeneration.title = 'Pet'} example::jsonSchema::shared::Pet extends meta::pure::metamodel::type::Any\n" +
                "[\n" +
                "  petType_inLine: $this.petType->in(['Feline', 'Canine', 'woof'])\n" +
                "]\n" +
                "{\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaOpenAPIExtension.discriminatorProperty>> petType: String[1];\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testStringProperty()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Test.json",
                        "{\n" +
                                "  \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
                                "  \"components\": {\n" +
                                "    \"schemas\": {\n" +
                                "      \"meta::json::schema::tests::ClassWithFunctionReferences\": {\n" +
                                "        \"title\": \"a custom title\",\n" +
                                "        \"type\": \"object\",\n" +
                                "        \"properties\": {\n" +
                                "          \"one\": {\n" +
                                "            \"$ref\": \"#/components/schemas/meta::json::schema::tests::functionWithStringType\"\n" +
                                "          },\n" +
                                "          \"optional\": {\n" +
                                "            \"$ref\": \"#/components/schemas/meta::json::schema::tests::functionWithStringType\"\n" +
                                "          },\n" +
                                "          \"many\": {\n" +
                                "            \"type\": \"array\",\n" +
                                "            \"items\": {\n" +
                                "              \"$ref\": \"#/components/schemas/meta::json::schema::tests::functionWithStringType\"\n" +
                                "            }\n" +
                                "          }\n" +
                                "        },\n" +
                                "        \"required\": [\n" +
                                "          \"one\"\n" +
                                "        ]\n" +
                                "      },\n" +
                                "      \"meta::json::schema::tests::SimpleClass\": {\n" +
                                "        \"title\": \"meta::json::schema::tests::SimpleClass\",\n" +
                                "        \"type\": \"object\",\n" +
                                "        \"properties\": {\n" +
                                "          \"p\": {\n" +
                                "            \"type\": \"string\"\n" +
                                "          },\n" +
                                "          \"a\": {\n" +
                                "            \"$ref\": \"#/components/schemas/meta::json::schema::tests::SimpleClass2\"\n" +
                                "          }\n" +
                                "        },\n" +
                                "        \"required\": [\n" +
                                "          \"p\"\n" +
                                "        ]\n" +
                                "      },\n" +
                                "      \"meta::json::schema::tests::SimpleClass2\": {\n" +
                                "        \"title\": \"meta::json::schema::tests::SimpleClass2\",\n" +
                                "        \"type\": \"object\",\n" +
                                "        \"properties\": {\n" +
                                "          \"p\": {\n" +
                                "            \"type\": \"string\"\n" +
                                "          }\n" +
                                "        },\n" +
                                "        \"required\": [\n" +
                                "          \"p\"\n" +
                                "        ]\n" +
                                "      },\n" +
                                "      \"meta::json::schema::tests::functionWithStringType\": {\n" +
                                "        \"pattern\": \"test\",\n" +
                                "        \"type\": \"string\"\n" +
                                "      }\n" +
                                "    }\n" +
                                "  }\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

        String expected = ">>>meta::json::schema::tests::ClassWithFunctionReferences\n" +
                "Class {meta::external::format::json::binding::toPure::JSONSchemaGeneration.title = 'a custom title'} meta::json::schema::tests::ClassWithFunctionReferences extends meta::pure::metamodel::type::Any\n" +
                "[\n" +
                "  one: $this.one->meta::json::schema::tests::functionWithStringType(),\n" +
                "  optional: $this.optional->meta::json::schema::tests::functionWithStringType(),\n" +
                "  many: $this.many->forAll(value: String[1]|$value->meta::json::schema::tests::functionWithStringType())\n" +
                "]\n" +
                "{\n" +
                "  one: String[1];\n" +
                "  optional: String[0..1];\n" +
                "  many: String[*];\n" +
                "}\n" +
                "\n" +
                ">>>meta::json::schema::tests::SimpleClass\n" +
                "Class meta::json::schema::tests::SimpleClass extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  p: String[1];\n" +
                "  a: meta::json::schema::tests::SimpleClass2[0..1];\n" +
                "}\n" +
                "\n" +
                ">>>meta::json::schema::tests::SimpleClass2\n" +
                "Class meta::json::schema::tests::SimpleClass2 extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  p: String[1];\n" +
                "}\n" +
                "\n" +
                ">>>meta::json::schema::tests::functionWithStringType_String_$0_1$__Boolean_1_\n" +
                "function meta::json::schema::tests::functionWithStringType(value: String[0..1]): Boolean[1]\n" +
                "{\n" +
                "   if($value->isNotEmpty(), |$value->makeString()->matches('test'), |true)\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    private JsonSchemaToModelConfiguration config(String sourceSchemaSet, String targetPackage)
    {
        JsonSchemaToModelConfiguration config = new JsonSchemaToModelConfiguration();
        config.sourceSchemaSet = sourceSchemaSet;
        config.targetBinding = targetPackage + "::TestBinding";
        config.targetPackage = targetPackage;
        return config;
    }
}
