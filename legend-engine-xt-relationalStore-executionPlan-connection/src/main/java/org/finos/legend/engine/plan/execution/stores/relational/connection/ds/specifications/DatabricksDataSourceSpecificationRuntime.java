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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications;

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.DatabricksDataSourceSpecificationKey;

import java.util.Properties;

public class DatabricksDataSourceSpecificationRuntime extends DataSourceSpecificationRuntime
{

    public static String DATABRICKS_HOSTNAME = "legend_databricks_hostname";
    public static String DATABRICKS_PORT = "legend_databricks_port";
    public static String DATABRICKS_PROTOCOL = "legend_databricks_protocol";
    public static String DATABRICKS_HTTP_PATH = "legend_databricks_http_path";

    public DatabricksDataSourceSpecificationRuntime(
            DatabricksDataSourceSpecificationKey key,
            DatabaseManager databaseManager,
            AuthenticationStrategyRuntime authenticationStrategyRuntime,
            Properties extraUserProperties
    )
    {
        super(key, databaseManager, authenticationStrategyRuntime, extraUserProperties);

        this.extraDatasourceProperties.put(DATABRICKS_HOSTNAME, key.getHostname());
        this.extraDatasourceProperties.put(DATABRICKS_PORT, key.getPort());
        this.extraDatasourceProperties.put(DATABRICKS_PROTOCOL, key.getProtocol());
        this.extraDatasourceProperties.put(DATABRICKS_HTTP_PATH, key.getHttpPath());
        this.extraDatasourceProperties.put("hostname", key.getHostname());
        this.extraDatasourceProperties.put("port", key.getPort());
        this.extraDatasourceProperties.put("protocol", key.getProtocol());
        this.extraDatasourceProperties.put("httpPath", key.getHttpPath());
    }

    public DatabricksDataSourceSpecificationRuntime(DatabricksDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategyRuntime authenticationStrategyRuntime)
    {
        this(key, databaseManager, authenticationStrategyRuntime, new Properties());
    }

    public Properties getConnectionProperties()
    {
        return this.extraDatasourceProperties;
    }
}