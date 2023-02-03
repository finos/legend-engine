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

public class TestServiceStoreSecuritySchemesCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
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
    public void testHttpSecurityScheme() throws Exception
    {

        test("###ServiceStore\n" +
                "ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore\n" +
                "(\n" +
                "   description : 'Showcase Service Store';\n" +
                "   securitySchemes : {\n" +
                "       http : Http\n" +
                "       {\n" +
                "          scheme : 'basic';\n" +
                "       }" +
                "   };\n" +
                ")\n" +
                "###Connection\n" +
                "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                "{\n" +
                "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                "    baseUrl : 'http://127.0.0.1:53008';\n" +
                "    auth: {\n" +
                "        http     : UserPassword\n" +
                "                   {\n" +
                "                       username : 'username';\n" +
                "                       password : PropertiesFileSecret\n" +
                "                       {\n" +
                "                           propertyName : 'ref1';\n" +
                "                       }\n" +
                "                   }" +
                "    };\n" +
                "}");
    }

    @Test
    public void testApiKeySecurityScheme() throws Exception
    {

        test("###ServiceStore\n" +
                "ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore\n" +
                "(\n" +
                "   description : 'Showcase Service Store';\n" +
                "   securitySchemes : {\n" +
                "       api : ApiKey\n" +
                "       {\n" +
                "           location : 'cookie';\n" +
                "           keyName : 'key1';\n" +
                "       }" +
                "   };\n" +
                ")\n" +
                "###Connection\n" +
                "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                "{\n" +
                "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                "    baseUrl : 'http://127.0.0.1:53008';\n" +
                "    auth: {\n" +
                "        api     : ApiKey\n" +
                "                   {\n" +
                "                       location : 'header';\n" +
                "                       keyName : 'key1';\n" +
                "                       value : SystemPropertiesSecret\n" +
                "                       {\n" +
                "                           systemPropertyName : 'reference1';\n" +
                "                       }\n" +
                "                   }" +
                "    };\n" +
                "}");
    }

    @Test
    public void testAuthentication()
    {
        test("###Connection\n" +
                "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                "{\n" +
                "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                "    baseUrl : 'http://127.0.0.1:53008';\n" +
                "    auth: {\n" +
                "        api     : ApiKey\n" +
                "                   {\n" +
                "                       location : 'header';\n" +
                "                       keyName : 'key1';\n" +
                "                       value : SystemPropertiesSecret\n" +
                "                       {\n" +
                "                           systemPropertyName : 'reference1';\n" +
                "                       }\n" +
                "                   }" +
                "    };\n" +
                "}\n\n" +
                "###ServiceStore\n" +
                "ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore\n" +
                "(\n" +
                "   description : 'Showcase Service Store';\n" +
                ")\n", "COMPILATION error at [7:20-14:20]: api security scheme not defined in service store ");
    }
}
