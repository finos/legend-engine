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

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LegendConstrainedKerberosCredentialTest
{

    private Subject userSubject;
    private Subject frontendSubject;
    private GSSCredential gssCredential;
    private LegendConstrainedKerberosCredential credential;

    @BeforeEach
    void setUp()
    {
        userSubject = new Subject();
        gssCredential = mock(GSSCredential.class);
        userSubject.getPublicCredentials().add(gssCredential);
        frontendSubject = new Subject();
        frontendSubject.getPrincipals().add(new KerberosPrincipal("user"));
        frontendSubject.getPrivateCredentials().add(new Object());
        credential = new LegendConstrainedKerberosCredential(userSubject, frontendSubject);
    }

    @Test
    void shouldReturnMergedSubject()
    {
        Subject mergedSubject = credential.getMergedSubject();

        // Verify principals
        Set<Principal> expectedPrincipals = new HashSet<>(frontendSubject.getPrincipals());
        assertEquals(expectedPrincipals, mergedSubject.getPrincipals());

        // Verify public credentials
        Set<Object> expectedPublicCredentials = new HashSet<>(userSubject.getPublicCredentials());
        assertEquals(expectedPublicCredentials, mergedSubject.getPublicCredentials());

        // Verify private credentials
        Set<Object> expectedPrivateCredentials = new HashSet<>(frontendSubject.getPrivateCredentials());
        assertEquals(expectedPrivateCredentials, mergedSubject.getPrivateCredentials());
    }

    @Test
    void shouldReturnTrueIfRemainingTimeGreaterThanZero() throws GSSException
    {
        when(gssCredential.getRemainingLifetime()).thenReturn(500);

        assertTrue(credential.isValid());
    }

    @Test
    void shouldReturnFalseIfRemainingTimeIsZero() throws GSSException
    {
        when(gssCredential.getRemainingLifetime()).thenReturn(0);

        assertFalse(credential.isValid());
    }

    @Test
    void shouldReturnFalseIfCredentialsAreMissing()
    {
        LegendConstrainedKerberosCredential legendConstrainedKerberosCredential = new LegendConstrainedKerberosCredential(new Subject(), frontendSubject);

        assertFalse(legendConstrainedKerberosCredential.isValid());
    }

    @Test
    void shouldThrowRuntimeExceptionIfGSSException() throws GSSException
    {

        when(gssCredential.getRemainingLifetime()).thenThrow(new GSSException(1));

        assertThrows(RuntimeException.class, () -> credential.isValid());
    }

}
