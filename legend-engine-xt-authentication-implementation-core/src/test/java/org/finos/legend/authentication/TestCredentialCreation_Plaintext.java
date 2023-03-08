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

import org.finos.legend.authentication.credentialprovider.impl.PlainTextCredentialProvider;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.authentication.vault.impl.SystemPropertiesCredentialVault;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.PlaintextAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.SystemPropertiesSecret;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextCredential;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestCredentialCreation_Plaintext
{
    private Identity identity;

    private CredentialVaultProvider credentialVaultProvider;

    @Before
    public void setup()
    {
        this.identity = new Identity("alice", new AnonymousCredential());

        System.setProperty("my.property1", "value1");
        this.credentialVaultProvider = CredentialVaultProvider.builder().with(new SystemPropertiesCredentialVault()).build();
    }

    @After
    public void clear()
    {
        System.clearProperty("my.property1");
    }

    @Test
    public void makeCredentialFromSystemProperties() throws Exception
    {
        PlainTextCredentialProvider credentialProvider = new PlainTextCredentialProvider(credentialVaultProvider);
        PlaintextAuthenticationSpecification authenticationSpecification = new PlaintextAuthenticationSpecification(new SystemPropertiesSecret("my.property1"));
        PlaintextCredential credential = credentialProvider.makeCredential(authenticationSpecification, identity);
        assertEquals("value1", credential.getValue());
    }

    @Test
    public void credentialNotFoundInSystemProperties() throws Exception
    {
        try
        {
            PlainTextCredentialProvider credentialProvider = new PlainTextCredentialProvider(credentialVaultProvider);
            PlaintextAuthenticationSpecification authenticationSpecification = new PlaintextAuthenticationSpecification(new SystemPropertiesSecret("my.property2"));
            credentialProvider.makeCredential(authenticationSpecification, identity);
        }
        catch (RuntimeException e)
        {
            assertEquals("Secret not found in system properties. System property name=my.property2", e.getMessage());
        }
    }
}
