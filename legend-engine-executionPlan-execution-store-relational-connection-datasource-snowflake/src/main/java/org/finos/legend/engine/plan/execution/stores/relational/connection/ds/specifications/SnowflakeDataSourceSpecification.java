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
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.eclipse.collections.api.list.MutableList;
import org.pac4j.core.profile.CommonProfile;

import java.util.Properties;
import javax.sql.DataSource;

public class SnowflakeDataSourceSpecification extends DataSourceSpecification
{
    public static String SNOWFLAKE_ACCOUNT_NAME = "legend_snowflake_accountName";
    public static String SNOWFLAKE_REGION = "legend_snowflake_region";
    public static String SNOWFLAKE_WAREHOUSE_NAME = "legend_snowflake_warehouseName";
    public static String SNOWFLAKE_DATABASE_NAME= "legend_snowflake_databaseName";
    public static String SNOWFLAKE_CLOUD_TYPE= "legend_snowflake_cloudType";
    public static String SNOWFLAKE_QUOTE_IDENTIFIERS= "legend_snowflake_quoteIdentifiers";


    public SnowflakeDataSourceSpecification(SnowflakeDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties, RelationalExecutorInfo relationalExecutorInfo)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties, relationalExecutorInfo);

        String warehouseName = updateSnowflakeIdentifiers(key.getWarehouseName(), key.getQuoteIdentifiers());
        String databaseName  = updateSnowflakeIdentifiers(key.getDatabaseName(), key.getQuoteIdentifiers());

        this.extraDatasourceProperties.put(SNOWFLAKE_ACCOUNT_NAME, key.getAccountName());
        this.extraDatasourceProperties.put(SNOWFLAKE_REGION, key.getRegion());
        this.extraDatasourceProperties.put(SNOWFLAKE_WAREHOUSE_NAME, warehouseName);
        this.extraDatasourceProperties.put(SNOWFLAKE_DATABASE_NAME, databaseName);
        this.extraDatasourceProperties.put(SNOWFLAKE_CLOUD_TYPE, key.getCloudType());
        this.extraDatasourceProperties.put(SNOWFLAKE_QUOTE_IDENTIFIERS, key.getQuoteIdentifiers());

        this.extraDatasourceProperties.put("account", key.getAccountName());
        this.extraDatasourceProperties.put("warehouse", warehouseName);
        this.extraDatasourceProperties.put("db", databaseName);
        this.extraDatasourceProperties.put("ocspFailOpen", true);
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

    public static String updateSnowflakeIdentifiers(String identifier, boolean quoteIdentifiers)
    {
        if (quoteIdentifiers && identifier != null && !(identifier.startsWith("\"") && identifier.endsWith("\"")))
        {
            identifier = "\"" + identifier + "\"";
        }
        return identifier;
    }
}
