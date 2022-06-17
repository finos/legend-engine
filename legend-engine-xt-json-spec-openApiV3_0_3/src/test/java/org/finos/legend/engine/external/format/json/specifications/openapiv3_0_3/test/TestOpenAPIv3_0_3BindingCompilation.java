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

import org.finos.legend.engine.external.format.json.specifications.TestJsonSchemaBindingCompilation;
import org.junit.Test;

public class TestOpenAPIv3_0_3BindingCompilation extends TestJsonSchemaBindingCompilation
{
    @Test
    public void testSimpleJsonSchema()
    {
        super.testSimpleJsonSchema("{\n" +
                "  \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
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
                "}\n");
    }

    @Test
    public void testVariousMultiplicityPropertiesWithJsonSchema()
    {
        super.testVariousMultiplicityPropertiesWithJsonSchema("{\n" +
                "  \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
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
                "}\n"
        );
    }

    @Test
    public void testNestingWithJsonSchema()
    {
        super.testNestingWithJsonSchema("{\n" +
                "  \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
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
                "      \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
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
                "}\n"
        );
    }

    @Test
    public void testMultiLevelNestingWithJsonSchema()
    {
        super.testMultiLevelNestingWithJsonSchema("{\n" +
                "  \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
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
                "      \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
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
                "      \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
                "      \"title\": \"test::Simple::AddressType\",\n" +
                "      \"enum\": [\n" +
                "\"HOME\",\n" +
                "\"OFFICE\",\n" +
                "\"WORKSHOP\"\n" +
                "      ],\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"test::Simple::Firm\":     {\n" +
                "      \"$schema\": \"https://spec.openapis.org/oas/v3.0.3#specification\",\n" +
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
                "}\n"
        );
    }
}
