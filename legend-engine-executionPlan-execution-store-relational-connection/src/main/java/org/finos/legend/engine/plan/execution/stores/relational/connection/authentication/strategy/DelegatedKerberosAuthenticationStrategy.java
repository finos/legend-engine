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

import java.sql.Connection;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.DelegatedKerberosAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;

public class DelegatedKerberosAuthenticationStrategy extends InteractiveAuthenticationStrategy
{
    private final String serverPrincipal;

    public DelegatedKerberosAuthenticationStrategy(String serverPrincipal)
    {
        this.serverPrincipal = serverPrincipal;
    }

    public DelegatedKerberosAuthenticationStrategy()
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
        return getConnectionUsingKerberos(ds.getDataSource(), kerberosHolder.get().getSubject());
    }

    @Override
    public Pair<String, Properties> handleConnection(String url, Properties properties, DatabaseManager databaseManager)
    {
        return Tuples.pair(url, properties);
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
