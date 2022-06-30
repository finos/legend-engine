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

import java.util.Optional;
import java.util.Properties;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SpannerDataSourceSpecificationKey;

public class SpannerDataSourceSpecification extends DataSourceSpecification
{

    public static String SPANNER_PROJECT_ID = "spanner_projectId";
    public static String SPANNER_INSTANCE_ID = "spanner_instanceId";
    public static String SPANNER_DATABASE_ID = "spanner_databaseId";
    public static String SPANNER_PROXY_HOST = "spanner_proxyHost";
    public static String SPANNER_PROXY_PORT = "spanner_proxyPort";

    public SpannerDataSourceSpecification(
            SpannerDataSourceSpecificationKey key,
            DatabaseManager databaseManager,
            AuthenticationStrategy authenticationStrategy,
            Properties extraUserProperties)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties);
        this.extraDatasourceProperties.put(SPANNER_PROJECT_ID, key.getProjectId());
        this.extraDatasourceProperties.put(SPANNER_INSTANCE_ID, key.getInstanceId());
        this.extraDatasourceProperties.put(SPANNER_DATABASE_ID, key.getDatabaseId());
        putIfNotEmpty(this.extraDatasourceProperties, SPANNER_PROXY_HOST, key.getProxyHost());
        putIfNotEmpty(this.extraDatasourceProperties, SPANNER_PROXY_PORT, key.getProxyPort());
    }

    public SpannerDataSourceSpecification(
            SpannerDataSourceSpecificationKey key,
            DatabaseManager databaseManager,
            AuthenticationStrategy authenticationStrategy)
    {
        this(key, databaseManager, authenticationStrategy, new Properties());
    }

    private void putIfNotEmpty(Properties connectionProperties, String propName, String propValue)
    {
        Optional.ofNullable(propValue).ifPresent(x -> connectionProperties.put(propName, propValue));
    }

}
