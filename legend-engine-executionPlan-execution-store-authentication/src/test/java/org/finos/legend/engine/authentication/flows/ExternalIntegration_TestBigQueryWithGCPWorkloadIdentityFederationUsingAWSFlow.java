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

package org.finos.legend.engine.authentication.flows;

import org.finos.legend.engine.authentication.vaults.InMemoryVaultForTesting;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.BigQueryDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.OAuthCredential;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.*;

public class ExternalIntegration_TestBigQueryWithGCPWorkloadIdentityFederationUsingAWSFlow {
    private final Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");
    private final InMemoryVaultForTesting inMemoryVault = new InMemoryVaultForTesting();

    @Before
    public void setup()
    {
        Vault.INSTANCE.registerImplementation(inMemoryVault);
    }

    @Test
    public void makeCredential() throws Exception {
        inMemoryVault.setValue("key1", System.getenv("AWS_ACCESS_KEY_ID"));
        inMemoryVault.setValue("secret1", System.getenv("AWS_SECRET_ACCESS_KEY"));
        BigQueryWithGCPWorkloadIdentityFederationUsingAWSFlow flow = new BigQueryWithGCPWorkloadIdentityFederationUsingAWSFlow();
        GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy authenticationStrategy = new GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy();
        authenticationStrategy.workloadProjectNumber = "412074507462";
        authenticationStrategy.workloadPoolId = "aws-wif-pool2";
        authenticationStrategy.workloadProviderId = "aws-wif-provider2";
        authenticationStrategy.serviceAccountEmail = "legend-integration-wif1@legend-integration-testing.iam.gserviceaccount.com";
        authenticationStrategy.awsAccountId = "564704738649";
        authenticationStrategy.awsRegion = "us-east-1";
        authenticationStrategy.awsRole = "gcp-wif";
        authenticationStrategy.awsAccessKeyIdVaultReference = "key1";
        authenticationStrategy.awsSecretAccessKeyVaultReference = "secret1";
        Credential credential = flow.makeCredential(identity1, new BigQueryDatasourceSpecification(), authenticationStrategy);
        assertTrue("Credential is not an instance of AccessTokenCredential", credential instanceof OAuthCredential);
        assertNotNull("Credential is null", ((OAuthCredential) credential).getAccessToken());
        assertFalse("Credential is empty", ((OAuthCredential) credential).getAccessToken().isEmpty());
    }
}