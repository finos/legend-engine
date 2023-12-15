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

package org.finos.legend.engine.language.pure.compiler.test.fromGrammar;

import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;

public class TestFunctionCompilationFromGrammar
{

  @Test
  public void testFunctionTest()
  {

      test("function model::MyFunc(): String[1]\n" +
              "{\n" +
              "  ''\n" +
              "}\n" +
              "[\n" +
              "  testSuite_1:\n" +
              "  {\n" +
              "    tests:\n" +
              "    [\n" +
              "      testFail:\n" +
              "      {\n" +
              "        asserts:\n" +
              "        [\n" +
              "          assertion_1:\n" +
              "            EqualToJson\n" +
              "            #{\n" +
              "              expected:\n" +
              "                ExternalFormat\n" +
              "                #{\n" +
              "                  contentType: 'application/json';\n" +
              "                  data: '[]';\n" +
              "                }#;\n" +
              "            }#\n" +
              "        ]\n" +
              "      },\n" +
              "      testPass:\n" +
              "      {\n" +
              "        asserts:\n" +
              "        [\n" +
              "          assertion_1:\n" +
              "            EqualToJson\n" +
              "            #{\n" +
              "              expected:\n" +
              "                ExternalFormat\n" +
              "                #{\n" +
              "                  contentType: 'application/json';\n" +
              "                  data: '[]';\n" +
              "                }#;\n" +
              "            }#\n" +
              "        ]\n" +
              "      }\n" +
              "    ]\n" +
              "  }\n" +
              "]\n");

      test("function model::MyFunc(): String[1]\n" +
              "{\n" +
              "  ''\n" +
              "}\n" +
              "[\n" +
              "  testSuite_1:\n" +
              "  {\n" +
              "    tests:\n" +
              "    [\n" +
              "      testDuplicate:\n" +
              "      {\n" +
              "        asserts:\n" +
              "        [\n" +
              "          assertion_1:\n" +
              "            EqualToJson\n" +
              "            #{\n" +
              "              expected:\n" +
              "                ExternalFormat\n" +
              "                #{\n" +
              "                  contentType: 'application/json';\n" +
              "                  data: '[]';\n" +
              "                }#;\n" +
              "            }#\n" +
              "        ]\n" +
              "      },\n" +
              "      testDuplicate:\n" +
              "      {\n" +
              "        asserts:\n" +
              "        [\n" +
              "          assertion_1:\n" +
              "            EqualToJson\n" +
              "            #{\n" +
              "              expected:\n" +
              "                ExternalFormat\n" +
              "                #{\n" +
              "                  contentType: 'application/json';\n" +
              "                  data: '[]';\n" +
              "                }#;\n" +
              "            }#\n" +
              "        ]\n" +
              "      }\n" +
              "    ]\n" +
              "  }\n" +
              "]\n", "COMPILATION error at [6:3-43:3]: Multiple tests found with ids : 'testDuplicate'");

      test("function model::MyFunc(): String[1]\n" +
              "{\n" +
              "  ''\n" +
              "}\n" +
              "[\n" +
              "  duplicateSuite:\n" +
              "  {\n" +
              "    tests:\n" +
              "    [\n" +
              "      testDuplicate:\n" +
              "      {\n" +
              "        asserts:\n" +
              "        [\n" +
              "          assertion_1:\n" +
              "            EqualToJson\n" +
              "            #{\n" +
              "              expected:\n" +
              "                ExternalFormat\n" +
              "                #{\n" +
              "                  contentType: 'application/json';\n" +
              "                  data: '[]';\n" +
              "                }#;\n" +
              "            }#\n" +
              "        ]\n" +
              "      }\n" +
              "    ]\n" +
              "  },\n" +
              "  duplicateSuite:\n" +
              "  {\n" +
              "    tests:\n" +
              "    [\n" +
              "      testDuplicate:\n" +
              "      {\n" +
              "        asserts:\n" +
              "        [\n" +
              "          assertion_1:\n" +
              "            EqualToJson\n" +
              "            #{\n" +
              "              expected:\n" +
              "                ExternalFormat\n" +
              "                #{\n" +
              "                  contentType: 'application/json';\n" +
              "                  data: '[]';\n" +
              "                }#;\n" +
              "            }#\n" +
              "        ]\n" +
              "      }\n" +
              "    ]\n" +
              "  }\n" +
              "]\n", "COMPILATION error at [1:1-50:1]: Multiple testSuites found with ids : 'duplicateSuite'");


      test("function model::MyFunc(firstName: String[1]): String[1]\n" +
              "{\n" +
              "  ''\n" +
              "}\n" +
              "[\n" +
              "  duplicateSuite:\n" +
              "  {\n" +
              "    tests:\n" +
              "    [\n" +
              "      testDuplicate:\n" +
              "      {\n" +
              "        parameters:\n" +
              "        [\n" +
              "          firstName = 'Nicole'\n" +
              "        ]\n" +
              "        asserts:\n" +
              "        [\n" +
              "          assertion_1:\n" +
              "            EqualToJson\n" +
              "            #{\n" +
              "              expected:\n" +
              "                ExternalFormat\n" +
              "                #{\n" +
              "                  contentType: 'application/json';\n" +
              "                  data: '[]';\n" +
              "                }#;\n" +
              "            }#\n" +
              "        ]\n" +
              "      }\n" +
              "    ]\n" +
              "  }\n" +
              "]\n");

      test("function model::MyFunc(firstName: String[1]): String[1]\n" +
              "{\n" +
              "  ''\n" +
              "}\n" +
              "[\n" +
              "  duplicateSuite:\n" +
              "  {\n" +
              "    tests:\n" +
              "    [\n" +
              "      testDuplicate:\n" +
              "      {\n" +
              "        asserts:\n" +
              "        [\n" +
              "          assertion_1:\n" +
              "            EqualToJson\n" +
              "            #{\n" +
              "              expected:\n" +
              "                ExternalFormat\n" +
              "                #{\n" +
              "                  contentType: 'application/json';\n" +
              "                  data: '[]';\n" +
              "                }#;\n" +
              "            }#\n" +
              "        ]\n" +
              "      }\n" +
              "    ]\n" +
              "  }\n" +
              "]\n", "COMPILATION error at [10:7-25:7]: Parameter value required for parameter: 'firstName'");

      test("function model::MyFunc(firstName: String[1]): String[1]\n" +
              "{\n" +
              "  ''\n" +
              "}\n" +
              "[\n" +
              "  duplicateSuite:\n" +
              "  {\n" +
              "    tests:\n" +
              "    [\n" +
              "      testDuplicate:\n" +
              "      {\n" +
              "        asserts:\n" +
              "        [\n" +
              "          assertion_1:\n" +
              "            EqualToJson\n" +
              "            #{\n" +
              "              expected:\n" +
              "                ExternalFormat\n" +
              "                #{\n" +
              "                  contentType: 'application/json';\n" +
              "                  data: '[]';\n" +
              "                }#;\n" +
              "            }#\n" +
              "        ]\n" +
              "      }\n" +
              "    ]\n" +
              "  }\n" +
              "]\n", "COMPILATION error at [10:7-25:7]: Parameter value required for parameter: 'firstName'");

      test("function model::MyFunc(): String[1]\n" +
              "{\n" +
              "  ''\n" +
              "}\n" +
              "[\n" +
              "  duplicateSuite:\n" +
              "  {\n" +
              "    tests:\n" +
              "    [\n" +
              "      testDuplicate:\n" +
              "      {\n" +
              "         parameters: [" +
              "              notFound =  'xx'   " +
              "         ]" +
              "        asserts:\n" +
              "        [\n" +
              "          assertion_1:\n" +
              "            EqualToJson\n" +
              "            #{\n" +
              "              expected:\n" +
              "                ExternalFormat\n" +
              "                #{\n" +
              "                  contentType: 'application/json';\n" +
              "                  data: '[]';\n" +
              "                }#;\n" +
              "            }#\n" +
              "        ]\n" +
              "      }\n" +
              "    ]\n" +
              "  }\n" +
              "]\n", "COMPILATION error at [10:7-25:7]: Parameter values not found in function parameter: notFound");

      test("function model::MyFunc(): String[1]\n" +
              "{\n" +
              "  ''\n" +
              "}\n" +
              "[\n" +
              "  duplicateSuite:\n" +
              "  {\n" +
              "    tests:\n" +
              "    [\n" +
              "      testDuplicate:\n" +
              "      {\n" +
              "        asserts:\n" +
              "        [\n" +
              "        ]\n" +
              "      }\n" +
              "    ]\n" +
              "  }\n" +
              "]\n", "COMPILATION error at [10:7-15:7]: Tests should have at least 1 assert");


      test("function model::Hello(name: String[1]): String[1]\n" +
              "{\n" +
              "  'Hello!. My name is ' + $name + '.';\n" +
              "}\n" +
              "[\n" +
              "  testSuite_1:\n" +
              "  {\n" +
              "    tests:\n" +
              "    [\n" +
              "      testFail:\n" +
              "      {\n" +
              "        parameters: \n" +
              "        [\n" +
              "          name = 'John'\n" +
              "        ]\n" +
              "        asserts:\n" +
              "        [\n" +
              "          assertion_1:\n" +
              "          EqualTo\n" +
              "          #{\n" +
              "             expected: 'Hello!. My name is John.';\n" +
              "          }#\n" +
              "        ]\n" +
              "      }\n" +
              "    ]\n" +
              "  }\n" +
              "]\n");
  }

}
