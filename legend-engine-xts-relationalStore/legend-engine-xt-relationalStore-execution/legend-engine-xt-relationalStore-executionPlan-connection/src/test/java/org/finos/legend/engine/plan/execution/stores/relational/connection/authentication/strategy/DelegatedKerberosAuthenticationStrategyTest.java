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

package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy;

import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.IdentityState;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.credential.LegendConstrainedKerberosCredential;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.security.auth.Subject;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DelegatedKerberosAuthenticationStrategyTest
{
    private ConnectionStateManager connectionStateManager;

    private IdentityState identityState;

    private Identity identity;

    private DelegatedKerberosAuthenticationStrategy strategy;
    private Properties properties;
    private CredentialSupplier supplier;

    @BeforeEach
    void setUp()
    {
        connectionStateManager = mock(ConnectionStateManager.class);
        identityState = mock(IdentityState.class);
        identity = mock(Identity.class);
        strategy = spy(new DelegatedKerberosAuthenticationStrategy());
        properties = new Properties();
        supplier = mock(CredentialSupplier.class);
    }

    @Test
    void shouldReturnSubjectFromKerberosCredentialWhenSupplierProvided() throws Exception
    {
        Subject expectedSubject = new Subject();
        LegendKerberosCredential legendKerberosCredential = mock(LegendKerberosCredential.class);

        when(legendKerberosCredential.getSubject()).thenReturn(expectedSubject);
        when(identityState.getCredentialSupplier()).thenReturn(Optional.of(supplier));
        when(identityState.getIdentity()).thenReturn(identity);
        when(supplier.getCredential(identity)).thenReturn(legendKerberosCredential);


        try (MockedStatic<ConnectionStateManager> mockedStatic = mockStatic(ConnectionStateManager.class))
        {
            mockedStatic.when(ConnectionStateManager::getInstance).thenReturn(connectionStateManager);
            when(connectionStateManager.getIdentityStateUsing(properties)).thenReturn(identityState);

            Subject result = strategy.resolveSubject(properties);

            assertEquals(expectedSubject, result);
            verify(legendKerberosCredential).getSubject();
        }
    }

    @Test
    void shouldReturnMergedSubjectFromConstrainedKerberosCredentialWhenSupplierProvided() throws Exception
    {
        Subject expectedSubject = new Subject();
        LegendConstrainedKerberosCredential legendKerberosCredential = mock(LegendConstrainedKerberosCredential.class);

        when(legendKerberosCredential.getMergedSubject()).thenReturn(expectedSubject);
        when(identityState.getCredentialSupplier()).thenReturn(Optional.of(supplier));
        when(identityState.getIdentity()).thenReturn(identity);
        when(supplier.getCredential(identity)).thenReturn(legendKerberosCredential);


        try (MockedStatic<ConnectionStateManager> mockedStatic = mockStatic(ConnectionStateManager.class))
        {
            mockedStatic.when(ConnectionStateManager::getInstance).thenReturn(connectionStateManager);
            when(connectionStateManager.getIdentityStateUsing(properties)).thenReturn(identityState);

            Subject result = strategy.resolveSubject(properties);

            assertEquals(expectedSubject, result);
            verify(legendKerberosCredential).getMergedSubject();
        }
    }

    @Test
    void shouldReturnSubjectFromKerberosCredentialOnIdentityWhenNoSupplier() throws Exception
    {
        Subject expectedSubject = new Subject();
        LegendKerberosCredential legendKerberosCredential = mock(LegendKerberosCredential.class);

        when(legendKerberosCredential.getSubject()).thenReturn(expectedSubject);
        when(identityState.getCredentialSupplier()).thenReturn(Optional.empty());
        when(identityState.getIdentity()).thenReturn(identity);
        when(identity.getCredential(LegendKerberosCredential.class)).thenReturn(Optional.of(legendKerberosCredential));

        try (MockedStatic<ConnectionStateManager> mockedStatic = mockStatic(ConnectionStateManager.class))
        {
            mockedStatic.when(ConnectionStateManager::getInstance).thenReturn(connectionStateManager);
            when(connectionStateManager.getIdentityStateUsing(properties)).thenReturn(identityState);

            Subject result = strategy.resolveSubject(properties);

            assertEquals(expectedSubject, result);
            verify(legendKerberosCredential).getSubject();
        }
    }

    @Test
    void shouldReturnMergedSubjectFromConstrainedKerberosCredentialOnIdentityWhenNoSupplier() throws Exception
    {
        Subject expectedSubject = new Subject();
        LegendConstrainedKerberosCredential legendKerberosCredential = mock(LegendConstrainedKerberosCredential.class);

        when(legendKerberosCredential.getMergedSubject()).thenReturn(expectedSubject);
        when(identityState.getCredentialSupplier()).thenReturn(Optional.empty());
        when(identityState.getIdentity()).thenReturn(identity);
        when(identity.getCredential(LegendConstrainedKerberosCredential.class)).thenReturn(Optional.of(legendKerberosCredential));

        try (MockedStatic<ConnectionStateManager> mockedStatic = mockStatic(ConnectionStateManager.class))
        {
            mockedStatic.when(ConnectionStateManager::getInstance).thenReturn(connectionStateManager);
            when(connectionStateManager.getIdentityStateUsing(properties)).thenReturn(identityState);

            Subject result = strategy.resolveSubject(properties);

            assertEquals(expectedSubject, result);
            verify(legendKerberosCredential).getMergedSubject();
        }
    }

    @Test
    void shouldThrowExceptionWhenUnsupportedCredentialTypeReturnedBySupplier() throws Exception
    {
        when(identityState.getCredentialSupplier()).thenReturn(Optional.of(supplier));
        when(identityState.getIdentity()).thenReturn(identity);
        when(supplier.getCredential(identity)).thenReturn(new AnonymousCredential());


        try (MockedStatic<ConnectionStateManager> mockedStatic = mockStatic(ConnectionStateManager.class))
        {
            mockedStatic.when(ConnectionStateManager::getInstance).thenReturn(connectionStateManager);
            when(connectionStateManager.getIdentityStateUsing(properties)).thenReturn(identityState);

            UnsupportedOperationException unsupportedOperationException = assertThrows(UnsupportedOperationException.class, () -> strategy.resolveSubject(properties));
            assertEquals("Unsupported credential type returned by supplier: class org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential",unsupportedOperationException.getMessage());

        }
    }

    @Test
    void shouldThrowExceptionWhenNoKerberosCredentialFoundOnIdentityState() throws Exception
    {
        when(identityState.getCredentialSupplier()).thenReturn(Optional.empty());
        when(identityState.getIdentity()).thenReturn(identity);
        when(identity.getCredential(any())).thenReturn(Optional.empty()).thenReturn(Optional.empty());

        try (MockedStatic<ConnectionStateManager> mockedStatic = mockStatic(ConnectionStateManager.class))
        {
            mockedStatic.when(ConnectionStateManager::getInstance).thenReturn(connectionStateManager);
            when(connectionStateManager.getIdentityStateUsing(properties)).thenReturn(identityState);

            UnsupportedOperationException unsupportedOperationException = assertThrows(UnsupportedOperationException.class, () -> strategy.resolveSubject(properties));
            assertTrue(unsupportedOperationException.getMessage().contains("Expected Kerberos credential was not found on identity state"));

        }
    }

}
