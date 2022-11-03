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

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ServiceStoreParserGrammar;
import org.junit.Test;

import java.util.List;

public class TestServiceStoreAuthGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{

    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return ServiceStoreParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###ServiceStore\n" +
                "ServiceStore " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "(\n" +
                ")\n";
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
                "       oauth : Oauth\n" +
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
                "         security : [http,oauth];\n" +
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
                "    auth: [\n" +
                "        oauth     : OauthAuthentication\n" +
                "              {\n" +
                "                   grantType                   : 'client_credentials';\n" +
                "                   clientId                    : 'testClientID';\n" +
                "                   clientSecretVaultReference  : 'ref';\n" +
                "                   authorizationServerUrl      : 'dummy.com';\n" +
                "              }\n" +
                "    ];\n" +
                "}");
    }

    @Test
    public void testServiceSecurity() throws Exception
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
                "         security : [http,oauth];\n" +
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
                "    auth: [\n" +
                "        oauth     : OauthAuthentication\n" +
                "              {\n" +
                "                   grantType                   : 'client_credentials';\n" +
                "                   clientId                    : 'testClientID';\n" +
                "                   clientSecretVaultReference  : 'ref';\n" +
                "                   authorizationServerUrl      : 'dummy.com';\n" +
                "              }\n" +
                "    ];\n" +
                "}", "PARSER error at [28:10-33]: These security schemes are not defined in ServiceStore - [oauth]");
    }

    @Test
    public void testDuplicatedSecuritySchemes()
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
                "        },\n" +
                "       oauth1 : Oauth\n" +
                "        {\n" +
                "           scopes : ['read'];\n" +
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
                "    auth: [\n" +
                "        oauth1     : OauthAuthentication\n" +
                "              {\n" +
                "                   grantType                   : 'client_credentials';\n" +
                "                   clientId                    : 'testClientID';\n" +
                "                   clientSecretVaultReference  : 'ref';\n" +
                "                   authorizationServerUrl      : 'dummy.com';\n" +
                "              },\n" +
                "       http : UsernamePasswordAuthentication\n" +
                "             {\n" +
                "                   username : 'username';\n" +
                "                   password : 'password';\n" +
                "             },\n" +
                "       api : ApiKeyAuthentication\n" +
                "             {\n" +
                "                   value : 'value1';\n" +
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
                " \n","PARSER error at [2:1-49:1]: Security schemes should have unique ids. Multiple schemes found with ids - [oauth1]");
    }



}
