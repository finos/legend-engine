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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.RelationalConnectionPlugin;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.RelationalConnectionPluginLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;

import java.util.Properties;

public abstract class DatabaseManager
{
    private static volatile ConcurrentHashMap<String, DatabaseManager> managersByName; //NOSONAR - ConcurrentHashMap is good enough

    public static DatabaseManager fromString(String dbType)
    {
        RelationalConnectionPlugin plugin = new RelationalConnectionPluginLoader().getPlugin(DatabaseType.valueOf(dbType));
        if (plugin == null)
        {
            throw new RuntimeException(dbType + " not supported yet");
        }
        return plugin.getDatabaseManager();
    }

    public abstract MutableList<String> getIds();

    @Deprecated
    public abstract String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy);

    @Deprecated
    public abstract Properties getExtraDataSourceProperties(AuthenticationStrategy authenticationStrategy);

    public abstract String getDriver();

    public abstract DriverWrapper getDriverWrapper();

    public abstract RelationalDatabaseCommands relationalDatabaseSupport();

    public boolean publishMetrics()
    {
        return true;
    }
}
