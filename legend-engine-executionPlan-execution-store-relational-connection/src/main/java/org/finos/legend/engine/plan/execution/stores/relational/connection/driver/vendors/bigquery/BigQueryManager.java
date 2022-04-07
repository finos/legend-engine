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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.bigquery;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.GCPApplicationDefaultCredentialsAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.GCPApplicationDefaultCredentialsAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.BigQueryDataSourceSpecification;

import java.util.Properties;

public class BigQueryManager extends DatabaseManager
{
    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("BigQuery");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy)
    {
        String url = String.format("jdbc:bigquery://https://www.googleapis.com/bigquery/v2:443;" +
                        "ProjectId=%s;" +
                        this.buildParamForOptionalProperty(extraUserDataSourceProperties, BigQueryDataSourceSpecification.BIGQUERY_DATASET_NAME, "DefaultDataset") +
                        this.buildParamForOptionalProperty(extraUserDataSourceProperties,BigQueryDataSourceSpecification.BIGQUERY_PROXY_HOST,"ProxyHost") +
                        this.buildParamForOptionalProperty(extraUserDataSourceProperties, BigQueryDataSourceSpecification.BIGQUERY_PROXY_PORT, "ProxyPort"),
                extraUserDataSourceProperties.getProperty(BigQueryDataSourceSpecification.BIGQUERY_PROJECT_ID));
        return url;
    }

    private String buildParamForOptionalProperty(Properties extraUserDataSourceProperties, String optionalPropertyKey, String optionalProperty)
    {
        String optionalPropertyValue = extraUserDataSourceProperties.getProperty(optionalPropertyKey);
        if (optionalPropertyValue != null && !optionalPropertyValue.trim().isEmpty())
        {
            return String.format("%s=%s;",optionalProperty,optionalPropertyValue);
        }
        else
        {
            return "";
        }
    }

    @Override
    public String getDriver()
    {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.bigquery.BigQueryDriver";
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport()
    {
        return new BigQueryCommands();
    }

    private boolean isEmbeddedMode(Properties properties)
    {
        return false;
    }

    @Override
    public boolean publishMetrics()
    {
        return false;
    }
}