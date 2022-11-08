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

package org.finos.legend.engine.protocol.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.jsonunit.JsonAssert;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.junit.Assert;
import org.junit.Test;

public class TestCompatibilityAndMigration
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    @Test
    public void testStringValueSpecification() throws Exception
    {
        check("{\n" +
                        "  \"_type\": \"data\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"_type\": \"function\",\n" +
                        "      \"body\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"string\",\n" +
                        "          \"multiplicity\": {\n" +
                        "            \"lowerBound\": 1,\n" +
                        "            \"upperBound\": 1\n" +
                        "          },\n" +
                        "          \"sourceInformation\": {\n" +
                        "            \"endColumn\": 4,\n" +
                        "            \"endLine\": 1,\n" +
                        "            \"sourceId\": \"a::a\",\n" +
                        "            \"startColumn\": 2,\n" +
                        "            \"startLine\": 1\n" +
                        "          },\n" +
                        "          \"values\": [\n" +
                        "            \"a\"\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"name\": \"a\",\n" +
                        "      \"package\": \"a\",\n" +
                        "      \"parameters\": [],\n" +
                        "      \"returnMultiplicity\": {\n" +
                        "        \"lowerBound\": 0\n" +
                        "      },\n" +
                        "      \"returnType\": \"String\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"a\",\n" +
                        "    \"returnType\" : \"String\",\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 0\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"string\",\n" +
                        "      \"sourceInformation\" : {\n" +
                        "        \"sourceId\" : \"a::a\",\n" +
                        "        \"startLine\" : 1,\n" +
                        "        \"startColumn\" : 2,\n" +
                        "        \"endLine\" : 1,\n" +
                        "        \"endColumn\" : 4\n" +
                        "      },\n" +
                        "      \"value\" : \"a\"\n" +
                        "    } ],\n" +
                        "    \"package\" : \"a\"\n" +
                        "  } ]\n" +
                        "}"
        );
    }

    @Test
    public void testStringCollectionValueSpecification() throws Exception
    {
        check("{\n" +
                        "  \"_type\": \"data\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"_type\": \"function\",\n" +
                        "      \"body\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"string\",\n" +
                        "          \"multiplicity\": {\n" +
                        "            \"lowerBound\": 2,\n" +
                        "            \"upperBound\": 2\n" +
                        "          },\n" +
                        "          \"sourceInformation\": {\n" +
                        "            \"endColumn\": 4,\n" +
                        "            \"endLine\": 1,\n" +
                        "            \"sourceId\": \"a::a\",\n" +
                        "            \"startColumn\": 2,\n" +
                        "            \"startLine\": 1\n" +
                        "          },\n" +
                        "          \"values\": [\n" +
                        "            \"a\",\n" +
                        "            \"b\"\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"name\": \"a\",\n" +
                        "      \"package\": \"a\",\n" +
                        "      \"parameters\": [],\n" +
                        "      \"returnMultiplicity\": {\n" +
                        "        \"lowerBound\": 0\n" +
                        "      },\n" +
                        "      \"returnType\": \"String\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"a\",\n" +
                        "    \"returnType\" : \"String\",\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 0\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"collection\",\n" +
                        "      \"sourceInformation\" : {\n" +
                        "        \"sourceId\" : \"a::a\",\n" +
                        "        \"startLine\" : 1,\n" +
                        "        \"startColumn\" : 2,\n" +
                        "        \"endLine\" : 1,\n" +
                        "        \"endColumn\" : 4\n" +
                        "      },\n" +
                        "      \"multiplicity\" : {\n" +
                        "        \"lowerBound\" : 2,\n" +
                        "        \"upperBound\" : 2\n" +
                        "      },\n" +
                        "      \"values\" : [ {\n" +
                        "        \"_type\" : \"string\",\n" +
                        "        \"value\" : \"a\"\n" +
                        "      }, {\n" +
                        "        \"_type\" : \"string\",\n" +
                        "        \"value\" : \"b\"\n" +
                        "      } ]\n" +
                        "    } ],\n" +
                        "    \"package\" : \"a\"\n" +
                        "  } ]\n" +
                        "}"
        );
    }


    @Test
    public void testPrimitiveType() throws Exception
    {
        check("{\n" +
                        "  \"_type\": \"data\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"_type\": \"function\",\n" +
                        "      \"body\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"primitiveType\",\n" +
                        "          \"name\": \"String\",\n" +
                        "          \"sourceInformation\": {\n" +
                        "            \"endColumn\": 9,\n" +
                        "            \"endLine\": 3,\n" +
                        "            \"sourceId\": \"\",\n" +
                        "            \"startColumn\": 4,\n" +
                        "            \"startLine\": 3\n" +
                        "          }\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"name\": \"a\",\n" +
                        "      \"package\": \"a\",\n" +
                        "      \"parameters\": [],\n" +
                        "      \"returnMultiplicity\": {\n" +
                        "        \"lowerBound\": 1,\n" +
                        "        \"upperBound\": 1\n" +
                        "      },\n" +
                        "      \"returnType\": \"Any\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"a\",\n" +
                        "    \"returnType\" : \"Any\",\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 1,\n" +
                        "      \"upperBound\" : 1\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"packageableElementPtr\",\n" +
                        "      \"sourceInformation\" : {\n" +
                        "        \"startLine\" : 3,\n" +
                        "        \"startColumn\" : 4,\n" +
                        "        \"endLine\" : 3,\n" +
                        "        \"endColumn\" : 9\n" +
                        "      },\n" +
                        "      \"fullPath\" : \"String\"\n" +
                        "    } ],\n" +
                        "    \"package\" : \"a\"\n" +
                        "  } ]\n" +
                        "}");
    }

    @Test
    public void testUnitType() throws Exception
    {
        check("{\n" +
                        "  \"_type\": \"data\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"_type\": \"measure\",\n" +
                        "      \"canonicalUnit\": {\n" +
                        "        \"_type\": \"unit\",\n" +
                        "        \"conversionFunction\": {\n" +
                        "          \"_type\": \"lambda\",\n" +
                        "          \"body\": [\n" +
                        "            {\n" +
                        "              \"_type\": \"var\",\n" +
                        "              \"name\": \"x\"\n" +
                        "            }\n" +
                        "          ],\n" +
                        "          \"parameters\": [\n" +
                        "            {\n" +
                        "              \"_type\": \"var\",\n" +
                        "              \"name\": \"x\"\n" +
                        "            }\n" +
                        "          ]\n" +
                        "        },\n" +
                        "        \"measure\": \"test::NewMeasure\",\n" +
                        "        \"name\": \"NewMeasure~UnitOne\",\n" +
                        "        \"package\": \"test\"\n" +
                        "      },\n" +
                        "      \"name\": \"NewMeasure\",\n" +
                        "      \"nonCanonicalUnits\": [],\n" +
                        "      \"package\": \"test\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"_type\": \"function\",\n" +
                        "      \"body\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"unitType\",\n" +
                        "          \"sourceInformation\": {\n" +
                        "            \"endColumn\": 26,\n" +
                        "            \"endLine\": 8,\n" +
                        "            \"sourceId\": \"\",\n" +
                        "            \"startColumn\": 3,\n" +
                        "            \"startLine\": 8\n" +
                        "          },\n" +
                        "          \"unitType\": \"test::NewMeasure~UnitOne\"\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"name\": \"f\",\n" +
                        "      \"package\": \"a\",\n" +
                        "      \"parameters\": [],\n" +
                        "      \"returnMultiplicity\": {\n" +
                        "        \"lowerBound\": 1,\n" +
                        "        \"upperBound\": 1\n" +
                        "      },\n" +
                        "      \"returnType\": \"Any\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"measure\",\n" +
                        "    \"name\" : \"NewMeasure\",\n" +
                        "    \"canonicalUnit\" : {\n" +
                        "      \"_type\" : \"unit\",\n" +
                        "      \"name\" : \"NewMeasure~UnitOne\",\n" +
                        "      \"measure\" : \"test::NewMeasure\",\n" +
                        "      \"conversionFunction\" : {\n" +
                        "        \"_type\" : \"lambda\",\n" +
                        "        \"body\" : [ {\n" +
                        "          \"_type\" : \"var\",\n" +
                        "          \"name\" : \"x\"\n" +
                        "        } ],\n" +
                        "        \"parameters\" : [ {\n" +
                        "          \"_type\" : \"var\",\n" +
                        "          \"name\" : \"x\"\n" +
                        "        } ]\n" +
                        "      },\n" +
                        "      \"package\" : \"test\"\n" +
                        "    },\n" +
                        "    \"package\" : \"test\"\n" +
                        "  }, {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"f\",\n" +
                        "    \"returnType\" : \"Any\",\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 1,\n" +
                        "      \"upperBound\" : 1\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"packageableElementPtr\",\n" +
                        "      \"sourceInformation\" : {\n" +
                        "        \"startLine\" : 8,\n" +
                        "        \"startColumn\" : 3,\n" +
                        "        \"endLine\" : 8,\n" +
                        "        \"endColumn\" : 26\n" +
                        "      },\n" +
                        "      \"fullPath\" : \"test::NewMeasure~UnitOne\"\n" +
                        "    } ],\n" +
                        "    \"package\" : \"a\"\n" +
                        "  } ]\n" +
                        "}");
    }

    @Test
    public void testCastType() throws Exception
    {
        check("{\n" +
                "  \"_type\": \"data\",\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"_type\": \"function\",\n" +
                "      \"body\": [\n" +
                "        {\n" +
                "          \"_type\": \"func\",\n" +
                "          \"function\": \"cast\",\n" +
                "          \"parameters\": [\n" +
                "            {\n" +
                "              \"_type\": \"packageableElementPtr\",\n" +
                "              \"fullPath\": \"a\",\n" +
                "              \"sourceInformation\": {\n" +
                "                \"endColumn\": 2,\n" +
                "                \"endLine\": 1,\n" +
                "                \"sourceId\": \"a::a\",\n" +
                "                \"startColumn\": 2,\n" +
                "                \"startLine\": 1\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"_type\": \"hackedClass\",\n" +
                "              \"fullPath\": \"Type\",\n" +
                "              \"sourceInformation\": {\n" +
                "                \"endColumn\": 7,\n" +
                "                \"endLine\": 2,\n" +
                "                \"sourceId\": \"a::a\",\n" +
                "                \"startColumn\": 4,\n" +
                "                \"startLine\": 2\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"sourceInformation\": {\n" +
                "            \"endColumn\": 8,\n" +
                "            \"endLine\": 1,\n" +
                "            \"sourceId\": \"a::a\",\n" +
                "            \"startColumn\": 5,\n" +
                "            \"startLine\": 1\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"name\": \"a\",\n" +
                "      \"package\": \"a\",\n" +
                "      \"parameters\": [],\n" +
                "      \"returnMultiplicity\": {\n" +
                "        \"lowerBound\": 0\n" +
                "      },\n" +
                "      \"returnType\": \"Any\"\n" +
                "    }\n" +
                "  ]\n" +
                "}", "{\n" +
                "  \"_type\" : \"data\",\n" +
                "  \"elements\" : [ {\n" +
                "    \"_type\" : \"function\",\n" +
                "    \"name\" : \"a\",\n" +
                "    \"returnType\" : \"Any\",\n" +
                "    \"returnMultiplicity\" : {\n" +
                "      \"lowerBound\" : 0\n" +
                "    },\n" +
                "    \"body\" : [ {\n" +
                "      \"_type\" : \"func\",\n" +
                "      \"sourceInformation\" : {\n" +
                "        \"sourceId\" : \"a::a\",\n" +
                "        \"startLine\" : 1,\n" +
                "        \"startColumn\" : 5,\n" +
                "        \"endLine\" : 1,\n" +
                "        \"endColumn\" : 8\n" +
                "      },\n" +
                "      \"function\" : \"cast\",\n" +
                "      \"parameters\" : [ {\n" +
                "        \"_type\" : \"packageableElementPtr\",\n" +
                "        \"sourceInformation\" : {\n" +
                "          \"sourceId\" : \"a::a\",\n" +
                "          \"startLine\" : 1,\n" +
                "          \"startColumn\" : 2,\n" +
                "          \"endLine\" : 1,\n" +
                "          \"endColumn\" : 2\n" +
                "        },\n" +
                "        \"fullPath\" : \"a\"\n" +
                "      }, {\n" +
                "        \"_type\" : \"genericTypeInstance\",\n" +
                "        \"sourceInformation\" : {\n" +
                "          \"sourceId\" : \"a::a\",\n" +
                "          \"startLine\" : 2,\n" +
                "          \"startColumn\" : 4,\n" +
                "          \"endLine\" : 2,\n" +
                "          \"endColumn\" : 7\n" +
                "        },\n" +
                "        \"fullPath\" : \"Type\"\n" +
                "      } ]\n" +
                "    } ],\n" +
                "    \"package\" : \"a\"\n" +
                "  } ]\n" +
                "}");
    }

    @Test
    public void testCastUnit() throws Exception
    {
        check("{\n" +
                "  \"_type\": \"data\",\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"_type\": \"function\",\n" +
                "      \"body\": [\n" +
                "        {\n" +
                "          \"_type\": \"func\",\n" +
                "          \"function\": \"cast\",\n" +
                "          \"parameters\": [\n" +
                "            {\n" +
                "              \"_type\": \"packageableElementPtr\",\n" +
                "              \"fullPath\": \"a\",\n" +
                "              \"sourceInformation\": {\n" +
                "                \"endColumn\": 2,\n" +
                "                \"endLine\": 1,\n" +
                "                \"sourceId\": \"a::a\",\n" +
                "                \"startColumn\": 2,\n" +
                "                \"startLine\": 1\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"_type\": \"hackedUnit\",\n" +
                "              \"sourceInformation\": {\n" +
                "                \"endColumn\": 9,\n" +
                "                \"endLine\": 2,\n" +
                "                \"sourceId\": \"a::a\",\n" +
                "                \"startColumn\": 4,\n" +
                "                \"startLine\": 2\n" +
                "              },\n" +
                "              \"unitType\": \"Type~A\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"sourceInformation\": {\n" +
                "            \"endColumn\": 8,\n" +
                "            \"endLine\": 1,\n" +
                "            \"sourceId\": \"a::a\",\n" +
                "            \"startColumn\": 5,\n" +
                "            \"startLine\": 1\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"name\": \"a\",\n" +
                "      \"package\": \"a\",\n" +
                "      \"parameters\": [],\n" +
                "      \"returnMultiplicity\": {\n" +
                "        \"lowerBound\": 0\n" +
                "      },\n" +
                "      \"returnType\": \"Any\"\n" +
                "    }\n" +
                "  ]\n" +
                "}", "{\n" +
                "  \"_type\" : \"data\",\n" +
                "  \"elements\" : [ {\n" +
                "    \"_type\" : \"function\",\n" +
                "    \"name\" : \"a\",\n" +
                "    \"returnType\" : \"Any\",\n" +
                "    \"returnMultiplicity\" : {\n" +
                "      \"lowerBound\" : 0\n" +
                "    },\n" +
                "    \"body\" : [ {\n" +
                "      \"_type\" : \"func\",\n" +
                "      \"sourceInformation\" : {\n" +
                "        \"sourceId\" : \"a::a\",\n" +
                "        \"startLine\" : 1,\n" +
                "        \"startColumn\" : 5,\n" +
                "        \"endLine\" : 1,\n" +
                "        \"endColumn\" : 8\n" +
                "      },\n" +
                "      \"function\" : \"cast\",\n" +
                "      \"parameters\" : [ {\n" +
                "        \"_type\" : \"packageableElementPtr\",\n" +
                "        \"sourceInformation\" : {\n" +
                "          \"sourceId\" : \"a::a\",\n" +
                "          \"startLine\" : 1,\n" +
                "          \"startColumn\" : 2,\n" +
                "          \"endLine\" : 1,\n" +
                "          \"endColumn\" : 2\n" +
                "        },\n" +
                "        \"fullPath\" : \"a\"\n" +
                "      }, {\n" +
                "        \"_type\" : \"genericTypeInstance\",\n" +
                "        \"sourceInformation\" : {\n" +
                "          \"sourceId\" : \"a::a\",\n" +
                "          \"startLine\" : 2,\n" +
                "          \"startColumn\" : 4,\n" +
                "          \"endLine\" : 2,\n" +
                "          \"endColumn\" : 9\n" +
                "        },\n" +
                "        \"fullPath\" : \"Type~A\"\n" +
                "      } ]\n" +
                "    } ],\n" +
                "    \"package\" : \"a\"\n" +
                "  } ]\n" +
                "}");
    }

    @Test
    public void testPath() throws Exception
    {
        check("{\n" +
                "  \"_type\": \"data\",\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"_type\": \"function\",\n" +
                "      \"body\": [\n" +
                "        {\n" +
                "          \"_type\": \"path\",\n" +
                "          \"path\": [\n" +
                "            {\n" +
                "              \"_type\": \"propertyPath\",\n" +
                "              \"parameters\": [],\n" +
                "              \"property\": \"name\",\n" +
                "              \"sourceInformation\": {\n" +
                "                \"endColumn\": 17,\n" +
                "                \"endLine\": 1,\n" +
                "                \"sourceId\": \"a::f\",\n" +
                "                \"startColumn\": 13,\n" +
                "                \"startLine\": 1\n" +
                "              }\n" +
                "            }\n" +
                "          ],\n" +
                "          \"sourceInformation\": {\n" +
                "            \"endColumn\": 22,\n" +
                "            \"endLine\": 1,\n" +
                "            \"sourceId\": \"a::f\",\n" +
                "            \"startColumn\": 11,\n" +
                "            \"startLine\": 1\n" +
                "          },\n" +
                "          \"startType\": \"A\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"name\": \"f\",\n" +
                "      \"package\": \"a\",\n" +
                "      \"parameters\": [],\n" +
                "      \"returnMultiplicity\": {\n" +
                "        \"lowerBound\": 1,\n" +
                "        \"upperBound\": 1\n" +
                "      },\n" +
                "      \"returnType\": \"String\"\n" +
                "    }\n" +
                "  ]\n" +
                "}", "{\n" +
                "  \"_type\" : \"data\",\n" +
                "  \"elements\" : [ {\n" +
                "    \"_type\" : \"function\",\n" +
                "    \"name\" : \"f\",\n" +
                "    \"returnType\" : \"String\",\n" +
                "    \"returnMultiplicity\" : {\n" +
                "      \"lowerBound\" : 1,\n" +
                "      \"upperBound\" : 1\n" +
                "    },\n" +
                "    \"body\" : [ {\n" +
                "      \"_type\" : \"classInstance\",\n" +
                "      \"type\" : \"path\",\n" +
                "      \"value\" : {\n" +
                "        \"sourceInformation\" : {\n" +
                "          \"sourceId\" : \"a::f\",\n" +
                "          \"startLine\" : 1,\n" +
                "          \"startColumn\" : 11,\n" +
                "          \"endLine\" : 1,\n" +
                "          \"endColumn\" : 22\n" +
                "        },\n" +
                "        \"startType\" : \"A\",\n" +
                "        \"path\" : [ {\n" +
                "          \"_type\" : \"propertyPath\",\n" +
                "          \"sourceInformation\" : {\n" +
                "            \"sourceId\" : \"a::f\",\n" +
                "            \"startLine\" : 1,\n" +
                "            \"startColumn\" : 13,\n" +
                "            \"endLine\" : 1,\n" +
                "            \"endColumn\" : 17\n" +
                "          },\n" +
                "          \"property\" : \"name\"\n" +
                "        } ]\n" +
                "      }\n" +
                "    } ],\n" +
                "    \"package\" : \"a\"\n" +
                "  } ]\n" +
                "}");
    }

    @Test
    public void testGraphfetch() throws Exception
    {
        check("{\n" +
                "  \"_type\": \"data\",\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"_type\": \"function\",\n" +
                "      \"body\": [\n" +
                "        {\n" +
                "          \"_type\": \"rootGraphFetchTree\",\n" +
                "          \"class\": \"A\",\n" +
                "          \"sourceInformation\": {\n" +
                "            \"endColumn\": 4,\n" +
                "            \"endLine\": 1,\n" +
                "            \"sourceId\": \"a::f\",\n" +
                "            \"startColumn\": 4,\n" +
                "            \"startLine\": 1\n" +
                "          },\n" +
                "          \"subTrees\": [\n" +
                "            {\n" +
                "              \"_type\": \"propertyGraphFetchTree\",\n" +
                "              \"parameters\": [],\n" +
                "              \"property\": \"name\",\n" +
                "              \"sourceInformation\": {\n" +
                "                \"endColumn\": 9,\n" +
                "                \"endLine\": 1,\n" +
                "                \"sourceId\": \"a::f\",\n" +
                "                \"startColumn\": 6,\n" +
                "                \"startLine\": 1\n" +
                "              },\n" +
                "              \"subTrees\": []\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ],\n" +
                "      \"name\": \"f\",\n" +
                "      \"package\": \"a\",\n" +
                "      \"parameters\": [],\n" +
                "      \"returnMultiplicity\": {\n" +
                "        \"lowerBound\": 1,\n" +
                "        \"upperBound\": 1\n" +
                "      },\n" +
                "      \"returnType\": \"String\"\n" +
                "    }\n" +
                "  ]\n" +
                "}", "{\n" +
                "  \"_type\" : \"data\",\n" +
                "  \"elements\" : [ {\n" +
                "    \"_type\" : \"function\",\n" +
                "    \"name\" : \"f\",\n" +
                "    \"returnType\" : \"String\",\n" +
                "    \"returnMultiplicity\" : {\n" +
                "      \"lowerBound\" : 1,\n" +
                "      \"upperBound\" : 1\n" +
                "    },\n" +
                "    \"body\" : [ {\n" +
                "      \"_type\" : \"classInstance\",\n" +
                "      \"type\" : \"rootGraphFetchTree\",\n" +
                "      \"value\" : {\n" +
                "        \"sourceInformation\" : {\n" +
                "          \"sourceId\" : \"a::f\",\n" +
                "          \"startLine\" : 1,\n" +
                "          \"startColumn\" : 4,\n" +
                "          \"endLine\" : 1,\n" +
                "          \"endColumn\" : 4\n" +
                "        },\n" +
                "        \"subTrees\" : [ {\n" +
                "          \"_type\" : \"propertyGraphFetchTree\",\n" +
                "          \"sourceInformation\" : {\n" +
                "            \"sourceId\" : \"a::f\",\n" +
                "            \"startLine\" : 1,\n" +
                "            \"startColumn\" : 6,\n" +
                "            \"endLine\" : 1,\n" +
                "            \"endColumn\" : 9\n" +
                "          },\n" +
                "          \"_type\" : \"propertyGraphFetchTree\",\n" +
                "          \"property\" : \"name\"\n" +
                "        } ],\n" +
                "        \"_type\" : \"rootGraphFetchTree\",\n" +
                "        \"class\" : \"A\"\n" +
                "      }\n" +
                "    } ],\n" +
                "    \"package\" : \"a\"\n" +
                "  } ]\n" +
                "}");
    }

    @Test
    public void testListInstance() throws Exception
    {
        check("{\n" +
                        "  \"_type\": \"data\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"_type\": \"function\",\n" +
                        "      \"body\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"listInstance\",\n" +
                        "          \"sourceInformation\": {\n" +
                        "            \"endColumn\": 4,\n" +
                        "            \"endLine\": 1,\n" +
                        "            \"sourceId\": \"a::a\",\n" +
                        "            \"startColumn\": 2,\n" +
                        "            \"startLine\": 1\n" +
                        "          }\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"name\": \"a\",\n" +
                        "      \"package\": \"a\",\n" +
                        "      \"parameters\": [],\n" +
                        "      \"returnMultiplicity\": {\n" +
                        "        \"lowerBound\": 0\n" +
                        "      },\n" +
                        "      \"returnType\": \"String\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"a\",\n" +
                        "    \"returnType\" : \"String\",\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 0\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"classInstance\",\n" +
                        "      \"type\" : \"listInstance\",\n" +
                        "      \"value\" : {\n" +
                        "        \"sourceInformation\" : {\n" +
                        "          \"sourceId\" : \"a::a\",\n" +
                        "          \"startLine\" : 1,\n" +
                        "          \"startColumn\" : 2,\n" +
                        "          \"endLine\" : 1,\n" +
                        "          \"endColumn\" : 4\n" +
                        "        }\n" +
                        "      }\n" +
                        "    } ],\n" +
                        "    \"package\" : \"a\"\n" +
                        "  } ]\n" +
                        "}"
        );

        check("{\n" +
                        "  \"_type\": \"data\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"_type\": \"function\",\n" +
                        "      \"body\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"listInstance\"\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"name\": \"a\",\n" +
                        "      \"package\": \"a\",\n" +
                        "      \"parameters\": [],\n" +
                        "      \"returnMultiplicity\": {\n" +
                        "        \"lowerBound\": 0\n" +
                        "      },\n" +
                        "      \"returnType\": \"String\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"a\",\n" +
                        "    \"returnType\" : \"String\",\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 0\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"classInstance\",\n" +
                        "      \"type\" : \"listInstance\",\n" +
                        "      \"value\" : { }\n" +
                        "    } ],\n" +
                        "    \"package\" : \"a\"\n" +
                        "  } ]\n" +
                        "}"
        );

        check("{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"a\",\n" +
                        "    \"returnType\" : \"String\",\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 0\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"classInstance\",\n" +
                        "      \"type\" : \"listInstance\",\n" +
                        "      \"value\" : { }\n" +
                        "    } ],\n" +
                        "    \"package\" : \"a\"\n" +
                        "  } ]\n" +
                        "}",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"a\",\n" +
                        "    \"returnType\" : \"String\",\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 0\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"classInstance\",\n" +
                        "      \"type\" : \"listInstance\",\n" +
                        "      \"value\" : { }\n" +
                        "    } ],\n" +
                        "    \"package\" : \"a\"\n" +
                        "  } ]\n" +
                        "}"
        );

    }


    @Test
    public void checkEmptyStringBugFix() throws Exception
    {
        check("{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"a\",\n" +
                        "    \"returnType\" : \"String\",\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 0\n" +
                        "    },\n" +
                        "    \"body\" : [ \n" +
                        "{\n" +
                        "  \"_type\": \"lambda\",\n" +
                        "  \"body\": [\n" +
                        "    {\n" +
                        "      \"_type\": \"func\",\n" +
                        "      \"function\": \"new\",\n" +
                        "      \"parameters\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"packageableElementPtr\",\n" +
                        "          \"fullPath\": \"A\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"_type\": \"string\",\n" +
                        "          \"multiplicity\": {\n" +
                        "            \"lowerBound\": 1,\n" +
                        "            \"upperBound\": 1\n" +
                        "          },\n" +
                        "          \"values\": []\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"_type\": \"collection\",\n" +
                        "          \"multiplicity\": {\n" +
                        "            \"lowerBound\": 0,\n" +
                        "            \"upperBound\": 0\n" +
                        "          },\n" +
                        "          \"values\": []\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"parameters\": [],\n" +
                        "  \"sourceInformation\": {\n" +
                        "    \"endColumn\": 5,\n" +
                        "    \"endLine\": 1,\n" +
                        "    \"sourceId\": \"\",\n" +
                        "    \"startColumn\": 1,\n" +
                        "    \"startLine\": 1\n" +
                        "  }\n" +
                        "}" + "    ],\n" +
                        "    \"package\" : \"a\"\n" +
                        "  } ]\n" +
                        "}",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"a\",\n" +
                        "    \"returnType\" : \"String\",\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 0\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"lambda\",\n" +
                        "      \"sourceInformation\" : {\n" +
                        "        \"startLine\" : 1,\n" +
                        "        \"startColumn\" : 1,\n" +
                        "        \"endLine\" : 1,\n" +
                        "        \"endColumn\" : 5\n" +
                        "      },\n" +
                        "      \"body\" : [ {\n" +
                        "        \"_type\" : \"func\",\n" +
                        "        \"function\" : \"new\",\n" +
                        "        \"parameters\" : [ {\n" +
                        "          \"_type\" : \"packageableElementPtr\",\n" +
                        "          \"fullPath\" : \"A\"\n" +
                        "        }, {\n" +
                        "          \"_type\" : \"string\"\n" +
                        "        }, {\n" +
                        "          \"_type\" : \"collection\",\n" +
                        "          \"multiplicity\" : {\n" +
                        "            \"lowerBound\" : 0,\n" +
                        "            \"upperBound\" : 0\n" +
                        "          }\n" +
                        "        } ]\n" +
                        "      } ]\n" +
                        "    } ],\n" +
                        "    \"package\" : \"a\"\n" +
                        "  } ]\n" +
                        "}");
    }

    private void check(String input, String output) throws Exception
    {
        PureModelContextData context = objectMapper.readValue(input, PureModelContextData.class);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
        JsonAssert.assertJsonEquals(output, json);
    }

}
