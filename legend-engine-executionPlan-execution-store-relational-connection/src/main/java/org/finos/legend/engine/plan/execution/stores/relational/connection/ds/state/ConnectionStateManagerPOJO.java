package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConnectionStateManagerPOJO
{
    private final ConcurrentMutableMap<String, DataSourceWithStatistics> pools;
    private final Set<DataSourceSpecification> dataSourceSpecifications;

    public ConnectionStateManagerPOJO(ConcurrentMutableMap<String, DataSourceWithStatistics> pools)
    {
        this.pools = pools;
        this.dataSourceSpecifications = buildDataSourceSpecifications();
    }

    private Set<DataSourceSpecification> buildDataSourceSpecifications()
    {
        Set<DataSourceSpecification> dataSourceSpecificationSet = new HashSet<>();
        this.pools.forEach(pool-> dataSourceSpecificationSet.add(pool.getDataSourceSpecification()));
        return dataSourceSpecificationSet;
    }

    static class RelationalStoreInfo
    {
        public final String connectionKeyShortId;
        public final String datasourceName;
        public final ConnectionKey connectionKey;
        public final AuthenticationStrategy authenticationStrategy;
        public final AuthenticationStatistics authenticationStatistics;

        public RelationalStoreInfo(DataSourceSpecification dataSourceSpecification)
        {
            this.connectionKey = dataSourceSpecification.getConnectionKey();
            this.connectionKeyShortId = connectionKey.shortId();
            this.datasourceName = dataSourceSpecification.toString();
            this.authenticationStrategy = dataSourceSpecification.getAuthenticationStrategy();
            this.authenticationStatistics = this.authenticationStrategy.getAuthenticationStatistics();
        }
    }

    public static class ConnectionPool
    {
        public final String name;
        public final String user;
        public final DataSourceStatistics statistics;
        @JsonProperty(value = "static", required = true)
        public final PoolStatic _static;
        public final PoolDynamic dynamic;
        public final String connectionKeyShortId;
        public final String datasourceName;


        public ConnectionPool(DataSourceWithStatistics dataSourceWithStatistics, RelationalStoreInfo relationalStoreInfo)
        {
            this.name = dataSourceWithStatistics.getPoolName();
            this.user = dataSourceWithStatistics.getPoolPrincipal();
            this.statistics = dataSourceWithStatistics.getStatistics();
            HikariDataSource hikariDataSource = (HikariDataSource)dataSourceWithStatistics.getDataSource();
            this._static = buildPoolStaticConfiguration(hikariDataSource);
            this.dynamic = buildPoolDynamicStats(hikariDataSource);
            this.connectionKeyShortId = relationalStoreInfo.connectionKeyShortId;
            this.datasourceName = relationalStoreInfo.datasourceName;
        }
    }

    static class PoolStatic
    {
        public final String jdbcURL;
        public final String connectionInitSql;
        public final long connectionTimeout;
        public final long idleTimeout;
        public final long maximumPoolSize;
        public final long minimumIdle;
        public final long houseKeeperFrequency;
        public final long maximumLifeTime;
        public final long leakDetectionThreshold;

        public PoolStatic(String jdbcURL,
                          String connectionInitSql,
                          long connectionTimeout,
                          long idleTimeout,
                          long maximumPoolSize,
                          long minimumIdle,
                          long houseKeeperFrequency,
                          long maximumLifeTime,
                          long leakDetectionThreshold)
        {
            this.jdbcURL = jdbcURL;
            this.connectionInitSql = connectionInitSql;
            this.connectionTimeout = connectionTimeout;
            this.idleTimeout = idleTimeout;
            this.maximumPoolSize = maximumPoolSize;
            this.minimumIdle = minimumIdle;
            this.houseKeeperFrequency = houseKeeperFrequency;
            this.maximumLifeTime = maximumLifeTime;
            this.leakDetectionThreshold = leakDetectionThreshold;
        }

    }

    public static class PoolDynamic
    {
        public long activeConnections;
        public long idleConnections;
        public long threadsAwaitingConnection;
        public long totalConnections;

        public PoolDynamic(
                long activeConnections,
                long idleConnections,
                long threadsAwaitingConnection,
                long totalConnections
        )
        {
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.threadsAwaitingConnection = threadsAwaitingConnection;
            this.totalConnections = totalConnections;
        }
    }

    @JsonProperty(value = "totalPools", required = true)
    public int getPoolsSize()
    {
        return pools != null ? pools.size() : 0;
    }

    @JsonProperty(value = "totalDataSourceSpecifications", required = true)
    public int getDataSourceSpecificationSize()
    {
        return dataSourceSpecifications != null ? dataSourceSpecifications.size() : 0;
    }
    @JsonProperty(value = "stores", required = true)
    public Set<RelationalStoreInfo> getStores()
    {
        return this.dataSourceSpecifications.stream().map(k -> new RelationalStoreInfo(k)).collect(Collectors.toSet());
    }

    @JsonProperty(value = "pools", required = true)
    public List<ConnectionPool> getPools()
    {
        return this.pools.valuesView().collect(k -> buildConnectionPool(k)).toList();
    }

    static ConnectionPool buildConnectionPool(DataSourceWithStatistics dataSourceWithStatistics)
    {
        return new ConnectionPool(dataSourceWithStatistics, new RelationalStoreInfo(dataSourceWithStatistics.getDataSourceSpecification()));
    }

    static PoolStatic buildPoolStaticConfiguration(HikariDataSource ds)
    {
        if (ds == null)
        {
            return null;
        }
        return new PoolStatic(
                ds.getJdbcUrl(),
                ds.getConnectionInitSql(),
                ds.getConnectionTimeout(),
                ds.getIdleTimeout(),
                ds.getMaximumPoolSize(),
                ds.getMinimumIdle(),
                Long.getLong(DataSourceSpecification.HIKARICP_HOUSEKEEPING_PERIOD_MS, 0),
                ds.getMaxLifetime(),
                ds.getLeakDetectionThreshold());
    }

    static PoolDynamic buildPoolDynamicStats(HikariDataSource ds)
    {
        if (ds == null)
        {
            return null;
        }
        HikariPoolMXBean mxBean = ds.getHikariPoolMXBean();
        return new PoolDynamic(
                mxBean.getActiveConnections(),
                mxBean.getIdleConnections(),
                mxBean.getThreadsAwaitingConnection(),
                mxBean.getTotalConnections()
        );
    }

}
