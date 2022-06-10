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

package org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils;

import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConnectionPoolTestUtils
{
    public static int countNumHikariPools(String identityName) throws Exception
    {
        List<DataSourceWithStatistics> pools = getConnectionPoolByUser(identityName);
        return pools.size();
    }

    public static List<DataSourceWithStatistics> getConnectionPoolByUser(String identityName) throws Exception
    {
        ConcurrentMutableMap<ConnectionKey, DataSourceSpecification> specifications = getDataSourceSpecifications();
        List<DataSourceWithStatistics> connectionPoolsForUser = getAllConnectionPoolsForUser(identityName, specifications.keySet());
        return connectionPoolsForUser;
    }

    public static ConcurrentMutableMap<ConnectionKey, DataSourceSpecification> getDataSourceSpecifications() throws Exception
    {
        ConcurrentMutableMap<ConnectionKey, DataSourceSpecification> dataSourceSpecifications = ConcurrentHashMap.newMap();
        getConnectionPools().valuesView().forEach(pool-> dataSourceSpecifications.putIfAbsent(pool.getConnectionKey(),pool.getDataSourceSpecification()));

        return dataSourceSpecifications;
    }


    public static ConcurrentMutableMap<String, DataSourceWithStatistics> getConnectionPools() throws Exception
    {
        return (ConcurrentHashMap) ReflectionUtils.getFieldUsingReflection(ConnectionStateManager.class, ConnectionStateManager.getInstance(), "connectionPools");
    }

    public static void resetDatasourceSpecificationSingletonState() throws Exception
    {
        getConnectionPools().clear();
    }

    private static List<DataSourceWithStatistics> getAllConnectionPoolsForUser(String identityName, Set<ConnectionKey> connectionKeys)
    {
        ConnectionStateManager connectionStateManager = ConnectionStateManager.getInstance();
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentityForTesting(identityName);
        Stream<String> poolNames = connectionKeys.stream().map(key -> connectionStateManager.poolNameFor(identity,key));
        List<DataSourceWithStatistics> connectionPoolsForUser = poolNames
                .map(poolName -> connectionStateManager.get(poolName))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return connectionPoolsForUser;
    }
}
