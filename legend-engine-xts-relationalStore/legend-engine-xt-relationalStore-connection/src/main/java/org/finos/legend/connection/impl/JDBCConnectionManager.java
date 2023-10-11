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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.connection.Authenticator;
import org.finos.legend.connection.ConnectionManager;
import org.finos.legend.connection.Database;
import org.finos.legend.connection.DatabaseManager;
import org.finos.legend.connection.LegendEnvironment;
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.connection.protocol.ConnectionSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class JDBCConnectionManager implements ConnectionManager
{
    private static final long HIKARICP_DEFAULT_CONNECTION_TIMEOUT = 30000L;
    private static final int HIKARICP_DEFAULT_MAX_POOL_SIZE = 100;
    private static final int HIKARICP_DEFAULT_MIN_POOL_SIZE = 0;

    private static final ConcurrentHashMap<String, DatabaseManager> managerByName = ConcurrentHashMap.newMap();
    private static final AtomicBoolean isInitialized = new AtomicBoolean();

    private static JDBCConnectionManager INSTANCE;
    private final ConcurrentMutableMap<String, ConnectionPool> poolIndex = ConcurrentHashMap.newMap();

    protected JDBCConnectionManager()
    {
        // singleton
    }

    public static synchronized JDBCConnectionManager getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new JDBCConnectionManager();
        }
        return INSTANCE;
    }

    private static void setup()
    {
        // register database managers
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
                    isInitialized.set(true);
                }
            }
        }
    }

    @Override
    public void initialize(LegendEnvironment environment)
    {
        JDBCConnectionManager.setup();
    }

    public Connection getConnection(Database database,
                                    String host,
                                    int port,
                                    String databaseName,
                                    Properties connectionProperties,
                                    ConnectionPoolConfig connectionPoolConfig,
                                    Function<Credential, Properties> authenticationPropertiesSupplier,
                                    Authenticator authenticator,
                                    Identity identity
    ) throws SQLException
    {
        StoreInstance storeInstance = authenticator.getStoreInstance();
        ConnectionSpecification connectionSpecification = storeInstance.getConnectionSpecification();
        AuthenticationConfiguration authenticationConfiguration = authenticator.getAuthenticationConfiguration();
        String poolName = getPoolName(identity, connectionSpecification, authenticationConfiguration);

        // TODO: @akphi - this is simplistic, we need to handle concurrency and errors
        Supplier<HikariDataSource> dataSourceSupplier = () -> this.buildDataSource(database, host, port, databaseName, connectionProperties, connectionPoolConfig, authenticationPropertiesSupplier, authenticator, identity);
        Function0<ConnectionPool> connectionPoolSupplier = () -> new ConnectionPool(dataSourceSupplier.get());
        ConnectionPool connectionPool = this.poolIndex.getIfAbsentPut(poolName, connectionPoolSupplier);

        return connectionPool.dataSource.getConnection();

//        try (Scope scope = GlobalTracer.get().buildSpan("Get Connection").startActive(true))
//        {
//            ConnectionKey connectionKey = this.getConnectionKey();
//            // Logs and traces -----
//            String principal = identityState.getIdentity().getName();
//            scope.span().setTag("Principal", principal);
//            scope.span().setTag("DataSourceSpecification", this.toString());
//            LOGGER.info("Get Connection as [{}] for datasource [{}]", principal, connectionKey.shortId());
//            // ---------------------
//            try
//            {
//                DataSourceWithStatistics dataSourceWithStatistics = this.connectionStateManager.getDataSourceForIdentityIfAbsentBuild(identityState, this, dataSourcePoolBuilder);
//                // Logs and traces and stats -----
//                String poolName = dataSourceWithStatistics.getPoolName();
//                scope.span().setTag("Pool", poolName);
//                int requests = dataSourceWithStatistics.requestConnection();
//                LOGGER.info("Principal [{}] has requested [{}] connections for pool [{}]", principal, requests, poolName);
//                return authenticationStrategy.getConnection(dataSourceWithStatistics, identityState.getIdentity());
//            }
//            catch (ConnectionException ce)
//            {
//                LOGGER.error("ConnectionException  {{}} : pool stats [{}] ", principal, connectionStateManager.getPoolStatisticsAsJSON(poolNameFor(identityState.getIdentity())));
//                LOGGER.error("ConnectionException ", ce);
//                throw ce;
//            }
//        }
    }

    protected HikariDataSource buildDataSource(
            Database database,
            String host,
            int port,
            String databaseName,
            Properties connectionProperties,
            ConnectionPoolConfig connectionPoolConfig,
            Function<Credential, Properties> authenticationPropertiesSupplier,
            Authenticator authenticator,
            Identity identity
    )
    {
        StoreInstance storeInstance = authenticator.getStoreInstance();
        ConnectionSpecification connectionSpecification = storeInstance.getConnectionSpecification();
        AuthenticationConfiguration authenticationConfiguration = authenticator.getAuthenticationConfiguration();
        DatabaseManager databaseManager = getManagerForDatabase(database);

        String jdbcUrl = databaseManager.buildURL(host, port, databaseName, connectionProperties);
        String poolName = getPoolName(identity, connectionSpecification, authenticationConfiguration);

        HikariConfig jdbcConfig = new HikariConfig();
        jdbcConfig.setDriverClassName(databaseManager.getDriver());
        jdbcConfig.setPoolName(poolName);
        jdbcConfig.setJdbcUrl(jdbcUrl);

        // NOTE: we could allow more granularity by allow specifying these pooling configurations at specification level
        jdbcConfig.setMinimumIdle(connectionPoolConfig != null && connectionPoolConfig.getMinPoolSize() != null ? connectionPoolConfig.getMinPoolSize() : HIKARICP_DEFAULT_MIN_POOL_SIZE);
        jdbcConfig.setMaximumPoolSize(connectionPoolConfig != null && connectionPoolConfig.getMaxPoolSize() != null ? connectionPoolConfig.getMaxPoolSize() : HIKARICP_DEFAULT_MAX_POOL_SIZE);
        jdbcConfig.setConnectionTimeout(connectionPoolConfig != null && connectionPoolConfig.getConnectionTimeout() != null ? connectionPoolConfig.getConnectionTimeout() : HIKARICP_DEFAULT_CONNECTION_TIMEOUT);

        // specific system configuration to disable statement cache for all databases
        // TODO: @akphi - document why we need to do this, check with @pierredebelen, @kevin-m-knight-gs, @epsstan
        // See https://github.com/brettwooldridge/HikariCP#statement-cache
        jdbcConfig.addDataSourceProperty("cachePrepStmts", false);
        jdbcConfig.addDataSourceProperty("prepStmtCacheSize", 0);
        jdbcConfig.addDataSourceProperty("prepStmtCacheSqlLimit", 0);
        jdbcConfig.addDataSourceProperty("useServerPrepStmts", false);

        jdbcConfig.setDataSource(new DataSourceWrapper(jdbcUrl, connectionProperties, databaseManager, authenticationPropertiesSupplier, authenticator, identity));
        return new HikariDataSource(jdbcConfig);
    }

    public ConnectionPool getPool(String poolName)
    {
        return this.poolIndex.get(poolName);
    }

    public int getPoolSize()
    {
        return this.poolIndex.size();
    }

    public void flushPool()
    {
        this.poolIndex.forEachKey(this.poolIndex::remove);
    }

    public static String getPoolName(Identity identity, ConnectionSpecification connectionSpecification, AuthenticationConfiguration authenticationConfiguration)
    {
        return String.format("DBPool|%s|%s|%s|%s",
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
            throw new RuntimeException(String.format("Can't find any matching managers for database type '%s'", database.getLabel()));
        }
        return manager;
    }

    public static class ConnectionPoolConfig
    {
        private final Integer minPoolSize;
        private final Integer maxPoolSize;
        private final Long connectionTimeout;

        private ConnectionPoolConfig(Integer minPoolSize, Integer maxPoolSize, Long connectionTimeout)
        {
            this.minPoolSize = minPoolSize;
            this.maxPoolSize = maxPoolSize;
            this.connectionTimeout = connectionTimeout;
        }

        public Integer getMinPoolSize()
        {
            return minPoolSize;
        }

        public Integer getMaxPoolSize()
        {
            return maxPoolSize;
        }

        public Long getConnectionTimeout()
        {
            return connectionTimeout;
        }

        public static class Builder
        {
            private Integer minPoolSize = null;
            private Integer maxPoolSize = null;
            private Long connectionTimeout = null;

            public Builder withMinPoolSize(Integer minPoolSize)
            {
                this.minPoolSize = minPoolSize;
                return this;
            }

            public Builder withMaxPoolSize(Integer maxPoolSize)
            {
                this.maxPoolSize = maxPoolSize;
                return this;
            }

            public Builder withConnectionTimeout(Long connectionTimeout)
            {
                this.connectionTimeout = connectionTimeout;
                return this;
            }

            public ConnectionPoolConfig build()
            {
                return new ConnectionPoolConfig(this.minPoolSize, this.maxPoolSize, this.connectionTimeout);
            }
        }
    }

    public static class ConnectionPool
    {
        private final HikariDataSource dataSource;

        public ConnectionPool(HikariDataSource dataSource)
        {
            this.dataSource = Objects.requireNonNull(dataSource);
        }

        public String getPoolName()
        {
            return this.dataSource.getPoolName();
        }

        public int getActiveConnections()
        {
            return this.dataSource.getHikariPoolMXBean().getActiveConnections();
        }

        public int getTotalConnections()
        {
            return this.dataSource.getHikariPoolMXBean().getTotalConnections();
        }

        public int getIdleConnections()
        {
            return this.dataSource.getHikariPoolMXBean().getIdleConnections();
        }

        public Properties getProperties()
        {
            return this.dataSource.getDataSourceProperties();
        }
    }

    private static class DataSourceWrapper implements DataSource
    {
        private final String url;
        private final Properties connectionProperties;
        private final Function<Credential, Properties> authenticationPropertiesSupplier;
        private final Authenticator authenticator;
        // TODO: @akphi - how do we get rid of this here?
        private final Identity identity;

        private final Driver driver;

        public DataSourceWrapper(
                String url, Properties connectionProperties,
                DatabaseManager databaseManager,
                Function<Credential, Properties> authenticationPropertiesSupplier,
                Authenticator authenticator,
                Identity identity
        )
        {
            this.url = url;
            this.connectionProperties = connectionProperties;
            try
            {
                this.driver = (Driver) Class.forName(databaseManager.getDriver()).getDeclaredConstructor().newInstance();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            this.authenticationPropertiesSupplier = authenticationPropertiesSupplier;
            this.authenticator = authenticator;
            this.identity = identity;
        }

        @Override
        public Connection getConnection() throws SQLException
        {
            Properties properties = new Properties();
            properties.putAll(this.connectionProperties);
            Credential credential;
            try
            {
                credential = authenticator.makeCredential(identity);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            Properties authenticationProperties = authenticationPropertiesSupplier.apply(credential);
            properties.putAll(authenticationProperties);
            // TODO: @akphi - prune unnecessary properties using DatabaseManager
            // TODO: @akphi - add logging and statistics like in execution DriverWrapper
            return driver.connect(this.url, properties);
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
