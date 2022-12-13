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
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.GCPApplicationDefaultCredentialsAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/*
    This class represents authentication using GCP ADC (Application Default Credentials).
    Basically, this means that when Legend executes in a GCP environment (like GKE), GCP injects credentials for a service account in the environment.
    BigQuery client libraries and drivers detect this credentials and use them when connecting to the database.

   See https://cloud.google.com/docs/authentication/production
   See BigQueryManager.java
 */
public class GCPApplicationDefaultCredentialsAuthenticationStrategyRuntime extends AuthenticationStrategyRuntime
{
    public GCPApplicationDefaultCredentialsAuthenticationStrategyRuntime()
    {
    }

    @Override
    public Connection getConnectionImpl(DataSourceWithStatistics ds, Identity identity) throws ConnectionException
    {
        try
        {
            return ds.getDataSource().getConnection();
        }
        catch (SQLException e)
        {
            throw new ConnectionException(e);
        }
    }

    public Pair<String, Properties> handleConnection(String url, Properties properties, DatabaseManager databaseManager)
    {
        Properties connectionProperties = new Properties();
        connectionProperties.putAll(properties);
        connectionProperties.put("OAuthType", "3");
        return Tuples.pair(url, connectionProperties);
    }

    @Override
    public AuthenticationStrategyKey getKey()
    {
        return new GCPApplicationDefaultCredentialsAuthenticationStrategyKey();
    }
}

