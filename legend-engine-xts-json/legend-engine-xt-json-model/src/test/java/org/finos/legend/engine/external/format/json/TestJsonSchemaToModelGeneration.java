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

import org.finos.legend.engine.external.format.json.toModel.JsonSchemaToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.transformation.toModel.SchemaToModelGenerationTest;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.junit.Assert;
import org.junit.Test;

public class TestJsonSchemaToModelGeneration extends SchemaToModelGenerationTest
{
    @Test
    public void testSimpleJsonSchema()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Test.json",
                        "{\n" +
                                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "  \"type\": \"object\",\n" +
                                "  \"properties\": {\n" +
                                "    \"stringField\": {\n" +
                                "      \"type\": \"string\"\n" +
                                "    },\n" +
                                "    \"floatField\": {\n" +
                                "      \"type\": \"number\"\n" +
                                "    },\n" +
                                "    \"decimalField\": {\n" +
                                "      \"type\": \"number\"\n" +
                                "    },\n" +
                                "    \"integerField\": {\n" +
                                "      \"type\": \"integer\"\n" +
                                "    },\n" +
                                "    \"dateField\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"format\": \"date-time\"\n" +
                                "    },\n" +
                                "    \"dateTimeField\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"format\": \"date-time\"\n" +
                                "    },\n" +
                                "    \"strictDateField\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"format\": \"date\"\n" +
                                "    },\n" +
                                "    \"booleanField\": {\n" +
                                "      \"type\": \"boolean\"\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"required\": [\n" +
                                "    \"stringField\",\n" +
                                "    \"floatField\",\n" +
                                "    \"decimalField\",\n" +
                                "    \"integerField\",\n" +
                                "    \"dateField\",\n" +
                                "    \"dateTimeField\",\n" +
                                "    \"strictDateField\",\n" +
                                "    \"booleanField\"\n" +
                                "  ]\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("example::jsonSchema"));

        Assert.assertEquals(1, model.getElements().size());

        String expected = ">>>example::jsonSchema::Test\n" +
                "Class example::jsonSchema::Test extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  floatField: Float[1];\n" +
                "  decimalField: Float[1];\n" +
                "  strictDateField: StrictDate[1];\n" +
                "  integerField: Integer[1];\n" +
                "  booleanField: Boolean[1];\n" +
                "  dateField: DateTime[1];\n" +
                "  dateTimeField: DateTime[1];\n" +
                "  stringField: String[1];\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testSimpleJsonSchemaWithBinding()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Test.json",
                        "{\n" +
                                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "  \"type\": \"object\",\n" +
                                "  \"properties\": {\n" +
                                "    \"stringField\": {\n" +
                                "      \"type\": \"string\"\n" +
                                "    },\n" +
                                "    \"floatField\": {\n" +
                                "      \"type\": \"number\"\n" +
                                "    },\n" +
                                "    \"decimalField\": {\n" +
                                "      \"type\": \"number\"\n" +
                                "    },\n" +
                                "    \"integerField\": {\n" +
                                "      \"type\": \"integer\"\n" +
                                "    },\n" +
                                "    \"dateField\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"format\": \"date-time\"\n" +
                                "    },\n" +
                                "    \"dateTimeField\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"format\": \"date-time\"\n" +
                                "    },\n" +
                                "    \"strictDateField\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"format\": \"date\"\n" +
                                "    },\n" +
                                "    \"booleanField\": {\n" +
                                "      \"type\": \"boolean\"\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"required\": [\n" +
                                "    \"stringField\",\n" +
                                "    \"floatField\",\n" +
                                "    \"decimalField\",\n" +
                                "    \"integerField\",\n" +
                                "    \"dateField\",\n" +
                                "    \"dateTimeField\",\n" +
                                "    \"strictDateField\",\n" +
                                "    \"booleanField\"\n" +
                                "  ]\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("example::jsonSchema"), true, "test::gen::TargetBinding");

        Assert.assertEquals(2, model.getElements().size());

