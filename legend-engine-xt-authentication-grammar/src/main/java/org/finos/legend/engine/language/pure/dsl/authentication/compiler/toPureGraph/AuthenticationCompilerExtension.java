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

package org.finos.legend.engine.language.pure.dsl.authentication.compiler.toPureGraph;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_connection_authentication_AuthenticationSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_connection_authentication_CredentialVaultSecret;

import java.util.List;
import java.util.Objects;


public class AuthenticationCompilerExtension implements IAuthenticationCompilerExtension
{

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.immutable.empty();
    }

    public List<Function2<AuthenticationSpecification, CompileContext, Root_meta_pure_runtime_connection_authentication_AuthenticationSpecification>> getExtraAuthenticationSpecificationProcessors()
    {
        return Lists.mutable.with((authSpec, context) ->
        {
            HelperAuthenticationBuilder.AuthenticationSpecificationBuilder builder = new HelperAuthenticationBuilder.AuthenticationSpecificationBuilder(context);
            return authSpec.accept(builder);
        });
    }

    public List<Function2<CredentialVaultSecret, CompileContext, Root_meta_pure_runtime_connection_authentication_CredentialVaultSecret>> getExtraCredentialVaultSecretProcessors()
    {
        return Lists.mutable.with((credentialVaultSecret, context) ->
        {
            HelperAuthenticationBuilder.CredentialVaultSecretBuilder builder = new HelperAuthenticationBuilder.CredentialVaultSecretBuilder(context);
            return credentialVaultSecret.accept(builder);
        });
    }

    private static Root_meta_pure_runtime_connection_authentication_CredentialVaultSecret buildSecret(CredentialVaultSecret credentialVaultSecret, CompileContext context)
    {
        List<Function2<CredentialVaultSecret, CompileContext, Root_meta_pure_runtime_connection_authentication_CredentialVaultSecret>> processors = ListIterate.flatCollect(IAuthenticationCompilerExtension.getExtensions(), ext -> ext.getExtraCredentialVaultSecretProcessors());

        return ListIterate
                .collect(processors, processor -> processor.value(credentialVaultSecret, context))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Can't compile credential Vault secret ", EngineErrorType.COMPILATION));

    }

}