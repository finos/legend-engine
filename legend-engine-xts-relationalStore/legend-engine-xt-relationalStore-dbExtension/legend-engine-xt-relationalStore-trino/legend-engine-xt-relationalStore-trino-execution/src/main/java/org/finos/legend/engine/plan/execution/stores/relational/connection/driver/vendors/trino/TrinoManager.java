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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.trino;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.Properties;

public class TrinoManager extends DatabaseManager
{
    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("Trino");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy)
    {
        String hostWithPort = host + ":" + port;
        String catalogSchema = getCatalogSchema(extraUserDataSourceProperties);
        return "jdbc:trino://" + hostWithPort + "/" + catalogSchema;
    }

    private String getCatalogSchema(Properties extraUserDataSourceProperties)
    {
        StringBuilder catalogSchema = new StringBuilder();
        if (extraUserDataSourceProperties.get("catalog") != null)
        {
            catalogSchema.append(extraUserDataSourceProperties.get("catalog"));
            if (extraUserDataSourceProperties.get("schema") != null)
            {
                catalogSchema.append("/");
                catalogSchema.append(extraUserDataSourceProperties.get("schema"));
            }
        }
        return catalogSchema.toString();
    }

    @Override
    public String getDriver()
    {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.trino.TrinoDriver";
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport()
    {
        return new TrinoCommands();
    }

    @Override
    public boolean publishMetrics()
    {
        return false;
    }

    @Override
    public Properties getExtraDataSourceProperties(AuthenticationStrategy authenticationStrategy, Identity identity)
    {
        Properties properties =  new Properties();
        properties.setProperty("user", identity.getName());
        return properties;
    }

}