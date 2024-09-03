// Copyright 2024 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.relational.authentication.strategy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import org.finos.legend.engine.plan.execution.stores.relational.authentication.strategy.key.DuckDBS3AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.vault.Vault;

public class DuckDBS3AuthenticationStrategy extends AuthenticationStrategy
{
    private final DuckDBS3AuthenticationStrategyKey key;

    public DuckDBS3AuthenticationStrategy(DuckDBS3AuthenticationStrategyKey key)
    {
        this.key = key;
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

    @Override
    public Properties getAuthenticationPropertiesForConnection()
    {
        String secretKey = Objects.requireNonNull(
                Vault.INSTANCE.getValue(this.key.getSecretAccessKeyVaultReference()),
                () -> "Secret reference not found: " + this.key.getSecretAccessKeyVaultReference()
        );

        Properties properties = super.getAuthenticationPropertiesForConnection();
        Optional<String> endpoint = Optional.ofNullable(this.key.getEndpoint());
        String s3Secret = "INSTALL iceberg;\n" +
                "LOAD iceberg;\n" +
                "CREATE OR REPLACE SECRET s3_" + this.key.getAccessKeyId() + " (" +
                "TYPE S3, " +
                "KEY_ID '" + this.key.getAccessKeyId() + "', " +
                "SECRET '" + secretKey + "', " +
                "REGION '" + this.key.getRegion() + "'" +
                ", URL_STYLE 'path'" + // todo this is to make MiniIO work, as VHost requires /etc/host changes...
                endpoint.map(x -> x.substring(x.indexOf("://") + 3)).map(x -> ", ENDPOINT '" + x + "'").orElse("") +
                endpoint.filter(x -> x.startsWith("http:")).map(x -> ", USE_SSL false").orElse("") +
                ");";
        properties.setProperty("connectionInitSql", s3Secret);
        return properties;
    }

    @Override
    public AuthenticationStrategyKey getKey()
    {
        return this.key;
    }
}