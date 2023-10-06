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

package org.finos.legend.connection.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.connection.ConnectionManager;
import org.finos.legend.connection.Database;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.ConnectionSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class JDBCConnectionManager implements ConnectionManager
{
    private static final ConcurrentHashMap<String, DatabaseManager> managerByName = ConcurrentHashMap.newMap();
    private static final AtomicBoolean isInitialized = new AtomicBoolean();

    private static void detectManagers()
    {
        if (!isInitialized.get())
        {
            synchronized (isInitialized)
            {
                if (!isInitialized.get())
                {
                    for (DatabaseManager manager : ServiceLoader.load(DatabaseManager.class))
                    {
                        manager.getIds().forEach(i -> managerByName.put(i, manager));
                    }
                    isInitialized.getAndSet(true);
                }
            }
        }
    }

    @Override
    public void initialize()
    {
        JDBCConnectionManager.detectManagers();
    }

    public static Connection getConnection(Database database, String host, int port, String databaseName, Identity identity, ConnectionSpecification connectionSpecification, AuthenticationConfiguration authenticationConfiguration, Properties properties) throws SQLException
    {
        // TODO: connection pooling
        // we might need to account for things like pooling and things
        DatabaseManager databaseManager = getManagerForDatabase(database);
        String jdbcUrl = databaseManager.buildURL(host, port, databaseName, properties);

        String poolName = getPoolName(identity, connectionSpecification, authenticationConfiguration);

        //        Properties properties = new Properties();
        //        String poolName = poolNameFor(identity);
        //        properties.putAll(this.databaseManager.getExtraDataSourceProperties(this.authenticationStrategy, identity));
        //        properties.putAll(this.extraDatasourceProperties);

        //        properties.put(AuthenticationStrategy.AUTHENTICATION_STRATEGY_KEY, this.authenticationStrategy.getKey().shortId());
        //        properties.put(ConnectionStateManager.POOL_NAME_KEY, poolName);
        //        properties.putAll(authenticationStrategy.getAuthenticationPropertiesForConnection());
        HikariConfig jdbcConfig = new HikariConfig();
        jdbcConfig.setDriverClassName(databaseManager.getDriver());
        jdbcConfig.setPoolName(poolName);
        jdbcConfig.setJdbcUrl(jdbcUrl);

        // TODO: @akphi - should we allow these to be configured per connection spec, or it's something people can configure extra to override the provided
        // values from connection specification, feels like this should be per connection builder instead
//        jdbcConfig.setMaximumPoolSize(maxPoolSize);
//        jdbcConfig.setMinimumIdle(minPoolSize);
//        jdbcConfig.setConnectionTimeout(connectionTimeout);

        // specific system configuration to disable database prepared statements
        // TODO?: add docs to explain this
        jdbcConfig.addDataSourceProperty("cachePrepStmts", false);
        jdbcConfig.addDataSourceProperty("prepStmtCacheSize", 0);
        jdbcConfig.addDataSourceProperty("prepStmtCacheSqlLimit", 0);
        jdbcConfig.addDataSourceProperty("useServerPrepStmts", false);

        jdbcConfig.setDataSource(new InternalDataSource(jdbcUrl, properties, databaseManager.getDriver()));

        return new HikariDataSource(jdbcConfig).getConnection();
    }

    private static String getPoolName(Identity identity, ConnectionSpecification connectionSpecification, AuthenticationConfiguration authenticationConfiguration)
    {
        return String.format("DBPool_%s_%s_%s_%s",
                connectionSpecification.shortId(),
                authenticationConfiguration.shortId(),
                identity.getName(),
                identity.getFirstCredential().getClass().getCanonicalName()
        );
    }

    private static DatabaseManager getManagerForDatabase(Database database)
    {
        if (!isInitialized.get())
        {
            throw new IllegalStateException("JDBC connection manager has not been configured properly");
        }
        DatabaseManager manager = managerByName.get(database.getLabel());
        if (manager == null)
        {
            throw new RuntimeException(String.format("Can't find matching manager for database type '%s'", database));
        }
        return manager;
    }

    private static class InternalDataSource implements DataSource
    {
        private final String url;
        private final Properties properties;
        private final Driver driver;

        public InternalDataSource(String url, Properties properties, String driverClassName)
        {
            this.url = url;
            this.properties = properties;
            try
            {
                this.driver = (Driver) Class.forName(driverClassName).getDeclaredConstructor().newInstance();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Connection getConnection() throws SQLException
        {
            // TODO: @akphi - add logging and statistics like in execution DriverWrapper
            return driver.connect(this.url, this.properties);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException
        {
            throw new RuntimeException();
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException
        {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException
        {
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException
        {
        }

        @Override
        public int getLoginTimeout() throws SQLException
        {
            return 0;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException
        {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException
        {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException
        {
            return null;
        }
    }
}
