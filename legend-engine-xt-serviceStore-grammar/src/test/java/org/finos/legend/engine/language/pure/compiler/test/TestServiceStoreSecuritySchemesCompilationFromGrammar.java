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
    public void testAuthentication()
    {
        test("###Connection\n" +
                "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                "{\n" +
                "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                "    baseUrl : 'http://127.0.0.1:53008';\n" +
                "    auth: [\n" +
                "        oauth1     : OauthAuthentication\n" +
                "              {\n" +
                "                   grantType                   : 'client_credentials';\n" +
                "                   clientId                    : 'testClientID';\n" +
                "                   authorizationServerUrl      : 'dummy.com';\n" +
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
    public void testSecuritySchemeAndAuthenticationCombination()
    {
        test("###Connection\n" +
                "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                "{\n" +
                "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                "    baseUrl : 'http://127.0.0.1:53008';\n" +
                "    auth: [\n" +
                "        http     : OauthAuthentication\n" +
                "              {\n" +
                "                   grantType                   : 'client_credentials';\n" +
                "                   clientId                    : 'testClientID';\n" +
                "                   authorizationServerUrl      : 'dummy.com';\n" +
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
                ")\n", "COMPILATION error at [2:1-14:1]: securityScheme-Authentication combination is not supported. Only supported combinations are \n" +
                " [Http, UsernamePasswordAuthentication], [ApiKey, VaultSpecification], [Oauth, OauthAuthentication]");
    }

}
