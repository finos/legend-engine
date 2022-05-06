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
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.redshift.RedshiftManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.RedshiftDataSourceSpecificationKey;

import java.util.Properties;

public class RedshiftDataSourceSpecification extends DataSourceSpecification
{

    public RedshiftDataSourceSpecification(RedshiftDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy)
    {
        this(key, databaseManager, authenticationStrategy,addPropertiesFromDataSource(key));
    }

    private RedshiftDataSourceSpecification(RedshiftDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties);
    }

    private static Properties addPropertiesFromDataSource(RedshiftDataSourceSpecificationKey key)
    {
        java.util.Properties props =  new Properties();
        props.put(RedshiftManager.CLUSTER_ID,key.getClusterID());
        props.put(RedshiftManager.REGION,key.getRegion());
        return props;
    }


    @Override
    protected String getJdbcUrl(String host, int port, String databaseName, Properties properties)
    {
        return super.getJdbcUrl(
                ((RedshiftDataSourceSpecificationKey)this.datasourceKey).getHost(),
                ((RedshiftDataSourceSpecificationKey)this.datasourceKey).getPort(),
                ((RedshiftDataSourceSpecificationKey)this.datasourceKey).getDatabaseName(),
                properties);
    }
}
