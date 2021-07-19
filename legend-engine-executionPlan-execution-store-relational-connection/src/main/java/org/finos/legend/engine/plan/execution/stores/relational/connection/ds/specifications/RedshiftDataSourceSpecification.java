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

import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.RedshiftDataSourceSpecificationKey;
import org.eclipse.collections.api.list.MutableList;
import org.pac4j.core.profile.CommonProfile;

import java.util.Properties;
import javax.sql.DataSource;

public class RedshiftDataSourceSpecification extends DataSourceSpecification
{
    public static String REDSHIFT_CLUSTER_ID = "legend_redshift_clusterID";
    public static String REDSHIFT_CLUSTER_NAME = "legend_redshift_clusterName";
    public static String REDSHIFT_DATABASE_NAME= "legend_redshift_databaseName";
    public static String REDSHIFT_PORT = "legend_redshift_port";
    public static String REDSHIFT_REGION = "legend_redshift_region";


    public RedshiftDataSourceSpecification(RedshiftDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties, RelationalExecutorInfo relationalExecutorInfo)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties, relationalExecutorInfo);

        this.extraDatasourceProperties.put(REDSHIFT_CLUSTER_ID, key.getClusterID());
        this.extraDatasourceProperties.put(REDSHIFT_CLUSTER_NAME, key.getClusterName());
        this.extraDatasourceProperties.put(REDSHIFT_DATABASE_NAME, key.getDatabaseName());
        this.extraDatasourceProperties.put(REDSHIFT_PORT, key.getPort());
        this.extraDatasourceProperties.put(REDSHIFT_REGION, key.getRegion());

    }

    public RedshiftDataSourceSpecification(RedshiftDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, RelationalExecutorInfo relationalExecutorInfo)
    {
        this(key, databaseManager, authenticationStrategy, new Properties(), relationalExecutorInfo);
    }

    @Override
    protected DataSource buildDataSource(MutableList<CommonProfile> profiles)
    {
        return this.buildDataSource(null, (int)this.extraDatasourceProperties.get(REDSHIFT_PORT), (String)this.extraDatasourceProperties.get(REDSHIFT_DATABASE_NAME), profiles);
    }
}
