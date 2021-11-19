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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.KerberosUtils;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;

import javax.security.auth.Subject;
import javax.sql.DataSource;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.util.Optional;
import java.util.Properties;

import static java.util.concurrent.TimeUnit.SECONDS;

public abstract class DataSourceSpecification
{
    public static MetricRegistry METRIC_REGISTRY;

    public static void setMetricRegistry(MetricRegistry metricRegistry)
    {
        METRIC_REGISTRY = metricRegistry;
    }

    public static final String HIKARICP_HOUSEKEEPING_PERIOD_MS = "com.zaxxer.hikari.housekeeping.periodMs";

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataSourceSpecification.class);

    // HikariCP Parameters
    protected static final int HIKARICP_MAX_POOL_SIZE = 100;
    protected static final int HIKARICP_MIN_IDLE = 0;
    //default is 30 seconds
    protected static final long HIKARICP_HOUSEKEEPER_FREQ_IN_MS = SECONDS.toMillis(30L);

    static
    {  //house keeper frequency can only be altered via system property and will affect all pools!!
        System.setProperty(HIKARICP_HOUSEKEEPING_PERIOD_MS, String.valueOf(Long.getLong(HIKARICP_HOUSEKEEPING_PERIOD_MS, HIKARICP_HOUSEKEEPER_FREQ_IN_MS)));
    }

    protected org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey datasourceKey;
    private DatabaseManager databaseManager;
    private AuthenticationStrategy authenticationStrategy;
    protected Properties extraDatasourceProperties = new Properties();

    private KeyLockManager<String> keyLockManager = KeyLockManager.newManager();
    private ConcurrentMutableMap<String, org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics> connectionPoolByUser = ConcurrentHashMap.newMap();

    private org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationStatistics dataSourceSpecificationStatistics = new org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationStatistics();

    public static String DATASOURCE_SPEC_INSTANCE = "DATASOURCE_SPEC_INSTANCE";
    private static final ConcurrentMutableMap<String, DataSourceSpecification> dataSourceSpecifications = ConcurrentHashMap.newMap();


    protected DataSourceSpecification(org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties, RelationalExecutorInfo relationalExecutorInfo)
    {
        this.datasourceKey = key;
        this.databaseManager = databaseManager;
        this.authenticationStrategy = authenticationStrategy;
        this.extraDatasourceProperties.putAll(extraUserProperties);

        synchronized (DataSourceSpecification.class)
        {
            relationalExecutorInfo.setDataSourceSpecifications(dataSourceSpecifications);
            ConnectionKey instanceKey = buildConnectionKey();
            String instanceId = instanceKey.shortId();
            dataSourceSpecifications.putIfAbsent(instanceId, this);
            this.extraDatasourceProperties.put(DATASOURCE_SPEC_INSTANCE, instanceId);
            relationalExecutorInfo.putInstanceKeyIfAbsent(instanceKey, instanceId);
        }
        MetricsHandler.observeCount("datastore specifications");
        LOGGER.info("Created new {}", this);
    }

    public ConnectionKey buildConnectionKey()
    {
        return new ConnectionKey(this.datasourceKey, this.authenticationStrategy.getKey());
    }

    public org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationStatistics getDataSourceSpecificationStatistics()
    {
        return dataSourceSpecificationStatistics;
    }

    public static DataSourceSpecification getInstance(String id)
    {
        return dataSourceSpecifications.get(id);
    }

    public AuthenticationStrategy getAuthenticationStrategy()
    {
        return authenticationStrategy;
    }

    public DatabaseManager getDatabaseManager()
    {
        return databaseManager;
    }

    @JsonProperty(value = "poolsByUser", required = true)
    public ConcurrentMutableMap<String, org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics> getConnectionPoolByUser()
    {
        return this.connectionPoolByUser;
    }

    public Connection getConnectionUsingProfiles(MutableList<CommonProfile> profiles)
    {
         Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(profiles);
        return this.getConnectionUsingIdentity(identity, Optional.empty());
    }

    public Connection getConnectionUsingSubject(Subject subject)
    {
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(subject);
        return this.getConnectionUsingIdentity(identity, Optional.empty());
    }

    public Connection getConnectionUsingIdentity(Identity identity, Optional<CredentialSupplier> databaseCredentialSupplierHolder)
    {
        String principal = identity.getName();
        Optional<LegendKerberosCredential> kerberosCredentialHolder = identity.getCredential(LegendKerberosCredential.class);

        Function0<DataSourceWithStatistics> dataSourceBuilder;
        if (kerberosCredentialHolder.isPresent())
        {
            dataSourceBuilder = () -> new DataSourceWithStatistics(KerberosUtils.doAs(identity, (PrivilegedAction<DataSource>) () -> this.buildDataSource(identity)));
        }
        else
        {
            dataSourceBuilder = () -> new DataSourceWithStatistics(this.buildDataSource(identity));
        }

        return getConnection(
                identity,
                principal,
                databaseCredentialSupplierHolder,
                dataSourceBuilder);
    }

    public void cacheConnectionState(Identity identity, Optional<CredentialSupplier> databaseCredentialSupplier)
    {
        String poolName = poolNameFor(identity);
        ConnectionStateManager connectionStateManager = ConnectionStateManager.getInstance();
        connectionStateManager.registerState(poolName, identity, databaseCredentialSupplier);
    }

    protected Connection getConnection(Identity identity, String principal, Optional<CredentialSupplier> databaseCredentialSupplier, Function0<DataSourceWithStatistics> dataSourceBuilder)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Get Connection").startActive(true))
        {
            // Logs and traces -----
            scope.span().setTag("Principal", principal);
            scope.span().setTag("DataSourceSpecification", this.toString());
            LOGGER.info("Get Connection as [{}] for datasource [{}]", principal, this);
            LOGGER.debug("connectionPoolByUser Size {} Keys {}", connectionPoolByUser.size(), connectionPoolByUser.keySet());
            // ---------------------

            cacheConnectionState(identity, databaseCredentialSupplier);
            DataSourceWithStatistics dataSourceWithStatistics = connectionPoolByUser.get(principal);
            if (dataSourceWithStatistics == null)
            {
                synchronized (keyLockManager.getLock(identity.getName()))
                {
                    dataSourceWithStatistics = connectionPoolByUser.get(principal);
                    if (dataSourceWithStatistics == null)
                    {
                        LOGGER.info("Pool entry not found for [{}] for datasource [{}], creating one", principal, this);
                        dataSourceWithStatistics = dataSourceBuilder.value();
                        connectionPoolByUser.put(principal, dataSourceWithStatistics);
                    }
                }
            }
            try
            {
                int requests = dataSourceWithStatistics.requestConnection();
                // Logs and traces and stats -----
                scope.span().setTag("Pool", dataSourceWithStatistics.getDataSource().toString());
                String poolName = getPoolName(dataSourceWithStatistics.getDataSource());
                LOGGER.info("Found pool for [{}] in datasource [{}] : pool Name [{}]", principal, dataSourceWithStatistics.getDataSource(), poolName);
                LOGGER.info("Principal [{}] has requested [{}] connections for pool [{}]", principal, requests, poolName);
                // -------------------------------
                return authenticationStrategy.getConnection(dataSourceWithStatistics, identity);
            }
            catch (ConnectionException ce)
            {
                LOGGER.error("ConnectionException  {{}} : pool stats [{}] ", principal, RelationalExecutorInfo.getPoolStatisticsAsJSON(this));
                LOGGER.error("ConnectionException ", ce);
                throw ce;
            }
        }
    }

    private String getPoolName(DataSource dataSource)
    {
        return ((HikariDataSource)dataSource).getPoolName();
    }

    protected String poolNameFor(Identity identity)
    {
        return "DBPool_" + this.datasourceKey.shortId() + "_" + this.authenticationStrategy.getKey().shortId() + "_" + identity.getName();
    }

    protected DataSource buildDataSource(Identity identity)
    {
        return this.buildDataSource(null, -1, null, identity);
    }

    protected HikariDataSource buildDataSource(String host, int port, String databaseName, Identity identity)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Create Pool").startActive(true))
        {
            Properties properties = new Properties();
            String poolName = poolNameFor(identity);
            properties.putAll(this.databaseManager.getExtraDataSourceProperties(this.authenticationStrategy, identity));
            properties.putAll(this.extraDatasourceProperties);

            properties.put(AuthenticationStrategy.AUTHENTICATION_STRATEGY_KEY, this.authenticationStrategy.getKey().shortId());
            properties.put(ConnectionStateManager.POOL_NAME_KEY, poolName);

            LOGGER.info("Building pool [{}] for [{}] ", poolName, identity.getName());
            HikariConfig jdbcConfig = new HikariConfig();
            jdbcConfig.setDriverClassName(databaseManager.getDriver());
            jdbcConfig.setPoolName(poolName);
            jdbcConfig.setMaximumPoolSize(HIKARICP_MAX_POOL_SIZE);
            jdbcConfig.setMinimumIdle(HIKARICP_MIN_IDLE);
            jdbcConfig.setJdbcUrl(this.databaseManager.buildURL(host, port, databaseName, properties, this.authenticationStrategy));
            jdbcConfig.setConnectionTimeout(authenticationStrategy.getConnectionTimeout());
            jdbcConfig.addDataSourceProperty("cachePrepStmts", false);
            jdbcConfig.addDataSourceProperty("prepStmtCacheSize", 0);
            jdbcConfig.addDataSourceProperty("prepStmtCacheSqlLimit", 0);
            jdbcConfig.addDataSourceProperty("useServerPrepStmts", false);
            jdbcConfig.addDataSourceProperty("privateProperty", "MyProperty");

            if (this.databaseManager.publishMetrics())
            {
                jdbcConfig.setMetricRegistry(METRIC_REGISTRY);
            }

            // Properties management --------------
            MapAdapter.adapt(properties).keyValuesView().forEach((Procedure<Pair>)p -> jdbcConfig.addDataSourceProperty(p.getOne().toString(), p.getTwo()));
            //-------------------------------------

            HikariDataSource ds = new HikariDataSource(jdbcConfig);
            scope.span().setTag("Pool", ds.getPoolName());
            LOGGER.info("New Connection Pool created {}", ds);
            return ds;
        }
    }

    private static class KeyLockManager<K>
    {
        private static final Function0<Object> NEW_LOCK = new Function0<Object>()
        {
            @Override
            public Object value()
            {
                return new Object();
            }
        };

        private final ConcurrentMutableMap<K, Object> locks = ConcurrentHashMap.newMap();

        private KeyLockManager()
        {
        }

        /**
         * Get a lock for key.  This "lock" is simply an Object
         * whose intrinsic lock may be used in a synchronized
         * statement.  Each key yields a unique lock.  Each call
         * to this method with a given key will yield the same
         * lock.  This method supports concurrent access.
         *
         * @param key lock key
         * @return key lock
         */
        public Object getLock(K key)
        {
            return this.locks.getIfAbsentPut(key, NEW_LOCK);
        }

        /**
         * Create a new key lock manager.
         *
         * @param <T> key type
         * @return new key lock manager
         */
        public static <T> KeyLockManager<T> newManager()
        {
            return new KeyLockManager<T>();
        }
    }

    @Override
    public String toString()
    {
        return "DataSourceSpecification[" +
                this.getClass().getSimpleName() + "," +
                this.datasourceKey.shortId() + "," +
                this.authenticationStrategy.getKey().shortId() + "," +
                super.toString() + "]";
    }
}
