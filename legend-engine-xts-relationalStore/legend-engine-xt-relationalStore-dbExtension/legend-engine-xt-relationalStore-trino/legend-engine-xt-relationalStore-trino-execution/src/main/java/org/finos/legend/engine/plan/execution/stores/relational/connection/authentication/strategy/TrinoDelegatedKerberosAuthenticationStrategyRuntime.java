//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.TrinoDelegatedKerberosAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.IdentityState;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.slf4j.Logger;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;

import java.sql.Connection;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class TrinoDelegatedKerberosAuthenticationStrategyRuntime extends org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TrinoDelegatedKerberosAuthenticationStrategyRuntime.class);
    public String serverPrincipal;
    public String kerberosRemoteServiceName;
    public Boolean kerberosUseCanonicalHostname;

    public TrinoDelegatedKerberosAuthenticationStrategyRuntime(String serverPrincipal, String kerberosRemoteServiceName, Boolean kerberosUseCanonicalHostname)
    {
        this.serverPrincipal = serverPrincipal;
        this.kerberosRemoteServiceName = kerberosRemoteServiceName;
        this.kerberosUseCanonicalHostname = kerberosUseCanonicalHostname;
    }

    @Override
    public Connection getConnectionImpl(DataSourceWithStatistics ds, Identity identity)
            throws ConnectionException
    {
        Optional<LegendKerberosCredential> kerberosHolder = identity.getCredential(LegendKerberosCredential.class);
        if (!kerberosHolder.isPresent())
        {
            throw new UnsupportedOperationException("Expected Kerberos credential was not found");
        }
        Properties properties = ds.getProperties();
        LegendKerberosCredential legendKerberosCredential = this.resolveCredential(properties);
        return getConnectionUsingKerberos(ds.getDataSource(), getSubjectWithSinglePrivateCredential(legendKerberosCredential));
    }

    /**
     * Trino client expects private credentials of type KerberosTicket should be one
     * So here we are checking for multiple kerberos private credentials and if found
     * then log and keep only single credential.
     * @param kerberosCredential
     * @return
     */
    private Subject getSubjectWithSinglePrivateCredential(LegendKerberosCredential kerberosCredential)
    {
        Set<KerberosTicket> credentials = kerberosCredential.getSubject().getPrivateCredentials(KerberosTicket.class);

        // If only one credential found or invalid credential then don't do anything
        if (!kerberosCredential.isValid() || credentials.size() <= 1)
        {
            return kerberosCredential.getSubject();
        }

        // In case of multiple credentials then log it
        LOGGER.info("Kerberos Subject with multiple private credentials found");
        logMultipleKerberosEntries(credentials);

        // Re-create the subject with single credential
        Set<Object> credentialsWithSingleKerberosPrivateCredentials = kerberosCredential.getSubject().getPrivateCredentials().stream()
                .filter(x -> !(x instanceof KerberosTicket))
                .collect(Collectors.toSet());
        KerberosTicket kerberosPrivateCredential = credentials.stream().filter(x -> isValidKerberosTGTPrincipal(x.getServer())).findFirst().get();
        credentialsWithSingleKerberosPrivateCredentials.add(kerberosPrivateCredential);
        return new Subject(kerberosCredential.getSubject().isReadOnly(), kerberosCredential.getSubject().getPrincipals(), kerberosCredential.getSubject().getPublicCredentials(),credentialsWithSingleKerberosPrivateCredentials);
    }

    private boolean isValidKerberosTGTPrincipal(KerberosPrincipal principal) 
    {
        if (principal == null) 
        {
            return false;
        }
        if (principal.getName().equals("krbtgt/" + principal.getRealm() + "@" + principal.getRealm())) 
        {
            return true;
        }
        return false;
    }
    
    private static void logMultipleKerberosEntries(Set<KerberosTicket> credentials)
    {
        credentials.stream()
                .filter(x -> x != null)
                .forEach(x -> LOGGER.info("Multiple kerberos credentials found. server : {}, client: {}, startTime: {}, endTime: {}, renewTime: {}", x.getServer(), x.getClient(), x.getStartTime(), x.getEndTime(), x.getAuthTime(), x.getRenewTill())
                );
    }

    private LegendKerberosCredential resolveCredential(Properties properties)
    {
        IdentityState identityState = ConnectionStateManager.getInstance().getIdentityStateUsing(properties);
        if (identityState.getCredentialSupplier().isPresent())
        {
            return (LegendKerberosCredential) super.getDatabaseCredential(identityState);
        }
        else
        {
            return identityState.getIdentity().getCredential(LegendKerberosCredential.class).get();
        }
    }

    @Override
    public Pair<String, Properties> handleConnection(String url, Properties properties, DatabaseManager databaseManager)
    {
        properties.setProperty("KerberosRemoteServiceName", this.kerberosRemoteServiceName);
        properties.setProperty("KerberosUseCanonicalHostname", String.valueOf(this.kerberosUseCanonicalHostname));
        properties.setProperty("KerberosDelegation", "true");

        return Tuples.pair(url, properties);
    }

    @Override
    public AuthenticationStrategyKey getKey()
    {
        return new TrinoDelegatedKerberosAuthenticationStrategyKey(this.serverPrincipal, this.kerberosRemoteServiceName, this.kerberosUseCanonicalHostname);
    }

    public String getServerPrincipal()
    {
        return serverPrincipal;
    }

    public String getKerberosRemoteServiceName()
    {
        return kerberosRemoteServiceName;
    }

    public Boolean getKerberosUseCanonicalHostname()
    {
        return kerberosUseCanonicalHostname;
    }
}
