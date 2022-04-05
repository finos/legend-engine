// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.test.roundtrip;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestDataGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testDataElementWithStereoTypeAndTagValue()
    {
        test("###Data\n" +
                "Data <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'something'} meta::data::MyData\n" +
                "{\n" +
                "  ExternalFormat\n" +
                "  #{\n" +
                "    contentType: 'application/json';\n" +
                "    data: '{\"some\":\"data\"}';\n" +
                "  }#\n" +
                "}\n"
        );
    }

    @Test
    public void testExternalFormatDataWithFlatData()
    {
        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ExternalFormat\n" +
                "  #{\n" +
                "    contentType: 'application/x.flatdata';\n" +
                "    data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "  }#\n" +
                "}\n"
        );
    }

    @Test
    public void testExternalFormatDataWithJson()
    {
        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ExternalFormat\n" +
                "  #{\n" +
                "    contentType: 'application/json';\n" +
                "    data: '{\"some\":\"data\"}';\n" +
                "  }#\n" +
                "}\n"
        );
    }

    @Test
    public void testExternalFormatDataWithNestedJson()
    {
        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ExternalFormat\n" +
                "  #{\n" +
                "    contentType: 'application/json';\n" +
                "    data: '{\\n" +
                "            \"_type\": \"data\",\\n" +
                "            \"version\": \"v1_0_0\",\\n" +
                "            \"elements\": [\\n" +
                "              {\\n" +
                "                \"_type\": \"dataSpace\",\\n" +
                "                \"name\": \"TestDataSpace\",\\n" +
                "                \"package\": \"test::model\",\\n" +
                "                \"groupId\": \"test.group\",\\n" +
                "                \"artifactId\": \"test-data-space\",\\n" +
                "                \"versionId\": \"0.4.3\",\\n" +
                "                \"executionContexts\": [\\n" +
                "                  {\\n" +
                "                    \"name\": \"INT\",\\n" +
                "                    \"description\": \"some description 1\",\\n" +
                "                    \"mapping\": {\\n" +
                "                      \"type\": \"MAPPING\",\\n" +
                "                      \"path\": \"test::model::TestMapping\"\\n" +
                "                    },\\n" +
                "                    \"defaultRuntime\": {\\n" +
                "                      \"type\": \"RUNTIME\",\\n" +
                "                      \"path\": \"test::model::TestRuntime\"\\n" +
                "                    }\\n" +
                "                  }\\n" +
                "                ],\\n" +
                "                \"defaultExecutionContext\": \"INT\",\\n" +
                "                \"description\": \"some description 2\",\\n" +
                "                \"featuredDiagrams\": [\\n" +
                "                  {\\n" +
                "                    \"type\": \"DIAGRAM\",\\n" +
                "                    \"path\": \"test::model::TestDiagram1\"\\n" +
                "                  },\\n" +
                "                  {\\n" +
                "                    \"type\": \"DIAGRAM\",\\n" +
                "                    \"path\": \"test::model::TestDiagram2\"\\n" +
                "                  }\\n" +
                "                ],\\n" +
                "                \"supportInfo\": {\\n" +
                "                  \"_type\": \"email\",\\n" +
                "                  \"address\": \"testEmail@test.org\"\\n" +
                "                }\\n" +
                "              }\\n" +
                "            ]\\n" +
                "          }\\n';\n" +
                "  }#\n" +
                "}\n"
        );
    }

    @Test
    public void testExternalFormatDataWithProtobuf()
    {
        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ExternalFormat\n" +
                "  #{\n" +
                "    contentType: 'application/x-protobuf';\n" +
                "    data: '1B4A 9DEA 230F FF20';\n" +
                "  }#\n" +
                "}\n");
    }

    @Test
    public void testExternalFormatDataWithXml()
    {
        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ExternalFormat\n" +
                "  #{\n" +
                "    contentType: 'application/xml';\n" +
                "    data: ' <?xml version=\"1.0\" encoding=\"utf-8\"?>\\n" +
                "            <firm>\\n" +
                "                <name>Acme Co.</name>\\n" +
                "                <ranking>2</ranking>\\n" +
                "                <addresses>\\n" +
                "                    <addressUse addressType=\"Headquarters\">\\n" +
                "                        <address>\\n" +
                "                            <firstLine>1207 Pineview Drive</firstLine>\\n" +
                "                            <city>Marshall</city>\\n" +
                "                            <region>Minnesota</region>\\n" +
                "                            <country>USA</country>\\n" +
                "                            <position latitude=\"44.44813578527556\" longitude=\"-95.82086758913208\" />\\n" +
                "                        </address>\\n" +
                "                    </addressUse>\\n" +
                "                    <addressUse addressType=\"Regional Office\">\\n" +
                "                        <address>\\n" +
                "                            <firstLine>Suite 15</firstLine>\\n" +
                "                            <secondLine>315 Stuart Street</secondLine>\\n" +
                "                            <city>Pittsburgh</city>\\n" +
                "                            <region>Pennsylvania</region>\\n" +
                "                            <country>USA</country>\\n" +
                "                            <position latitude=\"40.441913576497356\" longitude=\"Eighty Degrees West\" />\\n" +
                "                        </address>\\n" +
                "                    </addressUse>\\n" +
                "                </addresses>\\n" +
                "                <employees>\\n" +
                "                    <employee>\\n" +
                "                        <firstName>Jason</firstName>\\n" +
                "                        <lastName>Schlichting</lastName>\\n" +
                "                        <dateOfBirth>1968-03-05</dateOfBirth>\\n" +
                "                        <isAlive>true</isAlive>\\n" +
                "                        <heightInMeters>1.82</heightInMeters>\\n" +
                "                        <addresses>\\n" +
                "                            <addressUse addressType=\"Home\">\\n" +
                "                                <address>\\n" +
                "                                    <firstLine>1882 Cameron Road</firstLine>\\n" +
                "                                    <city>Randolph</city>\\n" +
                "                                    <region>New York</region>\\n" +
                "                                    <country>USA</country>\\n" +
                "                                    <position latitude=\"42.1629061280378\" longitude=\"-78.99153587769312\" />\\n" +
                "                                </address>\\n" +
                "                            </addressUse>\\n" +
                "                            <addressUse addressType=\"Holiday\">\\n" +
                "                                <address>\\n" +
                "                                    <firstLine>Appartment 31</firstLine>\\n" +
                "                                    <secondLine>3788 Levy Court</secondLine>\\n" +
                "                                    <city>Worcester</city>\\n" +
                "                                    <region>Massachusetts</region>\\n" +
                "                                    <country>USA</country>\\n" +
                "                                </address>\\n" +
                "                            </addressUse>\\n" +
                "                        </addresses>\\n" +
                "                    </employee>\\n" +
                "                    <employee>\\n" +
                "                        <firstName>Nancy</firstName>\\n" +
                "                        <lastName>Fraher</lastName>\\n" +
                "                        <dateOfBirth>1970-12-13</dateOfBirth>\\n" +
                "                        <isAlive>false</isAlive>\\n" +
                "                        <heightInMeters>1.71</heightInMeters>\\n" +
                "                        <addresses>\\n" +
                "                            <addressUse addressType=\"Home\">\\n" +
                "                                <address>\\n" +
                "                                    <firstLine>3220 Northwest Boulevard</firstLine>\\n" +
                "                                    <city>Rochelle Park</city>\\n" +
                "                                    <region>New Jersey</region>\\n" +
                "                                    <country>USA</country>\\n" +
                "                                </address>\\n" +
                "                            </addressUse>\\n" +
                "                        </addresses>\\n" +
                "                    </employee>\\n" +
                "                </employees>\\n" +
                "            </firm>';\n" +
                "  }#\n" +
                "}\n"
        );
    }

    @Test
    public void testModelStoreData()
    {
        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ModelStore\n" +
                "  #{\n" +
                "    my::Person:\n" +
                "      [\n" +
                "        ^my::Person(\n" +
                "          givenNames = ['Fred', 'William'],\n" +
                "          lastName = 'Bloggs',\n" +
                "          dateOfBirth = %2001-03-12,\n" +
                "          timeOfBirth = %12:23,\n" +
                "          timeOfDeath = %2020-09-11T12:56:24.487,\n" +
                "          isAlive = false,\n" +
                "          height = 1.76,\n" +
                "          girth = 0.98D,\n" +
                "          shoeSize = 10,\n" +
                "          score1 = -1,\n" +
                "          score2 = -1.3,\n" +
                "          score3 = -1.8D,\n" +
                "          gender = enums::Gender.MALE,\n" +
                "          address = ^my::Address(street = 'A Road')\n" +
                "        ),\n" +
                "        ^my::Person(\n" +
                "          givenNames = 'Jane',\n" +
                "          lastName = 'Doe',\n" +
                "          dateOfBirth = %1984-04-22,\n" +
                "          timeOfBirth = %19:41:21,\n" +
                "          isAlive = true,\n" +
                "          height = 1.61,\n" +
                "          girth = 0.81D,\n" +
                "          shoeSize = 4,\n" +
                "          score1 = -1,\n" +
                "          score2 = -1.3,\n" +
                "          score3 = -1.8D,\n" +
                "          gender = enums::Gender.FEMALE,\n" +
                "          address = ^my::Address(street = 'B Road')\n" +
                "        )\n" +
                "      ]\n" +
                "  }#\n" +
                "}\n"
        );

        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ModelStore\n" +
                "  #{\n" +
                "    my::Firm:\n" +
                "      [\n" +
                "        ^my::Firm(\n" +
                "          givenNames = 'Jane',\n" +
                "          lastName = 'Doe',\n" +
                "          dateOfBirth = %1984-04-22,\n" +
                "          timeOfBirth = %19:41:21,\n" +
                "          isAlive = true,\n" +
                "          height = 1.61,\n" +
                "          girth = 0.81D,\n" +
                "          shoeSize = 4,\n" +
                "          score1 = -1,\n" +
                "          score2 = -1.3,\n" +
                "          score3 = -1.8D,\n" +
                "          gender = enums::Gender.FEMALE,\n" +
                "          address = ^my::Address(street = 'B Road')\n" +
                "        )\n" +
                "      ],\n" +
                "    my::Person:\n" +
                "      [\n" +
                "        ^my::Person(\n" +
                "          givenNames = ['Fred', 'William'],\n" +
                "          lastName = 'Bloggs',\n" +
                "          dateOfBirth = %2001-03-12,\n" +
                "          timeOfBirth = %12:23,\n" +
                "          timeOfDeath = %2020-09-11T12:56:24.487,\n" +
                "          isAlive = false,\n" +
                "          height = 1.76,\n" +
                "          girth = 0.98D,\n" +
                "          shoeSize = 10,\n" +
                "          score1 = -1,\n" +
                "          score2 = -1.3,\n" +
                "          score3 = -1.8D,\n" +
                "          gender = enums::Gender.MALE,\n" +
                "          address = ^my::Address(street = 'A Road')\n" +
                "        ),\n" +
                "        ^my::Person(\n" +
                "          givenNames = 'Jane',\n" +
                "          lastName = 'Doe',\n" +
                "          dateOfBirth = %1984-04-22,\n" +
                "          timeOfBirth = %19:41:21,\n" +
                "          isAlive = true,\n" +
                "          height = 1.61,\n" +
                "          girth = 0.81D,\n" +
                "          shoeSize = 4,\n" +
                "          score1 = -1,\n" +
                "          score2 = -1.3,\n" +
                "          score3 = -1.8D,\n" +
                "          gender = enums::Gender.FEMALE,\n" +
                "          address = ^my::Address(street = 'B Road')\n" +
                "        )\n" +
                "      ]\n" +
                "  }#\n" +
                "}\n"
        );
    }

    @Test
    public void testReferenceData()
    {
        // NB This won't compile, but it's OK to test grammar round trip
        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  Reference\n" +
                "  #{\n" +
                "    meta::data::MyData\n" +
                "  }#\n" +
                "}\n"
        );
    }

}
