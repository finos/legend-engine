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
import org.finos.legend.connection.ConnectionManager;
import org.finos.legend.connection.Database;
import org.finos.legend.connection.jdbc.driver.JDBCConnectionDriver;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TODO?: @akphi - This is a temporary hack!
 * We probably need to have a mechanism to control the connection pool
 * We cloned DatabaseManager from relational executor, we should consider if we can eventually unify these 2
 */
public final class JDBCConnectionManager implements ConnectionManager
{
    private static final ConcurrentHashMap<String, JDBCConnectionDriver> driversByName = ConcurrentHashMap.newMap();
    private static final AtomicBoolean isInitialized = new AtomicBoolean();

    private static void detectDrivers()
    {
        if (!isInitialized.get())
        {
            synchronized (isInitialized)
            {
                if (!isInitialized.get())
                {
                    for (JDBCConnectionDriver driver : ServiceLoader.load(JDBCConnectionDriver.class))
                    {
                        JDBCConnectionManager.register(driver);
                    }
                    isInitialized.getAndSet(true);
                }
            }
        }
    }

    private static void register(JDBCConnectionDriver driver)
    {
        driver.getIds().forEach(i -> driversByName.put(i, driver));
    }

    public static JDBCConnectionDriver getDriverForDatabase(Database database)
    {
        if (!isInitialized.get())
        {
            throw new IllegalStateException("JDBC connection manager has not been configured properly");
        }
        JDBCConnectionDriver driver = driversByName.get(database.getLabel());
        if (driver == null)
        {
            throw new RuntimeException(String.format("Can't find matching JDBC connection driver for database type '%s'", database));
        }
        return driver;
    }

    @Override
    public void initialize()
    {
        JDBCConnectionManager.detectDrivers();
    }
}
