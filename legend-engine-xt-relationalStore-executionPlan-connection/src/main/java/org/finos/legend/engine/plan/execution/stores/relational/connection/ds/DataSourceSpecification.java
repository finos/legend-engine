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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.IdentityState;
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
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;

public abstract class DataSourceSpecification
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataSourceSpecification.class);

    // HikariCP Parameters:
    // Information parameters and its defaults: https://github.com/brettwooldridge/HikariCP#gear-configuration-knobs-baby
    public static final String HIKARICP_HOUSEKEEPING_PERIOD_MS = "com.zaxxer.hikari.housekeeping.periodMs";
    protected static final int HIKARICP_DEFAULT_MAX_POOL_SIZE = 100;
    protected static final int HIKARICP_DEFAULT_MIN_IDLE = 0;
    // minIdle 0 means housekeeper will always try to keep the pool empty if there are not really active connection
    private static final long HIKARICP_DEFAULT_HOUSEKEEPER_FREQ_IN_MS = SECONDS.toMillis(30L);
    //default house keeping interval is 30 seconds
    //house keeper evicts connections if:
    // - they are unused and idleTimeout is reached(defaults to 10 min)
    // - they are unused and maxLifetime is reached(defaults to 30 min)


    static
    {  //house keeper frequency can only be altered via system property and will affect all pools!!
        System.setProperty(HIKARICP_HOUSEKEEPING_PERIOD_MS, String.valueOf(Long.getLong(HIKARICP_HOUSEKEEPING_PERIOD_MS, HIKARICP_DEFAULT_HOUSEKEEPER_FREQ_IN_MS)));
    }

    public static MetricRegistry METRIC_REGISTRY;

    private final ConnectionStateManager connectionStateManager = ConnectionStateManager.getInstance();
    private final ConnectionKey connectionKey;
    private final DatabaseManager databaseManager;
    private final AuthenticationStrategy authenticationStrategy;
    protected final Properties extraDatasourceProperties = new Properties();
    protected final org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey datasourceKey;
    private final int minPoolSize ;
    private final int maxPoolSize;

    protected DataSourceSpecification(org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties)
    {
        this(key,databaseManager,authenticationStrategy,extraUserProperties, HIKARICP_DEFAULT_MAX_POOL_SIZE, HIKARICP_DEFAULT_MIN_IDLE);
    }

    protected DataSourceSpecification(org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties,int maxPoolSize,int minPoolSize)
    {
        this.datasourceKey = key;
        this.databaseManager = databaseManager;
        this.authenticationStrategy = authenticationStrategy;
        this.connectionKey = new ConnectionKey(this.datasourceKey, this.authenticationStrategy.getKey());
        this.extraDatasourceProperties.putAll(extraUserProperties);
        this.maxPoolSize  = maxPoolSize;
        this.minPoolSize = minPoolSize;
        MetricsHandler.observeCount("datastore specifications");
        MetricsHandler.incrementDatastoreSpecCount();
        LOGGER.info("Created new {}", this);
    }

    public static void setMetricRegistry(MetricRegistry metricRegistry)
    {
        METRIC_REGISTRY = metricRegistry;
    }

    public ConnectionKey getConnectionKey()
    {
        return this.connectionKey;
    }

    public AuthenticationStrategy getAuthenticationStrategy()
    {
        return authenticationStrategy;
    }

    public DatabaseManager getDatabaseManager()
    {
        return databaseManager;
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
        Optional<LegendKerberosCredential> kerberosCredentialHolder = identity.getCredential(LegendKerberosCredential.class);
        Supplier<DataSource> dataSourceBuilder;
        if (kerberosCredentialHolder.isPresent())
        {
            dataSourceBuilder = () -> KerberosUtils.doAs(identity, (PrivilegedAction<DataSource>) () -> this.buildDataSource(identity));
        }
        else
        {
            dataSourceBuilder = () -> this.buildDataSource(identity);
        }
        return getConnection(new IdentityState(identity,databaseCredentialSupplierHolder),dataSourceBuilder);
    }

    protected Connection getConnection(IdentityState identityState, Supplier<DataSource> dataSourcePoolBuilder)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Get Connection").startActive(true))
        {
            ConnectionKey connectionKey = this.getConnectionKey();
            // Logs and traces -----
            String principal = identityState.getIdentity().getName();
            scope.span().setTag("Principal", principal);
            scope.span().setTag("DataSourceSpecification", this.toString());
            LOGGER.info("Get Connection as [{}] for datasource [{}]", principal, connectionKey.shortId());
            // ---------------------
            try
            {
                DataSourceWithStatistics dataSourceWithStatistics = this.connectionStateManager.getDataSourceForIdentityIfAbsentBuild(identityState,this,dataSourcePoolBuilder);
                // Logs and traces and stats -----
                String poolName = dataSourceWithStatistics.getPoolName();
                scope.span().setTag("Pool", poolName);
                int requests = dataSourceWithStatistics.requestConnection();
                LOGGER.info("Principal [{}] has requested [{}] connections for pool [{}]", principal, requests, poolName);
                return authenticationStrategy.getConnection(dataSourceWithStatistics, identityState.getIdentity());
            }
            catch (ConnectionException ce)
            {
                LOGGER.error("ConnectionException  {{}} : pool stats [{}] ", principal, connectionStateManager.getPoolStatisticsAsJSON(poolNameFor(identityState.getIdentity())));
                LOGGER.error("ConnectionException ", ce);
                throw ce;
            }
        }
    }

    private String poolNameFor(Identity identity)
    {
        return this.connectionStateManager.poolNameFor(identity, getConnectionKey());
    }

    private DataSource buildDataSource(Identity identity)
    {
        return this.buildDataSource(null, -1, null, identity);
    }

    private HikariDataSource buildDataSource(String host, int port, String databaseName, Identity identity)
    {
        try (Scope scope = GlobalTracer.get().buildSpan("Create Pool").startActive(true))
        {
            Properties properties = new Properties();
            String poolName = poolNameFor(identity);
            properties.putAll(this.databaseManager.getExtraDataSourceProperties(this.authenticationStrategy, identity));
            properties.putAll(this.extraDatasourceProperties);

            properties.put(AuthenticationStrategy.AUTHENTICATION_STRATEGY_KEY, this.authenticationStrategy.getKey().shortId());
            properties.put(ConnectionStateManager.POOL_NAME_KEY, poolName);
            properties.putAll(authenticationStrategy.getAuthenticationPropertiesForConnection());
            LOGGER.info("Building pool [{}] for [{}] ", poolName, identity.getName());
            HikariConfig jdbcConfig = new HikariConfig();
            jdbcConfig.setDriverClassName(databaseManager.getDriver());
            jdbcConfig.setPoolName(poolName);
            jdbcConfig.setMaximumPoolSize(maxPoolSize);
            jdbcConfig.setMinimumIdle(minPoolSize);
            jdbcConfig.setJdbcUrl(getJdbcUrl(host, port, databaseName, properties));
            jdbcConfig.setConnectionTimeout(authenticationStrategy.getConnectionTimeout());
            /*
                Setting setInitializationFailTimeout=-1 disables Hikari's fail fast connection acquisition.
                With fail fast enabled, Hikari will attempt to create a connection as soon as the Hikari data source is constructed.
                This requires that all the authn properties required to establish a connection are available.

                In some cases, all the authn properties are not available when the Hikari data source is constructed.
                Therefore we disable the fail fast behavior.
             */
            jdbcConfig.setInitializationFailTimeout(-1);
            jdbcConfig.addDataSourceProperty("cachePrepStmts", false);
            jdbcConfig.addDataSourceProperty("prepStmtCacheSize", 0);
            jdbcConfig.addDataSourceProperty("prepStmtCacheSqlLimit", 0);
            jdbcConfig.addDataSourceProperty("useServerPrepStmts", false);

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

    protected String getJdbcUrl(String host, int port, String databaseName, Properties properties)
    {
        return this.databaseManager.buildURL(host, port, databaseName, properties, this.authenticationStrategy);
    }


    @Override
    public String toString()
    {
        return "DataSourceSpecification[" +
                this.getClass().getSimpleName() + "," +
                this.datasourceKey.shortId() + "," +
                this.authenticationStrategy.getKey().shortId() + "]";
    }
}
