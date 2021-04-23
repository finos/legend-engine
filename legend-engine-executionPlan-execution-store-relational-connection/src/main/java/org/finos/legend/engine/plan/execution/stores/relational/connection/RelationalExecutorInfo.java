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
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceStatistics;
import com.zaxxer.hikari.HikariDataSource;
import org.eclipse.collections.api.map.ConcurrentMutableMap;

import java.util.List;

public class RelationalExecutorInfo
{
    private ConcurrentMutableMap<ConnectionKey, DataSourceSpecification> dbSpecByKey;
    private ConcurrentMutableMap<String, DataSourceSpecification> dataSourceSpecifications;

    public void setDbSpecByKey(ConcurrentMutableMap<ConnectionKey, DataSourceSpecification> dbSpecByKey)
    {
        this.dbSpecByKey = dbSpecByKey;
    }

    public void setDataSourceSpecifications(ConcurrentMutableMap<String, DataSourceSpecification> dataSourceSpecifications)
    {
        this.dataSourceSpecifications = dataSourceSpecifications;
    }

    @JsonProperty(value = "datasourcesCacheSanityCheck", required = true)
    public boolean datasourcesCacheSanityCheck()
    {
        return dbSpecByKey.valuesView().size() == (dataSourceSpecifications == null ? 0 : dataSourceSpecifications.keySet().size());
    }

    @JsonProperty(value = "databases", required = true)
    public List<? extends Object> getDatabases()
    {
        return dbSpecByKey.keyValuesView().collect(
                k -> new KeyAndPools
                        (
                                k.getOne(),
                                k.getTwo().getConnectionPoolByUser().keyValuesView().collect(z ->
                                        {
                                            HikariDataSource ds = ((HikariDataSource) z.getTwo().getDataSource());
                                            return new Pool
                                                    (
                                                            z.getTwo().getStatistics(),
                                                            z.getOne(),
                                                            new PoolStatic(
                                                                    ds.getJdbcUrl(),
                                                                    ds.getConnectionInitSql(),
                                                                    ds.getConnectionTimeout(),
                                                                    ds.getIdleTimeout(),
                                                                    ds.getMaximumPoolSize(),
                                                                    ds.getMinimumIdle()),
                                                            new PoolDynamic(
                                                                    ds.getHikariPoolMXBean().getActiveConnections(),
                                                                    ds.getHikariPoolMXBean().getIdleConnections(),
                                                                    ds.getHikariPoolMXBean().getThreadsAwaitingConnection(),
                                                                    ds.getHikariPoolMXBean().getTotalConnections()
                                                            )
                                                    );
                                        }
                                ).toList(),
                                k.getTwo().getDataSourceSpecificationStatistics(),
                                k.getTwo().getAuthenticationStrategy().getAuthenticationStatistics()
                        )
        ).toList();
    }

    class KeyAndPools
    {
        public Object connectionKey;
        public DataSourceSpecificationStatistics dataSourceSpecificationStatistics;
        public AuthenticationStatistics authenticationStatistics;
        public Object pools;

        public KeyAndPools(ConnectionKey connectionKey, Object two, DataSourceSpecificationStatistics dataSourceSpecificationStatistics, AuthenticationStatistics authenticationStatistics)
        {
            this.connectionKey = connectionKey;
            this.pools = two;
            this.authenticationStatistics = authenticationStatistics;
            this.dataSourceSpecificationStatistics = dataSourceSpecificationStatistics;
        }
    }

    class Pool
    {
        public DataSourceStatistics statistics;
        public String user;
        @JsonProperty(value = "static", required = true)
        public PoolStatic _static;
        public PoolDynamic dynamic;

        public Pool(DataSourceStatistics statistics, String user, PoolStatic _static, PoolDynamic dynamic)
        {
            this.user = user;
            this.statistics = statistics;
            this._static = _static;
            this.dynamic = dynamic;
        }
    }

    class PoolStatic
    {
        public String jdbcURL;
        public String connectionInitSql;
        public long connectionTimeout;
        public long idleTimeout;
        public long maximumPoolSize;
        public long minimumIdle;

        public PoolStatic(String jdbcURL,
                          String connectionInitSql,
                          long connectionTimeout,
                          long idleTimeout,
                          long maximumPoolSize,
                          long minimumIdle
        )
        {
            this.jdbcURL = jdbcURL;
            this.connectionInitSql = connectionInitSql;
            this.connectionTimeout = connectionTimeout;
            this.idleTimeout = idleTimeout;
            this.maximumPoolSize = maximumPoolSize;
            this.minimumIdle = minimumIdle;
        }

    }

    class PoolDynamic
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
}
