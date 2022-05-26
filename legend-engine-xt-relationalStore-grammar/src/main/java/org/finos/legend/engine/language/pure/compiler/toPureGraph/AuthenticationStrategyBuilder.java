// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.*;
import org.finos.legend.pure.generated.*;

public class AuthenticationStrategyBuilder implements AuthenticationStrategyVisitor<Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy>
{
    public AuthenticationStrategyBuilder()
    {
    }

    @Override
    public Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy visit(AuthenticationStrategy authenticationStrategy)
    {
        if (authenticationStrategy instanceof TestDatabaseAuthenticationStrategy)
        {
            return new Root_meta_pure_alloy_connections_alloy_authentication_TestDatabaseAuthenticationStrategy_Impl("");
        }
        else if (authenticationStrategy instanceof DefaultH2AuthenticationStrategy)
        {
            return new Root_meta_pure_alloy_connections_alloy_authentication_DefaultH2AuthenticationStrategy_Impl("");
        }
        else if (authenticationStrategy instanceof DelegatedKerberosAuthenticationStrategy)
        {
            return new Root_meta_pure_alloy_connections_alloy_authentication_DelegatedKerberosAuthenticationStrategy_Impl("")
                    ._serverPrincipal(((DelegatedKerberosAuthenticationStrategy) authenticationStrategy).serverPrincipal);
        }
        else if (authenticationStrategy instanceof ApiTokenAuthenticationStrategy)
        {
            return new Root_meta_pure_alloy_connections_alloy_authentication_ApiTokenAuthenticationStrategy_Impl("")
                    ._apiToken(((ApiTokenAuthenticationStrategy) authenticationStrategy).apiToken);
        }
        else if (authenticationStrategy instanceof UserNamePasswordAuthenticationStrategy)
        {
            UserNamePasswordAuthenticationStrategy userNamePasswordAuthenticationStrategy = (UserNamePasswordAuthenticationStrategy) authenticationStrategy;
            return new Root_meta_pure_alloy_connections_alloy_authentication_UserNamePasswordAuthenticationStrategy_Impl("")
                    ._baseVaultReference(userNamePasswordAuthenticationStrategy.baseVaultReference)
                    ._userNameVaultReference(userNamePasswordAuthenticationStrategy.userNameVaultReference)
                    ._passwordVaultReference(userNamePasswordAuthenticationStrategy.passwordVaultReference);
        }
        else if (authenticationStrategy instanceof SnowflakePublicAuthenticationStrategy)
        {
            return new Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy_Impl("")
                    ._publicUserName(((SnowflakePublicAuthenticationStrategy) authenticationStrategy).publicUserName)
                    ._privateKeyVaultReference(((SnowflakePublicAuthenticationStrategy) authenticationStrategy).privateKeyVaultReference)
                    ._passPhraseVaultReference(((SnowflakePublicAuthenticationStrategy) authenticationStrategy).passPhraseVaultReference);
        }
        else if (authenticationStrategy instanceof GCPApplicationDefaultCredentialsAuthenticationStrategy)
        {
            return new Root_meta_pure_alloy_connections_alloy_authentication_GCPApplicationDefaultCredentialsAuthenticationStrategy_Impl("");
        }
        else if (authenticationStrategy instanceof GCPWorkloadIdentityFederationAuthenticationStrategy)
        {
            return new Root_meta_pure_alloy_connections_alloy_authentication_GCPWorkloadIdentityFederationAuthenticationStrategy_Impl("")
                    ._serviceAccountEmail(((GCPWorkloadIdentityFederationAuthenticationStrategy) authenticationStrategy).serviceAccountEmail)
                    ._additionalGcpScopes(
                            ((GCPWorkloadIdentityFederationAuthenticationStrategy) authenticationStrategy).additionalGcpScopes == null ?
                                    FastList.newList() :
                                    FastList.newList(((GCPWorkloadIdentityFederationAuthenticationStrategy) authenticationStrategy).additionalGcpScopes)
                    );
        }
        return null;
    }
}