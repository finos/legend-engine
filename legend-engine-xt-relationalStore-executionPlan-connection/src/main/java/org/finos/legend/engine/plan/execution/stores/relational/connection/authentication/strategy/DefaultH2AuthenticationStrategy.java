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
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.DefaultH2AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.IdentityState;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class DefaultH2AuthenticationStrategy extends AuthenticationStrategy
{
    private static final String SA_USER = "sa";
    private static final String SA_PASSWORD = "";
    private static final List<String> LEGEND_H2_EXTENSION_SQLs = getLegendH2ExtensionSQLs();

    @Override
    public Connection getConnectionImpl(DataSourceWithStatistics ds, Identity identity) throws ConnectionException
    {
        try
        {
            Connection connection = ds.getDataSource().getConnection();
            for (String sql : LEGEND_H2_EXTENSION_SQLs)
            {
                try (Statement statement = connection.createStatement())
                {
                    statement.execute(sql);
                }
                catch (SQLException ignored)
                {
                    // Ignored
                }
            }
            return connection;
        }
        catch (SQLException e)
        {
            throw new ConnectionException(e);
        }
    }

    public Pair<String, Properties> handleConnection(String url, Properties properties, DatabaseManager databaseManager)
    {
        PlaintextUserPasswordCredential plaintextUserPasswordCredential = this.resolveCredential(properties);
        properties.put("user", plaintextUserPasswordCredential.getUser());
        properties.put("password", plaintextUserPasswordCredential.getPassword());
        return Tuples.pair(url, properties);
    }

    /*
        Note : H2 use is not meant for production.
        As such we do not want to use idioms like connection pooling/authentication flows for H2.
        Even though the code below looks for a credential supplier obtained using a flow, it is merely meant for developer testing.

        Production flow providers should not provide a flow for H2 and the code below will simply instantiate a PlaintextUserPasswordCredential
     */
    private PlaintextUserPasswordCredential resolveCredential(Properties properties)
    {
        IdentityState identityState = ConnectionStateManager.getInstance().getIdentityStateUsing(properties);
        if (identityState == null || !identityState.getCredentialSupplier().isPresent())
        {
            return new PlaintextUserPasswordCredential(SA_USER, SA_PASSWORD);
        }
        return (PlaintextUserPasswordCredential) super.getDatabaseCredential(identityState);
    }

    @Override
    public AuthenticationStrategyKey getKey()
    {
        return new DefaultH2AuthenticationStrategyKey();
    }

    private static List<String> getLegendH2ExtensionSQLs()
    {
        return Arrays.asList(
                "CREATE ALIAS IF NOT EXISTS legend_h2_extension_json_navigate FOR \"org.finos.legend.engine.plan.execution.stores.relational.LegendH2Extensions.legend_h2_extension_json_navigate\";",
                "CREATE ALIAS IF NOT EXISTS legend_h2_extension_json_parse FOR \"org.finos.legend.engine.plan.execution.stores.relational.LegendH2Extensions.legend_h2_extension_json_parse\";",
                "CREATE ALIAS IF NOT EXISTS legend_h2_extension_base64_decode FOR \"org.finos.legend.engine.plan.execution.stores.relational.LegendH2Extensions.legend_h2_extension_base64_decode\";",
                "CREATE ALIAS IF NOT EXISTS legend_h2_extension_base64_encode FOR \"org.finos.legend.engine.plan.execution.stores.relational.LegendH2Extensions.legend_h2_extension_base64_encode\";"
        );
    }
}
