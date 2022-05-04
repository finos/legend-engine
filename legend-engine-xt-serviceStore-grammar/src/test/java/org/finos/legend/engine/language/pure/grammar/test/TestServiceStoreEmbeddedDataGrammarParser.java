package org.finos.legend.engine.language.pure.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.serviceStore.ServiceStoreEmbeddedDataParserGrammar;
import org.junit.Test;

import java.util.List;

public class TestServiceStoreEmbeddedDataGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return ServiceStoreEmbeddedDataParserGrammar.VOCABULARY;
    }

    @Override
    public List<Vocabulary> getDelegatedParserGrammarVocabulary()
    {
        return FastList.newListWith(
                ServiceStoreEmbeddedDataParserGrammar.VOCABULARY
        );
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Data\n" +
                "Data " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  ServiceStore\n" +
                "  #{\n" +
                "    []\n" +
                "  }#\n" +
                "}\n\n";
    }

    @Test
    public void testMissingFieldsErrorMessages()
    {
        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  ServiceStore\n" +
                "  #{\n" +
                "    [\n" +
                "      {\n" +
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
                "}\n",
                "PARSER error at [7:7-17:7]: Field 'request' is required"
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
                "}\n",
                "PARSER error at [8:9-11:10]: Field 'method' is required"
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
                "        };\n" +
                "      }\n" +
                "    ]\n" +
                "  }#\n" +
                "}\n",
                "PARSER error at [7:7-13:7]: Field 'response' is required"
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
                "        };\n" +
                "        response:\n" +
                "        {\n" +
                "        };\n" +
                "      }\n" +
                "    ]\n" +
                "  }#\n" +
                "}\n",
                "PARSER error at [13:9-15:10]: Field 'body' is required"
        );
    }

    @Test
    public void testUnSupportedMethodTypes()
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
                "          method: PUT;\n" +
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
                "}\n",
                "PARSER error at [10:11-22]: Unsupported HTTP Method type - PUT. Supported types are - GET,POST"
        );
    }

    @Test
    public void testWrongEmbeddedDataWithResponse()
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
                        "            ServiceStore\n" +
                        "            #{\n" +
                        "              []\n" +
                        "            }#;\n" +
                        "        };\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }#\n" +
                        "}\n",
                "PARSER error at [16:13-19:14]: Service response body should be ExternalFormat Embedded Data"
        );
    }

    @Test
    public void testNotSupportedContentPattern()
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
                "              Unknown\n" +
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
                "}\n",
                "PARSER error at [15:15-21]: Unknown contentPattern pattern type: Unknown"
        );
    }

    @Test
    public void testParametersDefinedMultipleTimes()
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
                "}\n",
                "PARSER error at [19:13-23:16]: Query Param : 'name' value should be defined only once"
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
                "            name:\n" +
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
                "}\n",
                "PARSER error at [19:13-23:16]: Header Param : 'name' value should be defined only once"
        );
    }
}
