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

package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.DelegatedKerberosAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.IdentityState;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;

import java.sql.Connection;
import java.util.Optional;
import java.util.Properties;

public class DelegatedKerberosAuthenticationStrategyRuntime extends InteractiveAuthenticationStrategyRuntime
{
    private final String serverPrincipal;

    public DelegatedKerberosAuthenticationStrategyRuntime(String serverPrincipal)
    {
        this.serverPrincipal = serverPrincipal;
    }

    public DelegatedKerberosAuthenticationStrategyRuntime()
    {
        this(null);
    }

    @Override
    public Connection getConnectionImpl(DataSourceWithStatistics ds, Identity identity) throws ConnectionException
    {
        Optional<LegendKerberosCredential> kerberosHolder = identity.getCredential(LegendKerberosCredential.class);
        if (!kerberosHolder.isPresent())
        {
            throw new UnsupportedOperationException("Expected Kerberos credential was not found");
        }
        Properties properties = ds.getProperties();
        LegendKerberosCredential legendKerberosCredential = this.resolveCredential(properties);
        return getConnectionUsingKerberos(ds.getDataSource(), legendKerberosCredential.getSubject());
    }

    @Override
    public Pair<String, Properties> handleConnection(String url, Properties properties, DatabaseManager databaseManager)
    {
        return Tuples.pair(url, properties);
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
    public DelegatedKerberosAuthenticationStrategyKey getKey()
    {
        return new DelegatedKerberosAuthenticationStrategyKey(this.serverPrincipal);
    }

    public String getServerPrincipal()
    {
        return serverPrincipal;
    }
}
