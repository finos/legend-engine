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


    public SnowflakeDataSourceSpecificationKey(String accountName, String region, String warehouseName, String databaseName, String cloudType)
    {
        this.accountName = accountName;
        this.region = region;
        this.warehouseName = warehouseName;
        this.databaseName = databaseName;
        this.cloudType = cloudType == null ? "privatelink" : cloudType;
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

    @Override
    public String toString()
    {
        return "SnowflakeDataSourceSpecificationKey{" +
                "accountName='" + accountName + '\'' +
                ", region='" + region + '\'' +
                ", warehouseName='" + warehouseName + '\'' +
                ", databaseName='" + databaseName + '\'' +
                ", cloudType='" + cloudType + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "Snowflake_" +
                "account:" + accountName + "_" +
                "warehouse:" + warehouseName + "_" +
                "db:" + databaseName + "_" +
                "cloudType:" + cloudType;
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
                Objects.equals(cloudType, that.cloudType);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(accountName, region, warehouseName, databaseName, cloudType);
    }
}
