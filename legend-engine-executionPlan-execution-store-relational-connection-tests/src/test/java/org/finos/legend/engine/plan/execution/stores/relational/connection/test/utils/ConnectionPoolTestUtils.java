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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;

public class ConnectionPoolTestUtils
{
    public static int countNumHikariPools(String identityName)
    {
        try
        {
            List<DataSourceWithStatistics> pools = getConnectionPoolByUser(identityName);
            return pools.size();
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static List<DataSourceWithStatistics> getConnectionPoolByUser(String identityName) throws NoSuchFieldException, IllegalAccessException
    {
        ConcurrentMutableMap<String, DataSourceSpecification> specifications = getDataSourceSpecifications();
        List<DataSourceWithStatistics> connectionPoolsForUser = getAllConnectionPoolsForUser(identityName, specifications);
        return connectionPoolsForUser;
    }

    public static DataSourceWithStatistics getSingleConnectionPoolForUser(String identityName) throws NoSuchFieldException, IllegalAccessException
    {
        List<DataSourceWithStatistics> pools = getConnectionPoolByUser(identityName);
        assert(1 == pools.size());
        return pools.get(0);
    }

    public static ConcurrentMutableMap getDataSourceSpecifications() throws NoSuchFieldException, IllegalAccessException
    {
        Field field = DataSourceSpecification.class.getDeclaredField("dataSourceSpecifications");
        field.setAccessible(true);
        return (ConcurrentMutableMap) field.get(null);
    }

    public static void resetDatasourceSpecificationSingletonState() throws Exception
    {
        ConcurrentMutableMap dataSourceSpecifications = ConnectionPoolTestUtils.getDataSourceSpecifications();
        dataSourceSpecifications.clear();
    }

    private static List<DataSourceWithStatistics> getAllConnectionPoolsForUser(String identityName, ConcurrentMutableMap<String, DataSourceSpecification> connectionPoolsBySpecification ) throws NoSuchFieldException, IllegalAccessException
    {
        List<DataSourceWithStatistics> connectionPoolsForUser = connectionPoolsBySpecification.stream()
                .map(specification -> specification.getConnectionPoolByUser())
                .flatMap(m -> m.entrySet().stream())
                .filter(e -> e.getKey().contains(identityName))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        return connectionPoolsForUser;
    }
}
