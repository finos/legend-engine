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
    public void serviceStoreSimpleExample() throws Exception
    {
        test("###ServiceStore\n" +
                "ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore\n" +
                "(\n" +
                "    SecuritySchemes : [\n" +
                "      http : Http\n" +
                "      {\n" +
                "        scheme : 'basic';\n" +
                "      },\n" +
                "      api : ApiKey\n" +
                "      {\n" +
                "        location : 'cookie';\n" +
                "        keyName : 'key1';\n" +
                "      },\n" +
                "      oauth1 : Oauth\n" +
                "      {\n" +
                "        scopes : ['read','openid'];\n" +
                "      },\n" +
                "      oauth2 : Oauth\n" +
                "      {\n" +
                "        scopes : ['read'];\n" +
                "      }\n" +
                "  ];\n" +
                "  ServiceGroup TradeServices\n" +
                "  (\n" +
                "    path : '/trades';\n" +
                "\n" +
                "    Service AllTradeService\n" +
                "    (\n" +
                "      path : '/allTradesService';\n" +
                "      method : GET;\n" +
                "      response : [meta::external::store::service::showcase::domain::S_Trade <- meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding];\n" +
                "      security : [http,oauth];\n" +
                "    )\n" +
                "  )\n" +
                "  ServiceGroup Trade\n" +
                "  (\n" +
                "    path : '/trades';\n" +
                "\n" +
                "    Service AllTradeService\n" +
                "    (\n" +
                "      path : '/allTrades';\n" +
                "      method : GET;\n" +
                "      response : [meta::external::store::service::showcase::domain::S_Trade <- meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding];\n" +
                "      security : [];\n" +
                "    )\n" +
                "  )\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                "{\n" +
                "  store: meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                "  baseUrl: 'http://127.0.0.1:53008';\n" +
                "  authSpecs: [\n" +
                "    oauth1 : OauthTokenGenerationSpecification\n" +
                "    {\n" +
                "      grantType : 'ClientCredentials';\n" +
                "      clientId : 'testClientID';\n" +
                "      clientSecretVaultReference : 'ref';\n" +
                "      authServerUrl : 'dummy.com';\n" +
                "    },\n" +
                "    oauth2 : OauthTokenGenerationSpecification\n" +
                "    {\n" +
                "      grantType : 'ClientCredentials';\n" +
                "      clientId : 'testClientID';\n" +
                "      clientSecretVaultReference : 'ref';\n" +
                "      authServerUrl : 'dummy.com';\n" +
                "    },\n" +
                "    http : UsernamePasswordSpecification\n" +
                "    {\n" +
                "      username : 'username';\n" +
                "      password : 'password';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n");
    }

}
