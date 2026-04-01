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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.aurora;

import java.util.Properties;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.postgres.PostgresCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.AuroraDatasourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.GlobalAuroraDatasourceSpecificationKey;
import org.finos.legend.engine.shared.core.identity.Identity;

public class AuroraManager extends DatabaseManager
{
    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("Aurora");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy)
    {
        String actualHost = extraUserDataSourceProperties.getProperty("Host");
        String actualPort = extraUserDataSourceProperties.getProperty("Port");
        String actualDb = extraUserDataSourceProperties.getProperty("DatabaseName");
        return "jdbc:aws-wrapper:postgresql://" + actualHost + ":" + actualPort + "/" + actualDb;
    }

    @Override
    public String getDriver()
    {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.aurora.AuroraDriver";
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport()
    {
        return new PostgresCommands();
    }

    @Override
    public Properties getObjectDataSourceProperties(DataSourceSpecificationKey key, AuthenticationStrategy authenticationStrategy, Identity identity)
    {
        Properties properties = new Properties();
        properties.put("clusterId", key.shortId());

        if (key instanceof AuroraDatasourceSpecificationKey)
        {
            AuroraDatasourceSpecificationKey auroraKey = (AuroraDatasourceSpecificationKey) key;
            if (auroraKey.getClusterInstanceHostPattern() != null)
            {
                properties.put("clusterInstanceHostPattern", auroraKey.getClusterInstanceHostPattern());
            }
            properties.put("wrapperPlugins", "initialConnection,failover2,efm2");
            properties.put("wrapperDialect", "aurora-pg");
        }
        else if (key instanceof GlobalAuroraDatasourceSpecificationKey)
        {
            GlobalAuroraDatasourceSpecificationKey globalKey = (GlobalAuroraDatasourceSpecificationKey) key;
            properties.put("failoverHomeRegion", globalKey.getRegion());
            properties.put("globalClusterInstanceHostPatterns", String.join(",", globalKey.getGlobalClusterInstanceHostPatterns()));
            properties.put("wrapperPlugins", "initialConnection,gdbFailover,efm2");
            properties.put("wrapperDialect", "global-aurora-pg");
        }

        return properties;
    }
}
