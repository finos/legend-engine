// Copyright 2023 Goldman Sachs
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

package org.finos.legend.connection.impl;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.connection.DatabaseType;
import org.finos.legend.connection.SnowflakeAccountType;
import org.finos.legend.connection.DatabaseManager;
import org.finos.legend.engine.shared.core.operational.Assert;

import java.util.List;
import java.util.Properties;

public class SnowflakeDatabaseManager implements DatabaseManager
{
    private static final String PRIVATELINK_SNOWFLAKECOMPUTING_COM = ".privatelink.snowflakecomputing.com";
    private static final String SNOWFLAKECOMPUTING_COM = ".snowflakecomputing.com";

    public static final String SNOWFLAKE_ACCOUNT_NAME = "legend_snowflake_accountName";
    public static final String SNOWFLAKE_REGION = "legend_snowflake_region";
    public static final String SNOWFLAKE_WAREHOUSE_NAME = "legend_snowflake_warehouseName";
    public static final String SNOWFLAKE_DATABASE_NAME = "legend_snowflake_databaseName";
    public static final String SNOWFLAKE_CLOUD_TYPE = "legend_snowflake_cloudType";
    public static final String SNOWFLAKE_QUOTE_IDENTIFIERS = "legend_snowflake_quoteIdentifiers";

    public static final String SNOWFLAKE_ACCOUNT_TYPE_NAME = "accountType";
    public static final String SNOWFLAKE_ORGANIZATION_NAME = "organization";

    public static final String SNOWFLAKE_PROXY_HOST = "proxyHost";
    public static final String SNOWFLAKE_PROXY_PORT = "proxyPort";
    public static final String SNOWFLAKE_NON_PROXY_HOSTS = "nonProxyHosts";
    public static final String SNOWFLAKE_USE_PROXY = "useProxy";
    public static final String SNOWFLAKE_ROLE = "role";
    public static final String SNOWFLAKE_ENABLE_QUERY_TAGS = "enableQueryTags";

    @Override
    public List<String> getIds()
    {
        return Lists.mutable.with(DatabaseType.SNOWFLAKE.getLabel());
    }

    @Override
    public String getDriver()
    {
        return "net.snowflake.client.jdbc.SnowflakeDriver";
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties properties)
    {
        Assert.assertTrue(properties.getProperty(SNOWFLAKE_ACCOUNT_NAME) != null, () -> SNOWFLAKE_ACCOUNT_NAME + " is not set");
        Assert.assertTrue(properties.getProperty(SNOWFLAKE_REGION) != null, () -> SNOWFLAKE_REGION + " is not set");
        Assert.assertTrue(properties.getProperty(SNOWFLAKE_WAREHOUSE_NAME) != null, () -> SNOWFLAKE_WAREHOUSE_NAME + " is not set");

        String accountName = properties.getProperty(SNOWFLAKE_ACCOUNT_NAME);
        String region = properties.getProperty(SNOWFLAKE_REGION);
        String cloudType = properties.getProperty(SNOWFLAKE_CLOUD_TYPE);
        String organisation = properties.getProperty(SNOWFLAKE_ORGANIZATION_NAME);

        String accountTypeName = properties.getProperty(SNOWFLAKE_ACCOUNT_TYPE_NAME);
        SnowflakeAccountType accountType = accountTypeName != null ? SnowflakeAccountType.valueOf(accountTypeName) : null;


        StringBuilder URL = new StringBuilder().append("jdbc:snowflake://");
        if (accountType != null)
        {
            if (SnowflakeAccountType.VPS.equals(accountType))
            {
                URL.append(accountName)
                        .append(".").append(organisation)
                        .append(".").append(region)
                        .append(".").append(cloudType);
                URL.append(PRIVATELINK_SNOWFLAKECOMPUTING_COM);
            }
            else if (SnowflakeAccountType.MultiTenant.equals(accountType))
            {
                this.buildMultiTenantHostname(accountName, region, URL);
                URL.append(PRIVATELINK_SNOWFLAKECOMPUTING_COM);
            }
        }
        else
        {
            URL.append(accountName)
                    .append(".").append(region)
                    .append(".").append(cloudType);
            URL.append(SNOWFLAKECOMPUTING_COM);
        }
        return URL.toString();
    }

    public void buildMultiTenantHostname(String accountName, String region, StringBuilder url)
    {
        url.append(accountName).append(".").append(region);
    }
}
