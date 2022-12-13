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

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.ApiTokenAuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.DefaultH2AuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.DelegatedKerberosAuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.GCPApplicationDefaultCredentialsAuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.GCPWorkloadIdentityFederationAuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.MiddleTierUserNamePasswordAuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.OAuthProfile;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.SnowflakePublicAuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.UserNamePasswordAuthenticationStrategyRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.ApiTokenAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.DefaultH2AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.DelegatedKerberosAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPWorkloadIdentityFederationAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.MiddleTierUserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;

import java.util.List;

public class AuthenticationStrategyRuntimeGenerator implements AuthenticationStrategyVisitor<AuthenticationStrategyRuntime>
{
    private final List<OAuthProfile> oauthProfiles;

    public AuthenticationStrategyRuntimeGenerator(List<OAuthProfile> oauthProfiles)
    {
        this.oauthProfiles = oauthProfiles;
    }

    @Override
    public AuthenticationStrategyRuntime visit(AuthenticationStrategy authenticationStrategy)
    {
        if (authenticationStrategy instanceof DelegatedKerberosAuthenticationStrategy)
        {
            DelegatedKerberosAuthenticationStrategy delegatedKerberosAuthenticationStrategy = (DelegatedKerberosAuthenticationStrategy) authenticationStrategy;
            return new DelegatedKerberosAuthenticationStrategyRuntime(
                    delegatedKerberosAuthenticationStrategy.serverPrincipal
            );
        }
        else if (authenticationStrategy instanceof MiddleTierUserNamePasswordAuthenticationStrategy)
        {
            MiddleTierUserNamePasswordAuthenticationStrategy middleTierUserNameAuthenticationStrategy = (MiddleTierUserNamePasswordAuthenticationStrategy) authenticationStrategy;
            return new MiddleTierUserNamePasswordAuthenticationStrategyRuntime(
                    middleTierUserNameAuthenticationStrategy.vaultReference
            );
        }
        else if (authenticationStrategy instanceof UserNamePasswordAuthenticationStrategy)
        {
            UserNamePasswordAuthenticationStrategy userNamePasswordAuthenticationStrategy = (UserNamePasswordAuthenticationStrategy) authenticationStrategy;
            return new UserNamePasswordAuthenticationStrategyRuntime(
                    userNamePasswordAuthenticationStrategy.baseVaultReference == null ? userNamePasswordAuthenticationStrategy.userNameVaultReference : userNamePasswordAuthenticationStrategy.baseVaultReference + userNamePasswordAuthenticationStrategy.userNameVaultReference,
                    userNamePasswordAuthenticationStrategy.baseVaultReference == null ? userNamePasswordAuthenticationStrategy.passwordVaultReference : userNamePasswordAuthenticationStrategy.baseVaultReference + userNamePasswordAuthenticationStrategy.passwordVaultReference
            );
        }
        else if (authenticationStrategy instanceof TestDatabaseAuthenticationStrategy)
        {
            return new TestDatabaseAuthenticationStrategyRuntime();
        }
        else if (authenticationStrategy instanceof DefaultH2AuthenticationStrategy)
        {
            return new DefaultH2AuthenticationStrategyRuntime();
        }
        else if (authenticationStrategy instanceof ApiTokenAuthenticationStrategy)
        {
            ApiTokenAuthenticationStrategy apiTokenStrategy = (ApiTokenAuthenticationStrategy) authenticationStrategy;
            return new ApiTokenAuthenticationStrategyRuntime(
                    apiTokenStrategy.apiToken
            );
        }
        else if (authenticationStrategy instanceof SnowflakePublicAuthenticationStrategy)
        {
            SnowflakePublicAuthenticationStrategy snowflakePublicAuthenticationStrategy = (SnowflakePublicAuthenticationStrategy) authenticationStrategy;
            return new SnowflakePublicAuthenticationStrategyRuntime(
                    snowflakePublicAuthenticationStrategy.privateKeyVaultReference,
                    snowflakePublicAuthenticationStrategy.passPhraseVaultReference,
                    snowflakePublicAuthenticationStrategy.publicUserName
            );
        }
        else if (authenticationStrategy instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPApplicationDefaultCredentialsAuthenticationStrategy)
        {
            return new GCPApplicationDefaultCredentialsAuthenticationStrategyRuntime();
        }
        else if (authenticationStrategy instanceof GCPWorkloadIdentityFederationAuthenticationStrategy)
        {
            GCPWorkloadIdentityFederationAuthenticationStrategy gcpWorkloadIdentityFederationAuthenticationStrategy = (GCPWorkloadIdentityFederationAuthenticationStrategy) authenticationStrategy;
            return new GCPWorkloadIdentityFederationAuthenticationStrategyRuntime(
                    gcpWorkloadIdentityFederationAuthenticationStrategy.serviceAccountEmail,
                    gcpWorkloadIdentityFederationAuthenticationStrategy.additionalGcpScopes
            );
        }
        return null;
    }
}
