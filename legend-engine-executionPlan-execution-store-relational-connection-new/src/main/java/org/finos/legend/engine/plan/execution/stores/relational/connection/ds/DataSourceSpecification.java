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
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;

import javax.security.auth.Subject;
import javax.sql.DataSource;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.util.Properties;

public abstract class DataSourceSpecification
{
    public static MetricRegistry METRIC_REGISTRY;

    public static void setMetricRegistry(MetricRegistry metricRegistry)
    {
        METRIC_REGISTRY = metricRegistry;
    }

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataSourceSpecification.class);

    // HikariCP Parameters
    protected static final int HIKARICP_MAX_POOL_SIZE = 100;
    protected static final int HIKARICP_MIN_IDLE = 0;

    protected DataSourceSpecificationKey datasourceKey;
    private DatabaseManager databaseManager;
    private AuthenticationStrategy authenticationStrategy;
    protected Properties extraDatasourceProperties;

    private KeyLockManager<String> keyLockManager = KeyLockManager.newManager();
    private ConcurrentMutableMap<String, DataSourceWithStatistics> connectionPoolByUser = ConcurrentHashMap.newMap();

    private DataSourceSpecificationStatistics dataSourceSpecificationStatistics = new DataSourceSpecificationStatistics();

    public static String DATASOURCE_SPEC_INSTANCE = "DATASOURCE_SPEC_INSTANCE";
    private static final ConcurrentMutableMap<String, DataSourceSpecification> dataSourceSpecifications = ConcurrentHashMap.newMap();

    protected DataSourceSpecification(DataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties, RelationalExecutorInfo relationalExecutorInfo)
    {
        this.datasourceKey = key;
        this.databaseManager = databaseManager;
        this.authenticationStrategy = authenticationStrategy;
        this.extraDatasourceProperties = new Properties();
        this.extraDatasourceProperties.putAll(extraUserProperties);
        relationalExecutorInfo.setDataSourceSpecifications(dataSourceSpecifications);

        synchronized (DataSourceSpecification.class)
        {
            String instanceKey = buildInstanceKey();
            dataSourceSpecifications.put(instanceKey, this);
            this.extraDatasourceProperties.put(DATASOURCE_SPEC_INSTANCE, instanceKey);
        }
        MetricsHandler.observeCount("datastore specifications");
        LOGGER.info("Create new {}", this);
    }

    private String buildInstanceKey()
    {
        return this.datasourceKey.shortId()+"_"+this.authenticationStrategy.getKey().shortId();
    }

    protected abstract DataSource buildDataSource(MutableList<CommonProfile> profiles);

    public DataSourceSpecificationStatistics getDataSourceSpecificationStatistics()
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
    public ConcurrentMutableMap<String, DataSourceWithStatistics> getConnectionPoolByUser()
    {
        return this.connectionPoolByUser;
    }

    public Connection getConnectionUsingProfiles(MutableList<CommonProfile> profiles)
    {
        Subject subject = ProfileManagerHelper.extractSubject(profiles);
        Connection c;
        if (subject == null)
        {
            LOGGER.info("subject is absent");
            String principal = this.authenticationStrategy.getAlternativePrincipal(profiles);
            c = getConnection(
                    null,
                    profiles,
                    principal,
                    () -> connectionPoolByUser.getIfAbsentPutWithKey(getUserNameFromPrincipal(principal), p -> new DataSourceWithStatistics(this.buildDataSource(profiles)))
            );
        }
        else
        {
            c = getConnectionUsingSubject(subject);
        }
        return c;
    }

    public Connection getConnectionUsingSubject(Subject subject)
    {
        LOGGER.info("subject is present, using privileged action");
        String principal = SubjectTools.getPrincipal(subject);
        return getConnection(
                subject,
                null,
                principal,
                () -> connectionPoolByUser.getIfAbsentPutWithKey(getUserNameFromPrincipal(principal), p -> new DataSourceWithStatistics(Subject.doAs(subject, (PrivilegedAction<DataSource>) () -> this.buildDataSource(null))))
        );
    }

    protected Connection getConnection(Subject subject, MutableList<CommonProfile> profiles, String principal, Function0<DataSourceWithStatistics> exec)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Get Connection").startActive(true))
        {
            // Logs and traces -----
            scope.span().setTag("Principal", principal);
            scope.span().setTag("DataSourceSpecification", this.toString());
            LOGGER.info("Get Connection for {} from {}", principal, this);
            LOGGER.debug("connectionPoolByUser Size {} Keys {}", connectionPoolByUser.size(), connectionPoolByUser.keySet());
            // ---------------------

            DataSourceWithStatistics dataSourceWithStatistics = connectionPoolByUser.get(principal);
            if (dataSourceWithStatistics == null)
            {
                synchronized (keyLockManager.getLock(getUserNameFromPrincipal(principal)))
                {
                    dataSourceWithStatistics = exec.value();
                }
            }

            // Logs and traces and stats -----
            scope.span().setTag("Pool", dataSourceWithStatistics.getDataSource().toString());
            LOGGER.info("Found {}", dataSourceWithStatistics.getDataSource());
            dataSourceWithStatistics.requestConnection();
            // -------------------------------

            return authenticationStrategy.getConnection(dataSourceWithStatistics, subject, profiles);
        }
    }

    protected HikariDataSource buildDataSource(String host, int port, String databaseName, MutableList<CommonProfile> profiles)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Create Pool").startActive(true))
        {
            Properties properties = new Properties();
            String poolName = "DBPool_" + this.datasourceKey.shortId() + "_" + this.authenticationStrategy.getKey().shortId() + "_" + (SubjectTools.getCurrentUsername() != null ? SubjectTools.getCurrentUsername() : this.authenticationStrategy.getAlternativePrincipal(profiles));
            properties.putAll(this.databaseManager.getExtraDataSourceProperties(this.authenticationStrategy));
            properties.putAll(this.extraDatasourceProperties);

            properties.put(AuthenticationStrategy.AUTHENTICATION_STRATEGY_KEY,  this.authenticationStrategy.getKey().shortId());
            properties.put(AuthenticationStrategy.AUTHENTICATION_STRATEGY_PROFILE_BY_POOL, poolName);
            // The profiles are associated with the Pool as the Pool can create connections by itself in its own threads.
            AuthenticationStrategy.registerProfilesByPool(poolName, profiles);

            HikariConfig jdbcConfig = new HikariConfig();
            jdbcConfig.setDriverClassName(databaseManager.getDriver());
            jdbcConfig.setPoolName(poolName);
            jdbcConfig.setMaximumPoolSize(HIKARICP_MAX_POOL_SIZE);
            jdbcConfig.setMinimumIdle(HIKARICP_MIN_IDLE);
            jdbcConfig.setJdbcUrl(this.databaseManager.buildURL(host, port, databaseName, properties, this.authenticationStrategy));
            jdbcConfig.setConnectionTimeout(authenticationStrategy.getConnectionTimeout());
            jdbcConfig.setUsername(authenticationStrategy.getLogin());
            jdbcConfig.setPassword(authenticationStrategy.getPassword());
            jdbcConfig.addDataSourceProperty("cachePrepStmts", false);
            jdbcConfig.addDataSourceProperty("prepStmtCacheSize", 0);
            jdbcConfig.addDataSourceProperty("prepStmtCacheSqlLimit", 0);
            jdbcConfig.addDataSourceProperty("useServerPrepStmts", false);
            jdbcConfig.addDataSourceProperty("privateProperty", "MyProperty");

//        jdbcConfig.setHealthCheckRegistry(new HealthCheckRegistry());
            if (this.databaseManager.publishMetrics())
            {
                jdbcConfig.setMetricRegistry(METRIC_REGISTRY);
            }

            // Properties management --------------
            MapAdapter.adapt(properties).keyValuesView().forEach((Procedure<Pair>) p -> jdbcConfig.addDataSourceProperty(p.getOne().toString(), p.getTwo()));
            //-------------------------------------

            HikariDataSource ds = new HikariDataSource(jdbcConfig);
            scope.span().setTag("Pool", ds.getPoolName());
            LOGGER.info("Create new Connection Pool  {}", ds);
            return ds;
        }
    }

    //make public in to legend subjectTools
    private static String getUserNameFromPrincipal(String principal)
    {
        return principal != null ? principal.split("@")[0] : null;
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
                this.authenticationStrategy.getKey().shortId()+"," +
                super.toString() + "]";
    }
}
