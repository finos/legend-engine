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

import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.GCPWorkloadIdentityFederationAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.BigQueryDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.OAuthCredential;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.vault.EnvironmentVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.*;

public class ExternalIntegration_TestBigQueryWithGCPWorkloadIdentityFederationFlow {
    private final Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");

    @Before
    public void setup()
    {
        Vault.INSTANCE.registerImplementation(new EnvironmentVaultImplementation());
    }

    @Test
    public void makeCredential() throws Exception {
        LegendDefaultDatabaseAuthenticationFlowProviderConfiguration defaultDatabaseAuthenticationFlowProviderConfiguration = new LegendDefaultDatabaseAuthenticationFlowProviderConfiguration();
        defaultDatabaseAuthenticationFlowProviderConfiguration.awsConfig.accountId = "564704738649";
        defaultDatabaseAuthenticationFlowProviderConfiguration.awsConfig.region = "us-east-1";
        defaultDatabaseAuthenticationFlowProviderConfiguration.awsConfig.role = "gcp-wif";
        defaultDatabaseAuthenticationFlowProviderConfiguration.awsConfig.awsAccessKeyIdVaultReference = "AWS_ACCESS_KEY_ID";
        defaultDatabaseAuthenticationFlowProviderConfiguration.awsConfig.awsSecretAccessKeyVaultReference = "AWS_SECRET_ACCESS_KEY";
        defaultDatabaseAuthenticationFlowProviderConfiguration.gcpWorkloadConfig.projectNumber = "412074507462";
        defaultDatabaseAuthenticationFlowProviderConfiguration.gcpWorkloadConfig.poolId = "aws-wif-pool2";
        defaultDatabaseAuthenticationFlowProviderConfiguration.gcpWorkloadConfig.providerId = "aws-wif-provider2";
        BigQueryWithGCPWorkloadIdentityFederationFlow flow = new BigQueryWithGCPWorkloadIdentityFederationFlow(defaultDatabaseAuthenticationFlowProviderConfiguration);
        GCPWorkloadIdentityFederationAuthenticationStrategy authenticationStrategy = new GCPWorkloadIdentityFederationAuthenticationStrategy();
        authenticationStrategy.serviceAccountEmail = "legend-integration-wif1@legend-integration-testing.iam.gserviceaccount.com";
        Credential credential = flow.makeCredential(identity1, new BigQueryDatasourceSpecification(), authenticationStrategy);
        assertTrue("Credential is not an instance of AccessTokenCredential", credential instanceof OAuthCredential);
        assertNotNull("Credential is null", ((OAuthCredential) credential).getAccessToken());
        assertFalse("Credential is empty", ((OAuthCredential) credential).getAccessToken().isEmpty());
    }
}