//  Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.test.to;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestDomainGrammarTo extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testFunctionWithEnumValueGrammarTo()
    {
        testTo("simpleFunctionWithEnumValue.json",
                "function showcase::testEnum(): meta::pure::metamodel::type::Any[*]\n" +
                            "{\n" +
                            "  |showcase::DataDefect.all()->filter(x|$x.status == showcase::Status.'Closed by DGO')\n" +
                            "}\n");
    }

    @Test
    public void testBackwardCompatibilityWithStoreTestData()
    {
        testTo("simpleFunctionWithStoreTestDataCompatibility.json",
                "function model::PersonQuery2(): meta::pure::tds::TabularDataSet[1]\n" +
                        "{\n" +
                        "  model::Person.all()->project([x: model::Person[1]|$x.firstName, x: model::Person[1]|$x.lastName], ['First Name', 'Last Name'])->from(execution::RelationalMapping, execution::Runtime)\n" +
                        "}\n" +
                        "{\n" +
                        "  testSuite_1\n" +
                        "  (\n" +
                        "    ModelStore:\n" +
                        "        ModelStore\n" +
                        "        #{\n" +
                        "          model::Firm:\n" +
                        "            ExternalFormat\n" +
                        "            #{\n" +
                        "              contentType: 'application/json';\n" +
                        "              data: '{\\n  \"employees\": [\\n    {\\n      \"firstName\": \"firstEmployeeName\",\\n      \"lastName\": \"secondEmployeeName\"\\n    }\\n  ],\\n  \"legalName\": \"Apple Inc\"\\n}';\n" +
                        "            }#\n" +
                        "        }#;\n" +
                        "    testPass | PersonQuery2() => (JSON) '[{\\n  \"First Name\" : \"Nicole\",\"Last Name\" : \"Smith\"} ]';\n" +
                        "  )\n" +
                        "}\n");
    }
}
