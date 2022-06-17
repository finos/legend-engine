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

package org.finos.legend.engine.external.format.json.specifications.draftv7.test;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.format.json.fromModel.ModelToJsonSchemaConfiguration;
import org.finos.legend.engine.external.shared.format.model.test.ModelToSchemaGenerationTest;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.Binding;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatSchemaSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;

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
        String expectedDefinition = "{\n" +
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
        assertThat(schemaSet.schemas.get(0).content, jsonEquals(expectedDefinition));
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
        String expectedDefinition = "{\n" +
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
        assertThat(schemaSet.schemas.get(0).content, jsonEquals(expectedDefinition));
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
        String expectedDefinition = "{\n" +
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
        assertThat(expectedDefinition, jsonEquals(schemaSet.schemas.get(0).content));
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
        String expectedDefinition = "{\n" +
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
                "      \"title\": \"test::Simple::AddressType\",\n" +
                "      \"enum\": [\n" +
                "\"HOME\",\n" +
                "\"OFFICE\",\n" +
                "\"WORKSHOP\"\n" +
                "      ],\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"test::Simple::Firm\":     {\n" +
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
        assertThat(schemaSet.schemas.get(0).content, jsonEquals(expectedDefinition));
    }

    @Test
    public void testPrimitiveTypeDomain()
    {
        String modelCode = "Class test::Simple::PrimitiveTypeDomain\n" +
                "{\n" +
                "   {doc.doc = 'String Field'}\n" +
                "   stringField : String[1];\n" +
                "\n" +
                "   {doc.doc = 'Float Field'}\n" +
                "   floatField: Float[1];\n" +
                "\n" +
                "   {doc.doc = 'Integer Field'}\n" +
                "   integerField: Integer[1];\n" +
                "\n" +
                "   {doc.doc = 'Date Field'}\n" +
                "   dateField: Date[1];\n" +
                "\n" +
                "   {doc.doc = 'DateTime Field'}\n" +
                "   dateTimeField: DateTime[1];\n" +
                "\n" +
                "   {doc.doc = 'StrictDate Field'}\n" +
                "   strictDateField: StrictDate[1];\n" +
                "\n" +
                "   {doc.doc = 'Boolean Field'}\n" +
                "   booleanField: Boolean[1];\n" +
                "\n" +
                "   stringNoDescriptionField : String[1];\n" +
                "\n" +
                "   {doc.doc = 'String Field- multiple'}\n" +
                "   srtingMultipleField: String[*];\n" +
                "\n" +
                "   {doc.doc = 'Float Field - multiple'}\n" +
                "   floatMultipleField: Float[*];\n" +
                "\n" +
                "   {doc.doc = 'Integer Field - multiple'}\n" +
                "   integerMultipleField: Integer[*];\n" +
                "\n" +
                "   {doc.doc = 'Date Field-multiple'}\n" +
                "   dateMultipleField: Date[*];\n" +
                "\n" +
                "   {doc.doc = 'DateTime Field-multiple'}\n" +
                "   dateTimeMultipleField: DateTime[*];\n" +
                "\n" +
                "   {doc.doc = 'StrictDate Field-multiple'}\n" +
                "   strictDateMultipleField: StrictDate[*];\n" +
                "\n" +
                "   {doc.doc = 'Field String - Range 1..3'}\n" +
                "   stringRangeField: String[1..3];\n" +
                "\n" +
                "   {doc.doc = 'Field String - Range 0..3'}\n" +
                "   stringRangeZeroField: String[0..3];\n" +
                "\n" +
                "   {doc.doc = 'Field Integer - Range 1..3'}\n" +
                "   integerRangeField: Integer[1..3];\n" +
                "\n" +
                "   {doc.doc = 'Field Integer - Range 0..3'}\n" +
                "   integerRangeZeroField: Integer[0..3];\n" +
                "\n" +
                "   {doc.doc = 'Field Float - range 1..3'}\n" +
                "   floatRangeField: Float[1..3];\n" +
                "\n" +
                "   {doc.doc = 'Field Float - range 0..3'}\n" +
                "   floatRangeZeroField: Float[0..3];\n" +
                "\n" +
                "   {doc.doc = 'Date Field-multiple 1..3'}\n" +
                "   dateRangeField: Date[1..3];\n" +
                "\n" +
                "   {doc.doc = 'Date Field-multiple 0..3'}\n" +
                "   dateRangeZeroField: Date[0..3];\n" +
                "\n" +
                "   {doc.doc = 'DateTime Field-multiple 1..3'}\n" +
                "   dateTimeRangeField: DateTime[1..3];\n" +
                "\n" +
                "   {doc.doc = 'DateTime Field-multiple 0..3'}\n" +
                "   dateTimeRangeZeroField: DateTime[0..3];\n" +
                "\n" +
                "   {doc.doc = 'StrictDate Field-multiple 1..3'}\n" +
                "   strictDateRangeField: StrictDate[1..3];\n" +
                "\n" +
                "   {doc.doc = 'StrictDate Field-multiple 0..3'}\n" +
                "   strictDateRangeZeroField: StrictDate[0..3];\n" +
                "\n" +
                "}";

        PureModelContextData generated = generateSchema(modelCode, config("test::gen", Lists.mutable.with("test::Simple::PrimitiveTypeDomain")));
        Binding binding = generated.getElementsOfType(Binding.class).stream().findFirst().get();
        Assert.assertEquals("test::gen::TestBinding", binding.getPath());
        Assert.assertEquals("test::gen::TestSchemaSet", binding.schemaSet);
        Assert.assertEquals(Collections.singletonList("test::Simple::PrimitiveTypeDomain"), binding.modelUnit.packageableElementIncludes);

        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        String expectedDefinition = "{\n" +
                "  \"title\" : \"test::Simple::PrimitiveTypeDomain\",\n" +
                "  \"type\" : \"object\",\n" +
                "  \"properties\" : {\n" +
                "    \"booleanField\" : {\n" +
                "      \"description\" : \"Boolean Field\",\n" +
                "      \"type\" : \"boolean\"\n" +
                "    },\n" +
                "    \"dateField\" : {\n" +
                "      \"description\" : \"Date Field\",\n" +
                "      \"type\" : \"string\",\n" +
                "      \"format\" : \"date-time\"\n" +
                "    },\n" +
                "    \"dateMultipleField\" : {\n" +
                "      \"description\" : \"Date Field-multiple\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"dateRangeField\" : {\n" +
                "      \"description\" : \"Date Field-multiple 1..3\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"minItems\" : 1,\n" +
                "      \"maxItems\" : 3,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"dateRangeZeroField\" : {\n" +
                "      \"description\" : \"Date Field-multiple 0..3\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"maxItems\" : 3,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"dateTimeField\" : {\n" +
                "      \"description\" : \"DateTime Field\",\n" +
                "      \"type\" : \"string\",\n" +
                "      \"format\" : \"date-time\"\n" +
                "    },\n" +
                "    \"dateTimeMultipleField\" : {\n" +
                "      \"description\" : \"DateTime Field-multiple\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\",\n" +
                "        \"format\" : \"date\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"dateTimeRangeField\" : {\n" +
                "      \"description\" : \"DateTime Field-multiple 1..3\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"minItems\" : 1,\n" +
                "      \"maxItems\" : 3,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\",\n" +
                "        \"format\" : \"date\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"dateTimeRangeZeroField\" : {\n" +
                "      \"description\" : \"DateTime Field-multiple 0..3\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"maxItems\" : 3,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\",\n" +
                "        \"format\" : \"date\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"floatField\" : {\n" +
                "      \"description\" : \"Float Field\",\n" +
                "      \"type\" : \"number\"\n" +
                "    },\n" +
                "    \"floatMultipleField\" : {\n" +
                "      \"description\" : \"Float Field - multiple\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"number\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"floatRangeField\" : {\n" +
                "      \"description\" : \"Field Float - range 1..3\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"minItems\" : 1,\n" +
                "      \"maxItems\" : 3,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"number\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"floatRangeZeroField\" : {\n" +
                "      \"description\" : \"Field Float - range 0..3\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"maxItems\" : 3,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"number\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"integerField\" : {\n" +
                "      \"description\" : \"Integer Field\",\n" +
                "      \"type\" : \"integer\"\n" +
                "    },\n" +
                "    \"integerMultipleField\" : {\n" +
                "      \"description\" : \"Integer Field - multiple\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"integer\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"integerRangeField\" : {\n" +
                "      \"description\" : \"Field Integer - Range 1..3\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"minItems\" : 1,\n" +
                "      \"maxItems\" : 3,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"integer\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"integerRangeZeroField\" : {\n" +
                "      \"description\" : \"Field Integer - Range 0..3\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"maxItems\" : 3,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"integer\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"srtingMultipleField\" : {\n" +
                "      \"description\" : \"String Field- multiple\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"strictDateField\" : {\n" +
                "      \"description\" : \"StrictDate Field\",\n" +
                "      \"type\" : \"string\",\n" +
                "      \"format\" : \"date\"\n" +
                "    },\n" +
                "    \"strictDateMultipleField\" : {\n" +
                "      \"description\" : \"StrictDate Field-multiple\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\",\n" +
                "        \"format\" : \"date-time\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"strictDateRangeField\" : {\n" +
                "      \"description\" : \"StrictDate Field-multiple 1..3\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"minItems\" : 1,\n" +
                "      \"maxItems\" : 3,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\",\n" +
                "        \"format\" : \"date-time\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"strictDateRangeZeroField\" : {\n" +
                "      \"description\" : \"StrictDate Field-multiple 0..3\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"maxItems\" : 3,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\",\n" +
                "        \"format\" : \"date-time\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"stringField\" : {\n" +
                "      \"description\" : \"String Field\",\n" +
                "      \"type\" : \"string\"\n" +
                "    },\n" +
                "    \"stringNoDescriptionField\" : {\n" +
                "      \"type\" : \"string\"\n" +
                "    },\n" +
                "    \"stringRangeField\" : {\n" +
                "      \"description\" : \"Field String - Range 1..3\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"minItems\" : 1,\n" +
                "      \"maxItems\" : 3,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"stringRangeZeroField\" : {\n" +
                "      \"description\" : \"Field String - Range 0..3\",\n" +
                "      \"type\" : \"array\",\n" +
                "      \"maxItems\" : 3,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"$schema\" : \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"required\" : [ \"stringField\", \"floatField\", \"integerField\", \"dateField\", \"dateTimeField\", \"strictDateField\", \"booleanField\", \"stringNoDescriptionField\", \"stringRangeField\", \"integerRangeField\", \"floatRangeField\", \"dateRangeField\", \"dateTimeRangeField\", \"strictDateRangeField\" ]\n" +
                "}";

        assertThat(schemaSet.schemas.get(0).content, jsonEquals(expectedDefinition));
    }

    @Test
    public void testDefinitions()
    {
        String modelCode = "Class <<typemodifiers.abstract>>\n" +
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
                "}";

        PureModelContextData generated = generateSchema(modelCode, config("test::gen", Lists.mutable.with("test::Simple::Cat")));//, "test::Simple::constraintFunctionSimpleNumberNested", "test::Simple::constraintFunctionSimpleNumber", "test::Simple::constraintRegEXFn", "test::Simple::constraintFunctionComplex", "test::Simple::matchString")));
        Binding binding = generated.getElementsOfType(Binding.class).stream().findFirst().get();
        Assert.assertEquals("test::gen::TestBinding", binding.getPath());
        Assert.assertEquals("test::gen::TestSchemaSet", binding.schemaSet);
        Assert.assertEquals(Lists.mutable.with("test::Simple::Cat"), binding.modelUnit.packageableElementIncludes); //, "test::Simple::constraintFunctionSimpleNumberNested", "test::Simple::constraintFunctionSimpleNumber", "test::Simple::constraintRegEXFn", "test::Simple::constraintFunctionComplex", "test::Simple::matchString"), binding.modelUnit.packageableElementIncludes);

        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        String expectedDefinition = "{\n" +
                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"title\": \"test::Simple::Cat\",\n" +
                "  \"description\": \"The Cat Class\",\n" +
                "  \"allOf\": [\n" +
                "    {\n" +
                "      \"$ref\": \"#/definitions/test::Simple::Animal\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"vegetarian\": {\n" +
                "      \"type\": \"boolean\",\n" +
                "      \"description\": \"Is the animal a vegetarian?\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\n" +
                "    \"vegetarian\"\n" +
                "  ],\n" +
                "  \"definitions\": {\n" +
                "    \"test::Simple::Animal\": {\n" +
                "      \"title\": \"test::Simple::Animal\",\n" +
                "      \"description\": \"The Animal Class\",\n" +
                "      \"allOf\": [\n" +
                "        {\n" +
                "          \"$ref\": \"#/definitions/test::Simple::Being\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"name\": {\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        \"age\": {\n" +
                "          \"type\": \"integer\"\n" +
                "        },\n" +
                "        \"type\": {\n" +
                "          \"$ref\": \"#/definitions/test::Simple::AnimalType\"\n" +
                "        },\n" +
                "        \"aliases\": {\n" +
                "          \"type\": \"array\",\n" +
                "          \"items\": {\n" +
                "            \"type\": \"string\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"extra\": {\n" +
                "          \"type\": \"array\",\n" +
                "          \"items\": {\n" +
                "            \"$ref\": \"#/definitions/test::Simple::OtherInfo\"\n" +
                "          },\n" +
                "          \"minItems\": 4,\n" +
                "          \"maxItems\": 4\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\": [\n" +
                "        \"name\",\n" +
                "        \"age\",\n" +
                "        \"type\",\n" +
                "        \"extra\"\n" +
                "      ]\n" +
                "    },\n" +
                "    \"test::Simple::AnimalType\": {\n" +
                "      \"title\": \"test::Simple::AnimalType\",\n" +
                "      \"description\": \"The AnimalType Enum\",\n" +
                "      \"enum\": [\n" +
                "        \"CAT\",\n" +
                "        \"DOG\",\n" +
                "        \"BIRD\",\n" +
                "        \"FISH\",\n" +
                "        \"HUMAN\"\n" +
                "      ],\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"test::Simple::Being\": {\n" +
                "      \"title\": \"test::Simple::Being\",\n" +
                "      \"description\": \"The Being Class\",\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"omg\": {\n" +
                "          \"type\": \"string\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\": [\n" +
                "        \"omg\"\n" +
                "      ]\n" +
                "    },\n" +
                "    \"test::Simple::OtherInfo\": {\n" +
                "      \"title\": \"test::Simple::OtherInfo\",\n" +
                "      \"description\": \"The OtherInfo Class\",\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"info\": {\n" +
                "          \"type\": \"string\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\": [\n" +
                "        \"info\"\n" +
                "      ]\n" +
                "    }\n" +
                "  }\n" +
                "}";
        assertThat(schemaSet.schemas.get(0).content, jsonEquals(expectedDefinition));
    }

    @Test
    public void testClassWithConstraints()
    {
        String modelCode = "Class\n" +
                "{doc.doc = 'Class With Constraints'}\n" +
                "  test::Simple::ClassWithConstraints\n" +
                "[\n" +
                "   simple: $this.simple->test::Simple::constraintFunctionSimpleNumber(),\n" +
                "   complex: $this.complex->test::Simple::constraintFunctionComplex() && $this.complex->makeString()->isNoShorterThan(4),\n" +
                "  duplicateConstraint: $this.duplicateConstraint->toOne()->instanceOf(String),\n" +
                "   inLine : $this.inLine->toOne()->instanceOf(String) || $this.inLine->toOne()->instanceOf(Number),\n" +
                "   constant: $this.constant=='constantValue',\n" +
                "   number:( ($this.number>10) && ($this.number < 15)) || (( $this.number >= 0 )&& ( $this.number <=2)),\n" +
                "   dateOrString: if( $this.dateOrString->isNotEmpty(),| $this.dateOrString->toOne()->instanceOf(String) || $this.dateOrString->toOne()->instanceOf(StrictDate),|true),\n" +
                "    manyString:    $this.manyString->forAll(v |$v->isNoShorterThan(4) && $v->isNoLongerThan(10) && $v->test::Simple::constraintRegEXFn())  && $this.manyString->isDistinct() ,\n" +
                "   uuid: $this.uuidProperty->isUUID()\n" +
                "]\n" +
                "{\n" +
                "   constant:String[0..1];\n" +
                "   duplicateConstraint:String[0..1];\n" +
                "    simple:Any[0..1];\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaGeneration.writeOnly>>dateOrString:Any[0..1];\n" +
                "  inLine: Any[0..1];\n" +
                "  complex:Any[0..1];\n" +
                "   <<meta::external::format::json::binding::toPure::JSONSchemaGeneration.readOnly>>number:Number[1];\n" +
                "   manyString:String[*];\n" +
                "   manyNumeric:Integer[*];\n" +
                "   <<meta::external::format::json::binding::toPure::JSONSchemaGeneration.writeOnly>> requiredMax:Integer[1..20];\n" +
                "   requiredMinItem:Integer[3..20];\n" +
                "   optionalMax:Integer[0..5];\n" +
                "   <<meta::external::format::json::binding::toPure::JSONSchemaTypeExtension.null>>uuidProperty:String[0..1];\n" +
                "   <<meta::external::format::json::binding::toPure::JSONSchemaTypeExtension.array>> forcedArrayOptional:String[0..1];\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaTypeExtension.array>> forcedArrayRequired:String[1];\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaTypeExtension.object>> objectType : meta::pure::metamodel::type::Any[0..1];\n" +
                "}" +
                "function test::Simple::constraintFunctionSimpleNumberNested(valueNumber:Any[0..1]):Boolean[1]\n" +
                "{\n" +
                "    $valueNumber->toOne()->instanceOf(Number)  ;\n" +
                "}\n" +
                "\n" +
                "function test::Simple::constraintFunctionSimpleNumber(valueNumber:Any[0..1]):Boolean[1]\n" +
                "{\n" +
                "    $valueNumber->toOne()->instanceOf(String)  ||  $valueNumber->test::Simple::constraintFunctionSimpleNumberNested() ;\n" +
                "}" +
                "function test::Simple::constraintRegEXFn(value:String[1]):Boolean[1]\n" +
                "{\n" +
                "   $value->matches('regexp');\n" +
                "}" +
                "function {doc.doc='a complex function'} test::Simple::constraintFunctionComplex(value:Any[0..1]):Boolean[1]\n" +
                "{\n" +
                "oneOf([((($value->makeString()->isNoLongerThan(32) && $value->makeString()->isNoShorterThan(4) && $value->makeString()->matches(test::Simple::matchString()) )\n" +
                "          || $value->makeString()->isNoShorterThan(7)\n" +
                "          || $value->makeString()->isNoShorterThan(2))  && $value->toOne()->instanceOf(String))\n" +
                "       , $value->toOne()->test::Simple::constraintFunctionSimpleNumber() ])\n" +
                "}" +
                "function <<access.private>> test::Simple::matchString():String[1]\n" +
                "{\n" +
                " 'abcd';\n" +
                "}";

        PureModelContextData generated = generateSchema(modelCode, config("test::gen", Lists.mutable.with("test::Simple::ClassWithConstraints")));//, "test::Simple::constraintFunctionSimpleNumberNested", "test::Simple::constraintFunctionSimpleNumber", "test::Simple::constraintRegEXFn", "test::Simple::constraintFunctionComplex", "test::Simple::matchString")));
        Binding binding = generated.getElementsOfType(Binding.class).stream().findFirst().get();
        Assert.assertEquals("test::gen::TestBinding", binding.getPath());
        Assert.assertEquals("test::gen::TestSchemaSet", binding.schemaSet);
        Assert.assertEquals(Lists.mutable.with("test::Simple::ClassWithConstraints"), binding.modelUnit.packageableElementIncludes); //, "test::Simple::constraintFunctionSimpleNumberNested", "test::Simple::constraintFunctionSimpleNumber", "test::Simple::constraintRegEXFn", "test::Simple::constraintFunctionComplex", "test::Simple::matchString"), binding.modelUnit.packageableElementIncludes);

        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        String expectedDefinition = "{\n" +
                "  \"title\" : \"test::Simple::ClassWithConstraints\",\n" +
                "  \"description\" : \"Class With Constraints\",\n" +
                "  \"definitions\" : {\n" +
                "    \"test::Simple::constraintFunctionComplex\" : {\n" +
                "      \"oneOf\" : [ {\n" +
                "        \"anyOf\" : [ {\n" +
                "          \"type\" : \"string\",\n" +
                "          \"minLength\" : 4,\n" +
                "          \"maxLength\" : 32,\n" +
                "          \"pattern\" : \"abcd\"\n" +
                "        }, {\n" +
                "          \"type\" : \"string\",\n" +
                "          \"minLength\" : 7\n" +
                "        }, {\n" +
                "          \"type\" : \"string\",\n" +
                "          \"minLength\" : 2\n" +
                "        } ],\n" +
                "        \"type\" : \"string\"\n" +
                "      }, {\n" +
                "        \"anyOf\" : [ {\n" +
                "          \"type\" : \"string\"\n" +
                "        }, {\n" +
                "          \"type\" : \"number\"\n" +
                "        } ]\n" +
                "      } ]\n" +
                "    },\n" +
                "    \"test::Simple::constraintFunctionSimpleNumber\" : {\n" +
                "      \"anyOf\" : [ {\n" +
                "        \"type\" : \"string\"\n" +
                "      }, {\n" +
                "        \"type\" : \"number\"\n" +
                "      } ]\n" +
                "    },\n" +
                "    \"test::Simple::constraintRegEXFn\" : {\n" +
                "      \"type\" : \"string\",\n" +
                "      \"pattern\" : \"regexp\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"type\" : \"object\",\n" +
                "  \"properties\" : {\n" +
                "    \"complex\" : {\n" +
                "      \"oneOf\" : [ {\n" +
                "        \"anyOf\" : [ {\n" +
                "          \"type\" : \"string\",\n" +
                "          \"minLength\" : 4,\n" +
                "          \"maxLength\" : 32,\n" +
                "          \"pattern\" : \"abcd\"\n" +
                "        }, {\n" +
                "          \"type\" : \"string\",\n" +
                "          \"minLength\" : 7\n" +
                "        }, {\n" +
                "          \"type\" : \"string\",\n" +
                "          \"minLength\" : 2\n" +
                "        } ],\n" +
                "        \"type\" : \"string\"\n" +
                "      }, {\n" +
                "        \"anyOf\" : [ {\n" +
                "          \"type\" : \"string\"\n" +
                "        }, {\n" +
                "          \"type\" : \"number\"\n" +
                "        } ]\n" +
                "      } ],\n" +
                "      \"type\" : \"string\",\n" +
                "      \"minLength\" : 4\n" +
                "    },\n" +
                "    \"constant\" : {\n" +
                "      \"type\" : \"string\",\n" +
                "      \"const\" : \"constantValue\"\n" +
                "    },\n" +
                "    \"dateOrString\" : {\n" +
                "      \"anyOf\" : [ {\n" +
                "        \"type\" : \"string\"\n" +
                "      }, {\n" +
                "        \"type\" : \"string\",\n" +
                "        \"format\" : \"date\"\n" +
                "      } ],\n" +
                "      \"writeOnly\" : true\n" +
                "    },\n" +
                "    \"duplicateConstraint\" : {\n" +
                "      \"type\" : \"string\"\n" +
                "    },\n" +
                "    \"forcedArrayOptional\" : {\n" +
                "      \"type\" : \"array\",\n" +
                "      \"maxItems\" : 1,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"forcedArrayRequired\" : {\n" +
                "      \"type\" : \"array\",\n" +
                "      \"minItems\" : 1,\n" +
                "      \"maxItems\" : 1,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"inLine\" : {\n" +
                "      \"anyOf\" : [ {\n" +
                "        \"type\" : \"string\"\n" +
                "      }, {\n" +
                "        \"type\" : \"number\"\n" +
                "      } ]\n" +
                "    },\n" +
                "    \"manyNumeric\" : {\n" +
                "      \"type\" : \"array\",\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"integer\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"manyString\" : {\n" +
                "      \"type\" : \"array\",\n" +
                "      \"uniqueItems\" : true,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\",\n" +
                "        \"minLength\" : 4,\n" +
                "        \"maxLength\" : 10,\n" +
                "        \"pattern\" : \"regexp\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"number\" : {\n" +
                "      \"anyOf\" : [ {\n" +
                "        \"type\" : \"number\",\n" +
                "        \"exclusiveMinimum\" : 10,\n" +
                "        \"exclusiveMaximum\" : 15\n" +
                "      }, {\n" +
                "        \"type\" : \"number\",\n" +
                "        \"minimum\" : 0,\n" +
                "        \"maximum\" : 2\n" +
                "      } ],\n" +
                "      \"readOnly\" : true\n" +
                "    },\n" +
                "    \"objectType\" : {\n" +
                "      \"type\" : \"object\"\n" +
                "    },\n" +
                "    \"optionalMax\" : {\n" +
                "      \"type\" : \"array\",\n" +
                "      \"maxItems\" : 5,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"integer\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"requiredMax\" : {\n" +
                "      \"writeOnly\" : true,\n" +
                "      \"type\" : \"array\",\n" +
                "      \"minItems\" : 1,\n" +
                "      \"maxItems\" : 20,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"integer\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"requiredMinItem\" : {\n" +
                "      \"type\" : \"array\",\n" +
                "      \"minItems\" : 3,\n" +
                "      \"maxItems\" : 20,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"integer\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"simple\" : {\n" +
                "      \"anyOf\" : [ {\n" +
                "        \"type\" : \"string\"\n" +
                "      }, {\n" +
                "        \"type\" : \"number\"\n" +
                "      } ]\n" +
                "    },\n" +
                "    \"uuidProperty\" : {\n" +
                "      \"type\" : [ \"string\", \"null\" ],\n" +
                "      \"format\" : \"uuid\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"$schema\" : \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"required\" : [ \"number\", \"requiredMax\", \"requiredMinItem\", \"forcedArrayRequired\" ]\n" +
                "}";

        assertThat(schemaSet.schemas.get(0).content, jsonEquals(expectedDefinition));
    }

    @Test
    public void testProfile()
    {
        String modelCode = "Class\n" +
                "<<meta::external::format::json::binding::toPure::JSONSchemaGeneration.noAdditionalProperties>> {doc.doc = 'Class With Profile',meta::external::format::json::binding::toPure::JSONSchemaJavaExtension.javaInterface = 'com.supertype' }\n" +
                "  test::Simple::ClassWithProfile  extends test::Simple::ClassWithProfileInterface\n" +
                "{\n" +
                "      { meta::external::format::json::binding::toPure::JSONSchemaGeneration.defaultValue='0',doc.doc='integer defaulted to zero', meta::external::format::json::binding::toPure::JSONSchemaGeneration.example='1236540789000125'}int:Integer[0..1];\n" +
                "\n" +
                "   {meta::external::format::json::binding::toPure::JSONSchemaGeneration.defaultValue='0',doc.doc='number defaulted to zero',meta::external::format::json::binding::toPure::JSONSchemaGeneration.example='1236540789000125'}size:Number[0..1];\n" +
                "   {meta::external::format::json::binding::toPure::JSONSchemaGeneration.defaultValue='test',meta::external::format::json::binding::toPure::JSONSchemaGeneration.example='abc'} name:String[0..1];\n" +
                "   {meta::external::format::json::binding::toPure::JSONSchemaGeneration.defaultValue='true',meta::external::format::json::binding::toPure::JSONSchemaGeneration.example='false'} bool:Boolean[1];\n" +
                "   interfaceProperty: test::Simple::ClassWithProfileInterface[0..1];\n" +
                "   pets: test::Simple::jsonSchemaEnum[1];\n" +
                "   abstractClass: test::Simple::Being[1];\n" +
                "}" +
                "Class\n" +
                "<<meta::external::format::json::binding::toPure::JSONSchemaGeneration.noAdditionalProperties>> { meta::external::format::json::binding::toPure::JSONSchemaJavaExtension.javaType = 'com.supertype' }\n" +
                "  test::Simple::ClassWithProfileInterface\n" +
                "{\n" +
                "      classType:String[1];\n" +
                "}" +
                "Class <<typemodifiers.abstract>>\n" +
                "{doc.doc = 'The Being Class'}\n" +
                "test::Simple::Being\n" +
                "{\n" +
                "   omg:String[1];\n" +
                "}\n" +
                "Enum\n" +
                "{doc.doc = 'Enum with Profile' , meta::external::format::json::binding::toPure::JSONSchemaGeneration.defaultValue='Guinea Pig'}\n" +
                "\n" +
                "  test::Simple::jsonSchemaEnum\n" +
                "{\n" +
                "   Dog,\n" +
                "   Cat,\n" +
                "   {meta::external::format::json::binding::toPure::JSONSchemaGeneration.name='Guinea Pig'}Guinea_Pig\n" +
                "\n" +
                "}\n";

        PureModelContextData generated = generateSchema(modelCode, config("test::gen", Lists.mutable.with("test::Simple::ClassWithProfile")));
        Binding binding = generated.getElementsOfType(Binding.class).stream().findFirst().get();
        Assert.assertEquals("test::gen::TestBinding", binding.getPath());
        Assert.assertEquals("test::gen::TestSchemaSet", binding.schemaSet);
        Assert.assertEquals(Collections.singletonList("test::Simple::ClassWithProfile"), binding.modelUnit.packageableElementIncludes);
        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        String expectedDefinition = "{\n" +
                "  \"allOf\" : [ {\n" +
                "    \"$ref\" : \"#/definitions/test::Simple::ClassWithProfileInterface\"\n" +
                "  } ],\n" +
                "  \"title\" : \"test::Simple::ClassWithProfile\",\n" +
                "  \"description\" : \"Class With Profile\",\n" +
                "  \"definitions\" : {\n" +
                "    \"test::Simple::Being\" : {\n" +
                "      \"title\" : \"test::Simple::Being\",\n" +
                "      \"description\" : \"The Being Class\",\n" +
                "      \"type\" : \"object\",\n" +
                "      \"properties\" : {\n" +
                "        \"omg\" : {\n" +
                "          \"type\" : \"string\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"required\" : [ \"omg\" ]\n" +
                "    },\n" +
                "    \"test::Simple::ClassWithProfileInterface\" : {\n" +
                "      \"title\" : \"test::Simple::ClassWithProfileInterface\",\n" +
                "      \"type\" : \"object\",\n" +
                "      \"properties\" : {\n" +
                "        \"classType\" : {\n" +
                "          \"type\" : \"string\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"additionalProperties\" : false,\n" +
                "      \"required\" : [ \"classType\" ],\n" +
                "      \"javaType\" : \"com.supertype\"\n" +
                "    },\n" +
                "    \"test::Simple::jsonSchemaEnum\" : {\n" +
                "      \"title\" : \"test::Simple::jsonSchemaEnum\",\n" +
                "      \"description\" : \"Enum with Profile\",\n" +
                "      \"type\" : \"string\",\n" +
                "      \"default\" : \"Guinea Pig\",\n" +
                "      \"enum\" : [ \"Dog\", \"Cat\", \"Guinea Pig\" ]\n" +
                "    }\n" +
                "  },\n" +
                "  \"type\" : \"object\",\n" +
                "  \"properties\" : {\n" +
                "    \"abstractClass\" : {\n" +
                "      \"$ref\" : \"#/definitions/test::Simple::Being\"\n" +
                "    },\n" +
                "    \"bool\" : {\n" +
                "      \"example\" : false,\n" +
                "      \"type\" : \"boolean\",\n" +
                "      \"default\" : true\n" +
                "    },\n" +
                "    \"int\" : {\n" +
                "      \"description\" : \"integer defaulted to zero\",\n" +
                "      \"example\" : 1236540789000125,\n" +
                "      \"type\" : \"integer\",\n" +
                "      \"default\" : 0\n" +
                "    },\n" +
                "    \"interfaceProperty\" : {\n" +
                "      \"oneOf\" : [ {\n" +
                "        \"$ref\" : \"#/definitions/test::Simple::ClassWithProfile\"\n" +
                "      }, {\n" +
                "        \"$ref\" : \"#/definitions/test::Simple::ClassWithProfileInterface\"\n" +
                "      } ]\n" +
                "    },\n" +
                "    \"name\" : {\n" +
                "      \"example\" : \"abc\",\n" +
                "      \"type\" : \"string\",\n" +
                "      \"default\" : \"test\"\n" +
                "    },\n" +
                "    \"pets\" : {\n" +
                "      \"$ref\" : \"#/definitions/test::Simple::jsonSchemaEnum\"\n" +
                "    },\n" +
                "    \"size\" : {\n" +
                "      \"description\" : \"number defaulted to zero\",\n" +
                "      \"example\" : 1.236540789000125E15,\n" +
                "      \"type\" : \"number\",\n" +
                "      \"default\" : 0.0\n" +
                "    }\n" +
                "  },\n" +
                "  \"additionalProperties\" : false,\n" +
                "  \"$schema\" : \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"required\" : [ \"bool\", \"pets\", \"abstractClass\" ],\n" +
                "  \"javaInterfaces\" : [ \"com.supertype\" ]\n" +
                "}";

        assertThat(schemaSet.schemas.get(0).content, jsonEquals(expectedDefinition));
    }

    @Test
    public void testClassWithFunctionReferences()
    {
        String modelCode = "function test::Simple::functionWithStringType(value: String[0..1]): Boolean[1]\n" +
                "{\n" +
                "  $value->meta::pure::functions::collection::isNotEmpty()->meta::pure::functions::lang::if(|$value->makeString()->matches('test') ,{|true});\n" +
                "}\n" +
                "\n" +
                "Class {meta::external::format::json::binding::toPure::JSONSchemaGeneration.title='a custom title'} test::Simple::ClassWithFunctionReferences\n" +
                "[\n" +
                "  one : $this.one->test::Simple::functionWithStringType(),\n" +
                "  optional : $this.optional->test::Simple::functionWithStringType(),\n" +
                "  many : $this.many->meta::pure::functions::collection::forAll({value|$value->test::Simple::functionWithStringType()})\n" +
                "]\n" +
                "{\n" +
                "  one : String[1];\n" +
                "  optional : String[0..1];\n" +
                "  many : String[*];\n" +
                "}\n";

        PureModelContextData generated = generateSchema(modelCode, config("test::gen", Lists.mutable.with("test::Simple::ClassWithFunctionReferences", "test::Simple::functionWithStringType_String_$0_1$__Boolean_1_")));
        Binding binding = generated.getElementsOfType(Binding.class).stream().findFirst().get();
        Assert.assertEquals("test::gen::TestBinding", binding.getPath());
        Assert.assertEquals("test::gen::TestSchemaSet", binding.schemaSet);
        Assert.assertEquals(Lists.mutable.with("test::Simple::ClassWithFunctionReferences", "test::Simple::functionWithStringType_String_$0_1$__Boolean_1_"), binding.modelUnit.packageableElementIncludes);

        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        String expectedDefinition = "{\n" +
                "  \"title\" : \"a custom title\",\n" +
                "  \"definitions\" : {\n" +
                "    \"test::Simple::functionWithStringType\" : {\n" +
                "      \"type\" : \"string\",\n" +
                "      \"pattern\" : \"test\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"type\" : \"object\",\n" +
                "  \"properties\" : {\n" +
                "    \"many\" : {\n" +
                "      \"type\" : \"array\",\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\",\n" +
                "        \"pattern\" : \"test\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"one\" : {\n" +
                "      \"type\" : \"string\",\n" +
                "      \"pattern\" : \"test\"\n" +
                "    },\n" +
                "    \"optional\" : {\n" +
                "      \"type\" : \"string\",\n" +
                "      \"pattern\" : \"test\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"$schema\" : \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"required\" : [ \"one\" ]\n" +
                "}";

        assertThat(schemaSet.schemas.get(0).content, jsonEquals(expectedDefinition));

        expectedDefinition = "{\n" +
                "  \"title\" : \"test::Simple::functionWithStringType\",\n" +
                "  \"type\" : \"string\",\n" +
                "  \"pattern\" : \"test\",\n" +
                "  \"$schema\" : \"http://json-schema.org/draft-07/schema#\"\n" +
                "}";

        assertThat(schemaSet.schemas.get(1).content, jsonEquals(expectedDefinition));
    }

    @Test
    public void testCustomProfile()
    {
        String modelCode = "Profile test::Simple::GeneratedProfile\n" +
                "{\n" +
                "   stereotypes:[customStereoType1,customStereoType2];\n" +
                "   tags:[customTag1,customTag2,customTag3];\n" +
                "\n" +
                "}\n" +
                "\n" +
                "Class <<test::Simple::GeneratedProfile.customStereoType1>>  {meta::pure::profiles::doc.doc='A simple description',test::Simple::GeneratedProfile.customTag1='10',test::Simple::GeneratedProfile.customTag2='first',test::Simple::GeneratedProfile.customTag2='second'} test::Simple::ClassWithCustomProfiles\n" +
                "{\n" +
                "   <<test::Simple::GeneratedProfile.customStereoType1>> simpleString : String[0..1];\n" +
                "   <<test::Simple::GeneratedProfile.customStereoType2>> {test::Simple::GeneratedProfile.customTag3='false'} arrayMaxItemOne : String[0..10];\n" +
                "}\n";

        PureModelContextData generated = generateSchema(modelCode, config("test::gen", Lists.mutable.with("test::Simple::ClassWithCustomProfiles", "test::Simple::GeneratedProfile")));
        Binding binding = generated.getElementsOfType(Binding.class).stream().findFirst().get();
        Assert.assertEquals("test::gen::TestBinding", binding.getPath());
        Assert.assertEquals("test::gen::TestSchemaSet", binding.schemaSet);
        Assert.assertEquals(Lists.mutable.with("test::Simple::ClassWithCustomProfiles", "test::Simple::GeneratedProfile"), binding.modelUnit.packageableElementIncludes);

        ExternalFormatSchemaSet schemaSet = generated.getElementsOfType(ExternalFormatSchemaSet.class).stream().findFirst().get();
        String expectedDefinition = "{\n" +
                "  \"title\" : \"test::Simple::ClassWithCustomProfiles\",\n" +
                "  \"description\" : \"A simple description\",\n" +
                "  \"type\" : \"object\",\n" +
                "  \"properties\" : {\n" +
                "    \"arrayMaxItemOne\" : {\n" +
                "      \"type\" : \"array\",\n" +
                "      \"maxItems\" : 10,\n" +
                "      \"items\" : {\n" +
                "        \"type\" : \"string\"\n" +
                "      },\n" +
                "      \"customTag3\" : \"false\",\n" +
                "      \"customStereoType2\" : true\n" +
                "    },\n" +
                "    \"simpleString\" : {\n" +
                "      \"type\" : \"string\",\n" +
                "      \"customStereoType1\" : true\n" +
                "    }\n" +
                "  },\n" +
                "  \"$schema\" : \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"customTag1\" : 10,\n" +
                "  \"customTag2\" : [ \"first\", \"second\" ],\n" +
                "  \"customStereoType1\" : true\n" +
                "}";


        assertThat(schemaSet.schemas.get(0).content, jsonEquals(expectedDefinition));
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
