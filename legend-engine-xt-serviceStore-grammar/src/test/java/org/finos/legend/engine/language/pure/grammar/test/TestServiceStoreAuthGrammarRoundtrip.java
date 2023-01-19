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

public class TestServiceStoreAuthGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{

    @Test
    public void testHttpSecurityScheme() throws Exception
    {

        test("###ServiceStore\n" +
                "ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore\n" +
                "(\n" +
                "  securitySchemes : {\n" +
                "    http : Http\n" +
                "    {\n" +
                "        scheme : 'basic';\n" +
                "    }\n" +
                "  };\n" +
                ")\n\n\n" +
                "###Connection\n" +
                "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                "{\n" +
                "  store: meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                "  baseUrl: 'http://127.0.0.1:53008';\n" +
                "  auth: {\n" +
                "    http : UserPassword\n" +
                "    {\n" +
                "      username: 'username';\n" +
                "      password: PropertiesFileSecret\n" +
                "      {\n" +
                "        propertyName: 'ref1';\n" +
                "      }\n" +
                "    }\n" +
                "  };\n" +
                "}\n");
    }

    @Test
    public void testMultipleSecuritySchemes() throws Exception
    {

        test("###ServiceStore\n" +
                "ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore\n" +
                "(\n" +
                "  securitySchemes : {\n" +
                "    api : ApiKey\n" +
                "    {\n" +
                "        location : 'cookie';\n" +
                "        keyName : 'key1';\n" +
                "    },\n" +
                "    http : Http\n" +
                "    {\n" +
                "        scheme : 'basic';\n" +
                "    }\n" +
                "  };\n" +
                ")\n\n\n" +
                "###Connection\n" +
                "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                "{\n" +
                "  store: meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                "  baseUrl: 'http://127.0.0.1:53008';\n" +
                "  auth: {\n" +
                "    api : ApiKey\n" +
                "    {\n" +
                "      location: 'header';\n" +
                "      keyName: 'key1';\n" +
                "      value: SystemPropertiesSecret\n" +
                "      {\n" +
                "        systemPropertyName: 'reference1';\n" +
                "      }\n" +
                "    },\n" +
                "    http : UserPassword\n" +
                "    {\n" +
                "      username: 'username';\n" +
                "      password: PropertiesFileSecret\n" +
                "      {\n" +
                "        propertyName: 'ref1';\n" +
                "      }\n" +
                "    }\n" +
                "  };\n" +
                "}\n");
    }

    @Test
    public void testApiSecurityScheme() throws Exception
    {

        test("###ServiceStore\n" +
                "ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore\n" +
                "(\n" +
                "  securitySchemes : {\n" +
                "    api : ApiKey\n" +
                "    {\n" +
                "        location : 'cookie';\n" +
                "        keyName : 'key1';\n" +
                "    }\n" +
                "  };\n" +
                ")\n\n\n" +
                "###Connection\n" +
                "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                "{\n" +
                "  store: meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                "  baseUrl: 'http://127.0.0.1:53008';\n" +
                "  auth: {\n" +
                "    api : ApiKey\n" +
                "    {\n" +
                "      location: 'header';\n" +
                "      keyName: 'key1';\n" +
                "      value: SystemPropertiesSecret\n" +
                "      {\n" +
                "        systemPropertyName: 'reference1';\n" +
                "      }\n" +
                "    }\n" +
                "  };\n" +
                "}\n");
    }

}
