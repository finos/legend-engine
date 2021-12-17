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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zaxxer.hikari.HikariDataSource;
import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.IdentityState;
import org.finos.legend.engine.shared.core.identity.Identity;

import javax.sql.DataSource;
import java.util.Optional;

public class DataSourceWithStatistics
{
    private final String poolName;
    private final DataSourceStatistics statistics;
    private final DataSource dataSource;
    private final IdentityState identityState;
    private final DataSourceSpecification dataSourceSpecification;

    public DataSourceWithStatistics(String poolName, DataSource dataSource, IdentityState identityState, DataSourceSpecification dataSourceSpecification)
    {
        this.poolName = poolName;
        this.statistics = new DataSourceStatistics();
        this.dataSource = dataSource;
        if (identityState == null)
        {
            throw new IllegalArgumentException("identity state cannot be null for " + poolName);
        }
        this.identityState = identityState;

        this.dataSourceSpecification = dataSourceSpecification;
    }

    public DataSourceWithStatistics(String poolName, IdentityState identityState, DataSourceSpecification dataSourceSpecification)
    {
        this(poolName, null, identityState, dataSourceSpecification);
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public DataSourceStatistics getStatistics()
    {
        return statistics;
    }

    public int requestConnection()
    {
        return this.statistics.requestConnection();
    }

    @JsonIgnore
    public String getPoolName()
    {
        return poolName;
    }

    public String getPoolPrincipal()
    {
        return this.identityState.getIdentity().getName();
    }

    public IdentityState getIdentityState()
    {
        return identityState;
    }

    public void close()
    {
        try
        {
            ((HikariDataSource)this.dataSource).close();
        }
        catch (Exception e)
        {
        }
    }

    public Identity getIdentity()
    {
        return identityState.getIdentity();
    }

    public Optional<CredentialSupplier> getCredentialSupplier()
    {
        return identityState.getCredentialSupplier();
    }

    public AuthenticationStrategy getAuthenticationStrategy()
    {
        return dataSourceSpecification.getAuthenticationStrategy();
    }

    public DatabaseManager getDatabaseManager()
    {
        return dataSourceSpecification.getDatabaseManager();
    }

    public int buildConnection()
    {
        return statistics.buildConnection();
    }

    public void logConnectionError() {
        this.statistics.logConnectionError();
    }

    public ConnectionKey getConnectionKey()
    {
        return dataSourceSpecification.getConnectionKey();
    }

    public DataSourceSpecification getDataSourceSpecification()
    {
        return dataSourceSpecification;
    }

}


