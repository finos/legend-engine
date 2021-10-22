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

package org.finos.legend.engine.plan.execution.stores.relational.connection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;


public class RelationalExecutorInfo
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RelationalExecutorInfo.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentMutableMap<ConnectionKey, DataSourceSpecification> dataSourceSpecificationsByConnectionKey = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<ConnectionKey, String> connectionKeyToDatasourceId = ConcurrentHashMap.newMap();
    private ConcurrentMutableMap<String, DataSourceSpecification> dataSourceSpecifications;


    public ConcurrentMutableMap<ConnectionKey, DataSourceSpecification> dbSpecByKey()
    {
        return dataSourceSpecificationsByConnectionKey;
    }

    public DataSourceSpecification get(String instanceKey, Supplier<DataSourceSpecification> dataSourceSpecificationSupplier)
    {
        return this.dataSourceSpecifications.getIfAbsentPut(instanceKey, dataSourceSpecificationSupplier::get);
    }

    public DataSourceSpecification getByConnectionKey(ConnectionKey connectionKey, Supplier<DataSourceSpecification> dataSourceSpecificationSupplier)
    {
        return this.dataSourceSpecificationsByConnectionKey.getIfAbsentPut(connectionKey, dataSourceSpecificationSupplier::get);
    }

    public void setDataSourceSpecifications(ConcurrentMutableMap<String, DataSourceSpecification> dataSourceSpecifications)
    {
        this.dataSourceSpecifications = dataSourceSpecifications;
    }

    public void putInstanceKeyIfAbsent(ConnectionKey instanceKey, String shortId)
    {
        this.connectionKeyToDatasourceId.putIfAbsent(instanceKey, shortId);
    }

    @JsonProperty(value = "datasourcesCacheSanityCheck", required = true)
    public boolean datasourcesCacheSanityCheck()
    {
        return dataSourceSpecificationsByConnectionKey.size() == (dataSourceSpecifications == null ? 0 : dataSourceSpecifications.size());
    }

    @JsonProperty(value = "totalConnectionsKeys", required = true)
    public int getTotalConnectionKeys()
    {
        return dataSourceSpecificationsByConnectionKey != null ? dataSourceSpecificationsByConnectionKey.size() : 0;
    }

    @JsonProperty(value = "totalDatasourceSpecificationsKeys", required = true)
    public int getTotalDatasourceSpecificationsKeys()
    {
        return dataSourceSpecifications != null ? dataSourceSpecifications.size() : 0;
    }

    @JsonProperty(value = "databases", required = true)
    public List<RelationalStoreInfo> getRelationalStores()
    {
        return dataSourceSpecificationsByConnectionKey.keyValuesView().collect(k -> buildRelationalStoreInfo(k.getOne(), k.getTwo())).toList();
    }

    private static RelationalStoreInfo buildRelationalStoreInfo(ConnectionKey connectionKey, DataSourceSpecification ds)
    {
        return new RelationalStoreInfo(
                connectionKey,
                ds.toString(),
                ds.getConnectionPoolByUser().keyValuesView().collect(z -> buildConnectionPool(z.getOne(), z.getTwo())).toList(),
                ds.getDataSourceSpecificationStatistics(),
                ds.getAuthenticationStrategy().getAuthenticationStatistics()
        );
    }

    private static ConnectionPool buildConnectionPool(String user, DataSourceWithStatistics dataSourceWithStatistics)
    {
        HikariDataSource ds = ((HikariDataSource)dataSourceWithStatistics.getDataSource());
        return new ConnectionPool
                (ds.getPoolName(),
                        dataSourceWithStatistics.getStatistics(),
                        user,
                        buildPoolStaticConfiguration(ds),
                        buildPoolDynamicStats(ds)
                );
    }

    private static PoolDynamic buildPoolDynamicStats(HikariDataSource ds)
    {
        HikariPoolMXBean mxBean = ds.getHikariPoolMXBean();
        return new PoolDynamic(
                mxBean.getActiveConnections(),
                mxBean.getIdleConnections(),
                mxBean.getThreadsAwaitingConnection(),
                mxBean.getTotalConnections()
        );
    }

    private static PoolStatic buildPoolStaticConfiguration(HikariDataSource ds)
    {
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


    public Optional<ConnectionPool> findByPoolName(String poolName)
    {
        Pair<String, DataSourceWithStatistics> found = findDataSourceByPoolName(poolName);
        return found == null ? Optional.empty() : Optional.of(buildConnectionPool(found.getOne(), found.getTwo()));
    }

    private Pair<String, DataSourceWithStatistics> findDataSourceByPoolName(String poolName)
    {
        Pair<String, DataSourceWithStatistics> found = null;
        if (this.dataSourceSpecifications != null && !this.dataSourceSpecifications.values().isEmpty())
        {
            Iterator<DataSourceSpecification> it = this.dataSourceSpecifications.values().iterator();
            while (it.hasNext())
            {
                found = findPool(it.next().getConnectionPoolByUser(), poolName);
                if (found != null)
                {
                    break;
                }
            }
        }
        return found;
    }

    private Pair<String, DataSourceWithStatistics> findPool(ConcurrentMutableMap<String, DataSourceWithStatistics> dsMap, String poolName)
    {
        return dsMap.keyValuesView().detect(kv -> ((HikariDataSource)kv.getTwo().getDataSource()).getPoolName().equals(poolName));
    }

    public List<ConnectionPool> getPoolInformationByUser(String user)
    {
        List<ConnectionPool> connectionPools = new ArrayList<>();
        dataSourceSpecificationsByConnectionKey.valuesView().forEach(kv -> {
            DataSourceWithStatistics dataSourceWithStatistics = kv.getConnectionPoolByUser().get(user);
            if (dataSourceWithStatistics != null)
            {
                connectionPools.add(buildConnectionPool(user, dataSourceWithStatistics));
            }
        });
        return connectionPools;
    }

    public Object softEvictConnections(String poolName)
    {
        StringBuffer result = new StringBuffer();
        Pair<String, DataSourceWithStatistics> datasource = findDataSourceByPoolName(poolName);
        if (datasource != null)
        {
            HikariDataSource hds = (HikariDataSource)datasource.getTwo().getDataSource();
            result.append("found [").append(hds.getPoolName());
            result.append("], active connections [");
            result.append(hds.getHikariPoolMXBean().getActiveConnections());
            result.append("] ,idle connections [");
            result.append(hds.getHikariPoolMXBean().getIdleConnections());
            result.append("] ,total connections [");
            result.append(hds.getHikariPoolMXBean().getTotalConnections());
            result.append("]");
            hds.getHikariPoolMXBean().softEvictConnections();
        }
        return result.toString();
    }

    public static String getPoolStatisticsAsJSON(DataSourceSpecification ds)
    {
        if (ds != null)
        {
            try
            {
                return mapper.writeValueAsString(buildRelationalStoreInfo(ds.buildConnectionKey(), ds));
            }
            catch (JsonProcessingException e)
            {
                LOGGER.error("error getPoolStatisticsAsJSON", e);
            }
        }
        return null;
    }


    static class RelationalStoreInfo
    {
        public final ConnectionKey connectionKey;
        public final String connectionKeyShortId;
        public final String datasourceName;
        public final Statistics statistics;
        public final List<ConnectionPool> pools;

        public RelationalStoreInfo(ConnectionKey connectionKey, String datasourceName, List<ConnectionPool> pools, DataSourceSpecificationStatistics dataSourceSpecificationStatistics, AuthenticationStatistics authenticationStatistics)
        {
            this.connectionKey = connectionKey;
            this.connectionKeyShortId = connectionKey.shortId();
            this.datasourceName = datasourceName;
            this.pools = pools;
            this.statistics = new Statistics(pools.size(), buildAggregatedPoolStats(pools), dataSourceSpecificationStatistics, authenticationStatistics);
        }

        private PoolDynamic buildAggregatedPoolStats(List<ConnectionPool> pools)
        {
            PoolDynamic aggregatePoolDynamic = new PoolDynamic(0, 0, 0, 0);
            pools.stream().forEach(pool -> aggregatePoolDynamic.addStats(pool.dynamic));
            return aggregatePoolDynamic;
        }
    }

    static class Statistics
    {
        public final DataSourceSpecificationStatistics dataSourceSpecificationStatistics;
        public final AuthenticationStatistics authenticationStatistics;
        public final int totalNumberOfPools;
        public final PoolDynamic aggregatedPoolDynamicInfo;

        public Statistics(int totalNumberOfPools, PoolDynamic aggregatedPoolDynamicInfo, DataSourceSpecificationStatistics dataSourceSpecificationStatistics, AuthenticationStatistics authenticationStatistics)
        {
            this.totalNumberOfPools = totalNumberOfPools;
            this.aggregatedPoolDynamicInfo = aggregatedPoolDynamicInfo;
            this.dataSourceSpecificationStatistics = dataSourceSpecificationStatistics;
            this.authenticationStatistics = authenticationStatistics;
        }
    }

    static class ConnectionPool
    {
        public final String name;
        public final DataSourceStatistics statistics;
        public final String user;
        @JsonProperty(value = "static", required = true)
        public PoolStatic _static;
        public PoolDynamic dynamic;

        public ConnectionPool(String name, DataSourceStatistics statistics, String user, PoolStatic _static, PoolDynamic dynamic)
        {
            this.name = name;
            this.user = user;
            this.statistics = statistics;
            this._static = _static;
            this.dynamic = dynamic;
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

    static class PoolDynamic
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

        public void addStats(PoolDynamic pool)
        {
            this.activeConnections += pool.activeConnections;
            this.idleConnections += pool.idleConnections;
            this.threadsAwaitingConnection += pool.threadsAwaitingConnection;
            this.totalConnections += pool.totalConnections;
        }
    }
}
