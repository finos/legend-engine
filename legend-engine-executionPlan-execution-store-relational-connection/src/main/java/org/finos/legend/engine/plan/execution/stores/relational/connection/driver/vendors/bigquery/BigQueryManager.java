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
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.GCPWorkloadIdentityFederationWithAWSAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.BigQueryDataSourceSpecification;

import java.util.Properties;

public class BigQueryManager extends DatabaseManager
{
    public final String BIGQUERY_JDBC_URL = "jdbc:bigquery://https://www.googleapis.com/bigquery/v2:443;";

    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("BigQuery");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy)
    {
        switch (authenticationStrategy.getKey().type())
        {
            case GCPApplicationDefaultCredentialsAuthenticationStrategyKey.TYPE:
                return buildUrlWithApplicationDefaultCredentials(extraUserDataSourceProperties, (GCPApplicationDefaultCredentialsAuthenticationStrategy) authenticationStrategy);
            case GCPWorkloadIdentityFederationWithAWSAuthenticationStrategyKey.TYPE:
                return buildUrlWithWorkloadIdentityFederation(extraUserDataSourceProperties, (GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy) authenticationStrategy);

        }
        throw new UnsupportedOperationException("Unsupported auth strategy :" + authenticationStrategy.getKey().type());
    }

    private String buildUrlWithApplicationDefaultCredentials(Properties extraUserDataSourceProperties, GCPApplicationDefaultCredentialsAuthenticationStrategy authenticationStrategy)
    {
        GCPApplicationDefaultCredentialsAuthenticationStrategy GCPApplicationDefaultCredentialsAuthenticationStrategy = authenticationStrategy;
        String url = String.format(BIGQUERY_JDBC_URL +
                        "ProjectId=%s;" +
                        this.buildParamForDefaultDataset(extraUserDataSourceProperties) +
                        "OAuthType=3;",
                extraUserDataSourceProperties.getProperty(BigQueryDataSourceSpecification.BIGQUERY_PROJECT_ID));
        return url;
    }

    private String buildUrlWithWorkloadIdentityFederation(Properties extraUserDataSourceProperties, GCPWorkloadIdentityFederationWithAWSAuthenticationStrategy authenticationStrategy){
        String url = String.format(BIGQUERY_JDBC_URL +
                        "ProjectId=%s;" +
                        this.buildParamForDefaultDataset(extraUserDataSourceProperties) +
                        "OAuthType=2;",
                extraUserDataSourceProperties.getProperty(BigQueryDataSourceSpecification.BIGQUERY_PROJECT_ID));
        return url;
    }

    private String buildParamForDefaultDataset(Properties extraUserDataSourceProperties)
    {
        String defaultDataset = extraUserDataSourceProperties.getProperty(BigQueryDataSourceSpecification.BIGQUERY_DATASET_NAME);
        if (defaultDataset != null && !defaultDataset.trim().isEmpty())
        {
            return String.format("DefaultDataset=%s;", defaultDataset);
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
