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
    private final String cloudType;
    private final Boolean quoteIdentifiers;

    private String proxyHost;
    private String proxyPort;
    private String nonProxyHosts;

    private SnowflakeAccountType accountType;
    private String organisation;

    private String role;


    public SnowflakeDataSourceSpecificationKey(String accountName, String region, String warehouseName, String databaseName, String cloudType, Boolean quoteIdentifiers, String proxyHost, String proxyPort, String nonProxyHosts, String accountType, String organisation, String role)
    {
        this(accountName, region, warehouseName, databaseName, cloudType, quoteIdentifiers);
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.nonProxyHosts = nonProxyHosts;

        this.accountType = accountType == null ? null : SnowflakeAccountType.valueOf(accountType);
        this.organisation = organisation;
        this.role = role;
    }

    public SnowflakeDataSourceSpecificationKey(String accountName, String region, String warehouseName, String databaseName, String cloudType, Boolean quoteIdentifiers)
    {
        this.accountName = accountName;
        this.region = region;
        this.warehouseName = warehouseName;
        this.databaseName = databaseName;
        this.cloudType = cloudType == null ? "privatelink" : cloudType;
        this.quoteIdentifiers = quoteIdentifiers == null ? false : quoteIdentifiers;
    }

    public SnowflakeDataSourceSpecificationKey(String accountName, String region, String warehouseName, String databaseName, String cloudType, Boolean quoteIdentifiers, String role)
    {
        this.accountName = accountName;
        this.region = region;
        this.warehouseName = warehouseName;
        this.databaseName = databaseName;
        this.cloudType = cloudType == null ? "privatelink" : cloudType;
        this.quoteIdentifiers = quoteIdentifiers == null ? false : quoteIdentifiers;
        this.role = role;
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

    public String getCloudType()
    {
        return cloudType;
    }

    public Boolean getQuoteIdentifiers()
    {
        return quoteIdentifiers;
    }

    public String getOrganisation()
    {
        return organisation;
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

    public String getRole()
    {
        return role;
    }

    @Override
    public String toString()
    {
        return "SnowflakeDataSourceSpecificationKey{" +
                "accountName='" + accountName + '\'' +
                ", region='" + region + '\'' +
                ", warehouseName='" + warehouseName + '\'' +
                ", databaseName='" + databaseName + '\'' +
                ", cloudType='" + cloudType + '\'' +
                ", quoteIdentifiers='" + quoteIdentifiers + '\'' +
                ", proxyHost='" + proxyHost + '\'' +
                ", proxyPort='" + proxyPort + '\'' +
                ", nonProxyHosts='" + nonProxyHosts + '\'' +
                ", accountType='" + accountType + '\'' +
                ", organisation='" + organisation + '\'' +
                ", role='" + role + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "Snowflake_" +
                "account:" + accountName + "_" +
                "region:" + region + "_" +
                "warehouse:" + warehouseName + "_" +
                "db:" + databaseName + "_" +
                "cloudType:" + cloudType + "_" +
                "proxyHost:" + proxyHost + "_" +
                "proxyPort:" + proxyPort + "_" +
                "nonProxyHosts:" + nonProxyHosts + "_" +
                "accountType:" + accountType + "_" +
                "organisation:" + organisation + "_" +
                "quoteIdentifiers:" + quoteIdentifiers +
                "role:" + role;
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
                Objects.equals(databaseName, that.databaseName) &&
                Objects.equals(cloudType, that.cloudType) &&
                Objects.equals(proxyHost, that.proxyHost) &&
                Objects.equals(proxyPort, that.proxyPort) &&
                Objects.equals(nonProxyHosts, that.nonProxyHosts) &&
                Objects.equals(accountType, that.accountType) &&
                Objects.equals(organisation, that.organisation) &&
                Objects.equals(quoteIdentifiers, that.quoteIdentifiers) &&
                Objects.equals(role, that.role);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(accountName, region, warehouseName, databaseName, cloudType, quoteIdentifiers, proxyHost, proxyPort, nonProxyHosts, accountType, organisation, role);
    }
}
