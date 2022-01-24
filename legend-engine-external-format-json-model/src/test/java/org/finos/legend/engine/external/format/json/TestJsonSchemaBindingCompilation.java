package org.finos.legend.engine.external.format.json;

import org.finos.legend.engine.external.shared.format.model.test.ExternalSchemaCompilationTest;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.junit.Test;

public class TestJsonSchemaBindingCompilation extends ExternalSchemaCompilationTest
{
    @Test
    public void testSimpleJsonSchema()
    {
        test("###Pure\n" +
                "Class test::model::A\n" +
                "{\n" +
                "  name        : String[1];\n" +
                "  employed    : Boolean[0..1];\n" +
                "  iq          : Integer[0..1];\n" +
                "  weightKg    : Float[0..1];\n" +
                "  heightM     : Decimal[1];\n" +
                "  dateOfBirth : StrictDate[1];\n" +
                "  timeOfDeath : DateTime[1];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                jsonSchemaSet("{\n" +
                        "  \"$schema\": \"http:\\/\\/json-schema.org\\/draft-07\\/schema#\",\n" +
                        "  \"title\": \"test::model::A\",\n" +
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
                        "}\n", "test/model/A.json") +
                "\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::model::A ];\n" +
                "}\n"
        );
    }

    @Test
    public void testVariousMultiplicityPropertiesWithJsonSchema()
    {
        test("###Pure\n" +
                "Class test::gen::Data\n" +
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
                "}\n" +
                "###ExternalFormat\n" +
                jsonSchemaSet("{\n" +
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
                        "}\n", "test/gen/Data.json") +
                "\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::gen::Data ];\n" +
                "}\n"
        );
    }

    @Test
    public void testNestingWithJsonSchema()
    {
        test("###Pure\n" +
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
                "}\n" +
                "###ExternalFormat\n" +
                jsonSchemaSet("{\n" +
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
                        "}\n", "test/Simple/Person.json") +
                "\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::Simple::Person, test::Simple::Firm ];\n" +
                "}\n"
        );
    }

    @Test
    public void testMultiLevelNestingWithJsonSchema()
    {
        test("###Pure\n" +
                "Enum test::Simple::AddressType\n" +
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
                "}\n" +
                "###ExternalFormat\n" +
                jsonSchemaSet("{\n" +
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
                        "}\n", "test/Simple/Person.json") +
                "\n" +
                "Binding test::Binding\n" +
                "{\n" +
                "  schemaSet: test::SchemaSet;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::Simple::Person, test::Simple::Firm, test::Simple::Address, test::Simple::AddressType ];\n" +
                "}\n"
        );
    }

    private String jsonSchemaSet(String jsonSchema, String location)
    {
        return "SchemaSet test::SchemaSet\n" +
                "{\n" +
                "  format: JSON;\n" +
                "  schemas: [ { location: '" + location + "';\n" +
                "               content: " + PureGrammarComposerUtility.convertString(jsonSchema, true) + "; } ];\n" +
                "}\n";
    }
}
