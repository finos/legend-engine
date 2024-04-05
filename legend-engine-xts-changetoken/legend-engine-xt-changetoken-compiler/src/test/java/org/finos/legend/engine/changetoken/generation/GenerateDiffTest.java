// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.changetoken.generation;

import org.junit.Test;

public class GenerateDiffTest extends GenerateDiffTestBase
{
    @Test
    public void testAddFieldWithDefaultValues()
    {
        expect(diff("###Pure\n" +
                                "Class model::pure::changetoken::tests::SampleClass\n" +
                                "{\n" +
                                "  name: String[1] = 'Example';\n" +
                                "  nameDefault: String[1];\n" +
                                "  nameOverwrite: String[1] = 'Ignored';\n" +
                                "  optionalDefault: String[0..1];\n" +
                                "  optionalOverwrite: String[0..1];\n" +
                                "  optionalOverwriteNull: String[0..1];\n" +
                                "  arrayDefault: String[*];\n" +
                                "  arrayOverwrite: String[*];\n" +
                                "  arrayOverwriteEmpty: String[*];\n" +
                                "  booleanTrue: Boolean[1] = true;\n" +
                                "  booleanFalse: Boolean[1] = false;\n" +
                                "  booleanDefault: Boolean[1];\n" +
                                "  booleanOverwrite: Boolean[1];\n" +
                                "  number: Number[1] = 1.1;\n" +
                                "  numberDefault: Number[1];\n" +
                                "  numberOverwrite: Number[1];\n" +
                                "  integer: Integer[1] = 3;\n" +
                                "  integerDefault: Integer[1];\n" +
                                "  integerOverwrite: Integer[1] = 8;\n" +
                                "  integers: Integer[2] = [1, 2];\n" +
                                "  integersDefault: Integer[2];\n" +
                                "  integersOverwrite: Integer[2] = [3, 4];\n" +
                                "  value: Float[1] = 4.2;\n" +
                                "  valueDefault: Float[1];\n" +
                                "  valueOverwrite: Float[1];\n" +
                                "  values: Float[3] = [1.0, 2.0, 3.0];\n" +
                                "  valuesDefault: Float[3];\n" +
                                "  valuesOverwrite: Float[3];\n" +
                                "  date: Date[1] = %2024-01-15T17:30:00;\n" +
                                "  strictDate: StrictDate[1] = %2024-01-15;\n" +
                                "  dateTime: DateTime[1] = %2024-01-15T19:30:00;\n" +
                                "  dateTimeDefault: DateTime[1];\n" +
                                "  dateTimeOverwrite: DateTime[1];\n" +
                                "  point: model::pure::changetoken::tests::distance::PointOfInterest[1] = ^model::pure::changetoken::tests::distance::PointOfInterest(name='London', location=^model::pure::changetoken::tests::distance::GeographicCoordinate(latitude = 51.507356, longitude = minus(0.127706)));\n" +
                                "  pointDefault: model::pure::changetoken::tests::distance::PointOfInterest[1];\n" +
                                "  pointOverwrite: model::pure::changetoken::tests::distance::PointOfInterest[0..1];\n" +
                                "  points: model::pure::changetoken::tests::distance::PointOfInterest[2] = [^model::pure::changetoken::tests::distance::PointOfInterest(name='Beijing', location=^model::pure::changetoken::tests::distance::GeographicCoordinate(latitude = 39.905489, longitude = 116.397771))," +
                                "    ^model::pure::changetoken::tests::distance::PointOfInterest(name='San Francisco', location=^model::pure::changetoken::tests::distance::GeographicCoordinate(latitude = 37.77493, longitude = -122.419416))];\n" +
                                "  pointsDefault: model::pure::changetoken::tests::distance::PointOfInterest[2];\n" +
                                "  pointsOverwrite: model::pure::changetoken::tests::distance::PointOfInterest[1..*];\n" +
                                "  side: model::pure::changetoken::tests::trade::Side[1] = model::pure::changetoken::tests::trade::Side.Buy;\n" +
                                "  sideDefault: model::pure::changetoken::tests::trade::Side[1];\n" +
                                "  sideOverwrite: model::pure::changetoken::tests::trade::Side[1];\n" +
                                "  sidesValid: model::pure::changetoken::tests::trade::Side[2] = [model::pure::changetoken::tests::trade::Side.Buy, model::pure::changetoken::tests::trade::Side.Sell];\n" +
                                "  sidesUnknown: model::pure::changetoken::tests::trade::Side[2];\n" +
                                "}\n",
                        "###Pure\n" +
                                "Class model::pure::changetoken::tests::distance::PointOfInterest\n" +
                                "{\n" +
                                "  name: String[1];\n" +
                                "  location: model::pure::changetoken::tests::distance::GeographicCoordinate[1];\n" +
                                "}\n" +
                                "\n" +
                                "Enum model::pure::changetoken::tests::trade::Side\n" +
                                "{\n" +
                                "  Unknown,\n" +
                                "  Buy,\n" +
                                "  Sell\n" +
                                "}\n" +
                                "\n" +
                                "Class {doc.doc = 'A class to represent GIS geographic coordinate'}\n" +
                                "model::pure::changetoken::tests::distance::GeographicCoordinate\n" +
                                "{\n" +
                                "  latitude: Float[1];\n" +
                                "  longitude: Float[1];\n" +
                                "}",
                        "",
                        "",
                        "{\n" +
                                "  \"@type\": \"meta::pure::changetoken::Versions\",\n" +
                                "  \"versions\": [\n" +
                                "    {\n" +
                                "      \"@type\": \"meta::pure::changetoken::Version\",\n" +
                                "      \"version\": \"da39a3ee5e6b4b0d3255bfef95601890afd80709\"\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}\n",
                        "{}",
                        "{}",
                        "{\n" +
                                "  \"model::pure::changetoken::tests::SampleClass\": {\n" +
                                "    \"arrayOverwrite\": [\n" +
                                "      \"one\"\n" +
                                "    ],\n" +
                                "    \"arrayOverwriteEmpty\": [],\n" +
                                "    \"optionalOverwrite\": \"two\",\n" +
                                "    \"optionalOverwriteNull\": null,\n" +
                                "    \"booleanOverwrite\": true,\n" +
                                "    \"dateTimeOverwrite\": \"2024-01-29T15:00:00\",\n" +
                                "    \"integerOverwrite\": -9,\n" +
                                "    \"integersOverwrite\": [\n" +
                                "      5,\n" +
                                "      6\n" +
                                "    ],\n" +
                                "    \"nameOverwrite\": \"Replaced\",\n" +
                                "    \"numberOverwrite\": 5.6,\n" +
                                "    \"pointOverwrite\": {\n" +
                                "      \"@type\": \"model::pure::changetoken::tests::distance::PointOfInterest\",\n" +
                                "      \"name\": \"Eiffel Tower\",\n" +
                                "      \"location\": {\n" +
                                "        \"@type\": \"model::pure::changetoken::tests::distance::GeographicCoordinate\",\n" +
                                "        \"latitude\": 48.8582,\n" +
                                "        \"longitude\": 2.294407\n" +
                                "      }\n" +
                                "    },\n" +
                                "    \"pointsOverwrite\": [\n" +
                                "      {\n" +
                                "        \"@type\": \"model::pure::changetoken::tests::distance::PointOfInterest\",\n" +
                                "        \"name\": \"Paris\",\n" +
                                "        \"location\": {\n" +
                                "          \"@type\": \"model::pure::changetoken::tests::distance::GeographicCoordinate\",\n" +
                                "          \"latitude\": 48.8582,\n" +
                                "          \"longitude\": 2.294407\n" +
                                "        }\n" +
                                "      }\n" +
                                "    ],\n" +
                                "    \"sideOverwrite\": \"model::pure::changetoken::tests::trade::Side.Buy\",\n" +
                                "    \"valueOverwrite\": 7.0,\n" +
                                "    \"valuesOverwrite\": [\n" +
                                "      1.2,\n" +
                                "      2.3,\n" +
                                "      4.5\n" +
                                "    ]\n" +
                                "  }\n" +
                                "}\n"),
                "{\n" +
                        "  \"@type\": \"meta::pure::changetoken::Versions\",\n" +
                        "  \"versions\": [\n" +
                        "    {\n" +
                        "      \"@type\": \"meta::pure::changetoken::Version\",\n" +
                        "      \"version\": \"da39a3ee5e6b4b0d3255bfef95601890afd80709\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"@type\": \"meta::pure::changetoken::Version\",\n" +
                        "      \"version\": \"e1eff07c27bcface06b1e217026a3d8a8a61a5ce\",\n" +
                        "      \"prevVersion\": \"da39a3ee5e6b4b0d3255bfef95601890afd80709\",\n" +
                        "      \"changeTokens\": [\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddedClass\",\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"arrayDefault\",\n" +
                        "          \"fieldType\": \"String[*]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": []\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"arrayOverwrite\",\n" +
                        "          \"fieldType\": \"String[*]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": [\n" +
                        "              \"one\"\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"arrayOverwriteEmpty\",\n" +
                        "          \"fieldType\": \"String[*]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": []\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"booleanDefault\",\n" +
                        "          \"fieldType\": \"Boolean[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": false\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"booleanFalse\",\n" +
                        "          \"fieldType\": \"Boolean[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": false\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"booleanOverwrite\",\n" +
                        "          \"fieldType\": \"Boolean[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": true\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"booleanTrue\",\n" +
                        "          \"fieldType\": \"Boolean[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": true\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"date\",\n" +
                        "          \"fieldType\": \"Date[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": \"2024-01-15T17:30:00+0000\"\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"dateTime\",\n" +
                        "          \"fieldType\": \"DateTime[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": \"2024-01-15T19:30:00+0000\"\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"dateTimeDefault\",\n" +
                        "          \"fieldType\": \"DateTime[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": \"1970-01-01T00:00:00.000+0000\"\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"dateTimeOverwrite\",\n" +
                        "          \"fieldType\": \"DateTime[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": \"2024-01-29T15:00:00\"\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"integer\",\n" +
                        "          \"fieldType\": \"Integer[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": 3\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"integerDefault\",\n" +
                        "          \"fieldType\": \"Integer[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": 0\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"integerOverwrite\",\n" +
                        "          \"fieldType\": \"Integer[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": -9\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"integers\",\n" +
                        "          \"fieldType\": \"Integer[2]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": [\n" +
                        "              1,\n" +
                        "              2\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"integersDefault\",\n" +
                        "          \"fieldType\": \"Integer[2]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": [\n" +
                        "              0,\n" +
                        "              0\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"integersOverwrite\",\n" +
                        "          \"fieldType\": \"Integer[2]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": [\n" +
                        "              5,\n" +
                        "              6\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"name\",\n" +
                        "          \"fieldType\": \"String[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": \"Example\"\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"nameDefault\",\n" +
                        "          \"fieldType\": \"String[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": \"\"\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"nameOverwrite\",\n" +
                        "          \"fieldType\": \"String[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": \"Replaced\"\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"number\",\n" +
                        "          \"fieldType\": \"Number[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": 1.1\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"numberDefault\",\n" +
                        "          \"fieldType\": \"Number[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": 0\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"numberOverwrite\",\n" +
                        "          \"fieldType\": \"Number[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": 5.6\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"optionalDefault\",\n" +
                        "          \"fieldType\": \"String[0..1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": null\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"optionalOverwrite\",\n" +
                        "          \"fieldType\": \"String[0..1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": \"two\"\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"optionalOverwriteNull\",\n" +
                        "          \"fieldType\": \"String[0..1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": null\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"point\",\n" +
                        "          \"fieldType\": \"model::pure::changetoken::tests::distance::PointOfInterest[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": {\n" +
                        "              \"@type\": \"model::pure::changetoken::tests::distance::PointOfInterest\",\n" +
                        "              \"name\": \"London\",\n" +
                        "              \"location\": {\n" +
                        "                \"@type\": \"model::pure::changetoken::tests::distance::GeographicCoordinate\",\n" +
                        "                \"latitude\": 51.507356,\n" +
                        "                \"longitude\": -0.127706\n" +
                        "              }\n" +
                        "            }\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"pointDefault\",\n" +
                        "          \"fieldType\": \"model::pure::changetoken::tests::distance::PointOfInterest[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": {\n" +
                        "              \"@type\": \"model::pure::changetoken::tests::distance::PointOfInterest\",\n" +
                        "              \"name\": \"\",\n" +
                        "              \"location\": {\n" +
                        "                \"@type\": \"model::pure::changetoken::tests::distance::GeographicCoordinate\",\n" +
                        "                \"latitude\": 0.0,\n" +
                        "                \"longitude\": 0.0\n" +
                        "              }\n" +
                        "            }\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"pointOverwrite\",\n" +
                        "          \"fieldType\": \"model::pure::changetoken::tests::distance::PointOfInterest[0..1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": {\n" +
                        "              \"@type\": \"model::pure::changetoken::tests::distance::PointOfInterest\",\n" +
                        "              \"name\": \"Eiffel Tower\",\n" +
                        "              \"location\": {\n" +
                        "                \"@type\": \"model::pure::changetoken::tests::distance::GeographicCoordinate\",\n" +
                        "                \"latitude\": 48.8582,\n" +
                        "                \"longitude\": 2.294407\n" +
                        "              }\n" +
                        "            }\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"points\",\n" +
                        "          \"fieldType\": \"model::pure::changetoken::tests::distance::PointOfInterest[2]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": [\n" +
                        "              {\n" +
                        "                \"@type\": \"model::pure::changetoken::tests::distance::PointOfInterest\",\n" +
                        "                \"name\": \"Beijing\",\n" +
                        "                \"location\": {\n" +
                        "                  \"@type\": \"model::pure::changetoken::tests::distance::GeographicCoordinate\",\n" +
                        "                  \"latitude\": 39.905489,\n" +
                        "                  \"longitude\": 116.397771\n" +
                        "                }\n" +
                        "              },\n" +
                        "              {\n" +
                        "                \"@type\": \"model::pure::changetoken::tests::distance::PointOfInterest\",\n" +
                        "                \"name\": \"San Francisco\",\n" +
                        "                \"location\": {\n" +
                        "                  \"@type\": \"model::pure::changetoken::tests::distance::GeographicCoordinate\",\n" +
                        "                  \"latitude\": 37.77493,\n" +
                        "                  \"longitude\": -122.419416\n" +
                        "                }\n" +
                        "              }\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"pointsDefault\",\n" +
                        "          \"fieldType\": \"model::pure::changetoken::tests::distance::PointOfInterest[2]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": [\n" +
                        "              {\n" +
                        "                \"@type\": \"model::pure::changetoken::tests::distance::PointOfInterest\",\n" +
                        "                \"name\": \"\",\n" +
                        "                \"location\": {\n" +
                        "                  \"@type\": \"model::pure::changetoken::tests::distance::GeographicCoordinate\",\n" +
                        "                  \"latitude\": 0.0,\n" +
                        "                  \"longitude\": 0.0\n" +
                        "                }\n" +
                        "              },\n" +
                        "              {\n" +
                        "                \"@type\": \"model::pure::changetoken::tests::distance::PointOfInterest\",\n" +
                        "                \"name\": \"\",\n" +
                        "                \"location\": {\n" +
                        "                  \"@type\": \"model::pure::changetoken::tests::distance::GeographicCoordinate\",\n" +
                        "                  \"latitude\": 0.0,\n" +
                        "                  \"longitude\": 0.0\n" +
                        "                }\n" +
                        "              }\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"pointsOverwrite\",\n" +
                        "          \"fieldType\": \"model::pure::changetoken::tests::distance::PointOfInterest[1..*]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": [\n" +
                        "              {\n" +
                        "                \"@type\": \"model::pure::changetoken::tests::distance::PointOfInterest\",\n" +
                        "                \"name\": \"Paris\",\n" +
                        "                \"location\": {\n" +
                        "                  \"@type\": \"model::pure::changetoken::tests::distance::GeographicCoordinate\",\n" +
                        "                  \"latitude\": 48.8582,\n" +
                        "                  \"longitude\": 2.294407\n" +
                        "                }\n" +
                        "              }\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"side\",\n" +
                        "          \"fieldType\": \"model::pure::changetoken::tests::trade::Side[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": \"model::pure::changetoken::tests::trade::Side.Buy\"\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"sideDefault\",\n" +
                        "          \"fieldType\": \"model::pure::changetoken::tests::trade::Side[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": \"model::pure::changetoken::tests::trade::Side.Unknown\"\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"sideOverwrite\",\n" +
                        "          \"fieldType\": \"model::pure::changetoken::tests::trade::Side[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": \"model::pure::changetoken::tests::trade::Side.Buy\"\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"sidesUnknown\",\n" +
                        "          \"fieldType\": \"model::pure::changetoken::tests::trade::Side[2]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": [\n" +
                        "              \"model::pure::changetoken::tests::trade::Side.Unknown\",\n" +
                        "              \"model::pure::changetoken::tests::trade::Side.Unknown\"\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"sidesValid\",\n" +
                        "          \"fieldType\": \"model::pure::changetoken::tests::trade::Side[2]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": [\n" +
                        "              \"model::pure::changetoken::tests::trade::Side.Buy\",\n" +
                        "              \"model::pure::changetoken::tests::trade::Side.Sell\"\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"strictDate\",\n" +
                        "          \"fieldType\": \"StrictDate[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": \"2024-01-15\"\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"value\",\n" +
                        "          \"fieldType\": \"Float[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": 4.2\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"valueDefault\",\n" +
                        "          \"fieldType\": \"Float[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": 0.0\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"valueOverwrite\",\n" +
                        "          \"fieldType\": \"Float[1]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": 7.0\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"values\",\n" +
                        "          \"fieldType\": \"Float[3]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": [\n" +
                        "              1.0,\n" +
                        "              2.0,\n" +
                        "              3.0\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"valuesDefault\",\n" +
                        "          \"fieldType\": \"Float[3]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": [\n" +
                        "              0.0,\n" +
                        "              0.0,\n" +
                        "              0.0\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"@type\": \"meta::pure::changetoken::AddField\",\n" +
                        "          \"fieldName\": \"valuesOverwrite\",\n" +
                        "          \"fieldType\": \"Float[3]\",\n" +
                        "          \"defaultValue\": {\n" +
                        "            \"@type\": \"meta::pure::changetoken::ConstValue\",\n" +
                        "            \"value\": [\n" +
                        "              1.2,\n" +
                        "              2.3,\n" +
                        "              4.5\n" +
                        "            ]\n" +
                        "          },\n" +
                        "          \"safeCast\": true,\n" +
                        "          \"class\": \"model::pure::changetoken::tests::SampleClass\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n");
    }
}
