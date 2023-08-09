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

import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TODO?: @akphi - This is a temporary hack!
 * We probably need to have a mechanism to control the connection pool
 * We cloned DatabaseManager from relational executor, we should consider if we can eventually unify these 2
 */
public abstract class JDBCConnectionManager
{
    private static final ConcurrentHashMap<String, JDBCConnectionManager> managersByName = ConcurrentHashMap.newMap();
    private static final AtomicBoolean dbManagerReady = new AtomicBoolean();

    private static void initialize()
    {
        if (!dbManagerReady.get())
        {
            synchronized (dbManagerReady)
            {
                if (!dbManagerReady.get())
                {
                    for (JDBCConnectionManager manager : ServiceLoader.load(JDBCConnectionManager.class))
                    {
                        JDBCConnectionManager.register(manager);
                    }
                    dbManagerReady.getAndSet(true);
                }
            }
        }
    }

    private static void register(JDBCConnectionManager databaseManager)
    {
        databaseManager.getIds().forEach(i -> managersByName.put(i, databaseManager));
    }

    public static JDBCConnectionManager getManagerForDatabaseType(String type)
    {
        // NOTE: we do this so that we don't have to manually initialize this manager somewhere
        // in the general flow
        initialize();
        JDBCConnectionManager manager = managersByName.get(type);
        if (manager == null)
        {
            throw new RuntimeException(String.format("Can't find matching JDBC connection manager for database type '%s'", type));
        }
        return manager;
    }

    public abstract List<String> getIds();

    public abstract String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties);

    // TODO?: @akphi - should we port over the driver wrapper stuffs from DatabaseManager as well?
    public abstract String getDriver();
}
