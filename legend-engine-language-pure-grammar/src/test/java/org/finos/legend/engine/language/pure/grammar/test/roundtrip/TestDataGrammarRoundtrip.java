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
    public void testTextData()
    {
        test("###Data\n" +
                "Data <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'something'} meta::data::MyData\n" +
                "Text #{\n" +
                "  contentType: 'application/json';\n" +
                "  data: '{\"some\":\"data\"}';\n" +
                "}#\n"
        );
    }

    @Test
    public void testBinaryData()
    {
        test("###Data\n" +
                "Data <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'something'} meta::data::MyData\n" +
                "Binary #{\n" +
                "  contentType: 'application/x-protobuf';\n" +
                "  data: '1B4A 9DEA 230F FF20';\n" +
                "}#\n"
        );
    }

    @Test
    public void testPureCollectionData()
    {
        test("###Data\n" +
                "Data <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'something'} meta::data::MyData\n" +
                "PureCollection #{\n" +
                "  data: [\n" +
                "    ^my::Person(\n" +
                "      givenNames = ['Fred', 'William'],\n" +
                "      lastName = 'Bloggs',\n" +
                "      dateOfBirth = %2001-03-12,\n" +
                "      timeOfBirth = %12:23,\n" +
                "      timeOfDeath = %2020-09-11T12:56:24.487,\n" +
                "      isAlive = false,\n" +
                "      height = 1.76,\n" +
                "      girth = 0.98D,\n" +
                "      shoeSize = 10,\n" +
                "      score1 = -1,\n" +
                "      score2 = -1.3,\n" +
                "      score3 = -1.8D,\n" +
                "      gender = enums::Gender.MALE,\n" +
                "      address = ^my::Address(street = 'A Road')\n" +
                "    ),\n" +
                "    ^my::Person(\n" +
                "      givenNames = 'Jane',\n" +
                "      lastName = 'Doe',\n" +
                "      dateOfBirth = %1984-04-22,\n" +
                "      timeOfBirth = %19:41:21,\n" +
                "      isAlive = true,\n" +
                "      height = 1.61,\n" +
                "      girth = 0.81D,\n" +
                "      shoeSize = 4,\n" +
                "      score1 = -1,\n" +
                "      score2 = -1.3,\n" +
                "      score3 = -1.8D,\n" +
                "      gender = enums::Gender.FEMALE,\n" +
                "      address = ^my::Address(street = 'B Road')\n" +
                "    )\n" +
                "  ];\n" +
                "}#\n"
        );
    }

    @Test
    public void testReferenceData()
    {
        // NB This nonsense won't compile, but it's OK to test grammar round trip
        test("###Data\n" +
                "Data <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'something'} meta::data::MyData\n" +
                "Reference #{ meta::data::MyData }#\n"
        );
    }

}
