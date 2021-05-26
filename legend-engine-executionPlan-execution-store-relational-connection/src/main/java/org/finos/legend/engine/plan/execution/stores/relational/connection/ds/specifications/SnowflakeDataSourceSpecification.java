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
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.eclipse.collections.api.list.MutableList;
import org.pac4j.core.profile.CommonProfile;

import java.util.Optional;
import java.util.Properties;
import javax.sql.DataSource;

public class SnowflakeDataSourceSpecification extends DataSourceSpecification
{
    public static String SNOWFLAKE_ACCOUNT_NAME = "alloy_snowflake_accountName";
    public static String SNOWFLAKE_REGION = "alloy_snowflake_region";
    public static String SNOWFLAKE_WAREHOUSE_NAME = "alloy_snowflake_warehouseName";
    public static String SNOWFLAKE_DATABASE_NAME= "alloy_snowflake_databaseName";

    public static String SNOWFLAKE_ACCOUNT_TYPE_NAME = "accountType";
    public static String SNOWFLAKE_ORGANISATION_NAME = "organisation";
    public static String SNOWFLAKE_CLOUD_TYPE_NAME = "cloudType";

    public SnowflakeDataSourceSpecification(SnowflakeDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties, RelationalExecutorInfo relationalExecutorInfo)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties, relationalExecutorInfo);

        this.extraDatasourceProperties.put(SNOWFLAKE_ACCOUNT_NAME, key.getAccountName());
        this.extraDatasourceProperties.put(SNOWFLAKE_REGION, key.getRegion());
        this.extraDatasourceProperties.put(SNOWFLAKE_WAREHOUSE_NAME, key.getWarehouseName());
        this.extraDatasourceProperties.put(SNOWFLAKE_DATABASE_NAME, key.getDatabaseName());

        this.extraDatasourceProperties.put("account", key.getAccountName());
        this.extraDatasourceProperties.put("warehouse", key.getWarehouseName());
        this.extraDatasourceProperties.put("db", key.getDatabaseName());
        this.extraDatasourceProperties.put("ocspFailOpen", true);

        StringBuilder accountType = new StringBuilder();
        Optional.ofNullable(key.getAccountType()).ifPresent(x -> accountType.append(x.toString()));
        putIfNotEmpty(this.extraDatasourceProperties, SNOWFLAKE_ACCOUNT_TYPE_NAME, accountType.toString());
        putIfNotEmpty(this.extraDatasourceProperties, SNOWFLAKE_ORGANISATION_NAME, key.getOrganisation());
        putIfNotEmpty(this.extraDatasourceProperties, SNOWFLAKE_CLOUD_TYPE_NAME, key.getCloudType());

        if (key.getProxyHost() != null)
        {
            this.extraDatasourceProperties.put("useProxy", true);
        }
        putIfNotEmpty(this.extraDatasourceProperties, "proxyHost", key.getProxyHost());
        putIfNotEmpty(this.extraDatasourceProperties, "proxyPort", key.getProxyPort());
        putIfNotEmpty(this.extraDatasourceProperties, "nonProxyHosts", key.getNonProxyHosts());
    }

    private static void putIfNotEmpty(Properties connectionProperties, String propName, String propValue)
    {
        Optional.ofNullable(propValue).ifPresent(x -> connectionProperties.put(propName, propValue));
    }

    public SnowflakeDataSourceSpecification(SnowflakeDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, RelationalExecutorInfo relationalExecutorInfo)
    {
        this(key, databaseManager, authenticationStrategy, new Properties(), relationalExecutorInfo);
    }

    @Override
    protected DataSource buildDataSource(MutableList<CommonProfile> profiles)
    {
        return this.buildDataSource(null, -1, null, profiles);
    }

    public Properties getConnectionProperties()
    {
        return this.extraDatasourceProperties;
    }
}
