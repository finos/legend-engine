package org.finos.legend.engine.external.format.json;

import org.finos.legend.engine.external.shared.format.model.test.ExternalSchemaCompilationTest;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.junit.Test;

public class TestJsonSchemaCompilation extends ExternalSchemaCompilationTest
{
    @Test
    public void testSimpleJsonSchema()
    {
        testJsonSchema("{\n" +
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
                "}", "example/jsonSchema/Test.json");
    }

    @Test
    public void testOptionalPropertiesWithJsonSchema()
    {
        testJsonSchema("{\n" +
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
                "}", "example/jsonSchema/Test.json");
    }

    @Test
    public void testVariousMultiplicityPropertiesWithJsonSchema()
    {
        testJsonSchema("{\n" +
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
                "}", "example/jsonSchema/Test.json");
    }

    @Test
    public void testNestedJsonSchema()
    {
        testJsonSchema("{\n" +
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
                "}", "demo/jsonSchema/Person.json");
    }

    @Test
    public void testMultiLevelNestingWithJsonSchema()
    {
        testJsonSchema("{\n" +
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
                "}", "demo/jsonSchema/Person.json");
    }

    private void testJsonSchema(String jsonSchema, String location)
    {
        testJsonSchema(jsonSchema, location, null);
    }

    private void testJsonSchema(String jsonSchema, String location, String expectedError)
    {
        test("###ExternalFormat\n" +
                        "SchemaSet test::Example1\n" +
                        "{\n" +
                        "  format: JSON;\n" +
                        "  schemas: [ { location: '" + location + "';\n" +
                        "               content: " + PureGrammarComposerUtility.convertString(jsonSchema, true) + "; } ];\n" +
                        "}\n",
                expectedError
        );
    }
}
