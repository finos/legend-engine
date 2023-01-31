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

package org.finos.legend.language.pure.dsl.authentication.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authentication.AuthenticationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.junit.Test;

import java.util.List;

public class TestAuthenticationGrammarParser_AuthenticationTypes extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return AuthenticationParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return null;
    }

    @Test
    public void userPasswordAuthentication()
    {
        PureModelContextData test = test("###AuthenticationDemo\n" +
                "import test::*;\n" +
                "AuthenticationDemo demo::demo1\n" +
                "{\n" +
                "  authentication: UserPassword\n" +
                "  {\n" +
                "    username: 'alice';\n" +
                "    password: PropertiesFileSecret\n" +
                "    {\n" +
                "      propertyName: 'property1';\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void apiTokenAuthentication()
    {
        PureModelContextData test = test("###AuthenticationDemo\n" +
                "import test::*;\n" +
                "AuthenticationDemo demo::demo1\n" +
                "{\n" +
                "  authentication: ApiKey\n" +
                "  {\n" +
                "    location: 'header';\n" +
                "    keyName: 'key1';\n" +
                "    value: PropertiesFileSecret\n" +
                "    {\n" +
                "      propertyName: 'property1';\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void encryptedKeyPairAuthentication()
    {
        PureModelContextData test = test("###AuthenticationDemo\n" +
                "import test::*;\n" +
                "AuthenticationDemo demo::demo1\n" +
                "{\n" +
                "  authentication: EncryptedPrivateKey\n" +
                "  {\n" +
                "    userName: 'alice';\n" +
                "    privateKey: PropertiesFileSecret\n" +
                "    {\n" +
                "      propertyName: 'property1';\n" +
                "    }\n" +
                "    passphrase: PropertiesFileSecret\n" +
                "    {\n" +
                "      propertyName: 'property1';\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void gcpWIFWithAWSIdP()
    {
        test("###AuthenticationDemo\n" +
                "import test::*;\n" +
                "AuthenticationDemo demo::demo1\n" +
                "{\n" +
                "  authentication: GCPWIFWithAWSIdP\n" +
                "  {\n" +
                "    serviceAccountEmail: 'a@b.com';\n" +
                "    idP: AWSIdP\n" +
                "    {\n" +
                "      accountId: 'account1';\n" +
                "      region: 'region1';\n" +
                "      role: 'role1';\n" +
                "      awsCredentials: Static\n" +
                "      {\n" +
                "           accessKeyId: PropertiesFileSecret\n" +
                "           {\n" +
                "              propertyName: 'property1';\n" +
                "           }\n" +
                "           secretAccessKey: PropertiesFileSecret\n" +
                "           {\n" +
                "              propertyName: 'property1';\n" +
                "           }\n" +
                "      }" +
                "    }\n" +
                "    workload: GCPWorkload\n" +
                "    {\n" +
                "      projectNumber: 'project1';\n" +
                "      providerId: 'provider1';\n" +
                "      poolId: 'pool1';\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
    }
}
