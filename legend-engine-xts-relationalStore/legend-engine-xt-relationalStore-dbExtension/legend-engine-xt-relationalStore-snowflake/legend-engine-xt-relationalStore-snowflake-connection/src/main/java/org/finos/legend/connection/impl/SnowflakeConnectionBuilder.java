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

import org.finos.legend.connection.Authenticator;
import org.finos.legend.connection.DatabaseType;
import org.finos.legend.connection.JDBCConnectionBuilder;
import org.finos.legend.connection.RelationalDatabaseStoreSupport;
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.connection.protocol.SnowflakeConnectionSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PrivateKeyCredential;

import java.sql.Connection;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import static org.finos.legend.connection.impl.SnowflakeDatabaseManager.*;

public class SnowflakeConnectionBuilder
{
    public static class WithKeyPair extends JDBCConnectionBuilder<PrivateKeyCredential, SnowflakeConnectionSpecification>
    {
        @Override
        public Connection getConnection(SnowflakeConnectionSpecification connectionSpecification, Authenticator<PrivateKeyCredential> authenticator, Identity identity) throws Exception
        {
            StoreInstance storeInstance = authenticator.getStoreInstance();
            RelationalDatabaseStoreSupport.cast(storeInstance.getStoreSupport(), DatabaseType.SNOWFLAKE);

            Properties connectionProperties = generateJDBCConnectionProperties(connectionSpecification);
            Function<Credential, Properties> authenticationPropertiesSupplier = cred ->
            {
                PrivateKeyCredential credential = (PrivateKeyCredential) cred;
                Properties properties = new Properties();
                properties.put("privateKey", credential.getPrivateKey());
                properties.put("user", credential.getUser());
                return properties;
            };

            return this.getConnectionManager().getConnection(DatabaseType.SNOWFLAKE, null, 0, connectionSpecification.databaseName, connectionProperties, this.getConnectionPoolConfig(), authenticationPropertiesSupplier, authenticator, identity);
        }
    }

    public static Properties generateJDBCConnectionProperties(SnowflakeConnectionSpecification connectionSpecification)
    {
        Properties properties = new Properties();
        // TODO: @akphi - handle quoted identifiers
        // this is a setting users can control when creating the database connection, we probably don't
        // want to do this when the database is configured as part of the system
        boolean quoteIdentifiers = false;
        String warehouseName = processIdentifier(connectionSpecification.warehouseName, quoteIdentifiers);
        String databaseName = processIdentifier(connectionSpecification.databaseName, quoteIdentifiers);
        properties.put(SNOWFLAKE_ROLE, processIdentifier(connectionSpecification.role, quoteIdentifiers));

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

        setNullableProperty(properties, SNOWFLAKE_ACCOUNT_TYPE_NAME, connectionSpecification.accountType);
        setNullableProperty(properties, SNOWFLAKE_ORGANIZATION_NAME, connectionSpecification.organization);
        setNullableProperty(properties, SNOWFLAKE_PROXY_HOST, connectionSpecification.proxyHost);
        setNullableProperty(properties, SNOWFLAKE_PROXY_PORT, connectionSpecification.proxyPort);
        setNullableProperty(properties, SNOWFLAKE_NON_PROXY_HOSTS, connectionSpecification.nonProxyHosts);
        properties.put(SNOWFLAKE_USE_PROXY, properties.get(SNOWFLAKE_PROXY_HOST) != null);

        return properties;
    }

    private static void setNullableProperty(Properties properties, String key, Object value)
    {
        Optional.ofNullable(value).ifPresent(x -> properties.put(key, value));
    }

    public static String processIdentifier(String identifier, boolean quoteIdentifiers)
    {
        if (quoteIdentifiers && identifier != null && !(identifier.startsWith("\"") && identifier.endsWith("\"")))
        {
            identifier = "\"" + identifier + "\"";
        }
        return identifier;
    }
}
