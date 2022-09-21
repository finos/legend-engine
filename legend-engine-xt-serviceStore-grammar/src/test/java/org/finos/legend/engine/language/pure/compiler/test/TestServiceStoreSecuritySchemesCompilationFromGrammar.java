// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.test;

import org.junit.Test;

public class TestServiceStoreSecuritySchemesCompilationFromGrammar  extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{

    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###ServiceStore\n" +
                "ServiceStore anything::somethingelse\n" +
                "(\n" +
                ")";
    }


    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::somethingelse'";
    }

    @Test
    public void testSecuritySchemes() throws Exception
    {

        test("###ServiceStore\n" +
                "ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore\n" +
                "(\n" +
                "   description : 'Showcase Service Store';\n" +
                "   SecuritySchemes : [\n" +
                "       http : Http\n" +
                "       {\n" +
                "          scheme : 'basic';\n" +
                "       },\n" +
                "       api : ApiKey\n" +
                "       {\n" +
                "           location : 'cookie';\n" +
                "           keyName : 'key1';\n" +
                "       },\n" +
                "       oauth1 : Oauth\n" +
                "        {\n" +
                "           scopes : ['read','openid'];\n" +
                "        }\n" +
                "   ];\n" +
                "   ServiceGroup TradeServices\n" +
                "   (\n" +
                "      path : '/trades';\n" +
                "\n" +
                "      Service AllTradeService\n" +
                "      (\n" +
                "         path : '/allTradesService';\n" +
                "         method : GET;\n" +
                "         security : [http,oauth1];\n" +
                "         response : [meta::external::store::service::showcase::domain::S_Trade <- meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding];\n" +
                "      )\n" +
                "   )\n" +
                "\n" +
                "   ServiceGroup Trade\n" +
                "      (\n" +
                "         path : '/trades';\n" +
                "\n" +
                "         Service AllTradeService\n" +
                "         (\n" +
                "            path : '/allTrades';\n" +
                "            method : GET;\n" +
                "            security : [];\n" +
                "            response : [meta::external::store::service::showcase::domain::S_Trade <- meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding];\n" +
                "         )\n" +
                "      )\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                "{\n" +
                "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                "    baseUrl : 'http://127.0.0.1:53008';\n" +
                "    authSpecs: [\n" +
                "        oauth1     : OauthTokenGenerationSpecification\n" +
                "              {\n" +
                "                   grantType                   : 'ClientCredentials';\n" +
                "                   clientId                    : 'testClientID';\n" +
                "                   clientSecretVaultReference  : 'ref';\n" +
                "                   authServerUrl               : 'dummy.com';\n" +
                "              },\n" +
                "       http : UsernamePasswordSpecification\n" +
                "             {\n" +
                "                   username : 'username';\n" +
                "                   password : 'password';\n" +
                "             }\n" +
                "    ];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                "SchemaSet meta::external::store::service::showcase::store::tradeSchemaSet\n" +
                "{\n" +
                "  format  : FlatData;\n" +
                "  schemas : [\n" +
                "    {\n" +
                "        content: 'section A: DelimitedWithHeadings{  scope.untilEof;  delimiter: \\',\\';  Record  {      s_tradeId       : STRING;      s_traderDetails : STRING;      s_tradeDetails  : STRING;  }}';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Binding meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding\n" +
                "{\n" +
                "  schemaSet     : meta::external::store::service::showcase::store::tradeSchemaSet;\n" +
                "  contentType   : 'application/x.flatdata';\n" +
                "  modelIncludes : [ meta::external::store::service::showcase::domain::S_Trade ];\n" +
                "}\n" +
                "###Pure\n" +
                "Class meta::external::store::service::showcase::domain::S_Trade\n" +
                "{\n" +
                "  s_tradeId       : String[1];\n" +
                "  s_traderDetails : String[1];\n" +
                "  s_tradeDetails  : String[1];\n" +
                "}\n" +
                " \n");
    }

    @Test
    public void testAuthSpec()
    {
        test("###Connection\n" +
                "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                "{\n" +
                "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                "    baseUrl : 'http://127.0.0.1:53008';\n" +
                "    authSpecs: [\n" +
                "        oauth1     : OauthTokenGenerationSpecification\n" +
                "              {\n" +
                "                   grantType                   : 'ClientCredentials';\n" +
                "                   clientId                    : 'testClientID';\n" +
                "                   authServerUrl               : 'dummy.com';\n" +
                "              }\n" +
                "    ];\n" +
                "}\n" +
                "###ServiceStore\n" +
                "ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore\n" +
                "(\n" +
                "   description : 'Showcase Service Store';\n" +
                ")\n", "COMPILATION error at [2:1-14:1]: Security Scheme not defined in ServiceStore: oauth1");
    }

    @Test
    public void testSecuritySchemeAndAuthSpecCombination()
    {
        test("###Connection\n" +
                "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                "{\n" +
                "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                "    baseUrl : 'http://127.0.0.1:53008';\n" +
                "    authSpecs: [\n" +
                "        http     : OauthTokenGenerationSpecification\n" +
                "              {\n" +
                "                   grantType                   : 'ClientCredentials';\n" +
                "                   clientId                    : 'testClientID';\n" +
                "                   authServerUrl               : 'dummy.com';\n" +
                "              }\n" +
                "    ];\n" +
                "}\n" +
                "###ServiceStore\n" +
                "ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore\n" +
                "(\n" +
                "   description : 'Showcase Service Store';\n" +
                "   SecuritySchemes : [\n" +
                "       http : Http\n" +
                "       {\n" +
                "          scheme : 'basic';\n" +
                "       }\n" +
                "    ];\n" +
                ")\n", "COMPILATION error at [2:1-14:1]: securityScheme-AuthSpec combination is not supported. Only supported combinations are \n" +
                " [Http, UsernamePasswordSpecification], [ApiKey, VaultSpecification], [Oauth, OauthTokenGenerationSpecification]");
    }

}
