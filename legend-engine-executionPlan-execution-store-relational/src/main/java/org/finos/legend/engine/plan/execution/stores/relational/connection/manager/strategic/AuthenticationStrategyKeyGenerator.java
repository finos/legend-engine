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

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.*;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.GCPApplicationDefaultCredentialsAuthenticationStrategyKey;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.*;

public class AuthenticationStrategyKeyGenerator implements AuthenticationStrategyVisitor<AuthenticationStrategyKey>
{
    @Override
    public AuthenticationStrategyKey visit(AuthenticationStrategy authenticationStrategy)
    {
        if(authenticationStrategy instanceof DelegatedKerberosAuthenticationStrategy)
        {
            return new DelegatedKerberosAuthenticationStrategyKey(((DelegatedKerberosAuthenticationStrategy) authenticationStrategy).serverPrincipal);
        }
        else if(authenticationStrategy instanceof UserNamePasswordAuthenticationStrategy)
        {
            UserNamePasswordAuthenticationStrategy userNamePasswordAuthStrategy = (UserNamePasswordAuthenticationStrategy) authenticationStrategy;
            String userNameVaultReference = userNamePasswordAuthStrategy.baseVaultReference == null ? userNamePasswordAuthStrategy.userNameVaultReference : userNamePasswordAuthStrategy.baseVaultReference + userNamePasswordAuthStrategy.userNameVaultReference;
            String passwordVaultReference = userNamePasswordAuthStrategy.baseVaultReference == null ? userNamePasswordAuthStrategy.passwordVaultReference : userNamePasswordAuthStrategy.baseVaultReference + userNamePasswordAuthStrategy.passwordVaultReference;
            return new UserNamePasswordAuthenticationStrategyKey(userNameVaultReference, passwordVaultReference);
        }
        else if(authenticationStrategy instanceof TestDatabaseAuthenticationStrategy)
        {
            return new TestDatabaseAuthenticationStrategyKey();
        }
        else if(authenticationStrategy instanceof DefaultH2AuthenticationStrategy)
        {
            return new DefaultH2AuthenticationStrategyKey();
        }
        else if (authenticationStrategy instanceof SnowflakePublicAuthenticationStrategy)
        {
            SnowflakePublicAuthenticationStrategy snowflakeDatasourceSpecification = (SnowflakePublicAuthenticationStrategy) authenticationStrategy;
            return new SnowflakePublicAuthenticationStrategyKey(
                    snowflakeDatasourceSpecification.privateKeyVaultReference,
                    snowflakeDatasourceSpecification.passPhraseVaultReference,
                    snowflakeDatasourceSpecification.publicUserName
            );
        }
        else if(authenticationStrategy instanceof GCPApplicationDefaultCredentialsAuthenticationStrategy)
        {
            GCPApplicationDefaultCredentialsAuthenticationStrategy GCPApplicationDefaultCredentialsAuthenticationStrategy = (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPApplicationDefaultCredentialsAuthenticationStrategy)authenticationStrategy;
            return new GCPApplicationDefaultCredentialsAuthenticationStrategyKey();
        }
        else if(authenticationStrategy instanceof GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy){
            GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy gcpWorkloadIdentityFederationWithAWSAuthenticationStrategy = (GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy)authenticationStrategy;
            return new GCPWorkloadIdentityFederationWithAWSAuthenticationStrategyKey(
                    gcpWorkloadIdentityFederationWithAWSAuthenticationStrategy.workloadProjectNumber,
                    gcpWorkloadIdentityFederationWithAWSAuthenticationStrategy.serviceAccountEmail,
                    gcpWorkloadIdentityFederationWithAWSAuthenticationStrategy.additionalGcpScopes,
                    gcpWorkloadIdentityFederationWithAWSAuthenticationStrategy.workloadPoolId,
                    gcpWorkloadIdentityFederationWithAWSAuthenticationStrategy.workloadProviderId,
                    gcpWorkloadIdentityFederationWithAWSAuthenticationStrategy.awsAccountId,
                    gcpWorkloadIdentityFederationWithAWSAuthenticationStrategy.awsRegion,
                    gcpWorkloadIdentityFederationWithAWSAuthenticationStrategy.awsRole,
                    gcpWorkloadIdentityFederationWithAWSAuthenticationStrategy.awsAccessKeyIdVaultReference,
                    gcpWorkloadIdentityFederationWithAWSAuthenticationStrategy.awsSecretAccessKeyVaultReference
            );
        }
        return null;
    }
}
