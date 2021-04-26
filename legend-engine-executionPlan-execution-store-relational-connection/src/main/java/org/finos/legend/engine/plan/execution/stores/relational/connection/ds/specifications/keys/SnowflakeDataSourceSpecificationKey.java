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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys;

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;

import java.util.Objects;

public class SnowflakeDataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private final String accountName;
    private final String region;
    private final String warehouseName;
    private final String databaseName;

    private String proxyHost;
    private String proxyPort;
    private String nonProxyHosts;

    private SnowflakeAccountType accountType;
    private String organisation;
    private String cloudType;

    public SnowflakeDataSourceSpecificationKey(String accountName, String region, String warehouseName, String databaseName)
    {
        this.accountName = accountName;
        this.region = region;
        this.warehouseName = warehouseName;
        this.databaseName = databaseName;
    }

    public String getAccountName()
    {
        return accountName;
    }

    public String getRegion()
    {
        return region;
    }

    public String getWarehouseName()
    {
        return warehouseName;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public SnowflakeDataSourceSpecificationKey(String accountName, String region, String warehouseName, String databaseName, String proxyHost, String proxyPort, String nonProxyHosts, String accountType, String organisation, String cloudType)
    {
        this.accountName = accountName;
        this.region = region;
        this.warehouseName = warehouseName;
        this.databaseName = databaseName;

        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.nonProxyHosts = nonProxyHosts;

        this.accountType = SnowflakeAccountType.valueOf(accountType);
        this.organisation = organisation;
        this.cloudType = cloudType;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public String getProxyPort()
    {
        return proxyPort;
    }

    public String getNonProxyHosts()
    {
        return nonProxyHosts;
    }

    public SnowflakeAccountType getAccountType()
    {
        return accountType;
    }

    public String getOrganisation()
    {
        return organisation;
    }

    public String getCloudType()
    {
        return cloudType;
    }


    @Override
    public String toString()
    {
        return "SnowflakeDataSourceSpecificationKey{" +
                "accountName='" + accountName + '\'' +
                ", region='" + region + '\'' +
                ", warehouseName='" + warehouseName + '\'' +
                ", databaseName='" + databaseName + '\'' +
                ", proxyHost='" + proxyHost + '\'' +
                ", proxyPort='" + proxyPort + '\'' +
                ", nonProxyHosts='" + nonProxyHosts + '\'' +
                ", accountType='" + accountType + '\'' +
                ", organisation='" + organisation + '\'' +
                ", cloudType='" + cloudType + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "Snowflake_" +
                "account:" + accountName + "_" +
                "warehouse:" + warehouseName + "_" +
                "db:" + databaseName;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        SnowflakeDataSourceSpecificationKey that = (SnowflakeDataSourceSpecificationKey) o;
        return Objects.equals(accountName, that.accountName) &&
                Objects.equals(region, that.region) &&
                Objects.equals(warehouseName, that.warehouseName) &&
                Objects.equals(databaseName, that.databaseName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(accountName, region, warehouseName, databaseName, proxyHost, proxyHost, nonProxyHosts, accountType, region, organisation);
    }
}
