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

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.format.json.fromModel.ModelToJsonSchemaConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.Binding;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatSchemaSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.finos.legend.engine.external.shared.format.model.test.ModelToSchemaGenerationTest.generateSchema;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestModelToJsonSchemaGeneration
{
    @Test
    public void testSimpleJsonSchema()
    {
        String modelCode = "Class test::gen::Data\n" +
                "{\n" +
                "  name        : String[1];\n" +
                "  employed    : Boolean[0..1];\n" +
                "  iq          : Integer[0..1];\n" +
                "  weightKg    : Float[0..1];\n" +
                "  heightM     : Decimal[1];\n" +
                "  dateOfBirth : StrictDate[1];\n" +
                "  timeOfDeath : DateTime[1];\n" +
                "}";

        PureModelContextData generated = generateSchema(modelCode, config("test::gen", Lists.mutable.with("test::gen::Data")));
        Binding binding = generated.getElementsOfType(Binding.class).stream().findFirst().get();
        Assert.assertEquals("test::gen::TestBinding", binding.getPath());
        Assert.assertEquals("test::gen::TestSchemaSet", binding.schemaSet);
        Assert.assertEquals(Collections.singletonList("test::gen::Data"), binding.modelUnit.packageableElementIncludes);

        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        String expectedDefinition = "{\n" +
                "  \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
                "  \"title\": \"test::gen::Data\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\":   {\n" +
                "    \"name\":     {\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"employed\":     {\n" +
                "      \"type\": \"boolean\"\n" +
                "    },\n" +
                "    \"iq\":     {\n" +
                "      \"type\": \"integer\"\n" +
                "    },\n" +
                "    \"weightKg\":     {\n" +
                "      \"type\": \"number\"\n" +
                "    },\n" +
                "    \"heightM\":     {\n" +
                "      \"type\": \"number\"\n" +
                "    },\n" +
                "    \"dateOfBirth\":     {\n" +
                "      \"type\": \"string\",\n" +
                "      \"format\": \"date\"\n" +
                "    },\n" +
                "    \"timeOfDeath\":     {\n" +
                "      \"type\": \"string\",\n" +
                "      \"format\": \"date-time\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\n" +
                "\"name\",\n" +
                "\"heightM\",\n" +
                "\"dateOfBirth\",\n" +
                "\"timeOfDeath\"\n" +
                "  ]\n" +
                "}\n";
        assertThat(schemaSet.schemas.get(0).content, jsonEquals(expectedDefinition));
    }

    @Test
    public void testDiscriminateOneOf()
    {
        String modelCode = "function test::Simple::subtypeAnimal(value: test::Simple::Animal[1]):Boolean[1]\n" +
                "{\n" +
                "  $value->meta::external::format::json::binding::fromPure::discriminateOneOf($value.type,[test::Simple::Dog,test::Simple::Fish,test::Simple::Cat],[mapSchema('CAT',test::Simple::Cat),mapSchema('dog',test::Simple::Dog),mapSchema('DOG',test::Simple::Dog)]);\n" +
                "}" +
                "Class <<typemodifiers.abstract>>\n" +
                "{doc.doc = 'The Being Class'}\n" +
                "test::Simple::Being\n" +
                "{\n" +
                "   omg:String[1];\n" +
                "}\n" +
                "\n" +
                "Class\n" +
                "{doc.doc = 'The Animal Class'}\n" +
                "test::Simple::Animal extends test::Simple::Being\n" +
                "{\n" +
                "\n" +
                "   name:String[1];\n" +
                "   age:Integer[1];\n" +
                "   <<meta::external::format::json::binding::toPure::JSONSchemaOpenAPIExtension.discriminatorProperty>> type: test::Simple::AnimalType[1];\n" +
                "   aliases:String[*];\n" +
                "   extra: test::Simple::OtherInfo[4];\n" +
                "}\n" +
                "\n" +
                "Class\n" +
                "{doc.doc = 'The Cat Class', meta::external::format::json::binding::toPure::JSONSchemaOpenAPIExtension.discriminatorName='CAT', meta::external::format::json::binding::toPure::JSONSchemaOpenAPIExtension.discriminatorName='cat'}\n" +
                "test::Simple::Cat extends test::Simple::Animal\n" +
                "{\n" +
                "   {doc.doc = 'Is the animal a vegetarian?'}\n" +
                "   vegetarian:Boolean[1];\n" +
                "}" +
                "Enum\n" +
                "{doc.doc = 'The AnimalType Enum'}\n" +
                "test::Simple::AnimalType\n" +
                "{\n" +
                "     CAT, DOG, BIRD, FISH, HUMAN\n" +
                "}" +
                "Class\n" +
                "{doc.doc = 'The OtherInfo Class'}\n" +
                "test::Simple::OtherInfo\n" +
                "{\n" +
                "   info:String[1];\n" +
                "}" +
                "Class\n" +
                "{doc.doc = 'The Dog Class', meta::external::format::json::binding::toPure::JSONSchemaOpenAPIExtension.discriminatorName='DOG'}\n" +
                "test::Simple::Dog extends test::Simple::Animal\n" +
                "{\n" +
                "   {doc.doc = 'Does the dog bark?'}\n" +
                "   hasBark:Boolean[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "Class\n" +
                "{doc.doc = 'The Fish Class'}\n" +
                "test::Simple::Fish extends test::Simple::Animal\n" +
                "{\n" +
                "\n" +
                "}";

        PureModelContextData generated = generateSchema(modelCode, config("test::gen", Lists.mutable.with("test::Simple::Animal", "test::Simple::subtypeAnimal_test::Simple::Animal_1__Boolean_1_")));
        Binding binding = generated.getElementsOfType(Binding.class).stream().findFirst().get();
        Assert.assertEquals("test::gen::TestBinding", binding.getPath());
        Assert.assertEquals("test::gen::TestSchemaSet", binding.schemaSet);
        Assert.assertEquals(Lists.mutable.with("test::Simple::Animal", "test::Simple::subtypeAnimal_Animal_1__Boolean_1_"), binding.modelUnit.packageableElementIncludes);

        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        String expectedDefinition = "{\n" +
                "  \"allOf\" : [ {\n" +
                "    \"$ref\" : \"#/components/schemas/test::Simple::Being\"\n" +
                "  } ],\n" +
                "  \"title\" : \"test::Simple::Animal\",\n" +
                "  \"description\" : \"The Animal Class\",\n" +
                "  \"discriminator\" : {\n" +
                "    \"propertyName\" : \"type\",\n" +
                "    \"mapping\" : {\n" +
                "      \"CAT\" : \"#/components/schemas/test::Simple::Cat\",\n" +
                "      \"DOG\" : \"#/components/schemas/test::Simple::Dog\",\n" +
                "      \"cat\" : \"#/components/schemas/test::Simple::Cat\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"type\" : \"object\",\n" +
                "  \"components\" : {\n" +
                "    \"schemas\" : {\n" +
                "      \"test::Simple::AnimalType\" : {\n" +
                "        \"title\" : \"test::Simple::AnimalType\",\n" +
                "        \"description\" : \"The AnimalType Enum\",\n" +
                "        \"type\" : \"string\",\n" +
                "        \"enum\" : [ \"CAT\", \"DOG\", \"BIRD\", \"FISH\", \"HUMAN\" ]\n" +
                "      },\n" +
                "      \"test::Simple::Being\" : {\n" +
                "        \"title\" : \"test::Simple::Being\",\n" +
                "        \"description\" : \"The Being Class\",\n" +
                "        \"type\" : \"object\",\n" +
                "        \"properties\" : {\n" +
                "          \"omg\" : {\n" +
                "            \"type\" : \"string\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"required\" : [ \"omg\" ]\n" +
                "      },\n" +
                "      \"test::Simple::Cat\" : {\n" +
                "        \"allOf\" : [ {\n" +
                "          \"$ref\" : \"#/components/schemas/test::Simple::Animal\"\n" +
                "        } ],\n" +
                "        \"title\" : \"test::Simple::Cat\",\n" +
                "        \"description\" : \"The Cat Class\",\n" +
                "        \"type\" : \"object\",\n" +
                "        \"properties\" : {\n" +
                "          \"vegetarian\" : {\n" +
                "            \"description\" : \"Is the animal a vegetarian?\",\n" +
                "            \"type\" : \"boolean\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"required\" : [ \"vegetarian\" ]\n" +
                "      },\n" +
                "      \"test::Simple::Dog\" : {\n" +
                "        \"allOf\" : [ {\n" +
                "          \"$ref\" : \"#/components/schemas/test::Simple::Animal\"\n" +
                "        } ],\n" +
                "        \"title\" : \"test::Simple::Dog\",\n" +
                "        \"description\" : \"The Dog Class\",\n" +
                "        \"type\" : \"object\",\n" +
                "        \"properties\" : {\n" +
                "          \"hasBark\" : {\n" +
                "            \"description\" : \"Does the dog bark?\",\n" +
                "            \"type\" : \"boolean\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"required\" : [ \"hasBark\" ]\n" +
                "      },\n" +
                "      \"test::Simple::Fish\" : {\n" +
                "        \"allOf\" : [ {\n" +
                "          \"$ref\" : \"#/components/schemas/test::Simple::Animal\"\n" +
                "        } ],\n" +
                "        \"title\" : \"test::Simple::Fish\",\n" +
                "        \"description\" : \"The Fish Class\",\n" +
                "        \"type\" : \"object\"\n" +
                "      },\n" +
                "      \"test::Simple::OtherInfo\" : {\n" +
                "        \"title\" : \"test::Simple::OtherInfo\",\n" +
                "        \"description\" : \"The OtherInfo Class\",\n" +
                "        \"type\" : \"object\",\n" +
                "        \"properties\" : {\n" +
                "          \"info\" : {\n" +
                "            \"type\" : \"string\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"required\" : [ \"info\" ]\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"properties\" : {\n" +
                "    \"age\" : {\n" +
                "      \"type\" : \"integer\"\n" +
                "    },\n" +
                "    \"aliases\" : {\n" +
                "      \"type\" : \"array\",\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"extra\" : {\n" +
                "      \"type\" : \"array\",\n" +
                "      \"minItems\" : 4,\n" +
                "      \"maxItems\" : 4,\n" +
                "      \"items\" : {\n" +
                "        \"$ref\" : \"#/components/schemas/test::Simple::OtherInfo\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"name\" : {\n" +
                "      \"type\" : \"string\"\n" +
                "    },\n" +
                "    \"type\" : {\n" +
                "      \"$ref\" : \"#/components/schemas/test::Simple::AnimalType\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"$schema\" : \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
                "  \"required\" : [ \"name\", \"age\", \"type\", \"extra\" ]\n" +
                "}";

        assertThat(schemaSet.schemas.get(0).content, jsonEquals(expectedDefinition));

        expectedDefinition = "{\n" +
                "  \"oneOf\" : [ {\n" +
                "    \"$ref\" : \"#/components/schemas/test::Simple::Dog\"\n" +
                "  }, {\n" +
                "    \"$ref\" : \"#/components/schemas/test::Simple::Fish\"\n" +
                "  }, {\n" +
                "    \"$ref\" : \"#/components/schemas/test::Simple::Cat\"\n" +
                "  } ],\n" +
                "  \"title\" : \"test::Simple::subtypeAnimal\",\n" +
                "  \"discriminator\" : {\n" +
                "    \"propertyName\" : \"type\",\n" +
                "    \"mapping\" : {\n" +
                "      \"CAT\" : \"#/components/schemas/test::Simple::Cat\",\n" +
                "      \"DOG\" : \"#/components/schemas/test::Simple::Dog\",\n" +
                "      \"dog\" : \"#/components/schemas/test::Simple::Dog\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"$schema\" : \"https://spec.openapis.org/oas/v3.0.3#specification\"\n" +
                "}";

        assertThat(schemaSet.schemas.get(1).content, jsonEquals(expectedDefinition));
    }

    private ModelToJsonSchemaConfiguration config(String targetPackage, List<String> sourceModels)
    {
        ModelToJsonSchemaConfiguration config = new ModelToJsonSchemaConfiguration("https://spec.openapis.org/oas/v3.0.3#specification");
        config.targetBinding = targetPackage + "::TestBinding";
        config.targetSchemaSet = targetPackage + "::TestSchemaSet";
        config.sourceModel = sourceModels;
        config.format = "JSON";
        return config;
    }
}
