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

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

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

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

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

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

        String expected = ">>>example::jsonSchema::Test\n" +
                "Class {meta::external::format::json::binding::toPure::JSONSchemaGeneration.title = 'meta::json::schema::tests::PrimitiveTypeDomain'} example::jsonSchema::Test extends meta::pure::metamodel::type::Any\n" +
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

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

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

        PureModelContextData model = generateModel(schemaCode, config("test::Simple::SchemaSet", "test::Simple"));

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

    @Test
    public void testEnumProperty()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Test.json",
                        "  {\n" +
                                "    \"type\": \"object\",\n" +
                                "    \"properties\": {\n" +
                                "      \"enumString\": {\n" +
                                "        \"type\": \"string\",\n" +
                                "        \"enum\": [\n" +
                                "          \"test\"\n" +
                                "        ]\n" +
                                "      }\n" +
                                "    }\n" +
                                "  }")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

        String expected = ">>>example::jsonSchema::Test\n" +
                "Class example::jsonSchema::Test extends meta::pure::metamodel::type::Any\n" +
                "[\n" +
                "  enumString_inLine: if($this.enumString->isNotEmpty(), |$this.enumString->toOne()->in('test'), |true)\n" +
                "]\n" +
                "{\n" +
                "  enumString: String[0..1];\n" +
                "}";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testFromJSONSchema()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Test.json",
                        "{\n" +
                                "    \"type\": \"object\",\n" +
                                "    \"readOnly\": true,\n" +
                                "    \"properties\": {\n" +
                                "      \"readOnlyId\": {\n" +
                                "        \"type\": \"string\",\n" +
                                "        \"readOnly\": true\n" +
                                "      },\n" +
                                "      \"inLineEnum\" : {\n" +
                                "        \"type\": \"string\",\n" +
                                "        \"default\": \"defaultValue\",\n" +
                                "        \"enum\": [\n" +
                                "          \"defaultValue\"\n" +
                                "        ]\n" +
                                "      },\n" +
                                "      \"formatWriteOnly\": {\n" +
                                "        \"type\": \"string\",\n" +
                                "        \"format\": \"date\",\n" +
                                "        \"writeOnly\": true\n" +
                                "      },\n" +
                                "      \"stringConstraints\": {\n" +
                                "        \"description\": \"A description\",\n" +
                                "        \"type\": \"string\",\n" +
                                "        \"example\": \"bleh bleh\",\n" +
                                "        \"minLength\": 10,\n" +
                                "        \"maxLength\": 300,\n" +
                                "        \"pattern\": \"^[a-z]{10, 300}$\"\n" +
                                "      },\n" +
                                "      \"dateTime\": {\n" +
                                "        \"type\": \"string\",\n" +
                                "        \"format\": \"date-time\"\n" +
                                "      },\n" +
                                "      \"int64\": {\n" +
                                "        \"type\": \"integer\",\n" +
                                "        \"format\": \"int64\"\n" +
                                "      },\n" +
                                "      \"int32\": {\n" +
                                "        \"type\": \"integer\",\n" +
                                "        \"format\": \"int32\"\n" +
                                "      },\n" +
                                "      \"byte\": {\n" +
                                "        \"type\": \"string\",\n" +
                                "        \"format\": \"byte\"\n" +
                                "      },\n" +
                                "      \"binary\": {\n" +
                                "        \"type\": \"string\",\n" +
                                "        \"format\": \"binary\"\n" +
                                "      },\n" +
                                "      \"uuid\": {\n" +
                                "        \"type\": \"string\",\n" +
                                "        \"format\": \"uuid\"\n" +
                                "      },\n" +
                                "      \"double\": {\n" +
                                "        \"type\": \"number\",\n" +
                                "        \"format\": \"double\"\n" +
                                "      },\n" +
                                "      \"float\": {\n" +
                                "        \"type\": \"number\",\n" +
                                "        \"format\": \"float\"\n" +
                                "      },\n" +
                                "      \"minMaxNumber\": {\n" +
                                "        \"type\": \"number\",\n" +
                                "        \"minimum\": 18,\n" +
                                "        \"maximum\": 150\n" +
                                "      },\n" +
                                "      \"anyOfConstraint\": {\n" +
                                "        \"description\": \"any of\",\n" +
                                "        \"anyOf\": [\n" +
                                "          {\n" +
                                "            \"type\": \"string\",\n" +
                                "            \"pattern\" : \"^[a-z]*$\",\n" +
                                "            \"maxLength\": 32\n" +
                                "          },\n" +
                                "          {\n" +
                                "            \"type\": \"number\",\n" +
                                "            \"maximum\": 5\n" +
                                "          },\n" +
                                "          {\n" +
                                "            \"type\": \"string\",\n" +
                                "            \"pattern\" : \"^[a-z]*$\",\n" +
                                "            \"maxLength\": 256\n" +
                                "          }\n" +
                                "        ]\n" +
                                "      },\n" +
                                "      \"oneOfConstraint\": {\n" +
                                "        \"description\": \"one of\",\n" +
                                "        \"oneOf\": [\n" +
                                "          {\n" +
                                "            \"type\": \"string\",\n" +
                                "            \"pattern\" : \"^[a-z]*$\",\n" +
                                "            \"maxLength\": 32\n" +
                                "          },\n" +
                                "          {\n" +
                                "            \"type\": \"number\",\n" +
                                "            \"maximum\": 10\n" +
                                "          },\n" +
                                "          {\n" +
                                "            \"type\": \"string\",\n" +
                                "            \"pattern\" : \"^[a-z]*$\",\n" +
                                "            \"maxLength\": 256\n" +
                                "          }\n" +
                                "        ]\n" +
                                "      },\n" +
                                "      \"arrayFeatures\": {\n" +
                                "        \"type\": \"array\",\n" +
                                "        \"items\": {\n" +
                                "          \"type\": \"string\",\n" +
                                "          \"minLength\": 10,\n" +
                                "          \"maxLength\": 300,\n" +
                                "          \"example\": \"array example\",\n" +
                                "          \"pattern\": \"^[a-z]{10, 300}$\"\n" +
                                "        },\n" +
                                "        \"uniqueItems\": true,\n" +
                                "        \"minItems\": 4,\n" +
                                "        \"maxItems\": 20\n" +
                                "      },\n" +
                                "      \"optionalArrayWithMax\": {\n" +
                                "        \"type\": \"array\",\n" +
                                "        \"items\": {\n" +
                                "          \"type\": \"string\",\n" +
                                "          \"pattern\": \"^[a-z]{10, 300}$\"\n" +
                                "        },\n" +
                                "        \"maxItems\": 20\n" +
                                "      },\n" +
                                "      \"arrayMaxItemOne\": {\n" +
                                "        \"type\": \"array\",\n" +
                                "        \"items\": {\n" +
                                "          \"type\": \"string\"\n" +
                                "        },\n" +
                                "        \"maxItems\": 1\n" +
                                "      },\n" +
                                "      \"arrayWithFeaturesOutsideOfItems\": {\n" +
                                "        \"type\": \"array\",\n" +
                                "        \"readOnly\": true,\n" +
                                "        \"example\": [\"array\", \"example\"],\n" +
                                "        \"items\": {\n" +
                                "          \"type\": \"string\",\n" +
                                "          \"format\": \"uuid\"\n" +
                                "        }\n" +
                                "      },\n" +
                                "      \"requiredArrayWithMax\": {\n" +
                                "        \"type\": \"array\",\n" +
                                "        \"items\": {\n" +
                                "          \"type\": \"string\",\n" +
                                "          \"format\": \"date-time\"\n" +
                                "        },\n" +
                                "        \"uniqueItems\": true,\n" +
                                "        \"maxItems\": 20\n" +
                                "      },\n" +
                                "      \"enumType\" : {\n" +
                                "        \"$ref\": \"ExampleEnum.json\"\n" +
                                "      },\n" +
                                "      \"objectType\": {\n" +
                                "        \"type\": \"object\"\n" +
                                "      },\n" +
                                "      \"constant\": {\n" +
                                "            \"type\": \"string\",\n" +
                                "            \"const\": \"my constant\"\n" +
                                "        }\n" +
                                "      },\n" +
                                "      \"required\": [\n" +
                                "        \"enumType\",\n" +
                                "        \"requiredArrayWithMax\"\n" +
                                "      ]\n" +
                                "  }")
                .withSchemaText("Test1", "ExampleEnum.json",
                        "  {\n" +
                                "    \"description\": \"Example Enum Doc\",\n" +
                                "    \"type\": \"string\",\n" +
                                "    \"readOnly\": true,\n" +
                                "    \"javaType\": \"org.example.ExampleEnum\",\n" +
                                "    \"enum\": [\n" +
                                "      \"\",\n" +
                                "      \"simpleEnum\",\n" +
                                "      \"enum::withcolon\",\n" +
                                "      \"enum withspace\",\n" +
                                "      \"123numberstart\",\n" +
                                "      \"() /other characters\",\n" +
                                "      \"true\"\n" +
                                "    ]\n" +
                                "  }")
                .withSchemaText("Test2", "SuperType.json",
                        "  {\n" +
                                "    \"description\": \"a Super Type Object\",\n" +
                                "    \"type\": \"object\"\n" +
                                "  }")
                .withSchemaText("Test3", "complexFragment.json",
                        "  {\n" +
                                "    \"description\": \"complex Fragment\",\n" +
                                "    \"writeOnly\": true,\n" +
                                "    \"anyOf\": [\n" +
                                "      {\n" +
                                "        \"type\": \"string\",\n" +
                                "        \"maxLength\": 32\n" +
                                "      },\n" +
                                "      {\n" +
                                "        \"type\": \"number\"\n" +
                                "      },\n" +
                                "      {\n" +
                                "        \"type\": \"string\",\n" +
                                "        \"pattern\": \"^*\",\n" +
                                "        \"maxLength\": 256\n" +
                                "      }\n" +
                                "    ]\n" +
                                "  }")
                .withSchemaText("Test4", "SimpleObject.json",
                        "  {\n" +
                                "    \"type\": \"object\",\n" +
                                "    \"description\": \"A simple description\",\n" +
                                "    \"allOf\": [\n" +
                                "      {\n" +
                                "        \"$ref\": \"SuperType\"\n" +
                                "      }\n" +
                                "    ],\n" +
                                "    \"properties\": {\n" +
                                "      \"simpleString\": {\n" +
                                "        \"type\": \"string\"\n" +
                                "      }, \"simpleReference\": {\n" +
                                "        \"$ref\": \"simpleReference.json\"\n" +
                                "      },\n" +
                                "      \"arrayMaxItemOne\": {\n" +
                                "        \"type\": \"array\",\n" +
                                "        \"uniqueItems\": true,\n" +
                                "        \"items\": {\n" +
                                "          \"type\": \"string\",\n" +
                                "          \"format\": \"uuid\"\n" +
                                "        },\n" +
                                "        \"maxItems\": 10\n" +
                                "      }\n" +
                                "    }\n" +
                                "  }")
                .withSchemaText("Test5", "simpleReference.json",
                        "{\n" +
                                "  \"readOnly\": true,\n" +
                                "  \"type\": \"string\",\n" +
                                "  \"format\": \"uuid\"\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

        String expected = ">>>example::jsonSchema::ExampleEnum\n" +
                "Enum <<meta::external::format::json::binding::toPure::JSONSchemaGeneration.readOnly>> {meta::external::format::json::binding::toPure::JSONSchemaJavaExtension.javaType = 'org.example.ExampleEnum', meta::pure::profiles::doc.doc = 'Example Enum Doc', example::jsonSchema::profile::GeneratedProfile.javaType = 'org.example.ExampleEnum'} example::jsonSchema::ExampleEnum\n" +
                "{\n" +
                "  simpleEnum,\n" +
                "  {meta::external::format::json::binding::toPure::JSONSchemaGeneration.name = 'enum::withcolon'} enum__withcolon,\n" +
                "  {meta::external::format::json::binding::toPure::JSONSchemaGeneration.name = 'enum withspace'} enum_withspace,\n" +
                "  {meta::external::format::json::binding::toPure::JSONSchemaGeneration.name = '123numberstart'} _123numberstart,\n" +
                "  {meta::external::format::json::binding::toPure::JSONSchemaGeneration.name = '() /other characters'} ____other_characters,\n" +
                "  {meta::external::format::json::binding::toPure::JSONSchemaGeneration.name = 'true'} _true\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::SimpleObject\n" +
                "Class {meta::pure::profiles::doc.doc = 'A simple description'} example::jsonSchema::SimpleObject extends example::jsonSchema::SuperType\n" +
                "[\n" +
                "  simpleReference: $this.simpleReference->example::jsonSchema::simpleReference(),\n" +
                "  arrayMaxItemOne_inLine: if($this.arrayMaxItemOne->isNotEmpty(), |$this.arrayMaxItemOne->forAll(value: String[1]|$value->isUUID()) && $this.arrayMaxItemOne->isDistinct(), |true)\n" +
                "]\n" +
                "{\n" +
                "  simpleReference: String[0..1];\n" +
                "  simpleString: String[0..1];\n" +
                "  arrayMaxItemOne: String[0..10];\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::SuperType\n" +
                "Class {meta::pure::profiles::doc.doc = 'a Super Type Object'} example::jsonSchema::SuperType extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::Test\n" +
                "Class <<meta::external::format::json::binding::toPure::JSONSchemaGeneration.readOnly>> example::jsonSchema::Test extends meta::pure::metamodel::type::Any\n" +
                "[\n" +
                "  constant_inLine: if($this.constant->isNotEmpty(), |$this.constant->toOne() == 'my constant', |true),\n" +
                "  anyOfConstraint_inLine: if($this.anyOfConstraint->isNotEmpty(), |[($this.anyOfConstraint->makeString()->matches('^[a-z]*$') && ($this.anyOfConstraint->makeString()->isNoLongerThan(32) && $this.anyOfConstraint->toOne()->instanceOf(String))), (($this.anyOfConstraint->toOne()->cast(@Number) <= 5) && $this.anyOfConstraint->toOne()->instanceOf(Float)), ($this.anyOfConstraint->makeString()->matches('^[a-z]*$') && ($this.anyOfConstraint->makeString()->isNoLongerThan(256) && $this.anyOfConstraint->toOne()->instanceOf(String)))]->or(), |true),\n" +
                "  minMaxNumber_inLine: if($this.minMaxNumber->isNotEmpty(), |($this.minMaxNumber->toOne()->cast(@Number) >= 18) && ($this.minMaxNumber->toOne()->cast(@Number) <= 150), |true),\n" +
                "  uuid_inLine: if($this.uuid->isNotEmpty(), |$this.uuid->isUUID(), |true),\n" +
                "  inLineEnum_inLine: if($this.inLineEnum->isNotEmpty(), |$this.inLineEnum->toOne()->in('defaultValue'), |true),\n" +
                "  requiredArrayWithMax_inLine: $this.requiredArrayWithMax->isDistinct(),\n" +
                "  optionalArrayWithMax_inLine: $this.optionalArrayWithMax->forAll(value: String[1]|$value->makeString()->matches('^[a-z]{10, 300}$')),\n" +
                "  oneOfConstraint_inLine: if($this.oneOfConstraint->isNotEmpty(), |[($this.oneOfConstraint->makeString()->matches('^[a-z]*$') && ($this.oneOfConstraint->makeString()->isNoLongerThan(32) && $this.oneOfConstraint->toOne()->instanceOf(String))), (($this.oneOfConstraint->toOne()->cast(@Number) <= 10) && $this.oneOfConstraint->toOne()->instanceOf(Float)), ($this.oneOfConstraint->makeString()->matches('^[a-z]*$') && ($this.oneOfConstraint->makeString()->isNoLongerThan(256) && $this.oneOfConstraint->toOne()->instanceOf(String)))]->oneOf(), |true),\n" +
                "  stringConstraints_inLine: if($this.stringConstraints->isNotEmpty(), |$this.stringConstraints->makeString()->matches('^[a-z]{10, 300}$') && ($this.stringConstraints->makeString()->isNoLongerThan(300) && $this.stringConstraints->makeString()->isNoShorterThan(10)), |true),\n" +
                "  arrayFeatures_inLine: $this.arrayFeatures->forAll(value: String[1]|$value->makeString()->matches('^[a-z]{10, 300}$') && ($value->makeString()->isNoLongerThan(300) && $value->makeString()->isNoShorterThan(10))) && $this.arrayFeatures->isDistinct()\n" +
                "]\n" +
                "{\n" +
                "  dateTime: DateTime[0..1];\n" +
                "  constant: String[0..1];\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaTypeExtension.byte>> byte: String[0..1];\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaTypeExtension.double>> double: Float[0..1];\n" +
                "  {meta::pure::profiles::doc.doc = 'any of'} anyOfConstraint: meta::pure::metamodel::type::Any[0..1];\n" +
                "  enumType: example::jsonSchema::ExampleEnum[1];\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaGeneration.readOnly>> {meta::external::format::json::binding::toPure::JSONSchemaGeneration.example = 'array', meta::external::format::json::binding::toPure::JSONSchemaGeneration.example = 'example'} arrayWithFeaturesOutsideOfItems: String[*];\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaGeneration.writeOnly>> formatWriteOnly: StrictDate[0..1];\n" +
                "  minMaxNumber: Float[0..1];\n" +
                "  float: Float[0..1];\n" +
                "  uuid: String[0..1];\n" +
                "  {meta::external::format::json::binding::toPure::JSONSchemaGeneration.defaultValue = 'defaultValue'} inLineEnum: String[0..1];\n" +
                "  requiredArrayWithMax: DateTime[1..20];\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaTypeExtension.object>> objectType: meta::pure::metamodel::type::Any[0..1];\n" +
                "  optionalArrayWithMax: String[0..20];\n" +
                "  {meta::pure::profiles::doc.doc = 'one of'} oneOfConstraint: meta::pure::metamodel::type::Any[0..1];\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaTypeExtension.int32>> int32: Integer[0..1];\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaTypeExtension.int64>> int64: Integer[0..1];\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaTypeExtension.binary>> binary: String[0..1];\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaGeneration.readOnly>> readOnlyId: String[0..1];\n" +
                "  {meta::external::format::json::binding::toPure::JSONSchemaGeneration.example = 'bleh bleh', meta::pure::profiles::doc.doc = 'A description'} stringConstraints: String[0..1];\n" +
                "  {meta::external::format::json::binding::toPure::JSONSchemaGeneration.example = 'array example'} arrayFeatures: String[4..20];\n" +
                "  <<meta::external::format::json::binding::toPure::JSONSchemaTypeExtension.array>> arrayMaxItemOne: String[0..1];\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::complexFragment_Any_$0_1$__Boolean_1_\n" +
                "function <<meta::external::format::json::binding::toPure::JSONSchemaGeneration.writeOnly>> {meta::pure::profiles::doc.doc = 'complex Fragment'} example::jsonSchema::complexFragment(value: meta::pure::metamodel::type::Any[0..1]): Boolean[1]\n" +
                "{\n" +
                "   if($value->isNotEmpty(), |[($value->makeString()->isNoLongerThan(32) && $value->toOne()->instanceOf(String)), $value->toOne()->instanceOf(Float), ($value->makeString()->matches('^*') && ($value->makeString()->isNoLongerThan(256) && $value->toOne()->instanceOf(String)))]->or(), |true)\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::profile::GeneratedProfile\n" +
                "Profile example::jsonSchema::profile::GeneratedProfile\n" +
                "{\n" +
                "  tags: [javaType];\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::simpleReference_String_$0_1$__Boolean_1_\n" +
                "function <<meta::external::format::json::binding::toPure::JSONSchemaGeneration.readOnly>> example::jsonSchema::simpleReference(value: String[0..1]): Boolean[1]\n" +
                "{\n" +
                "   if($value->isNotEmpty(), |$value->isUUID(), |true)\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testStringProperty()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Test.json",
                        "  {\n" +
                                "    \"type\": \"object\",\n" +
                                "    \"properties\": {\n" +
                                "      \"test\" :     {\n" +
                                "        \"type\": \"string\",\n" +
                                "        \"maxLength\": 10,\n" +
                                "        \"minLength\": 1\n" +
                                "      }\n" +
                                "    }\n" +
                                "  }")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

        String expected = ">>>example::jsonSchema::Test\n" +
                "Class example::jsonSchema::Test extends meta::pure::metamodel::type::Any\n" +
                "[\n" +
                "  test_inLine: if($this.test->isNotEmpty(), |$this.test->makeString()->isNoLongerThan(10) && $this.test->makeString()->isNoShorterThan(1), |true)\n" +
                "]\n" +
                "{\n" +
                "  test: String[0..1];\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testDefinitions()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Test.json",
                        "{\n" +
                                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "  \"title\": \"meta::json::schema::tests::parent::parent::levela::parentClass2\",\n" +
                                "  \"type\": \"object\",\n" +
                                "  \"properties\": {\n" +
                                "    \"childClass4\": {\n" +
                                "      \"$ref\": \"#/definitions/meta::json::schema::tests::parent::level1::levela::childClass4\"\n" +
                                "    },\n" +
                                "    \"childClass3\": {\n" +
                                "      \"$ref\": \"#/definitions/meta::json::schema::tests::parent::level1::level2::childClass3\"\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"required\": [\n" +
                                "    \"childClass3\"\n" +
                                "  ],\n" +
                                "  \"definitions\": {\n" +
                                "    \"meta::json::schema::tests::parent::level1::level2::childClass3\": {\n" +
                                "      \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "      \"title\": \"meta::json::schema::tests::parent::level1::level2::childClass3\",\n" +
                                "      \"type\": \"object\"\n" +
                                "    },\n" +
                                "    \"meta::json::schema::tests::parent::level1::levela::childClass4\": {\n" +
                                "      \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "      \"title\": \"meta::json::schema::tests::parent::level1::levela::childClass4\",\n" +
                                "      \"type\": \"object\"\n" +
                                "    }\n" +
                                "  }\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

        String expected = ">>>example::jsonSchema::Test\n" +
                "Class {meta::external::format::json::binding::toPure::JSONSchemaGeneration.title = 'meta::json::schema::tests::parent::parent::levela::parentClass2'} example::jsonSchema::Test extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  childClass4: meta::json::schema::tests::parent::level1::levela::childClass4[0..1];\n" +
                "  childClass3: meta::json::schema::tests::parent::level1::level2::childClass3[1];\n" +
                "}\n" +
                "\n" +
                ">>>meta::json::schema::tests::parent::level1::level2::childClass3\n" +
                "Class meta::json::schema::tests::parent::level1::level2::childClass3 extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "}\n" +
                "\n" +
                ">>>meta::json::schema::tests::parent::level1::levela::childClass4\n" +
                "Class meta::json::schema::tests::parent::level1::levela::childClass4 extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testTypeInclusionForConstraintFunctions()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Test.json",
                        "{\n" +
                                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "  \"title\": \"a custom title\",\n" +
                                "  \"type\": \"object\",\n" +
                                "  \"properties\": {\n" +
                                "    \"one\": {\n" +
                                "      \"$ref\": \"functionWithStringType.json\"\n" +
                                "    },\n" +
                                "    \"many\": {\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"items\": {\n" +
                                "        \"$ref\": \"functionWithStringType.json\"\n" +
                                "      }\n" +
                                "    },\n" +
                                "    \"optional\": {\n" +
                                "      \"$ref\": \"functionWithStringType.json\"\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"required\": [\n" +
                                "    \"one\"\n" +
                                "  ]\n" +
                                "}")
                .withSchemaText("Test1", "functionWithStringType.json",
                        "{\n" +
                                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "  \"pattern\": \"test\",\n" +
                                "  \"type\": \"string\",\n" +
                                "  \"title\": \"meta::json::schema::tests::functionWithStringType\"\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

        String expected = ">>>example::jsonSchema::Test\n" +
                "Class {meta::external::format::json::binding::toPure::JSONSchemaGeneration.title = 'a custom title'} example::jsonSchema::Test extends meta::pure::metamodel::type::Any\n" +
                "[\n" +
                "  one: $this.one->example::jsonSchema::functionWithStringType(),\n" +
                "  optional: $this.optional->example::jsonSchema::functionWithStringType(),\n" +
                "  many: $this.many->forAll(value: String[1]|$value->example::jsonSchema::functionWithStringType())\n" +
                "]\n" +
                "{\n" +
                "  one: String[1];\n" +
                "  optional: String[0..1];\n" +
                "  many: String[*];\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::functionWithStringType_String_$0_1$__Boolean_1_\n" +
                "function {meta::external::format::json::binding::toPure::JSONSchemaGeneration.title = 'meta::json::schema::tests::functionWithStringType'} example::jsonSchema::functionWithStringType(value: String[0..1]): Boolean[1]\n" +
                "{\n" +
                "   if($value->isNotEmpty(), |$value->makeString()->matches('test'), |true)\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testCustomProfile()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Test.json",
                        "{\n" +
                                "  \"x-customStereoType1\": true,\n" +
                                "  \"description\": \"A simple description\",\n" +
                                "  \"title\": \"meta::json::schema::tests::ClassWithCustomProfiles\",\n" +
                                "  \"x-customTag2\": [\n" +
                                "    \"first\",\n" +
                                "    \"second\"\n" +
                                "  ],\n" +
                                "  \"type\": \"object\",\n" +
                                "  \"properties\": {\n" +
                                "    \"simpleString\": {\n" +
                                "      \"x-customStereoType1\": true,\n" +
                                "      \"type\": \"string\"\n" +
                                "    },\n" +
                                "    \"arrayMaxItemOne\": {\n" +
                                "      \"x-customStereoType2\": true,\n" +
                                "      \"maxItems\": 10,\n" +
                                "      \"type\": \"array\",\n" +
                                "      \"x-customTag3\": \"false\",\n" +
                                "      \"items\": {\n" +
                                "        \"type\": \"string\"\n" +
                                "      }\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"x-customTag1\": 10\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

        String expected = ">>>example::jsonSchema::Test\n" +
                "Class <<example::jsonSchema::profile::GeneratedProfile.customStereoType1>> {meta::external::format::json::binding::toPure::JSONSchemaGeneration.title = 'meta::json::schema::tests::ClassWithCustomProfiles', meta::pure::profiles::doc.doc = 'A simple description', example::jsonSchema::profile::GeneratedProfile.customTag1 = '10', example::jsonSchema::profile::GeneratedProfile.customTag2 = 'first', example::jsonSchema::profile::GeneratedProfile.customTag2 = 'second'} example::jsonSchema::Test extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  <<example::jsonSchema::profile::GeneratedProfile.customStereoType1>> simpleString: String[0..1];\n" +
                "  arrayMaxItemOne: String[0..10];\n" +
                "}\n" +
                "\n" +
                ">>>example::jsonSchema::profile::GeneratedProfile\n" +
                "Profile example::jsonSchema::profile::GeneratedProfile\n" +
                "{\n" +
                "  stereotypes: [customStereoType1];\n" +
                "  tags: [customTag1, customTag2];\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testInLineFunctions()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Test.json",
                        "{\n" +
                                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "  \"title\": \"meta::json::schema::tests::ClassWithInLineFunctions\",\n" +
                                "  \"type\": \"object\",\n" +
                                "  \"properties\": {\n" +
                                "    \"number\": {\n" +
                                "      \"type\": \"number\",\n" +
                                "      \"enum\": [\n" +
                                "        1,\n" +
                                "        2\n" +
                                "      ]\n" +
                                "    },\n" +
                                "    \"string\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"enum\": [\n" +
                                "        \"a\",\n" +
                                "        \"b\"\n" +
                                "      ]\n" +
                                "    },\n" +
                                "    \"stringOptional\": {\n" +
                                "      \"type\": \"string\",\n" +
                                "      \"enum\": [\n" +
                                "        \"a\",\n" +
                                "        \"b\"\n" +
                                "      ]\n" +
                                "    },\n" +
                                "    \"integer\": {\n" +
                                "      \"type\": \"integer\",\n" +
                                "      \"enum\": [\n" +
                                "        1,\n" +
                                "        2\n" +
                                "      ]\n" +
                                "    },\n" +
                                "    \"numberOptional\": {\n" +
                                "      \"type\": \"number\",\n" +
                                "      \"enum\": [\n" +
                                "        1,\n" +
                                "        2\n" +
                                "      ]\n" +
                                "    },\n" +
                                "    \"multipleOf\": {\n" +
                                "      \"type\": \"number\",\n" +
                                "      \"multipleOf\": 0.1\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"required\": [\n" +
                                "    \"number\",\n" +
                                "    \"string\",\n" +
                                "    \"integer\"\n" +
                                "  ]\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

        String expected = ">>>example::jsonSchema::Test\n" +
                "Class {meta::external::format::json::binding::toPure::JSONSchemaGeneration.title = 'meta::json::schema::tests::ClassWithInLineFunctions'} example::jsonSchema::Test extends meta::pure::metamodel::type::Any\n" +
                "[\n" +
                "  multipleOf_inLine: if($this.multipleOf->isNotEmpty(), |$this.multipleOf->toOne()->rem(0.1) == 0, |true),\n" +
                "  number_inLine: $this.number->in([1, 2]),\n" +
                "  string_inLine: $this.string->in(['a', 'b']),\n" +
                "  stringOptional_inLine: if($this.stringOptional->isNotEmpty(), |$this.stringOptional->toOne()->in(['a', 'b']), |true),\n" +
                "  integer_inLine: $this.integer->in([1, 2]),\n" +
                "  numberOptional_inLine: if($this.numberOptional->isNotEmpty(), |$this.numberOptional->toOne()->in([1, 2]), |true)\n" +
                "]\n" +
                "{\n" +
                "  multipleOf: Float[0..1];\n" +
                "  number: Float[1];\n" +
                "  string: String[1];\n" +
                "  stringOptional: String[0..1];\n" +
                "  integer: Integer[1];\n" +
                "  numberOptional: Float[0..1];\n" +
                "}\n";
        Assert.assertEquals(modelTextsFromString(expected), modelTextsFromContextData(model));
    }

    @Test
    public void testSchemaCollection()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("meta::json::schema::tests::SchemaTest", "JSON")
                .withSchemaText("Test", "model.json",
                        "{\n" +
                                "  \"definitions\": {\n" +
                                "    \"meta::json::schema::tests::ClassWithFunctionReferences\": {\n" +
                                "      \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "      \"title\": \"a custom title\",\n" +
                                "      \"type\": \"object\",\n" +
                                "      \"properties\": {\n" +
                                "        \"one\": {\n" +
                                "          \"$ref\": \"#/definitions/meta::json::schema::tests::functionWithStringType\"\n" +
                                "        },\n" +
                                "        \"optional\": {\n" +
                                "          \"$ref\": \"#/definitions/meta::json::schema::tests::functionWithStringType\"\n" +
                                "        },\n" +
                                "        \"many\": {\n" +
                                "          \"type\": \"array\",\n" +
                                "          \"items\": {\n" +
                                "            \"$ref\": \"#/definitions/meta::json::schema::tests::functionWithStringType\"\n" +
                                "          }\n" +
                                "        }\n" +
                                "      },\n" +
                                "      \"required\": [\n" +
                                "        \"one\"\n" +
                                "      ]\n" +
                                "    },\n" +
                                "    \"meta::json::schema::tests::SimpleClass\": {\n" +
                                "      \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "      \"title\": \"meta::json::schema::tests::SimpleClass\",\n" +
                                "      \"type\": \"object\",\n" +
                                "      \"properties\": {\n" +
                                "        \"p\": {\n" +
                                "          \"type\": \"string\"\n" +
                                "        },\n" +
                                "        \"a\": {\n" +
                                "          \"$ref\": \"#/definitions/meta::json::schema::tests::SimpleClass2\"\n" +
                                "        }\n" +
                                "      },\n" +
                                "      \"required\": [\n" +
                                "        \"p\"\n" +
                                "      ]\n" +
                                "    },\n" +
                                "    \"meta::json::schema::tests::SimpleClass2\": {\n" +
                                "      \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "      \"title\": \"meta::json::schema::tests::SimpleClass2\",\n" +
                                "      \"type\": \"object\",\n" +
                                "      \"properties\": {\n" +
                                "        \"p\": {\n" +
                                "          \"type\": \"string\"\n" +
                                "        }\n" +
                                "      },\n" +
                                "      \"required\": [\n" +
                                "        \"p\"\n" +
                                "      ]\n" +
                                "    },\n" +
                                "    \"meta::json::schema::tests::functionWithStringType\": {\n" +
                                "      \"pattern\": \"test\",\n" +
                                "      \"type\": \"string\"\n" +
                                "    }\n" +
                                "  }\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("meta::json::schema::tests::SchemaTest", "meta::json::schema::tests"));

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

    @Test
    public void testNullable()
    {
        String schemaCode = newExternalSchemaSetGrammarBuilder("test::Simple", "JSON")
                .withSchemaText("Test", "Test.json",
                        "{\n" +
                                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                                "  \"title\": \"meta::json::schema::tests::NullableEnum\",\n" +
                                "  \"enum\": [\n" +
                                "    \"one\",\n" +
                                "    \"two\",\n" +
                                "    null\n" +
                                "  ],\n" +
                                "  \"type\": [\n" +
                                "    \"string\",\n" +
                                "    \"null\"\n" +
                                "  ]\n" +
                                "}")
                .build();

        PureModelContextData model = generateModel(schemaCode, config("test::Simple", "example::jsonSchema"));

        String expected = ">>>example::jsonSchema::Test\n" +
                "Enum <<meta::external::format::json::binding::toPure::JSONSchemaTypeExtension.null>> {meta::external::format::json::binding::toPure::JSONSchemaGeneration.title = 'meta::json::schema::tests::NullableEnum'} example::jsonSchema::Test\n" +
                "{\n" +
                "  one,\n" +
                "  two\n" +
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
