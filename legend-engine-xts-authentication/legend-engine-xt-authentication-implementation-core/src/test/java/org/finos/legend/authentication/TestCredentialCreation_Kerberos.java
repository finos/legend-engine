// Copyright 2023 Goldman Sachs
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

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.authentication.credentialprovider.impl.ApikeyCredentialProvider;
import org.finos.legend.authentication.credentialprovider.impl.KerberosCredentialProvider;
import org.finos.legend.authentication.intermediationrule.impl.ApiKeyFromVaultRule;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.authentication.vault.impl.CredentialVaultProviderForTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiKeyAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.KerberosAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.credential.ApiTokenCredential;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.junit.Before;
import org.junit.Test;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TestCredentialCreation_Kerberos
{
    private Subject testSubject;
    private Identity identity;

    @Before
    public void setup()
    {
        Set<KerberosPrincipal> principals = new HashSet<>();
        principals.add(new KerberosPrincipal("peter@test.com"));
        testSubject = new Subject(false, principals, Sets.fixedSize.empty(), Sets.fixedSize.empty());
        this.identity = new Identity("peter", new LegendKerberosCredential(testSubject));
    }

    @Test
    public void makeCredential() throws Exception
    {
        KerberosCredentialProvider credentialProvider = new KerberosCredentialProvider();

        KerberosAuthenticationSpecification authenticationSpecification = new KerberosAuthenticationSpecification();
        LegendKerberosCredential credential = credentialProvider.makeCredential(authenticationSpecification, identity);

        assertEquals(testSubject, credential.getSubject());
    }

}
