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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2;

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.EmbeddedH2DataSourceSpecification;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

import java.util.Properties;

public class H2Manager extends DatabaseManager
{
    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("H2");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy)
    {
        if(isEmbeddedMode(extraUserDataSourceProperties))
        {
            String dataDirectoryPath =  extraUserDataSourceProperties.getProperty(EmbeddedH2DataSourceSpecification.H2_DATA_DIRECTORY_PATH).replace("\\", "/");
            String autoServerMode =  extraUserDataSourceProperties.getProperty(EmbeddedH2DataSourceSpecification.H2_AUTO_SERVER_MODE);
            return "jdbc:h2:file:" + dataDirectoryPath + "/" + databaseName + ";AUTO_SERVER=" + autoServerMode;
        }

        return "jdbc:h2:tcp://" + host + ":" + port + "/mem:" +databaseName;
    }

    @Override
    public Properties getExtraDataSourceProperties(AuthenticationStrategy authenticationStrategy)
    {
        return new Properties();
    }

    @Override
    public String getDriver()
    {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Driver";
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport() {
        return new H2Commands();
    }

    private boolean isEmbeddedMode(Properties properties)
    {
        return properties.containsKey(EmbeddedH2DataSourceSpecification.H2_DATA_DIRECTORY_PATH);
    }

    @Override
    public boolean publishMetrics()
    {
        return false;
    }
}
