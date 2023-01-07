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

package org.finos.legend.authentication.vault.impl;

import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.SystemPropertiesSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSSecretsManagerSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.StaticAWSCredentials;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class TestAWSSecretsManagerVault
{
    private String accessKeyFromGithubEnv;
    private String secretAccessKeyFromGithubEnv;
    private String secretIdFromGithubEnv;

    @Before
    public void setup()
    {
        this.accessKeyFromGithubEnv = System.getenv("TestAWSSecretsManagerVault_ACCESS_KEY_ID");
        this.secretAccessKeyFromGithubEnv = System.getenv("TestAWSSecretsManagerVault_SECRET_ACCESS_KEY");
        this.secretIdFromGithubEnv = System.getenv("TestAWSSecretsManagerVault_SECRET_ID");

        if (isEmpty(accessKeyFromGithubEnv) || isEmpty(secretAccessKeyFromGithubEnv) || isEmpty(secretIdFromGithubEnv))
        {
            assumeTrue("One/more secrets were not injected into the test environment", false);
        }
    }

    public boolean isEmpty(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    @Test
    public void testLookupSecretWithStaticCredentials() throws Exception
    {
        System.setProperty("TestAWSSecretsManagerVault_ACCESS_KEY_ID", this.accessKeyFromGithubEnv);
        System.setProperty("TestAWSSecretsManagerVault_SECRET_ACCESS_KEY", this.secretAccessKeyFromGithubEnv);
        CredentialVaultProvider credentialVaultProvider = new CredentialVaultProvider();
        credentialVaultProvider.register(new SystemPropertiesCredentialVault());

        AWSSecretsManagerVault awsSecretsManagerVault = new AWSSecretsManagerVault(credentialVaultProvider);
        AWSSecretsManagerSecret awsSecretsManagerSecret = new AWSSecretsManagerSecret(
                this.secretIdFromGithubEnv,
                null,
                null,
                new StaticAWSCredentials(
                        new SystemPropertiesSecret("TestAWSSecretsManagerVault_ACCESS_KEY_ID"),
                        new SystemPropertiesSecret("TestAWSSecretsManagerVault_SECRET_ACCESS_KEY")
                )
        );
        String rawSecret = awsSecretsManagerVault.lookupSecret(awsSecretsManagerSecret, null);
        assertTrue(rawSecret != null);
    }
}
