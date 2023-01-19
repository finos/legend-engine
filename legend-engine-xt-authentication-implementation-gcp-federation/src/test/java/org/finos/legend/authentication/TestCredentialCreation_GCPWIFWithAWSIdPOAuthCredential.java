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

package org.finos.legend.authentication;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.authentication.credentialprovider.impl.GCPWIFWithAWSIdPOAuthCredentialProvider;
import org.finos.legend.authentication.intermediationrule.impl.GCPWIFWithAWSIdPRule;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.authentication.vault.impl.SystemPropertiesCredentialVault;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.GCPWIFWithAWSIdPAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.SystemPropertiesSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.StaticAWSCredentials;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.credential.OAuthCredential;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class TestCredentialCreation_GCPWIFWithAWSIdPOAuthCredential
{
    private Identity identity;
    private String accountIdFromGithub;
    private String accessKeyIdFromGithub;
    private String secretAccessKeyFromGithub;

    // TODO - epsstan - Update Github/Maven build to inject secrets

    @Before
    public void setup()
    {
        this.identity = new Identity("alice", new AnonymousCredential());

        this.accessKeyIdFromGithub = System.getenv("GCPWIFWithAWSIdP_AWS_ACCESS_KEY_ID");
        this.secretAccessKeyFromGithub = System.getenv("GCPWIFWithAWSIdP_AWS_SECRET_ACCESS_KEY");
        this.accountIdFromGithub = System.getenv("GCPWIFWithAWSIdP_AWS_ACCOUNT_ID");

        // For testing, use creds for the developer-wif-user1 user

        if (isEmpty(accessKeyIdFromGithub) || isEmpty(secretAccessKeyFromGithub) || isEmpty(accountIdFromGithub))
        {
            assumeTrue("One more secrets were not injected into the test environment", false);
        }
    }

    public boolean isEmpty(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    @Test
    public void makeCredentialFromPropertiesVault() throws Exception
    {
        System.setProperty("GCPWIFWithAWSIdP_AWS_ACCESS_KEY_ID", this.accessKeyIdFromGithub);
        System.setProperty("GCPWIFWithAWSIdP_AWS_SECRET_ACCESS_KEY", this.secretAccessKeyFromGithub);
        CredentialVaultProvider credentialVaultProvider = new CredentialVaultProvider();
        credentialVaultProvider.register(new SystemPropertiesCredentialVault());

        GCPWIFWithAWSIdPOAuthCredentialProvider credentialProvider = new GCPWIFWithAWSIdPOAuthCredentialProvider();
        credentialProvider.configureWithRules(FastList.newListWith(new GCPWIFWithAWSIdPRule(credentialVaultProvider)));

        GCPWIFWithAWSIdPAuthenticationSpecification authenticationSpecification = new GCPWIFWithAWSIdPAuthenticationSpecification();
        authenticationSpecification.serviceAccountEmail = "integration-bq-sa1@legend-integration-testing.iam.gserviceaccount.com";
        authenticationSpecification.idPConfiguration = new GCPWIFWithAWSIdPAuthenticationSpecification.IdPConfiguration(
                "us-east-1",
                this.accountIdFromGithub,
                "integration-wif-role1",
                new StaticAWSCredentials(
                        new SystemPropertiesSecret("GCPWIFWithAWSIdP_AWS_ACCESS_KEY_ID"),
                        new SystemPropertiesSecret("GCPWIFWithAWSIdP_AWS_SECRET_ACCESS_KEY")
                ));
        authenticationSpecification.workloadConfiguration = new GCPWIFWithAWSIdPAuthenticationSpecification.WorkloadConfiguration(
                "412074507462",
                "integration-wif-pool1",
                "integration-wif-pool1-provider");

        OAuthCredential oAuthCredential = credentialProvider.makeCredential(authenticationSpecification, this.identity);
        assertTrue(oAuthCredential.getAccessToken() != null);
        assertTrue(!oAuthCredential.getAccessToken().isEmpty());
        assertTrue(oAuthCredential.getAccessToken().startsWith("ya29"));
    }
}
