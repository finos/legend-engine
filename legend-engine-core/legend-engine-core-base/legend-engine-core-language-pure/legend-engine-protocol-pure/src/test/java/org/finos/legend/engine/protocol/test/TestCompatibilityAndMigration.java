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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.javacrumbs.jsonunit.JsonAssert;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.junit.Assert;
import org.junit.Test;

import java.util.Objects;
import java.util.Scanner;

public class TestCompatibilityAndMigration
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

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
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"String\"\n" +
                        "      }\n" +
                        "    },\n" +
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
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"String\"\n" +
                        "      }\n" +
                        "    },\n" +
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
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"Any\"\n" +
                        "      }\n" +
                        "    },\n" +
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
                        "        \"name\": \"UnitOne\",\n" +
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
                        "      \"name\" : \"UnitOne\",\n" +
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
                        "      }\n" +
                        "    },\n" +
                        "    \"package\" : \"test\"\n" +
                        "  }, {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"f\",\n" +
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"Any\"\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 1,\n" +
                        "      \"upperBound\" : 1\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"unitType\",\n" +
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
                        "}",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"a\",\n" +
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"Any\"\n" +
                        "      }\n" +
                        "    },\n" +
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
                        "        \"genericType\" : {\n" +
                        "          \"rawType\" : {\n" +
                        "            \"_type\" : \"packageableType\",\n" +
                        "            \"fullPath\" : \"Type\"\n" +
                        "          }\n" +
                        "        }\n" +
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
                        "}",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"a\",\n" +
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"Any\"\n" +
                        "      }\n" +
                        "    },\n" +
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
                        "        \"genericType\" : {\n" +
                        "          \"rawType\" : {\n" +
                        "            \"_type\" : \"packageableType\",\n" +
                        "            \"fullPath\" : \"Type~A\"\n" +
                        "          }\n" +
                        "        }\n" +
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
                "}",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"f\",\n" +
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"String\"\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 1,\n" +
                        "      \"upperBound\" : 1\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"classInstance\",\n" +
                        "      \"sourceInformation\" : {\n" +
                        "        \"sourceId\" : \"a::f\",\n" +
                        "        \"startLine\" : 1,\n" +
                        "        \"startColumn\" : 11,\n" +
                        "        \"endLine\" : 1,\n" +
                        "        \"endColumn\" : 22\n" +
                        "      },\n" +
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
                "}",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"f\",\n" +
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"String\"\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 1,\n" +
                        "      \"upperBound\" : 1\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"classInstance\",\n" +
                        "      \"sourceInformation\" : {\n" +
                        "        \"sourceId\" : \"a::f\",\n" +
                        "        \"startLine\" : 1,\n" +
                        "        \"startColumn\" : 4,\n" +
                        "        \"endLine\" : 1,\n" +
                        "        \"endColumn\" : 4\n" +
                        "      },\n" +
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
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"String\"\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 0\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"classInstance\",\n" +
                        "      \"sourceInformation\" : {\n" +
                        "        \"sourceId\" : \"a::a\",\n" +
                        "        \"startLine\" : 1,\n" +
                        "        \"startColumn\" : 2,\n" +
                        "        \"endLine\" : 1,\n" +
                        "        \"endColumn\" : 4\n" +
                        "      },\n" +
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
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"String\"\n" +
                        "      }\n" +
                        "    },\n" +
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
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"String\"\n" +
                        "      }\n" +
                        "    },\n" +
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
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"String\"\n" +
                        "      }\n" +
                        "    },\n" +
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
                        "}\n");
    }


    @Test
    public void testModelStoreData() throws Exception
    {
        check("{\n" +
                "  \"_type\": \"data\",\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"_type\": \"dataElement\",\n" +
                "      \"data\": {\n" +
                "        \"_type\": \"modelStore\",\n" +
                "        \"instances\": {\n" +
                "          \"my::Person\": {\n" +
                "            \"_type\" : \"collection\",\n" +
                "            \"multiplicity\" : {\n" +
                "              \"lowerBound\" : 1,\n" +
                "              \"upperBound\" : 1\n" +
                "            },\n" +
                "            \"values\" : [ {\n" +
                "              \"_type\" : \"func\",\n" +
                "              \"function\" : \"new\",\n" +
                "              \"parameters\" : [ {\n" +
                "                \"_type\" : \"packageableElementPtr\",\n" +
                "                \"fullPath\" : \"my::Person\"\n" +
                "              }, {\n" +
                "                \"_type\" : \"string\",\n" +
                "                \"value\" : \"dummy\"\n" +
                "              }, {\n" +
                "                \"_type\" : \"collection\",\n" +
                "                \"multiplicity\" : {\n" +
                "                  \"lowerBound\" : 1,\n" +
                "                  \"upperBound\" : 1\n" +
                "                },\n" +
                "                \"values\" : [ {\n" +
                "                  \"_type\" : \"keyExpression\",\n" +
                "                  \"add\" : false,\n" +
                "                  \"expression\" : {\n" +
                "                    \"_type\" : \"collection\",\n" +
                "                    \"multiplicity\" : {\n" +
                "                      \"lowerBound\" : 2,\n" +
                "                      \"upperBound\" : 2\n" +
                "                    },\n" +
                "                    \"values\" : [ {\n" +
                "                      \"_type\" : \"string\",\n" +
                "                      \"value\" : \"Fred\"\n" +
                "                    }, {\n" +
                "                      \"_type\" : \"string\",\n" +
                "                      \"value\" : \"William\"\n" +
                "                    } ]\n" +
                "                  },\n" +
                "                  \"key\" : {\n" +
                "                    \"_type\" : \"string\",\n" +
                "                    \"value\" : \"givenNames\"\n" +
                "                  }\n" +
                "                } ]\n" +
                "              } ]\n" +
                "            } ]\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"name\": \"dataWithModelStore\",\n" +
                "      \"package\": \"my\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"_type\": \"dataElement\",\n" +
                "      \"name\": \"dataWithModelStore2\",\n" +
                "      \"data\": {\n" +
                "        \"_type\": \"modelStore\",\n" +
                "        \"modelData\": [\n" +
                "          {\n" +
                "            \"_type\": \"modelInstanceData\",\n" +
                "            \"model\": \"my::Person\",\n" +
                "            \"instances\": {\n" +
                "          \"_type\" : \"collection\",\n" +
                "          \"multiplicity\" : {\n" +
                "            \"lowerBound\" : 1,\n" +
                "            \"upperBound\" : 1\n" +
                "          },\n" +
                "          \"values\" : [ {\n" +
                "            \"_type\" : \"func\",\n" +
                "            \"function\" : \"new\",\n" +
                "            \"parameters\" : [ {\n" +
                "              \"_type\" : \"packageableElementPtr\",\n" +
                "              \"fullPath\" : \"my::Person\"\n" +
                "            }, {\n" +
                "              \"_type\" : \"string\",\n" +
                "              \"value\" : \"dummy\"\n" +
                "            }, {\n" +
                "              \"_type\" : \"collection\",\n" +
                "              \"multiplicity\" : {\n" +
                "                \"lowerBound\" : 1,\n" +
                "                \"upperBound\" : 1\n" +
                "              },\n" +
                "              \"values\" : [ {\n" +
                "                \"_type\" : \"keyExpression\",\n" +
                "                \"add\" : false,\n" +
                "                \"expression\" : {\n" +
                "                  \"_type\" : \"collection\",\n" +
                "                  \"multiplicity\" : {\n" +
                "                    \"lowerBound\" : 2,\n" +
                "                    \"upperBound\" : 2\n" +
                "                  },\n" +
                "                  \"values\" : [ {\n" +
                "                    \"_type\" : \"string\",\n" +
                "                    \"value\" : \"Fred\"\n" +
                "                  }, {\n" +
                "                    \"_type\" : \"string\",\n" +
                "                    \"value\" : \"William\"\n" +
                "                  } ]\n" +
                "                },\n" +
                "                \"key\" : {\n" +
                "                  \"_type\" : \"string\",\n" +
                "                  \"value\" : \"givenNames\"\n" +
                "                }\n" +
                "              } ]\n" +
                "            } ]\n" +
                "          } ]\n" +
                "        }\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      \"package\": \"my\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n", "{\n" +
                "  \"_type\": \"data\",\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"_type\": \"dataElement\",\n" +
                "      \"data\": {\n" +
                "        \"_type\": \"modelStore\",\n" +
                "        \"modelData\": [\n" +
                "          {\n" +
                "            \"_type\": \"modelInstanceData\",\n" +
                "            \"model\": \"my::Person\",\n" +
                "            \"instances\": {\n" +
                "              \"_type\": \"collection\",\n" +
                "              \"multiplicity\": {\n" +
                "                \"lowerBound\": 1,\n" +
                "                \"upperBound\": 1\n" +
                "              },\n" +
                "              \"values\": [\n" +
                "                {\n" +
                "                  \"_type\": \"func\",\n" +
                "                  \"function\": \"new\",\n" +
                "                  \"parameters\": [\n" +
                "                    {\n" +
                "                      \"_type\": \"packageableElementPtr\",\n" +
                "                      \"fullPath\": \"my::Person\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                      \"_type\": \"string\",\n" +
                "                      \"value\": \"dummy\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                      \"_type\": \"collection\",\n" +
                "                      \"multiplicity\": {\n" +
                "                        \"lowerBound\": 1,\n" +
                "                        \"upperBound\": 1\n" +
                "                      },\n" +
                "                      \"values\": [\n" +
                "                        {\n" +
                "                          \"_type\": \"keyExpression\",\n" +
                "                          \"add\": false,\n" +
                "                          \"expression\": {\n" +
                "                            \"_type\": \"collection\",\n" +
                "                            \"multiplicity\": {\n" +
                "                              \"lowerBound\": 2,\n" +
                "                              \"upperBound\": 2\n" +
                "                            },\n" +
                "                            \"values\": [\n" +
                "                              {\n" +
                "                                \"_type\": \"string\",\n" +
                "                                \"value\": \"Fred\"\n" +
                "                              },\n" +
                "                              {\n" +
                "                                \"_type\": \"string\",\n" +
                "                                \"value\": \"William\"\n" +
                "                              }\n" +
                "                            ]\n" +
                "                          },\n" +
                "                          \"key\": {\n" +
                "                            \"_type\": \"string\",\n" +
                "                            \"value\": \"givenNames\"\n" +
                "                          }\n" +
                "                        }\n" +
                "                      ]\n" +
                "                    }\n" +
                "                  ]\n" +
                "                }\n" +
                "              ]\n" +
                "            }\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      \"name\": \"dataWithModelStore\",\n" +
                "      \"package\": \"my\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"_type\": \"dataElement\",\n" +
                "      \"name\": \"dataWithModelStore2\",\n" +
                "      \"data\": {\n" +
                "        \"_type\": \"modelStore\",\n" +
                "        \"modelData\": [\n" +
                "          {\n" +
                "            \"_type\": \"modelInstanceData\",\n" +
                "            \"model\": \"my::Person\",\n" +
                "            \"instances\": {\n" +
                "              \"_type\": \"collection\",\n" +
                "              \"multiplicity\": {\n" +
                "                \"lowerBound\": 1,\n" +
                "                \"upperBound\": 1\n" +
                "              },\n" +
                "              \"values\": [\n" +
                "                {\n" +
                "                  \"_type\": \"func\",\n" +
                "                  \"function\": \"new\",\n" +
                "                  \"parameters\": [\n" +
                "                    {\n" +
                "                      \"_type\": \"packageableElementPtr\",\n" +
                "                      \"fullPath\": \"my::Person\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                      \"_type\": \"string\",\n" +
                "                      \"value\": \"dummy\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                      \"_type\": \"collection\",\n" +
                "                      \"multiplicity\": {\n" +
                "                        \"lowerBound\": 1,\n" +
                "                        \"upperBound\": 1\n" +
                "                      },\n" +
                "                      \"values\": [\n" +
                "                        {\n" +
                "                          \"_type\": \"keyExpression\",\n" +
                "                          \"add\": false,\n" +
                "                          \"expression\": {\n" +
                "                            \"_type\": \"collection\",\n" +
                "                            \"multiplicity\": {\n" +
                "                              \"lowerBound\": 2,\n" +
                "                              \"upperBound\": 2\n" +
                "                            },\n" +
                "                            \"values\": [\n" +
                "                              {\n" +
                "                                \"_type\": \"string\",\n" +
                "                                \"value\": \"Fred\"\n" +
                "                              },\n" +
                "                              {\n" +
                "                                \"_type\": \"string\",\n" +
                "                                \"value\": \"William\"\n" +
                "                              }\n" +
                "                            ]\n" +
                "                          },\n" +
                "                          \"key\": {\n" +
                "                            \"_type\": \"string\",\n" +
                "                            \"value\": \"givenNames\"\n" +
                "                          }\n" +
                "                        }\n" +
                "                      ]\n" +
                "                    }\n" +
                "                  ]\n" +
                "                }\n" +
                "              ]\n" +
                "            }\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      \"package\": \"my\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n");
    }

    @Test
    public void testDataElementReference() throws Exception
    {
        check(
                "{\n" +
                        "  \"_type\": \"data\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"_type\": \"dataElement\",\n" +
                        "      \"data\": {\n" +
                        "        \"_type\": \"reference\",\n" +
                        "        \"dataElement\": \"com::path::exampleReference\"\n" +
                        "      },\n" +
                        "      \"name\": \"dataElementReferenceExample\",\n" +
                        "      \"package\": \"my\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "{\n" +
                        "  \"_type\": \"data\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"_type\": \"dataElement\",\n" +
                        "      \"data\": {\n" +
                        "        \"_type\": \"reference\",\n" +
                        "        \"dataElement\":{\"path\":\"com::path::exampleReference\",\"type\":\"DATA\"}" +
                        "      },\n" +
                        "      \"name\": \"dataElementReferenceExample\",\n" +
                        "      \"package\": \"my\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n");
    }

    @Test
    public void testGenericTypeInstance() throws Exception
    {
        check(
                "{\n" +
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
                        "              \"_type\": \"collection\",\n" +
                        "              \"multiplicity\": {\n" +
                        "                \"lowerBound\": 0,\n" +
                        "                \"upperBound\": 0\n" +
                        "              },\n" +
                        "              \"values\": []\n" +
                        "            },\n" +
                        "            {\n" +
                        "              \"_type\": \"genericTypeInstance\",\n" +
                        "              \"fullPath\": \"String\"\n" +
                        "            }\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"name\": \"f__String_$0_1$_\",\n" +
                        "      \"package\": \"withPath\",\n" +
                        "      \"parameters\": [],\n" +
                        "      \"postConstraints\": [],\n" +
                        "      \"preConstraints\": [],\n" +
                        "      \"returnMultiplicity\": {\n" +
                        "        \"lowerBound\": 0,\n" +
                        "        \"upperBound\": 1\n" +
                        "      },\n" +
                        "      \"returnType\": \"String\",\n" +
                        "      \"stereotypes\": [],\n" +
                        "      \"taggedValues\": []\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"f__String_$0_1$_\",\n" +
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"String\"\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 0,\n" +
                        "      \"upperBound\" : 1\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"func\",\n" +
                        "      \"function\" : \"cast\",\n" +
                        "      \"parameters\" : [ {\n" +
                        "        \"_type\" : \"collection\",\n" +
                        "        \"multiplicity\" : {\n" +
                        "          \"lowerBound\" : 0,\n" +
                        "          \"upperBound\" : 0\n" +
                        "        }\n" +
                        "      }, {\n" +
                        "        \"_type\" : \"genericTypeInstance\",\n" +
                        "        \"genericType\" : {\n" +
                        "          \"rawType\" : {\n" +
                        "            \"_type\" : \"packageableType\",\n" +
                        "            \"fullPath\" : \"String\"\n" +
                        "          }\n" +
                        "        }\n" +
                        "      } ]\n" +
                        "    } ],\n" +
                        "    \"package\" : \"withPath\"\n" +
                        "  } ]\n" +
                        "}");
    }

    @Test
    public void testVariable() throws Exception
    {
        check("{\n" +
                        "  \"_type\": \"data\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"_type\": \"function\",\n" +
                        "      \"body\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"collection\",\n" +
                        "          \"multiplicity\": {\n" +
                        "            \"lowerBound\": 0,\n" +
                        "            \"upperBound\": 0\n" +
                        "          },\n" +
                        "          \"values\": []\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"name\": \"f_Type_1__String_$0_1$_\",\n" +
                        "      \"package\": \"withPath\",\n" +
                        "      \"parameters\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"var\",\n" +
                        "          \"class\": \"a::Type\",\n" +
                        "          \"multiplicity\": {\n" +
                        "            \"lowerBound\": 1,\n" +
                        "            \"upperBound\": 1\n" +
                        "          },\n" +
                        "          \"name\": \"s\"\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"postConstraints\": [],\n" +
                        "      \"preConstraints\": [],\n" +
                        "      \"returnMultiplicity\": {\n" +
                        "        \"lowerBound\": 0,\n" +
                        "        \"upperBound\": 1\n" +
                        "      },\n" +
                        "      \"returnType\": \"String\",\n" +
                        "      \"stereotypes\": [],\n" +
                        "      \"taggedValues\": []\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"_type\": \"sectionIndex\",\n" +
                        "      \"name\": \"SectionIndex\",\n" +
                        "      \"package\": \"__internal__\",\n" +
                        "      \"sections\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"importAware\",\n" +
                        "          \"elements\": [\n" +
                        "            \"withPath::f_Type_1__String_$0_1$_\"\n" +
                        "          ],\n" +
                        "          \"imports\": [],\n" +
                        "          \"parserName\": \"Pure\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"f_Type_1__String_$0_1$_\",\n" +
                        "    \"parameters\" : [ {\n" +
                        "      \"_type\" : \"var\",\n" +
                        "      \"name\" : \"s\",\n" +
                        "      \"genericType\" : {\n" +
                        "        \"rawType\" : {\n" +
                        "          \"_type\" : \"packageableType\",\n" +
                        "          \"fullPath\" : \"a::Type\"\n" +
                        "        }\n" +
                        "      },\n" +
                        "      \"multiplicity\" : {\n" +
                        "        \"lowerBound\" : 1,\n" +
                        "        \"upperBound\" : 1\n" +
                        "      }\n" +
                        "    } ],\n" +
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"String\"\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 0,\n" +
                        "      \"upperBound\" : 1\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"collection\",\n" +
                        "      \"multiplicity\" : {\n" +
                        "        \"lowerBound\" : 0,\n" +
                        "        \"upperBound\" : 0\n" +
                        "      }\n" +
                        "    } ],\n" +
                        "    \"package\" : \"withPath\"\n" +
                        "  }, {\n" +
                        "    \"_type\" : \"sectionIndex\",\n" +
                        "    \"name\" : \"SectionIndex\",\n" +
                        "    \"sections\" : [ {\n" +
                        "      \"_type\" : \"importAware\",\n" +
                        "      \"parserName\" : \"Pure\",\n" +
                        "      \"elements\" : [ \"withPath::f_Type_1__String_$0_1$_\" ]\n" +
                        "    } ],\n" +
                        "    \"package\" : \"__internal__\"\n" +
                        "  } ]\n" +
                        "}");
    }

    @Test
    public void testClass() throws Exception
    {
        check("{\n" +
                        "  \"_type\": \"data\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"_type\": \"class\",\n" +
                        "      \"constraints\": [],\n" +
                        "      \"name\": \"Class\",\n" +
                        "      \"originalMilestonedProperties\": [],\n" +
                        "      \"package\": \"my\",\n" +
                        "      \"properties\": [\n" +
                        "        {\n" +
                        "          \"multiplicity\": {\n" +
                        "            \"lowerBound\": 1,\n" +
                        "            \"upperBound\": 1\n" +
                        "          },\n" +
                        "          \"name\": \"prop1\",\n" +
                        "          \"stereotypes\": [],\n" +
                        "          \"taggedValues\": [],\n" +
                        "          \"type\": \"Result<String|*>\"\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"qualifiedProperties\": [\n" +
                        "        {\n" +
                        "          \"body\": [\n" +
                        "            {\n" +
                        "              \"_type\": \"collection\",\n" +
                        "              \"multiplicity\": {\n" +
                        "                \"lowerBound\": 0,\n" +
                        "                \"upperBound\": 0\n" +
                        "              },\n" +
                        "              \"values\": []\n" +
                        "            }\n" +
                        "          ],\n" +
                        "          \"name\": \"prop2\",\n" +
                        "          \"parameters\": [\n" +
                        "            {\n" +
                        "              \"_type\": \"var\",\n" +
                        "              \"genericType\": {\n" +
                        "                \"multiplicityArguments\": [],\n" +
                        "                \"rawType\": {\n" +
                        "                  \"_type\": \"packageableType\",\n" +
                        "                  \"fullPath\": \"String\"\n" +
                        "                },\n" +
                        "                \"typeArguments\": [],\n" +
                        "                \"typeVariableValues\": []\n" +
                        "              },\n" +
                        "              \"multiplicity\": {\n" +
                        "                \"lowerBound\": 1,\n" +
                        "                \"upperBound\": 1\n" +
                        "              },\n" +
                        "              \"name\": \"val\"\n" +
                        "            }\n" +
                        "          ],\n" +
                        "          \"returnMultiplicity\": {\n" +
                        "            \"lowerBound\": 0,\n" +
                        "            \"upperBound\": 1\n" +
                        "          },\n" +
                        "          \"returnType\": \"Result<Integer|0..1>\",\n" +
                        "          \"stereotypes\": [],\n" +
                        "          \"taggedValues\": []\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"stereotypes\": [],\n" +
                        "      \"superTypes\": [],\n" +
                        "      \"taggedValues\": []\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"_type\": \"sectionIndex\",\n" +
                        "      \"name\": \"SectionIndex\",\n" +
                        "      \"package\": \"__internal__\",\n" +
                        "      \"sections\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"importAware\",\n" +
                        "          \"elements\": [\n" +
                        "            \"my::Class\"\n" +
                        "          ],\n" +
                        "          \"imports\": [],\n" +
                        "          \"parserName\": \"Pure\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "{\n" +
                        "  \"_type\": \"data\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"_type\": \"class\",\n" +
                        "      \"name\": \"Class\",\n" +
                        "      \"properties\": [\n" +
                        "        {\n" +
                        "          \"name\": \"prop1\",\n" +
                        "          \"genericType\": {\n" +
                        "            \"rawType\": {\n" +
                        "              \"_type\": \"packageableType\",\n" +
                        "              \"fullPath\": \"Result<String|*>\"\n" +
                        "            }\n" +
                        "          },\n" +
                        "          \"multiplicity\": {\n" +
                        "            \"lowerBound\": 1,\n" +
                        "            \"upperBound\": 1\n" +
                        "          }\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"qualifiedProperties\": [\n" +
                        "        {\n" +
                        "          \"name\": \"prop2\",\n" +
                        "          \"parameters\": [\n" +
                        "            {\n" +
                        "              \"_type\": \"var\",\n" +
                        "              \"name\": \"val\",\n" +
                        "              \"genericType\": {\n" +
                        "                \"rawType\": {\n" +
                        "                  \"_type\": \"packageableType\",\n" +
                        "                  \"fullPath\": \"String\"\n" +
                        "                }\n" +
                        "              },\n" +
                        "              \"multiplicity\": {\n" +
                        "                \"lowerBound\": 1,\n" +
                        "                \"upperBound\": 1\n" +
                        "              }\n" +
                        "            }\n" +
                        "          ],\n" +
                        "          \"returnGenericType\": {\n" +
                        "            \"rawType\": {\n" +
                        "              \"_type\": \"packageableType\",\n" +
                        "              \"fullPath\": \"Result<Integer|0..1>\"\n" +
                        "            }\n" +
                        "          },\n" +
                        "          \"returnMultiplicity\": {\n" +
                        "            \"lowerBound\": 0,\n" +
                        "            \"upperBound\": 1\n" +
                        "          },\n" +
                        "          \"body\": [\n" +
                        "            {\n" +
                        "              \"_type\": \"collection\",\n" +
                        "              \"multiplicity\": {\n" +
                        "                \"lowerBound\": 0,\n" +
                        "                \"upperBound\": 0\n" +
                        "              }\n" +
                        "            }\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"package\": \"my\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"_type\": \"sectionIndex\",\n" +
                        "      \"name\": \"SectionIndex\",\n" +
                        "      \"sections\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"importAware\",\n" +
                        "          \"parserName\": \"Pure\",\n" +
                        "          \"elements\": [\n" +
                        "            \"my::Class\"\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"package\": \"__internal__\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}");
    }

    @Test
    public void testFunction() throws Exception
    {
        check("{\n" +
                        "  \"_type\": \"data\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"_type\": \"function\",\n" +
                        "      \"body\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"collection\",\n" +
                        "          \"multiplicity\": {\n" +
                        "            \"lowerBound\": 0,\n" +
                        "            \"upperBound\": 0\n" +
                        "          },\n" +
                        "          \"values\": []\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"name\": \"f_Result_1__Result<Integer|2>_$0_1$_\",\n" +
                        "      \"package\": \"\",\n" +
                        "      \"parameters\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"var\",\n" +
                        "          \"genericType\": {\n" +
                        "            \"multiplicityArguments\": [\n" +
                        "              {\n" +
                        "                \"lowerBound\": 1,\n" +
                        "                \"upperBound\": 1\n" +
                        "              }\n" +
                        "            ],\n" +
                        "            \"rawType\": {\n" +
                        "              \"_type\": \"packageableType\",\n" +
                        "              \"fullPath\": \"Result\"\n" +
                        "            },\n" +
                        "            \"typeArguments\": [\n" +
                        "              {\n" +
                        "                \"multiplicityArguments\": [],\n" +
                        "                \"rawType\": {\n" +
                        "                  \"_type\": \"packageableType\",\n" +
                        "                  \"fullPath\": \"String\"\n" +
                        "                },\n" +
                        "                \"typeArguments\": [],\n" +
                        "                \"typeVariableValues\": []\n" +
                        "              }\n" +
                        "            ],\n" +
                        "            \"typeVariableValues\": []\n" +
                        "          },\n" +
                        "          \"multiplicity\": {\n" +
                        "            \"lowerBound\": 1,\n" +
                        "            \"upperBound\": 1\n" +
                        "          },\n" +
                        "          \"name\": \"x\"\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"postConstraints\": [],\n" +
                        "      \"preConstraints\": [],\n" +
                        "      \"returnMultiplicity\": {\n" +
                        "        \"lowerBound\": 0,\n" +
                        "        \"upperBound\": 1\n" +
                        "      },\n" +
                        "      \"returnType\": \"Result<Integer|2>\",\n" +
                        "      \"stereotypes\": [],\n" +
                        "      \"taggedValues\": []\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"_type\": \"sectionIndex\",\n" +
                        "      \"name\": \"SectionIndex\",\n" +
                        "      \"package\": \"__internal__\",\n" +
                        "      \"sections\": [\n" +
                        "        {\n" +
                        "          \"_type\": \"importAware\",\n" +
                        "          \"elements\": [\n" +
                        "            \"f_Result_1__Result<Integer|2>_$0_1$_\"\n" +
                        "          ],\n" +
                        "          \"imports\": [],\n" +
                        "          \"parserName\": \"Pure\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "{\n" +
                        "  \"_type\" : \"data\",\n" +
                        "  \"elements\" : [ {\n" +
                        "    \"_type\" : \"function\",\n" +
                        "    \"name\" : \"f_Result_1__Result<Integer|2>_$0_1$_\",\n" +
                        "    \"parameters\" : [ {\n" +
                        "      \"_type\" : \"var\",\n" +
                        "      \"name\" : \"x\",\n" +
                        "      \"genericType\" : {\n" +
                        "        \"rawType\" : {\n" +
                        "          \"_type\" : \"packageableType\",\n" +
                        "          \"fullPath\" : \"Result\"\n" +
                        "        },\n" +
                        "        \"typeArguments\" : [ {\n" +
                        "          \"rawType\" : {\n" +
                        "            \"_type\" : \"packageableType\",\n" +
                        "            \"fullPath\" : \"String\"\n" +
                        "          }\n" +
                        "        } ],\n" +
                        "        \"multiplicityArguments\" : [ {\n" +
                        "          \"lowerBound\" : 1,\n" +
                        "          \"upperBound\" : 1\n" +
                        "        } ]\n" +
                        "      },\n" +
                        "      \"multiplicity\" : {\n" +
                        "        \"lowerBound\" : 1,\n" +
                        "        \"upperBound\" : 1\n" +
                        "      }\n" +
                        "    } ],\n" +
                        "    \"returnGenericType\" : {\n" +
                        "      \"rawType\" : {\n" +
                        "        \"_type\" : \"packageableType\",\n" +
                        "        \"fullPath\" : \"Result<Integer|2>\"\n" +
                        "      }\n" +
                        "    },\n" +
                        "    \"returnMultiplicity\" : {\n" +
                        "      \"lowerBound\" : 0,\n" +
                        "      \"upperBound\" : 1\n" +
                        "    },\n" +
                        "    \"body\" : [ {\n" +
                        "      \"_type\" : \"collection\",\n" +
                        "      \"multiplicity\" : {\n" +
                        "        \"lowerBound\" : 0,\n" +
                        "        \"upperBound\" : 0\n" +
                        "      }\n" +
                        "    } ]\n" +
                        "  }, {\n" +
                        "    \"_type\" : \"sectionIndex\",\n" +
                        "    \"name\" : \"SectionIndex\",\n" +
                        "    \"sections\" : [ {\n" +
                        "      \"_type\" : \"importAware\",\n" +
                        "      \"parserName\" : \"Pure\",\n" +
                        "      \"elements\" : [ \"f_Result_1__Result<Integer|2>_$0_1$_\" ]\n" +
                        "    } ],\n" +
                        "    \"package\" : \"__internal__\"\n" +
                        "  } ]\n" +
                        "}");
    }

    @Test
    public void testStoreTestData() throws Exception
    {
        check(
                new Scanner(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("simpleFunctionBefore.json"), "Can't find resource '" + "simpleFunctionBefore.json" + "'"), "UTF-8").useDelimiter("\\A").next(),
                new Scanner(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("simpleFunctionAfter.json"), "Can't find resource '" + "simpleFunctionAfter.json" + "'"), "UTF-8").useDelimiter("\\A").next()
        );
    }

    @Test
    public void testPackageableElementPointerCompatibility() throws Exception
    {
        String asString = "\"abc::myPath::MyName\"";
        String expected = "{\"path\":\"abc::myPath::MyName\"}";
        PackageableElementPointer pointerFromStringConstructor = objectMapper.readValue(asString, PackageableElementPointer.class);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(pointerFromStringConstructor);
        JsonAssert.assertJsonEquals(expected, json);
        PackageableElementPointer expectedPointerFromObjectConstructor = objectMapper.readValue(expected, PackageableElementPointer.class);
        Assert.assertEquals(expectedPointerFromObjectConstructor, pointerFromStringConstructor);
    }

    @Test
    public void testPackageableElementPointerToPathSerializerConverter() throws Exception
    {
        String expected = "{\"pointer\":\"abc::myPath::MyName\"}";
        SampleElementWithPackageableElementPointer sampleUsingPtr = objectMapper.readValue(expected, SampleElementWithPackageableElementPointer.class);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sampleUsingPtr);
        JsonAssert.assertJsonEquals(expected, json);
        Assert.assertEquals("abc::myPath::MyName", sampleUsingPtr.pointer.path);
    }

    private void check(String input, String output) throws Exception
    {
        PureModelContextData context = objectMapper.readValue(input, PureModelContextData.class);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
        JsonAssert.assertJsonEquals(output, json);
    }

    public static class SampleElementWithPackageableElementPointer
    {
        @JsonSerialize(converter = PackageableElementPointer.ToPathSerializerConverter.class)
        public PackageableElementPointer pointer;
    }
}
