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

import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionExtension;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.Iterate;

import java.util.Properties;
import java.util.ServiceLoader;

public abstract class DatabaseManager
{
    private static volatile MutableMap<String, DatabaseManager> managersByName;

    private static void initialize()
    {
        if (managersByName == null)
        {
            synchronized (DatabaseManager.class)
            {
                if (managersByName == null)
                {
                    managersByName = Maps.mutable.empty();
                    register(new H2Manager());
                    MutableList<ConnectionExtension> extensions = Iterate.addAllTo(ServiceLoader.load(ConnectionExtension.class), Lists.mutable.empty());
                    extensions.flatCollect(ConnectionExtension::getAdditionalDatabaseManager).forEach(DatabaseManager::register);
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

    public abstract Properties getExtraDataSourceProperties(AuthenticationStrategy authenticationStrategy);

    public abstract String getDriver();

    public abstract RelationalDatabaseCommands relationalDatabaseSupport();

    public boolean publishMetrics()
    {
        return true;
    }
}
