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
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.DeltaLakeDataSourceSpecificationKey;
import org.pac4j.core.profile.CommonProfile;

import javax.sql.DataSource;
import java.util.Properties;

public class DeltaLakeDataSourceSpecification extends DataSourceSpecification
{

    public static String DELTALAKE_SHARD = "legend_deltalake_shard";
    public static String DELTALAKE_HTTP_PATH = "legend_deltalake_http_path";
    public static String DELTALAKE_API_TOKEN = "legend_deltalake_api_token";

    public DeltaLakeDataSourceSpecification(DeltaLakeDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties, RelationalExecutorInfo relationalExecutorInfo)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties, relationalExecutorInfo);

        this.extraDatasourceProperties.put(DELTALAKE_SHARD, key.getShard());
        this.extraDatasourceProperties.put(DELTALAKE_HTTP_PATH, key.getHttpPath());
        this.extraDatasourceProperties.put(DELTALAKE_API_TOKEN, key.getToken());
        this.extraDatasourceProperties.put("shard", key.getShard());
        this.extraDatasourceProperties.put("httpPath", key.getHttpPath());
        this.extraDatasourceProperties.put("token", key.getToken());
    }

    public DeltaLakeDataSourceSpecification(DeltaLakeDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, RelationalExecutorInfo relationalExecutorInfo)
    {
        this(key, databaseManager, authenticationStrategy, new Properties(), relationalExecutorInfo);
    }

    @Override
    protected DataSource buildDataSource(MutableList<CommonProfile> profiles)
    {
        return this.buildDataSource(null, -1, null, profiles);
    }
}
