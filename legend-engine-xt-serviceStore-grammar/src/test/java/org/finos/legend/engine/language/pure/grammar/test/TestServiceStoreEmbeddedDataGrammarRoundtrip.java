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

package org.finos.legend.engine.language.pure.grammar.test;

import org.junit.Test;

public class TestServiceStoreEmbeddedDataGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testServiceStoreEmbeddedDataSimple()
    {
        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ServiceStore\n" +
                "  #{\n" +
                "    [\n" +
                "      {\n" +
                "        request:\n" +
                "        {\n" +
                "          method: GET;\n" +
                "          url: '/employees';\n" +
                "        };\n" +
                "        response:\n" +
                "        {\n" +
                "          body:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName A\",\\n" +
                "                           \"lastName\": \"LastName A\",\\n" +
                "                           \"firmId\": \"A\"\\n" +
                "                       },\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName B\",\\n" +
                "                           \"lastName\": \"LastName B\",\\n" +
                "                           \"firmId\": \"B\"\\n" +
                "                       }\\n" +
                "                     ]\\n';\n" +
                "            }#;\n" +
                "        };\n" +
                "      }\n" +
                "    ]\n" +
                "  }#\n" +
                "}\n"
        );
    }

    @Test
    public void testServiceStoreEmbeddedDataWithQueryParams()
    {
        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ServiceStore\n" +
                "  #{\n" +
                "    [\n" +
                "      {\n" +
                "        request:\n" +
                "        {\n" +
                "          method: GET;\n" +
                "          url: '/employees';\n" +
                "          queryParameters:\n" +
                "          {\n" +
                "            name:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected: 'FirstName A';\n" +
                "              }#\n" +
                "          };\n" +
                "        };\n" +
                "        response:\n" +
                "        {\n" +
                "          body:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName A\",\\n" +
                "                           \"lastName\": \"LastName A\",\\n" +
                "                           \"firmId\": \"A\"\\n" +
                "                       }\\n" +
                "                     ]\\n';\n" +
                "            }#;\n" +
                "        };\n" +
                "      }\n" +
                "    ]\n" +
                "  }#\n" +
                "}\n"
        );

        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ServiceStore\n" +
                "  #{\n" +
                "    [\n" +
                "      {\n" +
                "        request:\n" +
                "        {\n" +
                "          method: GET;\n" +
                "          url: '/employees';\n" +
                "          queryParameters:\n" +
                "          {\n" +
                "            id:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected: '123';\n" +
                "              }#,\n" +
                "            name:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected: 'FirstName A';\n" +
                "              }#\n" +
                "          };\n" +
                "        };\n" +
                "        response:\n" +
                "        {\n" +
                "          body:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName A\",\\n" +
                "                           \"lastName\": \"LastName A\",\\n" +
                "                           \"firmId\": \"A\"\\n" +
                "                       }\\n" +
                "                     ]\\n';\n" +
                "            }#;\n" +
                "        };\n" +
                "      }\n" +
                "    ]\n" +
                "  }#\n" +
                "}\n"
        );
    }

    @Test
    public void testServiceStoreEmbeddedDataWithHeaderParams()
    {
        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ServiceStore\n" +
                "  #{\n" +
                "    [\n" +
                "      {\n" +
                "        request:\n" +
                "        {\n" +
                "          method: GET;\n" +
                "          url: '/employees';\n" +
                "          headerParameters:\n" +
                "          {\n" +
                "            name:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected: 'FirstName A';\n" +
                "              }#\n" +
                "          };\n" +
                "        };\n" +
                "        response:\n" +
                "        {\n" +
                "          body:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName A\",\\n" +
                "                           \"lastName\": \"LastName A\",\\n" +
                "                           \"firmId\": \"A\"\\n" +
                "                       },\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName B\",\\n" +
                "                           \"lastName\": \"LastName B\",\\n" +
                "                           \"firmId\": \"B\"\\n" +
                "                       }\\n" +
                "                     ]\\n';\n" +
                "            }#;\n" +
                "        };\n" +
                "      }\n" +
                "    ]\n" +
                "  }#\n" +
                "}\n"
        );

        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ServiceStore\n" +
                "  #{\n" +
                "    [\n" +
                "      {\n" +
                "        request:\n" +
                "        {\n" +
                "          method: GET;\n" +
                "          url: '/employees';\n" +
                "          headerParameters:\n" +
                "          {\n" +
                "            id:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected: 'FirstName A';\n" +
                "              }#,\n" +
                "            name:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected: 'FirstName A';\n" +
                "              }#\n" +
                "          };\n" +
                "        };\n" +
                "        response:\n" +
                "        {\n" +
                "          body:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName A\",\\n" +
                "                           \"lastName\": \"LastName A\",\\n" +
                "                           \"firmId\": \"A\"\\n" +
                "                       },\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName B\",\\n" +
                "                           \"lastName\": \"LastName B\",\\n" +
                "                           \"firmId\": \"B\"\\n" +
                "                       }\\n" +
                "                     ]\\n';\n" +
                "            }#;\n" +
                "        };\n" +
                "      }\n" +
                "    ]\n" +
                "  }#\n" +
                "}\n"
        );
    }

    @Test
    public void testServiceStoreEmbeddedDataWithBodyPatterns()
    {
        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ServiceStore\n" +
                "  #{\n" +
                "    [\n" +
                "      {\n" +
                "        request:\n" +
                "        {\n" +
                "          method: POST;\n" +
                "          url: '/employees';\n" +
                "          bodyPatterns:\n" +
                "          [\n" +
                "            EqualToJson\n" +
                "            #{\n" +
                "              expected:'{\"name\": \"FirstName A\"}';\n" +
                "            }#\n" +
                "          ];\n" +
                "        };\n" +
                "        response:\n" +
                "        {\n" +
                "          body:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName A\",\\n" +
                "                           \"lastName\": \"LastName A\",\\n" +
                "                           \"firmId\": \"A\"\\n" +
                "                       },\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName B\",\\n" +
                "                           \"lastName\": \"LastName B\",\\n" +
                "                           \"firmId\": \"B\"\\n" +
                "                       }\\n" +
                "                     ]\\n';\n" +
                "            }#;\n" +
                "        };\n" +
                "      }\n" +
                "    ]\n" +
                "  }#\n" +
                "}\n"
        );
    }

    @Test
    public void testServiceStoreEmbeddedDataWithMultipleStubMappings()
    {
        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ServiceStore\n" +
                "  #{\n" +
                "    [\n" +
                "      {\n" +
                "        request:\n" +
                "        {\n" +
                "          method: GET;\n" +
                "          url: '/employees1';\n" +
                "          queryParameters:\n" +
                "          {\n" +
                "            id:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected: '123';\n" +
                "              }#,\n" +
                "            name:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected: 'FirstName A';\n" +
                "              }#\n" +
                "          };\n" +
                "        };\n" +
                "        response:\n" +
                "        {\n" +
                "          body:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName A\",\\n" +
                "                           \"lastName\": \"LastName A\",\\n" +
                "                           \"firmId\": \"A\"\\n" +
                "                       }\\n" +
                "                     ]\\n';\n" +
                "            }#;\n" +
                "        };\n" +
                "      },\n" +
                "      {\n" +
                "        request:\n" +
                "        {\n" +
                "          method: GET;\n" +
                "          url: '/employees2';\n" +
                "          headerParameters:\n" +
                "          {\n" +
                "            name:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected: 'FirstName A';\n" +
                "              }#\n" +
                "          };\n" +
                "        };\n" +
                "        response:\n" +
                "        {\n" +
                "          body:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName A\",\\n" +
                "                           \"lastName\": \"LastName A\",\\n" +
                "                           \"firmId\": \"A\"\\n" +
                "                       },\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName B\",\\n" +
                "                           \"lastName\": \"LastName B\",\\n" +
                "                           \"firmId\": \"B\"\\n" +
                "                       }\\n" +
                "                     ]\\n';\n" +
                "            }#;\n" +
                "        };\n" +
                "      },\n" +
                "      {\n" +
                "        request:\n" +
                "        {\n" +
                "          method: POST;\n" +
                "          url: '/employees3';\n" +
                "          headerParameters:\n" +
                "          {\n" +
                "            headerParam:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected: 'FirstName A';\n" +
                "              }#\n" +
                "          };\n" +
                "          queryParameters:\n" +
                "          {\n" +
                "            id:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected: '123';\n" +
                "              }#\n" +
                "          };\n" +
                "          bodyPatterns:\n" +
                "          [\n" +
                "            EqualToJson\n" +
                "            #{\n" +
                "              expected:'{\"name\": \"FirstName A\"}';\n" +
                "            }#\n" +
                "          ];\n" +
                "        };\n" +
                "        response:\n" +
                "        {\n" +
                "          body:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName A\",\\n" +
                "                           \"lastName\": \"LastName A\",\\n" +
                "                           \"firmId\": \"A\"\\n" +
                "                       },\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName B\",\\n" +
                "                           \"lastName\": \"LastName B\",\\n" +
                "                           \"firmId\": \"B\"\\n" +
                "                       }\\n" +
                "                     ]\\n';\n" +
                "            }#;\n" +
                "        };\n" +
                "      },\n" +
                "      {\n" +
                "        request:\n" +
                "        {\n" +
                "          method: POST;\n" +
                "          url: '/employees4';\n" +
                "          bodyPatterns:\n" +
                "          [\n" +
                "            EqualToJson\n" +
                "            #{\n" +
                "              expected:'{\"name\": \"FirstName A\"}';\n" +
                "            }#\n" +
                "          ];\n" +
                "        };\n" +
                "        response:\n" +
                "        {\n" +
                "          body:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName A\",\\n" +
                "                           \"lastName\": \"LastName A\",\\n" +
                "                           \"firmId\": \"A\"\\n" +
                "                       },\\n" +
                "                       {\\n" +
                "                           \"firstName\": \"FirstName B\",\\n" +
                "                           \"lastName\": \"LastName B\",\\n" +
                "                           \"firmId\": \"B\"\\n" +
                "                       }\\n" +
                "                     ]\\n';\n" +
                "            }#;\n" +
                "        };\n" +
                "      }\n" +
                "    ]\n" +
                "  }#\n" +
                "}\n"
        );
    }
}
