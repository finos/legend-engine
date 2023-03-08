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

import org.finos.legend.authentication.credentialprovider.CredentialBuilder;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.authentication.credentialprovider.impl.ApikeyCredentialProvider;
import org.finos.legend.authentication.credentialprovider.impl.PrivateKeyCredentialProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.PlaintextAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.SystemPropertiesSecret;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextCredential;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestCredentialProviderProviderDefaults
{
    private Identity identity;

    @Before
    public void setup()
    {
        this.identity = new Identity("alice", new AnonymousCredential());
        System.setProperty("my.property1", "value1");
    }

    @Test
    public void defaultProviderProviderCanLoadFromSystemProperties() throws Exception
    {
        CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.defaultBuilder().build();

        assertEquals(1, credentialProviderProvider.getConfiguredCredentialProviders().size());

        PlaintextCredential credential = (PlaintextCredential) CredentialBuilder.makeCredential(credentialProviderProvider, new PlaintextAuthenticationSpecification(new SystemPropertiesSecret("my.property1")), identity);
        assertEquals("value1", credential.getValue());
    }

    @Test
    public void defaultProviderProviderCanBeEnrichedWithProviders()
    {
        CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.defaultBuilder()
                .with(new PrivateKeyCredentialProvider())
                .with(new ApikeyCredentialProvider())
                .build();

        assertEquals(3, credentialProviderProvider.getConfiguredCredentialProviders().size());
    }
}
