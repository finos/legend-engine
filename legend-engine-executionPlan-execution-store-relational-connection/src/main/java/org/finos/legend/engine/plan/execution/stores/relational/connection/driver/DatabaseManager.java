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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionExtension;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.bigquery.BigQueryManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.sqlserver.SqlServerManager;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DatabaseManager
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DatabaseManager.class);
    private static final ConcurrentHashMap<String, DatabaseManager> managersByName = ConcurrentHashMap.newMap();
    private static final AtomicBoolean dbManagerReady = new AtomicBoolean();

    private static void initialize()
    {
        if (!dbManagerReady.get())
        {
            synchronized (dbManagerReady)
            {
                if (!dbManagerReady.get())
                {
                    LOGGER.info("DatabaseManager starting initialization");
                    long start = System.currentTimeMillis();
                    register(new H2Manager());
                    register(new SqlServerManager());
                    register(new SnowflakeManager());
                    register(new BigQueryManager());
                    MutableList<ConnectionExtension> extensions = Iterate.addAllTo(ServiceLoader.load(ConnectionExtension.class), Lists.mutable.empty());
                    extensions.flatCollect(ConnectionExtension::getAdditionalDatabaseManager).forEach(DatabaseManager::register);
                    dbManagerReady.getAndSet(true);
                    LOGGER.info("DatabaseManager initialisation took {}", System.currentTimeMillis() - start);
                }
            }
        }
    }

    private static void register(DatabaseManager databaseManager)
    {
        databaseManager.getIds().forEach(i -> managersByName.put(i, databaseManager));
    }

    public static DatabaseManager fromString(String dbType)
    {
        initialize();
        DatabaseManager result = managersByName.get(dbType);
        if (result == null)
        {
            throw new RuntimeException(dbType + " not supported yet");
        }
        return result;
    }

    public abstract MutableList<String> getIds();

    public abstract String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy);

    public Properties getExtraDataSourceProperties(AuthenticationStrategy authenticationStrategy, Identity identity)
    {
        return new Properties();
    }

    public abstract String getDriver();

    public abstract RelationalDatabaseCommands relationalDatabaseSupport();

    public boolean publishMetrics()
    {
        return true;
    }
}
