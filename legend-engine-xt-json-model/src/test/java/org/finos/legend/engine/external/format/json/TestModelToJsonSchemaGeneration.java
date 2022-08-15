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

package org.finos.legend.engine.external.format.json;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.format.json.fromModel.ModelToJsonSchemaConfiguration;
import org.finos.legend.engine.external.shared.format.model.test.ModelToSchemaGenerationTest;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchemaSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class TestModelToJsonSchemaGeneration extends ModelToSchemaGenerationTest
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
        String expectedDefiniiton = "{\n" +
                "  \"$schema\": \"http:\\/\\/json-schema.org\\/draft-07\\/schema#\",\n" +
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
        Assert.assertEquals(expectedDefiniiton, schemaSet.schemas.get(0).content);
    }

    @Test
    public void testVariousMultiplicityPropertiesWithJsonSchema()
    {
        String modelCode = "Class test::gen::Data\n" +
                "{\n" +
                "  floatField: Float[1];\n" +
                "  floatRangeField: Float[1..3];\n" +
                "  stringRangeZeroField: String[0..3];\n" +
                "  decimalRangeZeroField: Float[0..3];\n" +
                "  booleanField: Boolean[1];\n" +
                "  strictDateRangeZeroField: String[0..3];\n" +
                "  dateTimeField: DateTime[1];\n" +
                "  strictDateRangeField: String[1..3];\n" +
                "  integerRangeZeroField: Integer[0..3];\n" +
                "  strictDateField: StrictDate[1];\n" +
                "  strictDateMultipleField: String[*];\n" +
                "  dateTimeRangeField: String[1..3];\n" +
                "  floatRangeZeroField: Float[0..3];\n" +
                "  integerMultipleField: Integer[*];\n" +
                "  decimalField: Float[1];\n" +
                "  decimalRangeField: Float[1..3];\n" +
                "  dateRangeZeroField: String[0..3];\n" +
                "  dateTimeMultipleField: String[*];\n" +
                "  stringRangeField: String[1..3];\n" +
                "  dateField: DateTime[1];\n" +
                "  dateTimeRangeZeroField: String[0..3];\n" +
                "  floatMultipleField: Float[*];\n" +
                "  stringNoDescriptionField: String[1];\n" +
                "  integerRangeField: Integer[1..3];\n" +
                "  srtingMultipleField: String[*];\n" +
                "  dateRangeField: String[1..3];\n" +
                "  integerField: Integer[1];\n" +
                "  decimalMultipleField: Float[*];\n" +
                "  stringField: String[1];\n" +
                "  dateMultipleField: String[*];\n" +
                "}\n";

        PureModelContextData generated = generateSchema(modelCode, config("test::gen", Lists.mutable.with("test::gen::Data")));
        Binding binding = generated.getElementsOfType(Binding.class).stream().findFirst().get();
        Assert.assertEquals("test::gen::TestBinding", binding.getPath());
        Assert.assertEquals("test::gen::TestSchemaSet", binding.schemaSet);
        Assert.assertEquals(Collections.singletonList("test::gen::Data"), binding.modelUnit.packageableElementIncludes);

        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        String expectedDefiniiton = "{\n" +
                "  \"$schema\": \"http:\\/\\/json-schema.org\\/draft-07\\/schema#\",\n" +
                "  \"title\": \"test::gen::Data\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\":   {\n" +
                "    \"floatField\":     {\n" +
                "      \"type\": \"number\"\n" +
                "    },\n" +
                "    \"floatRangeField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"number\"\n" +
                "      },\n" +
                "      \"minItems\": 1,\n" +
                "      \"maxItems\": 3\n" +
                "    },\n" +
                "    \"stringRangeZeroField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"string\"\n" +
                "      },\n" +
                "      \"maxItems\": 3\n" +
                "    },\n" +
                "    \"decimalRangeZeroField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"number\"\n" +
                "      },\n" +
                "      \"maxItems\": 3\n" +
                "    },\n" +
                "    \"booleanField\":     {\n" +
                "      \"type\": \"boolean\"\n" +
                "    },\n" +
                "    \"strictDateRangeZeroField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"string\"\n" +
                "      },\n" +
                "      \"maxItems\": 3\n" +
                "    },\n" +
                "    \"dateTimeField\":     {\n" +
                "      \"type\": \"string\",\n" +
                "      \"format\": \"date-time\"\n" +
                "    },\n" +
                "    \"strictDateRangeField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"string\"\n" +
                "      },\n" +
                "      \"minItems\": 1,\n" +
                "      \"maxItems\": 3\n" +
                "    },\n" +
                "    \"integerRangeZeroField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"integer\"\n" +
                "      },\n" +
                "      \"maxItems\": 3\n" +
                "    },\n" +
                "    \"strictDateField\":     {\n" +
                "      \"type\": \"string\",\n" +
                "      \"format\": \"date\"\n" +
                "    },\n" +
                "    \"strictDateMultipleField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"string\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"dateTimeRangeField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"string\"\n" +
                "      },\n" +
                "      \"minItems\": 1,\n" +
                "      \"maxItems\": 3\n" +
                "    },\n" +
                "    \"floatRangeZeroField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"number\"\n" +
                "      },\n" +
                "      \"maxItems\": 3\n" +
                "    },\n" +
                "    \"integerMultipleField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"integer\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"decimalField\":     {\n" +
                "      \"type\": \"number\"\n" +
                "    },\n" +
                "    \"decimalRangeField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"number\"\n" +
                "      },\n" +
                "      \"minItems\": 1,\n" +
                "      \"maxItems\": 3\n" +
                "    },\n" +
                "    \"dateRangeZeroField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"string\"\n" +
                "      },\n" +
                "      \"maxItems\": 3\n" +
                "    },\n" +
                "    \"dateTimeMultipleField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"string\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"stringRangeField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"string\"\n" +
                "      },\n" +
                "      \"minItems\": 1,\n" +
                "      \"maxItems\": 3\n" +
                "    },\n" +
                "    \"dateField\":     {\n" +
                "      \"type\": \"string\",\n" +
                "      \"format\": \"date-time\"\n" +
                "    },\n" +
                "    \"dateTimeRangeZeroField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"string\"\n" +
                "      },\n" +
                "      \"maxItems\": 3\n" +
                "    },\n" +
                "    \"floatMultipleField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"number\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"stringNoDescriptionField\":     {\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"integerRangeField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"integer\"\n" +
                "      },\n" +
                "      \"minItems\": 1,\n" +
                "      \"maxItems\": 3\n" +
                "    },\n" +
                "    \"srtingMultipleField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"string\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"dateRangeField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"string\"\n" +
                "      },\n" +
                "      \"minItems\": 1,\n" +
                "      \"maxItems\": 3\n" +
                "    },\n" +
                "    \"integerField\":     {\n" +
                "      \"type\": \"integer\"\n" +
                "    },\n" +
                "    \"decimalMultipleField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"number\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"stringField\":     {\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"dateMultipleField\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"type\": \"string\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\n" +
                "\"floatField\",\n" +
                "\"floatRangeField\",\n" +
                "\"booleanField\",\n" +
                "\"dateTimeField\",\n" +
                "\"strictDateRangeField\",\n" +
                "\"strictDateField\",\n" +
                "\"dateTimeRangeField\",\n" +
                "\"decimalField\",\n" +
                "\"decimalRangeField\",\n" +
                "\"stringRangeField\",\n" +
                "\"dateField\",\n" +
                "\"stringNoDescriptionField\",\n" +
                "\"integerRangeField\",\n" +
                "\"dateRangeField\",\n" +
                "\"integerField\",\n" +
                "\"stringField\"\n" +
                "  ]\n" +
                "}\n";
        Assert.assertEquals(expectedDefiniiton, schemaSet.schemas.get(0).content);
    }

    @Test
    public void testNestingWithJsonSchema()
    {
        String modelCode =
                "Class test::Simple::Person\n" +
                        "{\n" +
                        "  firstName: String[1];\n" +
                        "  lastName: String[1];\n" +
                        "  middleName: String[0..1];\n" +
                        "  age: Integer[0..1];\n" +
                        "  firm: test::Simple::Firm[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::Simple::Firm\n" +
                        "{\n" +
                        "  legalName: String[1];\n" +
                        "}\n";

        PureModelContextData generated = generateSchema(modelCode, config("test::gen", Lists.mutable.with("test::Simple::Person", "test::Simple::Firm")));
        Binding binding = generated.getElementsOfType(Binding.class).stream().findFirst().get();
        Assert.assertEquals("test::gen::TestBinding", binding.getPath());
        Assert.assertEquals("test::gen::TestSchemaSet", binding.schemaSet);
        Assert.assertEquals(Lists.mutable.with("test::Simple::Person", "test::Simple::Firm"), binding.modelUnit.packageableElementIncludes);

        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        String expectedDefiniiton = "{\n" +
                "  \"$schema\": \"http:\\/\\/json-schema.org\\/draft-07\\/schema#\",\n" +
                "  \"title\": \"test::Simple::Person\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\":   {\n" +
                "    \"firstName\":     {\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"lastName\":     {\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"middleName\":     {\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"age\":     {\n" +
                "      \"type\": \"integer\"\n" +
                "    },\n" +
                "    \"firm\":     {\n" +
                "      \"$ref\": \"#\\/definitions\\/test::Simple::Firm\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\n" +
                "\"firstName\",\n" +
                "\"lastName\",\n" +
                "\"firm\"\n" +
                "  ],\n" +
                "  \"definitions\":   {\n" +
                "    \"test::Simple::Firm\":     {\n" +
                "      \"$schema\": \"http:\\/\\/json-schema.org\\/draft-07\\/schema#\",\n" +
                "      \"title\": \"test::Simple::Firm\",\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\":       {\n" +
                "        \"legalName\":         {\n" +
                "          \"type\": \"string\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\": [\n" +
                "\"legalName\"\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
        Assert.assertEquals(expectedDefiniiton, schemaSet.schemas.get(0).content);
    }

    @Test
    public void testMultiLevelNestingWithJsonSchema()
    {
        String modelCode = "Enum test::Simple::AddressType\n" +
                "{\n" +
                "  HOME,\n" +
                "  OFFICE,\n" +
                "  WORKSHOP\n" +
                "}\n" +
                "\n" +
                "Class test::Simple::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "  middleName: String[0..1];\n" +
                "  age: Integer[0..1];\n" +
                "  addresses: test::Simple::Address[*];\n" +
                "  firm: test::Simple::Firm[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Simple::Address\n" +
                "{\n" +
                "  addressType: test::Simple::AddressType[1];\n" +
                "  addressLine1: String[1];\n" +
                "  addressLine2: String[0..1];\n" +
                "  addressLine3: String[0..1];\n" +
                "}\n" +
                "\n" +
                "Class test::Simple::Firm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  addresses: test::Simple::Address[*];\n" +
                "}\n";

        PureModelContextData generated = generateSchema(modelCode, config("test::gen", Lists.mutable.with("test::Simple::Person", "test::Simple::Firm", "test::Simple::Address", "test::Simple::AddressType")));
        Binding binding = generated.getElementsOfType(Binding.class).stream().findFirst().get();
        Assert.assertEquals("test::gen::TestBinding", binding.getPath());
        Assert.assertEquals("test::gen::TestSchemaSet", binding.schemaSet);
        Assert.assertEquals(Lists.mutable.with("test::Simple::Person", "test::Simple::Firm", "test::Simple::Address", "test::Simple::AddressType"), binding.modelUnit.packageableElementIncludes);

        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        String expectedDefiniiton = "{\n" +
                "  \"$schema\": \"http:\\/\\/json-schema.org\\/draft-07\\/schema#\",\n" +
                "  \"title\": \"test::Simple::Person\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\":   {\n" +
                "    \"firstName\":     {\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"lastName\":     {\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"middleName\":     {\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"age\":     {\n" +
                "      \"type\": \"integer\"\n" +
                "    },\n" +
                "    \"addresses\":     {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\":       {\n" +
                "        \"$ref\": \"#\\/definitions\\/test::Simple::Address\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"firm\":     {\n" +
                "      \"$ref\": \"#\\/definitions\\/test::Simple::Firm\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\n" +
                "\"firstName\",\n" +
                "\"lastName\",\n" +
                "\"firm\"\n" +
                "  ],\n" +
                "  \"definitions\":   {\n" +
                "    \"test::Simple::Address\":     {\n" +
                "      \"$schema\": \"http:\\/\\/json-schema.org\\/draft-07\\/schema#\",\n" +
                "      \"title\": \"test::Simple::Address\",\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\":       {\n" +
                "        \"addressType\":         {\n" +
                "          \"$ref\": \"#\\/definitions\\/test::Simple::AddressType\"\n" +
                "        },\n" +
                "        \"addressLine1\":         {\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        \"addressLine2\":         {\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        \"addressLine3\":         {\n" +
                "          \"type\": \"string\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\": [\n" +
                "\"addressType\",\n" +
                "\"addressLine1\"\n" +
                "      ]\n" +
                "    },\n" +
                "    \"test::Simple::AddressType\":     {\n" +
                "      \"$schema\": \"http:\\/\\/json-schema.org\\/draft-07\\/schema#\",\n" +
                "      \"title\": \"test::Simple::AddressType\",\n" +
                "      \"enum\": [\n" +
                "\"HOME\",\n" +
                "\"OFFICE\",\n" +
                "\"WORKSHOP\"\n" +
                "      ],\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"test::Simple::Firm\":     {\n" +
                "      \"$schema\": \"http:\\/\\/json-schema.org\\/draft-07\\/schema#\",\n" +
                "      \"title\": \"test::Simple::Firm\",\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\":       {\n" +
                "        \"legalName\":         {\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        \"addresses\":         {\n" +
                "          \"type\": \"array\",\n" +
                "          \"items\":           {\n" +
                "            \"$ref\": \"#\\/definitions\\/test::Simple::Address\"\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\": [\n" +
                "\"legalName\"\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
        Assert.assertEquals(expectedDefiniiton, schemaSet.schemas.get(0).content);
    }

    private ModelToJsonSchemaConfiguration config(String targetPackage, List<String> sourceModels)
    {
        ModelToJsonSchemaConfiguration config = new ModelToJsonSchemaConfiguration();
        config.targetBinding = targetPackage + "::TestBinding";
        config.targetSchemaSet = targetPackage + "::TestSchemaSet";
        config.sourceModel = sourceModels;
        config.format = "JSON";
        return config;
    }
}
