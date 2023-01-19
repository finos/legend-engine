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

import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.authentication.vault.impl.PropertiesFileCredentialVault;
import org.finos.legend.authentication.vault.impl.SystemPropertiesCredentialVault;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.EnvironmentCredentialVaultSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.SystemPropertiesSecret;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class TestCredentialVaultProvider
{
    @Test
    public void vaultRegistration() throws Exception
    {
        CredentialVaultProvider credentialVaultProvider = new CredentialVaultProvider();

        PropertiesFileCredentialVault propertiesFileCredentialVault = new PropertiesFileCredentialVault(new Properties());
        credentialVaultProvider.register(propertiesFileCredentialVault);
        Assert.assertEquals(propertiesFileCredentialVault, credentialVaultProvider.getVault(new PropertiesFileSecret("a")));

        SystemPropertiesCredentialVault systemPropertiesCredentialVault = new SystemPropertiesCredentialVault();
        credentialVaultProvider.register(systemPropertiesCredentialVault);
        Assert.assertEquals(systemPropertiesCredentialVault, credentialVaultProvider.getVault(new SystemPropertiesSecret("a")));

        try
        {
            credentialVaultProvider.getVault(new EnvironmentCredentialVaultSecret("a"));
            Assert.fail("failed to throw exception");
        }
        catch (Exception e)
        {
            Assert.assertEquals("CredentialVault for secret of type 'class org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.EnvironmentCredentialVaultSecret' has not been registered in the system", e.getMessage());
        }
    }
}
