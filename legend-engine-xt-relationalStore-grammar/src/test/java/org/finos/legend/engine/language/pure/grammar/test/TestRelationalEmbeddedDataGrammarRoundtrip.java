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

package org.finos.legend.engine.language.pure.grammar.test;

import org.junit.Test;

public class TestRelationalEmbeddedDataGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{

    @Test
    public void testRelationalEmbeddedData()
    {
        test("###Data\n" +
                "Data my::RelationalData\n" +
                "{\n" +
                "  Relational\n" +
                "  #{\n" +
                "    mySchema.MyTable:\n" +
                "      'id,firstName,lastName,age\\n'+\n" +
                "      '1,John,Doe\\n'+\n" +
                "      '2,Nicole,Smith\\n'+\n" +
                "      '3,Nick,Smith\\n';\n" +
                "\n" +
                "    mySchema.MyTable2:\n" +
                "      'id,name\\n'+\n" +
                "      '1,John\\n'+\n" +
                "      '2,Jack\\n';\n" +
                "  }#\n" +
                "}\n"
        );

        test("###Service\n" +
                "Service service::SimpleRelationalPassWithSpecialEmbeddedData\n" +
                "{\n" +
                "  pattern: '/d2c48a9c-70fa-46e3-8173-c355e774004f';\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |model::Firm.all()->project([x|$x.employees.firstName, x|$x.employees.lastName, x|$x.legalName], ['Employees/First Name', 'Employees/Last Name', 'Legal Name']);\n" +
                "    mapping: execution::RelationalMapping;\n" +
                "    runtime: execution::Runtime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection_1:\n" +
                "            Relational\n" +
                "            #{\n" +
                "              default.PersonTable:\n" +
                "                'id,firm_id,firstName,lastName\\n'+\n" +
                "                '1,1,John;\\'\",Doe\\n'+\n" +
                "                '2,1,Nicole,Smith\\n'+\n" +
                "                '3,2,Time,Smith\\n';\n" +
                "\n" +
                "              default.FirmTable:\n" +
                "                'id,legal_name\\n'+\n" +
                "                '1,Finos\\n'+\n" +
                "                '2,Apple\\n';\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          serializationFormat: PURE_TDSOBJECT;\n" +
                "          asserts:\n" +
                "          [\n" +
                "            shouldPass:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '[{\"Employees/First Name\":\"John;\\'\\\\\"\",\"Employees/Last Name\":\"Doe\",\"Legal Name\":\"Finos\"},{\"Employees/First Name\":\"Nicole\",\"Employees/Last Name\":\"Smith\",\"Legal Name\":\"Finos\"},{\"Employees/First Name\":\"Time\",\"Employees/Last Name\":\"Smith\",\"Legal Name\":\"Apple\"}]\\n';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n");
    }
}
