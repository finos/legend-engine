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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SpannerDataSourceSpecificationKey;
import org.pac4j.core.profile.CommonProfile;

import javax.sql.DataSource;
import java.util.Properties;

public class SpannerDataSourceSpecification extends DataSourceSpecification
{
    public static String SPANNER_PROJECT_ID = "spanner_projectId";
    public static String SPANNER_INSTANCE_ID = "spanner_instanceId";
    public static String SPANNER_DATABASE_ID = "spanner_databaseId";

    public SpannerDataSourceSpecification(SpannerDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties, RelationalExecutorInfo relationalExecutorInfo)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties, relationalExecutorInfo);
        this.extraDatasourceProperties.put(SPANNER_PROJECT_ID, key.getProjectId());
        this.extraDatasourceProperties.put(SPANNER_INSTANCE_ID, key.getInstanceId());
        this.extraDatasourceProperties.put(SPANNER_DATABASE_ID, key.getDatabaseId());

        this.extraDatasourceProperties.put("projectId", key.getProjectId());
        this.extraDatasourceProperties.put("instanceId", key.getInstanceId());
        this.extraDatasourceProperties.put("databaseId", key.getDatabaseId());
    }

    public SpannerDataSourceSpecification(SpannerDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, RelationalExecutorInfo relationalExecutorInfo)
    {
        this(key, databaseManager, authenticationStrategy, new Properties(), relationalExecutorInfo);
    }

    @Override
    protected DataSource buildDataSource(MutableList<CommonProfile> profiles)
    {
        return this.buildDataSource(null, -1, null, profiles);
    }
}
