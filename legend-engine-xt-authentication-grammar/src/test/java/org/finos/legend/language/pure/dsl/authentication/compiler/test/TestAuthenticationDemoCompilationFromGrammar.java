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

package org.finos.legend.language.pure.dsl.authentication.compiler.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_connection_authentication_AuthenticationSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_connection_authentication_CredentialVaultSecret;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_connection_authentication_PropertiesFileSecret;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_connection_authentication_UserPasswordAuthenticationSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_connection_authentication_demo_AuthenticationDemo;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestAuthenticationDemoCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    protected String getDuplicatedElementTestCode()
    {
        return null;
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return null;
    }

    @Test
    public void userPasswordAuthentication()
    {
        Pair<PureModelContextData, PureModel> result = test("###AuthenticationDemo\n" +
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

        PureModel pureModel = result.getTwo();

        PackageableElement packageableElement = pureModel.getPackageableElement("demo::demo1");
        Root_meta_pure_runtime_connection_authentication_demo_AuthenticationDemo authenticationDemo = (Root_meta_pure_runtime_connection_authentication_demo_AuthenticationDemo) packageableElement;

        Root_meta_pure_runtime_connection_authentication_AuthenticationSpecification authenticationSpecification = authenticationDemo._authentication();
        Root_meta_pure_runtime_connection_authentication_UserPasswordAuthenticationSpecification userPasswordAuthenticationSpecification = (Root_meta_pure_runtime_connection_authentication_UserPasswordAuthenticationSpecification)authenticationSpecification;
        assertEquals("alice", userPasswordAuthenticationSpecification._username());

        Root_meta_pure_runtime_connection_authentication_CredentialVaultSecret credentialVaultSecret = userPasswordAuthenticationSpecification._password();
        Root_meta_pure_runtime_connection_authentication_PropertiesFileSecret propertiesFileVaultSecret = (Root_meta_pure_runtime_connection_authentication_PropertiesFileSecret) credentialVaultSecret;
        assertEquals("property1", propertiesFileVaultSecret._propertyName());
    }

    @Override
    public void testDuplicatedElement()
    {
        // TODO - epsstan
    }
}
