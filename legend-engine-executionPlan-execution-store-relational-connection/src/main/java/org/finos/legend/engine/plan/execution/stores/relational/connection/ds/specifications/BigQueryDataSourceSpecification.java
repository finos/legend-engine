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

import java.util.Properties;

import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.BigQueryDataSourceSpecificationKey;

public class BigQueryDataSourceSpecification extends DataSourceSpecification
{
    public static String BIGQUERY_PROJECT_ID = "bigquery_projectId";
    public static String BIGQUERY_DATASET_NAME = "bigquery_defaultDataset";

    public BigQueryDataSourceSpecification(BigQueryDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties, RelationalExecutorInfo relationalExecutorInfo)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties, relationalExecutorInfo);
        this.extraDatasourceProperties.put(BIGQUERY_PROJECT_ID, key.getProjectId());
        this.extraDatasourceProperties.put(BIGQUERY_DATASET_NAME, key.getDefaultDataset());

        this.extraDatasourceProperties.put("projectId", key.getProjectId());
        this.extraDatasourceProperties.put("defaultDataset", key.getDefaultDataset());
    }

    public BigQueryDataSourceSpecification(BigQueryDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, RelationalExecutorInfo relationalExecutorInfo)
    {
        this(key, databaseManager, authenticationStrategy, new Properties(), relationalExecutorInfo);
    }
}