        String expected = ">>>example::jsonSchema::Test\n" +
                "Class example::jsonSchema::Test extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  floatField: Float[1];\n" +
                "  decimalField: Float[1];\n" +
                "  strictDateField: StrictDate[1];\n" +
                "  integerField: Integer[1];\n" +
                "  booleanField: Boolean[1];\n" +
                "  dateField: DateTime[1];\n" +
                "  dateTimeField: DateTime[1];\n" +
                "  stringField: String[1];\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));

        Binding genBinding = model.getElementsOfType(Binding.class).get(0);
        Assert.assertEquals("test::gen::TargetBinding", genBinding.getPath());
        Assert.assertEquals("application/json", genBinding.contentType);
        Assert.assertArrayEquals(new String[] {"example::jsonSchema::Test"}, genBinding.modelUnit.packageableElementIncludes.toArray());
    }

    @Test
    public void testOptionalPropertiesWithJsonSchema()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Test.json",
                        "{\n" +
                                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "  \"type\": \"object\",\n" +
                                "  \"properties\": {\n" +
                                "    \"stringField\": {\n" +
                                "      \"type\": \"string\"\n" +
                                "    },\n" +
                                "    \"floatField\": {\n" +
                                "      \"type\": \"number\"\n" +
                                "    },\n" +
                                "    \"decimalField\": {\n" +
                                "      \"type\": \"number\"\n" +
                                "    },\n" +
                                "    \"integerField\": {\n" +
                                "      \"type\": \"integer\"\n" +
                                "    },\n" +
                                "    \"dateField\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"format\": \"date-time\"\n" +
                                "    },\n" +
                                "    \"dateTimeField\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"format\": \"date-time\"\n" +
                                "    },\n" +
                                "    \"strictDateField\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"format\": \"date\"\n" +
                                "    },\n" +
                                "    \"booleanField\": {\n" +
                                "      \"type\": \"boolean\"\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"required\": [\n" +
                                "    \"booleanField\"\n" +
                                "  ]\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("example::jsonSchema"));

        String expected = ">>>example::jsonSchema::Test\n" +
                "Class example::jsonSchema::Test extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  floatField: Float[0..1];\n" +
                "  decimalField: Float[0..1];\n" +
                "  strictDateField: StrictDate[0..1];\n" +
                "  integerField: Integer[0..1];\n" +
                "  booleanField: Boolean[1];\n" +
                "  dateField: DateTime[0..1];\n" +
                "  dateTimeField: DateTime[0..1];\n" +
                "  stringField: String[0..1];\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testVariousMultiplicityPropertiesWithJsonSchema()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Test.json",
                        "{\n" +
                                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "  \"title\": \"meta::json::schema::tests::PrimitiveTypeDomain\",\n" +
                                "  \"type\": \"object\",\n" +
                                "  \"properties\": {\n" +
                                "    \"stringField\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"description\": \"String Field\"\n" +
                                "    },\n" +
                                "    \"floatField\": {\n" +
                                "      \"type\": \"number\",\n" +
                                "      \"description\": \"Float Field\"\n" +
                                "    },\n" +
                                "    \"decimalField\": {\n" +
                                "      \"type\": \"number\",\n" +
                                "      \"description\": \"Decimal Field\"\n" +
                                "    },\n" +
                                "    \"integerField\": {\n" +
                                "      \"type\": \"integer\",\n" +
                                "      \"description\": \"Integer Field\"\n" +
                                "    },\n" +
                                "    \"dateField\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"description\": \"Date Field\",\n" +
                                "      \"format\": \"date-time\"\n" +
                                "    },\n" +
                                "    \"dateTimeField\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"description\": \"DateTime Field\",\n" +
                                "      \"format\": \"date-time\"\n" +
                                "    },\n" +
                                "    \"strictDateField\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"description\": \"StrictDate Field\",\n" +
                                "      \"format\": \"date\"\n" +
                                "    },\n" +
                                "    \"booleanField\": {\n" +
                                "      \"type\": \"boolean\",\n" +
                                "      \"description\": \"Boolean Field\"\n" +
                                "    },\n" +
                                "    \"stringNoDescriptionField\": {\n" +
                                "      \"type\": \"string\"\n" +
                                "    },\n" +
                                "    \"srtingMultipleField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"String Field- multiple\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"string\"\n" +
                                "      }\n" +
                                "    },\n" +
                                "    \"floatMultipleField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"Float Field - multiple\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"number\"\n" +
                                "      }\n" +
                                "    },\n" +
                                "    \"decimalMultipleField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"Decimal Field - multiple\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"number\"\n" +
                                "      }\n" +
                                "    },\n" +
                                "    \"integerMultipleField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"Integer Field - multiple\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"integer\"\n" +
                                "      }\n" +
                                "    },\n" +
                                "    \"dateMultipleField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"Date Field-multiple\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"string\"\n" +
                                "      }\n" +
                                "    },\n" +
                                "    \"dateTimeMultipleField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"DateTime Field-multiple\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"string\"\n" +
                                "      }\n" +
                                "    },\n" +
                                "    \"strictDateMultipleField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"StrictDate Field-multiple\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"string\"\n" +
                                "      }\n" +
                                "    },\n" +
                                "    \"stringRangeField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"Field String - Range 1..3\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"string\"\n" +
                                "      },\n" +
                                "      \"minItems\": 1,\n" +
                                "      \"maxItems\": 3\n" +
                                "    },\n" +
                                "    \"stringRangeZeroField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"Field String - Range 0..3\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"string\"\n" +
                                "      },\n" +
                                "      \"maxItems\": 3\n" +
                                "    },\n" +
                                "    \"integerRangeField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"Field Integer - Range 1..3\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"integer\"\n" +
                                "      },\n" +
                                "      \"minItems\": 1,\n" +
                                "      \"maxItems\": 3\n" +
                                "    },\n" +
                                "    \"integerRangeZeroField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"Field Integer - Range 0..3\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"integer\"\n" +
                                "      },\n" +
                                "      \"maxItems\": 3\n" +
                                "    },\n" +
                                "    \"floatRangeField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"Field Float - range 1..3\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"number\"\n" +
                                "      },\n" +
                                "      \"minItems\": 1,\n" +
                                "      \"maxItems\": 3\n" +
                                "    },\n" +
                                "    \"floatRangeZeroField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"Field Float - range 0..3\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"number\"\n" +
                                "      },\n" +
                                "      \"maxItems\": 3\n" +
                                "    },\n" +
                                "    \"decimalRangeField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"Field Decimal - range 1..3\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"number\"\n" +
                                "      },\n" +
                                "      \"minItems\": 1,\n" +
                                "      \"maxItems\": 3\n" +
                                "    },\n" +
                                "    \"decimalRangeZeroField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"Field Decimal - range 0..3\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"number\"\n" +
                                "      },\n" +
                                "      \"maxItems\": 3\n" +
                                "    },\n" +
                                "    \"dateRangeField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"Date Field-multiple 1..3\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"string\"\n" +
                                "      },\n" +
                                "      \"minItems\": 1,\n" +
                                "      \"maxItems\": 3\n" +
                                "    },\n" +
                                "    \"dateRangeZeroField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"Date Field-multiple 0..3\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"string\"\n" +
                                "      },\n" +
                                "      \"maxItems\": 3\n" +
                                "    },\n" +
                                "    \"dateTimeRangeField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"DateTime Field-multiple 1..3\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"string\"\n" +
                                "      },\n" +
                                "      \"minItems\": 1,\n" +
                                "      \"maxItems\": 3\n" +
                                "    },\n" +
                                "    \"dateTimeRangeZeroField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"DateTime Field-multiple 0..3\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"string\"\n" +
                                "      },\n" +
                                "      \"maxItems\": 3\n" +
                                "    },\n" +
                                "    \"strictDateRangeField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"StrictDate Field-multiple 1..3\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"string\"\n" +
                                "      },\n" +
                                "      \"minItems\": 1,\n" +
                                "      \"maxItems\": 3\n" +
                                "    },\n" +
                                "    \"strictDateRangeZeroField\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"description\": \"StrictDate Field-multiple 0..3\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"string\"\n" +
                                "      },\n" +
                                "      \"maxItems\": 3\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"required\": [\n" +
                                "    \"stringField\",\n" +
                                "    \"floatField\",\n" +
                                "    \"decimalField\",\n" +
                                "    \"integerField\",\n" +
                                "    \"dateField\",\n" +
                                "    \"dateTimeField\",\n" +
                                "    \"strictDateField\",\n" +
                                "    \"booleanField\",\n" +
                                "    \"stringNoDescriptionField\",\n" +
                                "    \"stringRangeField\",\n" +
                                "    \"integerRangeField\",\n" +
                                "    \"floatRangeField\",\n" +
                                "    \"decimalRangeField\",\n" +
                                "    \"dateRangeField\",\n" +
                                "    \"dateTimeRangeField\",\n" +
                                "    \"strictDateRangeField\"\n" +
                                "  ]\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("example::jsonSchema"));

        String expected = ">>>example::jsonSchema::Test\n" +
                "Class {meta::json::schema::JSONSchemaGeneration.title = 'meta::json::schema::tests::PrimitiveTypeDomain'} example::jsonSchema::Test extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  {meta::pure::profiles::doc.doc = 'Float Field'} floatField: Float[1];\n" +
                "  {meta::pure::profiles::doc.doc = 'Field Float - range 1..3'} floatRangeField: Float[1..3];\n" +
                "  {meta::pure::profiles::doc.doc = 'Field String - Range 0..3'} stringRangeZeroField: String[0..3];\n" +
                "  {meta::pure::profiles::doc.doc = 'Field Decimal - range 0..3'} decimalRangeZeroField: Float[0..3];\n" +
                "  {meta::pure::profiles::doc.doc = 'Boolean Field'} booleanField: Boolean[1];\n" +
                "  {meta::pure::profiles::doc.doc = 'StrictDate Field-multiple 0..3'} strictDateRangeZeroField: String[0..3];\n" +
                "  {meta::pure::profiles::doc.doc = 'DateTime Field'} dateTimeField: DateTime[1];\n" +
                "  {meta::pure::profiles::doc.doc = 'StrictDate Field-multiple 1..3'} strictDateRangeField: String[1..3];\n" +
                "  {meta::pure::profiles::doc.doc = 'Field Integer - Range 0..3'} integerRangeZeroField: Integer[0..3];\n" +
                "  {meta::pure::profiles::doc.doc = 'StrictDate Field'} strictDateField: StrictDate[1];\n" +
                "  {meta::pure::profiles::doc.doc = 'StrictDate Field-multiple'} strictDateMultipleField: String[*];\n" +
                "  {meta::pure::profiles::doc.doc = 'DateTime Field-multiple 1..3'} dateTimeRangeField: String[1..3];\n" +
                "  {meta::pure::profiles::doc.doc = 'Field Float - range 0..3'} floatRangeZeroField: Float[0..3];\n" +
                "  {meta::pure::profiles::doc.doc = 'Integer Field - multiple'} integerMultipleField: Integer[*];\n" +
                "  {meta::pure::profiles::doc.doc = 'Decimal Field'} decimalField: Float[1];\n" +
                "  {meta::pure::profiles::doc.doc = 'Field Decimal - range 1..3'} decimalRangeField: Float[1..3];\n" +
                "  {meta::pure::profiles::doc.doc = 'Date Field-multiple 0..3'} dateRangeZeroField: String[0..3];\n" +
                "  {meta::pure::profiles::doc.doc = 'DateTime Field-multiple'} dateTimeMultipleField: String[*];\n" +
                "  {meta::pure::profiles::doc.doc = 'Field String - Range 1..3'} stringRangeField: String[1..3];\n" +
                "  {meta::pure::profiles::doc.doc = 'Date Field'} dateField: DateTime[1];\n" +
                "  {meta::pure::profiles::doc.doc = 'DateTime Field-multiple 0..3'} dateTimeRangeZeroField: String[0..3];\n" +
                "  {meta::pure::profiles::doc.doc = 'Float Field - multiple'} floatMultipleField: Float[*];\n" +
                "  stringNoDescriptionField: String[1];\n" +
                "  {meta::pure::profiles::doc.doc = 'Field Integer - Range 1..3'} integerRangeField: Integer[1..3];\n" +
                "  {meta::pure::profiles::doc.doc = 'String Field- multiple'} srtingMultipleField: String[*];\n" +
                "  {meta::pure::profiles::doc.doc = 'Date Field-multiple 1..3'} dateRangeField: String[1..3];\n" +
                "  {meta::pure::profiles::doc.doc = 'Integer Field'} integerField: Integer[1];\n" +
                "  {meta::pure::profiles::doc.doc = 'Decimal Field - multiple'} decimalMultipleField: Float[*];\n" +
                "  {meta::pure::profiles::doc.doc = 'String Field'} stringField: String[1];\n" +
                "  {meta::pure::profiles::doc.doc = 'Date Field-multiple'} dateMultipleField: String[*];\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testNestedJsonSchema()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Test.json",
                        "{\n" +
                                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "  \"type\": \"object\",\n" +
                                "  \"properties\": {\n" +
                                "    \"firstName\": {\n" +
                                "      \"type\": \"string\"\n" +
                                "    },\n" +
                                "    \"lastName\": {\n" +
                                "      \"type\": \"string\"\n" +
                                "    },\n" +
                                "    \"middleName\": {\n" +
                                "      \"type\": \"string\"\n" +
                                "    },\n" +
                                "    \"firm\": {\n" +
                                "      \"$ref\": \"#/definitions/demo::jsonSchema::Firm\"\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"required\": [\n" +
                                "    \"firstName\",\n" +
                                "    \"lastName\",\n" +
                                "    \"firm\"\n" +
                                "  ],\n" +
                                "  \"definitions\": {\n" +
                                "    \"demo::jsonSchema::Firm\": {\n" +
                                "      \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "      \"title\": \"demo::jsonSchema::Firm\",\n" +
                                "      \"type\": \"object\",\n" +
                                "      \"properties\": {\n" +
                                "        \"legalName\": {\n" +
                                "          \"type\": \"string\"\n" +
                                "        },\n" +
                                "        \"employeesCount\": {\n" +
                                "          \"type\": \"integer\"\n" +
                                "        }\n" +
                                "      },\n" +
                                "      \"required\": [\n" +
                                "        \"legalName\",\n" +
                                "        \"employeesCount\"\n" +
                                "      ]\n" +
                                "    }\n" +
                                "  }\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("example::jsonSchema"));

        String expected = ">>>demo::jsonSchema::Firm\n" +
                "Class demo::jsonSchema::Firm extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  employeesCount: Integer[1];\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::Test\n" +
                "Class example::jsonSchema::Test extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "  firm: demo::jsonSchema::Firm[1];\n" +
                "  middleName: String[0..1];\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testMultiLevelNestingWithJsonSchema()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple::SchemaSet", "JSON")
                .withSchemaText("Test", "Person.json",
                        "{\n" +
                                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "  \"title\": \"test::Simple::Person\",\n" +
                                "  \"type\": \"object\",\n" +
                                "  \"properties\": {\n" +
                                "    \"firstName\": {\n" +
                                "      \"type\": \"string\"\n" +
                                "    },\n" +
                                "    \"lastName\": {\n" +
                                "      \"type\": \"string\"\n" +
                                "    },\n" +
                                "    \"middleName\": {\n" +
                                "      \"type\": \"string\"\n" +
                                "    },\n" +
                                "    \"age\": {\n" +
                                "      \"type\": \"integer\"\n" +
                                "    },\n" +
                                "    \"addresses\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"items\": {\n" +
                                "        \"$ref\": \"#/definitions/test::Simple::Address\"\n" +
                                "      }\n" +
                                "    },\n" +
                                "    \"firm\": {\n" +
                                "      \"$ref\": \"#/definitions/test::Simple::Firm\"\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"required\": [\n" +
                                "    \"firstName\",\n" +
                                "    \"lastName\",\n" +
                                "    \"firm\"\n" +
                                "  ],\n" +
                                "  \"definitions\": {\n" +
                                "    \"test::Simple::Address\": {\n" +
                                "      \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "      \"title\": \"test::Simple::Address\",\n" +
                                "      \"type\": \"object\",\n" +
                                "      \"properties\": {\n" +
                                "        \"addressType\": {\n" +
                                "          \"$ref\": \"#/definitions/test::Simple::AddressType\"\n" +
                                "        },\n" +
                                "        \"addressLine1\": {\n" +
                                "          \"type\": \"string\"\n" +
                                "        },\n" +
                                "        \"addressLine2\": {\n" +
                                "          \"type\": \"string\"\n" +
                                "        },\n" +
                                "        \"addressLine3\": {\n" +
                                "          \"type\": \"string\"\n" +
                                "        }\n" +
                                "      },\n" +
                                "      \"required\": [\n" +
                                "        \"addressType\",\n" +
                                "        \"addressLine1\"\n" +
                                "      ]\n" +
                                "    },\n" +
                                "    \"test::Simple::AddressType\": {\n" +
                                "      \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "      \"title\": \"test::Simple::AddressType\",\n" +
                                "      \"enum\": [\n" +
                                "        \"HOME\",\n" +
                                "        \"OFFICE\",\n" +
                                "        \"WORKSHOP\"\n" +
                                "      ],\n" +
                                "      \"type\": \"string\"\n" +
                                "    },\n" +
                                "    \"test::Simple::Firm\": {\n" +
                                "      \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "      \"title\": \"test::Simple::Firm\",\n" +
                                "      \"type\": \"object\",\n" +
                                "      \"properties\": {\n" +
                                "        \"legalName\": {\n" +
                                "          \"type\": \"string\"\n" +
                                "        },\n" +
                                "        \"addresses\": {\n" +
                                "          \"type\": \"array\",\n" +
                                "          \"items\": {\n" +
                                "            \"$ref\": \"#/definitions/test::Simple::Address\"\n" +
                                "          }\n" +
                                "        }\n" +
                                "      },\n" +
                                "      \"required\": [\n" +
                                "        \"legalName\"\n" +
                                "      ]\n" +
                                "    }\n" +
                                "  }\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple"));

        String expected = ">>>test::Simple::Address\n" +
                "Class test::Simple::Address extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  addressType: test::Simple::AddressType[1];\n" +
                "  addressLine1: String[1];\n" +
                "  addressLine2: String[0..1];\n" +
                "  addressLine3: String[0..1];\n" +
                "}\n" +
                "\n" +
                ">>>test::Simple::AddressType\n" +
                "Enum test::Simple::AddressType\n" +
                "{\n" +
                "  HOME,\n" +
                "  OFFICE,\n" +
                "  WORKSHOP\n" +
                "}\n" +
                "\n" +
                ">>>test::Simple::Firm\n" +
                "Class test::Simple::Firm extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  addresses: test::Simple::Address[*];\n" +
                "}\n" +
                "\n" +
                ">>>test::Simple::Person\n" +
                "Class test::Simple::Person extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "  firm: test::Simple::Firm[1];\n" +
                "  addresses: test::Simple::Address[*];\n" +
                "  middleName: String[0..1];\n" +
                "  age: Integer[0..1];\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    private JsonSchemaToModelConfiguration config(String targetPackage)
    {
        JsonSchemaToModelConfiguration config = new JsonSchemaToModelConfiguration();
        config.targetPackage = targetPackage;
        return config;
    }
}
