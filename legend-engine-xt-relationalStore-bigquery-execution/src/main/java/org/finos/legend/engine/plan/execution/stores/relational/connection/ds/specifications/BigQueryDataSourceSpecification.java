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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications;

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.BigQueryDataSourceSpecificationKey;

import java.util.Optional;
import java.util.Properties;

public class BigQueryDataSourceSpecification extends DataSourceSpecification
{
    public static String BIGQUERY_PROJECT_ID = "bigquery_projectId";
    public static String BIGQUERY_DATASET_NAME = "bigquery_defaultDataset";
    public static String BIGQUERY_PROXY_HOST = "bigquery_proxyHost";
    public static String BIGQUERY_PROXY_PORT = "bigquery_proxyPort";


    public BigQueryDataSourceSpecification(BigQueryDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties);
        this.extraDatasourceProperties.put(BIGQUERY_PROJECT_ID, key.getProjectId());
        this.extraDatasourceProperties.put(BIGQUERY_DATASET_NAME, key.getDefaultDataset());
        putIfNotEmpty(this.extraDatasourceProperties, BIGQUERY_PROXY_HOST, key.getProxyHost());
        putIfNotEmpty(this.extraDatasourceProperties, BIGQUERY_PROXY_PORT, key.getProxyPort());

        this.extraDatasourceProperties.put("projectId", key.getProjectId());
        this.extraDatasourceProperties.put("defaultDataset", key.getDefaultDataset());
    }

    private void putIfNotEmpty(Properties connectionProperties, String propName, String propValue)
    {
        Optional.ofNullable(propValue).ifPresent(x -> connectionProperties.put(propName, propValue));
    }

    public BigQueryDataSourceSpecification(BigQueryDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy)
    {
        this(key, databaseManager, authenticationStrategy, new Properties());
    }
}
