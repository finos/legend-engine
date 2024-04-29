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
import org.finos.legend.authentication.credentialprovider.impl.UserPasswordCredentialProvider;
import org.finos.legend.authentication.intermediationrule.impl.UserPasswordFromVaultRule;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.authentication.vault.impl.CredentialVaultProviderForTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestCredentialCreation_UserPassword
{
    private Identity identity;

    private CredentialVaultProvider credentialVaultProvider;

    @Before
    public void setup()
    {
        this.identity = new Identity("alice", new AnonymousCredential());
        this.credentialVaultProvider = CredentialVaultProviderForTest.buildForTest()
                .withProperties("reference1", "key1")
                .build();
    }

    @Test
    public void makeCredentialFromPropertiesVault() throws Exception
    {
        UserPasswordCredentialProvider credentialProvider = new UserPasswordCredentialProvider();
        credentialProvider.configureWithRules(FastList.newListWith(new UserPasswordFromVaultRule(credentialVaultProvider)));

        UserPasswordAuthenticationSpecification authenticationSpecification = new UserPasswordAuthenticationSpecification("hello", new PropertiesFileSecret("reference1"));
        PlaintextUserPasswordCredential credential = credentialProvider.makeCredential(authenticationSpecification, identity);

        assertEquals("hello", credential.getUser());
        assertEquals("key1", credential.getPassword());
    }
}
