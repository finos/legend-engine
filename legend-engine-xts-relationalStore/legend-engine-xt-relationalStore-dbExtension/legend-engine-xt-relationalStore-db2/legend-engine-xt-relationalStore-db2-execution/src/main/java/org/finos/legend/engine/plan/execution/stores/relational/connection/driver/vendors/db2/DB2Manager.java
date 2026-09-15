// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.db2;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;

import java.util.Properties;

public class DB2Manager extends DatabaseManager
{
    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("DB2");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy)
    {
        return "jdbc:db2://" + host + ":" + port + "/" + databaseName;
    }

    @Override
    public String getDriver()
    {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.db2.DB2Driver";
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport()
    {
        return new DB2Commands();
    }
}
