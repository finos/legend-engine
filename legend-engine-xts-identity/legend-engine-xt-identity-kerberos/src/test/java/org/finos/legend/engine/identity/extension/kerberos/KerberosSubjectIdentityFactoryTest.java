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

package org.finos.legend.engine.identity.extension.kerberos;

import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendConstrainedKerberosCredential;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.finos.legend.engine.identity.extension.kerberos.KerberosSubjectIdentityFactory.SERVICE_SUBJECT;
import static org.finos.legend.engine.identity.extension.kerberos.KerberosSubjectIdentityFactory.USER_SUBJECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KerberosSubjectIdentityFactoryTest
{
    private final KerberosSubjectIdentityFactory factory = new KerberosSubjectIdentityFactory();

    @Test
    void shouldReturnEmptyIfSubjectNotFound()
    {
        Optional<Identity> result = factory.makeIdentity("not a subject");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnUnknownEntityIfPrincipleIsNull()
    {
        Subject subject = new Subject();
        Map<String,Subject> subjects = new HashMap<>();
        subjects.put(USER_SUBJECT,subject);
        Optional<Identity> result = factory.makeIdentity(subjects);
        assertTrue(result.isPresent());
        assertEquals("_UNKNOWN_",result.get().getName());
    }

    @Test
    void shouldCreateIdentityWithConstraintKerberosIfPrivateCredsEmpty()
    {
        Subject mockFrontendSubject = new Subject();
        mockFrontendSubject.getPrincipals().add(new KerberosPrincipal("system@REALM"));
        Subject subject = new Subject();
        subject.getPrincipals().add(new KerberosPrincipal("user@REALM"));
        Map<String,Subject> inputSubjects = new HashMap<>();
        inputSubjects.put(USER_SUBJECT,subject);
        inputSubjects.put(SERVICE_SUBJECT,mockFrontendSubject);

        Optional<Identity> result = factory.makeIdentity(inputSubjects);

        assertTrue(result.isPresent());
        Identity identity = result.get();
        assertEquals("user", identity.getName());
        assertInstanceOf(LegendConstrainedKerberosCredential.class, identity.getCredentials().stream().findFirst().get());

    }

    @Test
    void shouldCreateIdentityWithKerberosIfPrivateCredsNotEmpty()
    {
        Subject subject = new Subject();
        subject.getPrincipals().add(new KerberosPrincipal("user@REALM"));
        subject.getPrivateCredentials().add(new Object());
        Map<String,Subject> subjects = new HashMap<>();
        subjects.put(USER_SUBJECT,subject);
        Optional<Identity> result = factory.makeIdentity(subjects);

        assertTrue(result.isPresent());
        Identity identity = result.get();
        assertEquals("user", identity.getName());
        assertInstanceOf(LegendKerberosCredential.class, identity.getCredentials().stream().findFirst().get());
    }

}