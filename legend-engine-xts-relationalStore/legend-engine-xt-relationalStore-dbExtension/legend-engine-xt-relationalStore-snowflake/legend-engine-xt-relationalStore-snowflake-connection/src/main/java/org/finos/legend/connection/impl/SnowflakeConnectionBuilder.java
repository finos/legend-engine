// Copyright 2023 Goldman Sachs
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

package org.finos.legend.connection.impl;

import org.finos.legend.connection.ConnectionBuilder;
import org.finos.legend.connection.DatabaseType;
import org.finos.legend.connection.RelationalDatabaseStoreSupport;
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.connection.StoreSupport;
import org.finos.legend.connection.jdbc.JDBCConnectionManager;
import org.finos.legend.connection.jdbc.driver.JDBCConnectionDriver;
import org.finos.legend.connection.protocol.ConnectionSpecification;
import org.finos.legend.connection.protocol.SnowflakeConnectionSpecification;
import org.finos.legend.engine.shared.core.identity.credential.PrivateKeyCredential;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;
import java.util.Properties;

import static org.finos.legend.connection.jdbc.driver.Snowflake_JDBCConnectionDriver.*;

public class SnowflakeConnectionBuilder
{
    public static class WithKeyPair extends ConnectionBuilder<Connection, PrivateKeyCredential, SnowflakeConnectionSpecification>
    {
        @Override
        public Connection getConnection(StoreInstance storeInstance, PrivateKeyCredential credential) throws Exception
        {
            SnowflakeConnectionSpecification connectionSpecification = this.getCompatibleConnectionSpecification(storeInstance);
            StoreSupport storeSupport = storeInstance.getStoreSupport();
            if (!(storeSupport instanceof RelationalDatabaseStoreSupport) || !DatabaseType.SNOWFLAKE.equals(((RelationalDatabaseStoreSupport) storeSupport).getDatabase()))
            {
                throw new RuntimeException("Can't get connection: only support Snowflake databases");
            }
            JDBCConnectionDriver driver = JDBCConnectionManager.getDriverForDatabase(DatabaseType.SNOWFLAKE);
            Properties properties = collectExtraSnowflakeConnectionProperties(connectionSpecification);
            properties.put("privateKey", credential.getPrivateKey());
            properties.put("user", credential.getUser());

            return DriverManager.getConnection(driver.buildURL(null, 0, connectionSpecification.databaseName, properties), properties);
        }
    }

    private static Properties collectExtraSnowflakeConnectionProperties(SnowflakeConnectionSpecification connectionSpecification)
    {
        Properties properties = new Properties();
        // TODO: @akphi - handle quoted identifiers
        // this is a setting users can control when creating the database connection, we probably don't
        // want to do this when the database is configured as part of the system
        boolean quoteIdentifiers = false;
        String warehouseName = updateSnowflakeIdentifiers(connectionSpecification.warehouseName, quoteIdentifiers);
        String databaseName = updateSnowflakeIdentifiers(connectionSpecification.databaseName, quoteIdentifiers);
        properties.put(SNOWFLAKE_ROLE, updateSnowflakeIdentifiers(connectionSpecification.role, quoteIdentifiers));

        properties.put(SNOWFLAKE_ACCOUNT_NAME, connectionSpecification.accountName);
        properties.put(SNOWFLAKE_REGION, connectionSpecification.region);
        properties.put(SNOWFLAKE_WAREHOUSE_NAME, warehouseName);
        properties.put(SNOWFLAKE_DATABASE_NAME, databaseName);
        properties.put(SNOWFLAKE_CLOUD_TYPE, connectionSpecification.cloudType);
        properties.put(SNOWFLAKE_QUOTE_IDENTIFIERS, quoteIdentifiers);
        properties.put(SNOWFLAKE_ENABLE_QUERY_TAGS, connectionSpecification.enableQueryTags != null && connectionSpecification.enableQueryTags);

        properties.put("account", connectionSpecification.accountName);
        properties.put("warehouse", warehouseName);
        properties.put("db", databaseName);
        properties.put("ocspFailOpen", true);

        setProperty(properties, SNOWFLAKE_ACCOUNT_TYPE_NAME, connectionSpecification.accountType);
        setProperty(properties, SNOWFLAKE_ORGANIZATION_NAME, connectionSpecification.organization);
        setProperty(properties, SNOWFLAKE_PROXY_HOST, connectionSpecification.proxyHost);
        setProperty(properties, SNOWFLAKE_PROXY_PORT, connectionSpecification.proxyPort);
        setProperty(properties, SNOWFLAKE_NON_PROXY_HOSTS, connectionSpecification.nonProxyHosts);
        properties.put(SNOWFLAKE_USE_PROXY, properties.get(SNOWFLAKE_PROXY_HOST) != null);

        return properties;
    }

    private static void setProperty(Properties properties, String key, Object value)
    {
        Optional.ofNullable(value).ifPresent(x -> properties.put(key, value));
    }

    private static String updateSnowflakeIdentifiers(String identifier, boolean quoteIdentifiers)
    {
        if (quoteIdentifiers && identifier != null && !(identifier.startsWith("\"") && identifier.endsWith("\"")))
        {
            identifier = "\"" + identifier + "\"";
        }
        return identifier;
    }
}
