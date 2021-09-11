// Copyright 2021 Databricks
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.databricks;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.DatabricksDataSourceSpecification;
import org.finos.legend.engine.shared.core.operational.Assert;

import java.util.Properties;

public class DatabricksManager extends DatabaseManager
{
    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("Databricks");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy)
    {
        Assert.assertTrue(extraUserDataSourceProperties.getProperty(DatabricksDataSourceSpecification.DATABRICKS_HTTP_PATH) != null, () -> DatabricksDataSourceSpecification.DATABRICKS_HTTP_PATH + " is not set");
        Assert.assertTrue(extraUserDataSourceProperties.getProperty(DatabricksDataSourceSpecification.DATABRICKS_SHARD) != null, () -> DatabricksDataSourceSpecification.DATABRICKS_SHARD + " is not set");
        String httpPath = extraUserDataSourceProperties.getProperty(DatabricksDataSourceSpecification.DATABRICKS_HTTP_PATH);
        String shard = extraUserDataSourceProperties.getProperty(DatabricksDataSourceSpecification.DATABRICKS_SHARD);
        return String.format("jdbc:spark://%s;transportMode=http;ssl=1;httpPath=%s;AuthMech=3;",
                shard,
                httpPath
        );
    }

    @Override
    public Properties getExtraDataSourceProperties(AuthenticationStrategy authenticationStrategy)
    {
        return new Properties();
    }

    @Override
    public String getDriver()
    {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.databricks.DatabricksDriver";
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport()
    {
        return new DatabricksCommands();
    }

    @Override
    public boolean publishMetrics()
    {
        return false;
    }
}