//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.redshift;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.Properties;

public class RedshiftManager extends DatabaseManager
{

    public static final String USE_IAM = "USE_IAM";
    public static final String CLUSTER_ID = "ClusterID";
    public static final String REGION = "Region";

    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("Redshift");
    }


    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategyRuntime authenticationStrategyRuntime)
    {
        String protocol = "";
        if (extraUserDataSourceProperties.containsKey(USE_IAM))
        {
            protocol = "iam:";
        }
        String url = "jdbc:redshift:" + protocol + "//" + host + ":" + port + "/" + databaseName;
        return url;
    }

    @Override
    public Properties getExtraDataSourceProperties(AuthenticationStrategyRuntime authenticationStrategyRuntime, Identity identity)
    {
        return new Properties();
    }

    @Override
    public String getDriver()
    {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.redshift.RedshiftDriver";
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport()
    {
        return new RedshiftCommands();
    }
}
