// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.compiler.test.fromProtocol;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromProtocol;
import org.junit.Test;

public class TestDomainCompilationFromProtocol extends TestCompilationFromProtocol.TestCompilationFromProtocolTestSuite
{
    @Test
    public void testCreatePackageWithReservedName()
    {
        testWithProtocolPath("packageWithReservedName.json",
                "COMPILATION error: Error in 'testing::$implicit::Something': Can't create package with reserved name '$implicit'");
    }

    @Test
    public void testAppliedFunctionWithUnderscore()
    {
        testWithProtocolPath("functionWithUnderscores.json");
    }

    @Test
    public void testCreatePackageWithWrongStrictTimeDomain()
    {
        testWithProtocolPath("packageWithWrongStrictTimeDomain.json",
                "COMPILATION error: Error in 'ui::TestClassSibling': Can't find type 'Stricttime'");
    }

    @Test
    public void testCreatePackageWithCorrectStrictTimeDomain()
    {
        testWithProtocolPath("packageWithCorrectStrictTimeDomain.json");
    }

    @Test
    public void testCompilePathVariable()
    {
        testWithProtocolPath("queryWithPathVariable.json");
    }

    @Test
    public void testCompileLambdaVariable()
    {
        testWithProtocolPath("functionWithLambdaVariable.json");
    }

    @Test
    public void testFunctionWithDateTimeComplication()
    {
        testWithProtocolPath("functionWithDateTime.json");
    }

    @Test
    public void testFunctionWithDateTimeContainsPercentInProtocolComplication()
    {
        testWithProtocolPath("functionWithDateTimeContainingPercent.json");
    }

    @Test
    public void testFunctionLoadingWithPackageOffset()
    {
        testProtocolLoadingModelWithPackageOffset("functionExample.json", null, "update::");
    }

    @Test
    public void testProfileLoadingWithPackageOffset()
    {
        testProtocolLoadingModelWithPackageOffset("profileUsedInClassExample.json", null, "update::");
    }

    @Test
    public void testNewConstructorWithMissingTypeArgumentsCompiles()
    {
        testWithJson("{\n" +
                "  \"_type\": \"data\",\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"_type\": \"function\",\n" +
                "      \"body\": [\n" +
                "        {\n" +
                "          \"_type\": \"func\",\n" +
                "          \"function\": \"new\",\n" +
                "          \"parameters\": [\n" +
                "            {\n" +
                // ---------- type to construct, no type arguments
                "              \"_type\": \"packageableElementPtr\",\n" +
                "              \"fullPath\": \"BasicColumnSpecification\"\n" +
                // ---------- type to construct, no type arguments
                "            },\n" +
                "            {\n" +
                "              \"_type\": \"string\",\n" +
                "              \"value\": \"\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"_type\": \"collection\",\n" +
                "              \"multiplicity\": {\n" +
                "                \"lowerBound\": 1,\n" +
                "                \"upperBound\": 1\n" +
                "              },\n" +
                "              \"values\": [\n" +
                "                {\n" +
                "                  \"_type\": \"keyExpression\",\n" +
                "                  \"add\": false,\n" +
                "                  \"expression\": {\n" +
                "                    \"_type\": \"lambda\",\n" +
                "                    \"body\": [\n" +
                "                      {\n" +
                "                        \"_type\": \"integer\",\n" +
                "                        \"value\": 1\n" +
                "                      }\n" +
                "                    ],\n" +
                "                    \"parameters\": [\n" +
                "                      {\n" +
                "                        \"_type\": \"var\",\n" +
                "                        \"genericType\": {\n" +
                "                          \"multiplicityArguments\": [],\n" +
                "                          \"rawType\": {\n" +
                "                            \"_type\": \"packageableType\",\n" +
                "                            \"fullPath\": \"TDSRow\"\n" +
                "                          },\n" +
                "                          \"typeArguments\": [],\n" +
                "                          \"typeVariableValues\": []\n" +
                "                        },\n" +
                "                        \"multiplicity\": {\n" +
                "                          \"lowerBound\": 1,\n" +
                "                          \"upperBound\": 1\n" +
                "                        },\n" +
                "                        \"name\": \"r\"\n" +
                "                      }\n" +
                "                    ]\n" +
                "                  },\n" +
                "                  \"key\": {\n" +
                "                    \"_type\": \"string\",\n" +
                "                    \"value\": \"func\"\n" +
                "                  }\n" +
                "                }\n" +
                "              ]\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ],\n" +
                "      \"name\": \"new__Any_1_\",\n" +
                "      \"package\": \"test\",\n" +
                "      \"parameters\": [],\n" +
                "      \"postConstraints\": [],\n" +
                "      \"preConstraints\": [],\n" +
                "      \"returnGenericType\": {\n" +
                "        \"multiplicityArguments\": [],\n" +
                "        \"rawType\": {\n" +
                "          \"_type\": \"packageableType\",\n" +
                "          \"fullPath\": \"Any\"\n" +
                "        },\n" +
                "        \"typeArguments\": [],\n" +
                "        \"typeVariableValues\": []\n" +
                "      },\n" +
                "      \"returnMultiplicity\": {\n" +
                "        \"lowerBound\": 1,\n" +
                "        \"upperBound\": 1\n" +
                "      },\n" +
                "      \"stereotypes\": [],\n" +
                "      \"taggedValues\": [],\n" +
                "      \"tests\": []\n" +
                "    },\n" +
                "    {\n" +
                "      \"_type\": \"sectionIndex\",\n" +
                "      \"name\": \"SectionIndex\",\n" +
                "      \"package\": \"__internal__\",\n" +
                "      \"sections\": [\n" +
                "        {\n" +
                "          \"_type\": \"importAware\",\n" +
                "          \"elements\": [\n" +
                "            \"test::new__Any_1_\"\n" +
                "          ],\n" +
                "          \"imports\": [],\n" +
                "          \"parserName\": \"Pure\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}", null);
    }
}
