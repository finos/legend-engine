// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.shared.core.identity.credential;

import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KerberosUtilsTest
{
    @Test
    void shouldReturnSubjectFromLegendKerberosCredentials()
    {
        Subject expectedSubject = new Subject();
        LegendKerberosCredential legendKerberosCredential = new LegendKerberosCredential(expectedSubject);
        Identity identity = new Identity("User",legendKerberosCredential);

        Subject result = KerberosUtils.getSubjectFromIdentity(identity);

        assertEquals(expectedSubject, result);
    }

    @Test
    void shouldReturnMergedSubjectFromLegendConstrainedKerberosCredentials()
    {
        Subject userSubject = new Subject();
        userSubject.getPublicCredentials().add(new Object());
        Subject frontEndSubject = new Subject();
        frontEndSubject.getPrincipals().add(new KerberosPrincipal("system"));
        frontEndSubject.getPrivateCredentials().add(new Object());
        LegendConstrainedKerberosCredential legendConstrainedKerberosCredential = new LegendConstrainedKerberosCredential(userSubject,frontEndSubject);
        Identity identity = new Identity("User",legendConstrainedKerberosCredential);

        Subject result = KerberosUtils.getSubjectFromIdentity(identity);

        Subject expectedSubject = new Subject();
        expectedSubject.getPrincipals().addAll(frontEndSubject.getPrincipals());
        expectedSubject.getPublicCredentials().addAll(userSubject.getPublicCredentials());
        expectedSubject.getPrivateCredentials().addAll(frontEndSubject.getPrivateCredentials());
        assertEquals(expectedSubject, result);
    }

    @Test
    void shouldReturnNullIfCredentialsMissing()
    {
        Identity identity = new Identity("user");

        assertNull(KerberosUtils.getSubjectFromIdentity(identity));
    }
}