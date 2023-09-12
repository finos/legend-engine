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

package org.finos.legend.engine.language.pure.dsl.mastery.compiler.toPureGraph;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.CredentialSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.NTLMAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.TokenAuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_authentication_AuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_authentication_CredentialSecret;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_authentication_NTLMAuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_authentication_NTLMAuthenticationStrategy_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_authentication_TokenAuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_authentication_TokenAuthenticationStrategy_Impl;

import java.util.List;

public class HelperAuthenticationBuilder
{

    public static Root_meta_pure_mastery_metamodel_authentication_AuthenticationStrategy buildAuthentication(AuthenticationStrategy authenticationStrategy, CompileContext context)
    {

        if (authenticationStrategy instanceof NTLMAuthenticationStrategy)
        {
            return buildNTLMAuthentication((NTLMAuthenticationStrategy) authenticationStrategy, context);
        }

        if (authenticationStrategy instanceof TokenAuthenticationStrategy)
        {
            return buildTokenAuthentication((TokenAuthenticationStrategy) authenticationStrategy, context);
        }
        return null;
    }

    public static Root_meta_pure_mastery_metamodel_authentication_NTLMAuthenticationStrategy buildNTLMAuthentication(NTLMAuthenticationStrategy authenticationStrategy, CompileContext context)
    {
       return new Root_meta_pure_mastery_metamodel_authentication_NTLMAuthenticationStrategy_Impl("")
               ._credential(buildCredentialSecret(authenticationStrategy.credential, context));
    }

    public static Root_meta_pure_mastery_metamodel_authentication_TokenAuthenticationStrategy buildTokenAuthentication(TokenAuthenticationStrategy authenticationStrategy, CompileContext context)
    {
        return new Root_meta_pure_mastery_metamodel_authentication_TokenAuthenticationStrategy_Impl("")
                ._tokenUrl(authenticationStrategy.tokenUrl)
                ._credential(buildCredentialSecret(authenticationStrategy.credential, context));
    }

    public static Root_meta_pure_mastery_metamodel_authentication_CredentialSecret buildCredentialSecret(CredentialSecret credentialSecret, CompileContext context)
    {

        if (credentialSecret == null)
        {
            return null;
        }
        List<IMasteryCompilerExtension> extensions = IMasteryCompilerExtension.getExtensions();
        List<Function2<CredentialSecret, CompileContext, Root_meta_pure_mastery_metamodel_authentication_CredentialSecret>> processors = ListIterate.flatCollect(extensions, IMasteryCompilerExtension::getExtraSecretProcessors);
        return IMasteryCompilerExtension.process(credentialSecret, processors, context);
    }
}
