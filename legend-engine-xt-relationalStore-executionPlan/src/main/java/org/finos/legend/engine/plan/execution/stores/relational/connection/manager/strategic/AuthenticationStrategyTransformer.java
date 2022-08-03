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

package org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic;

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.GCPApplicationDefaultCredentialsAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.OAuthProfile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.ApiTokenAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.DefaultH2AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.DelegatedKerberosAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPWorkloadIdentityFederationAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.MiddleTierUserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;

import java.util.List;

public class AuthenticationStrategyTransformer implements AuthenticationStrategyVisitor<AuthenticationStrategy>
{
    private final List<OAuthProfile> oauthProfiles;

    public AuthenticationStrategyTransformer(List<OAuthProfile> oauthProfiles)
    {
        this.oauthProfiles = oauthProfiles;
    }

    @Override
    public AuthenticationStrategy visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy authenticationStrategy)
    {
        if (authenticationStrategy instanceof DelegatedKerberosAuthenticationStrategy)
        {
            DelegatedKerberosAuthenticationStrategy delegatedKerberosAuthenticationStrategy = (DelegatedKerberosAuthenticationStrategy) authenticationStrategy;
            return new org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.DelegatedKerberosAuthenticationStrategy(
                    delegatedKerberosAuthenticationStrategy.serverPrincipal
            );
        }
        else if (authenticationStrategy instanceof MiddleTierUserNamePasswordAuthenticationStrategy)
        {
            MiddleTierUserNamePasswordAuthenticationStrategy middleTierUserNameAuthenticationStrategy = (MiddleTierUserNamePasswordAuthenticationStrategy) authenticationStrategy;
            return new org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.MiddleTierUserNamePasswordAuthenticationStrategy(
                    middleTierUserNameAuthenticationStrategy.vaultReference
            );
        }
        else if (authenticationStrategy instanceof UserNamePasswordAuthenticationStrategy)
        {
            UserNamePasswordAuthenticationStrategy userNamePasswordAuthenticationStrategy = (UserNamePasswordAuthenticationStrategy) authenticationStrategy;
            return new org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.UserNamePasswordAuthenticationStrategy(
                    userNamePasswordAuthenticationStrategy.baseVaultReference == null ? userNamePasswordAuthenticationStrategy.userNameVaultReference : userNamePasswordAuthenticationStrategy.baseVaultReference + userNamePasswordAuthenticationStrategy.userNameVaultReference,
                    userNamePasswordAuthenticationStrategy.baseVaultReference == null ? userNamePasswordAuthenticationStrategy.passwordVaultReference : userNamePasswordAuthenticationStrategy.baseVaultReference + userNamePasswordAuthenticationStrategy.passwordVaultReference
            );
        }
        else if (authenticationStrategy instanceof TestDatabaseAuthenticationStrategy)
        {
            return new org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategy();
        }
        else if (authenticationStrategy instanceof DefaultH2AuthenticationStrategy)
        {
            return new org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.DefaultH2AuthenticationStrategy();
        }
        else if (authenticationStrategy instanceof ApiTokenAuthenticationStrategy)
        {
            ApiTokenAuthenticationStrategy apiTokenStrategy = (ApiTokenAuthenticationStrategy) authenticationStrategy;
            return new org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.ApiTokenAuthenticationStrategy(
                    apiTokenStrategy.apiToken
            );
        }
        else if (authenticationStrategy instanceof SnowflakePublicAuthenticationStrategy)
        {
            SnowflakePublicAuthenticationStrategy snowflakePublicAuthenticationStrategy = (SnowflakePublicAuthenticationStrategy) authenticationStrategy;
            return new org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.SnowflakePublicAuthenticationStrategy(
                    snowflakePublicAuthenticationStrategy.privateKeyVaultReference,
                    snowflakePublicAuthenticationStrategy.passPhraseVaultReference,
                    snowflakePublicAuthenticationStrategy.publicUserName
            );
        }
        else if (authenticationStrategy instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPApplicationDefaultCredentialsAuthenticationStrategy)
        {
            return new GCPApplicationDefaultCredentialsAuthenticationStrategy();
        }
        else if (authenticationStrategy instanceof GCPWorkloadIdentityFederationAuthenticationStrategy)
        {
            GCPWorkloadIdentityFederationAuthenticationStrategy gcpWorkloadIdentityFederationAuthenticationStrategy = (GCPWorkloadIdentityFederationAuthenticationStrategy) authenticationStrategy;
            return new org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.GCPWorkloadIdentityFederationAuthenticationStrategy(
                    gcpWorkloadIdentityFederationAuthenticationStrategy.serviceAccountEmail,
                    gcpWorkloadIdentityFederationAuthenticationStrategy.additionalGcpScopes
            );
        }
        return null;
    }
}
