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

public class RedshiftDataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private final String clusterID;
    private final String clusterName;
    private final String databaseName;
    private final int port;
    private final String region;


    public RedshiftDataSourceSpecificationKey(String clusterID, String clusterName, String databaseName, int port, String region)
    {
        this.clusterID = clusterID;
        this.clusterName = clusterName;
        this.databaseName = databaseName;
        this.port = port;
        this.region = region;
    }

    public String getClusterID()
    {
        return clusterID;
    }

    public String getClusterName()
    {
        return clusterName;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public int getPort()
    {
        return port;
    }

    public String getRegion()
    {
        return region;
    }

    @Override
    public String toString()
    {
        return "RedshiftDataSourceSpecificationKey{" +
                "clusterID='" + clusterID + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", databaseName='" + databaseName + '\'' +
                ", port='" + port + '\'' +
                ", region='" + region + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "Redshift_" +
                "clusterID:" + clusterID + "_" +
                "clusterName:" + clusterName + "_" +
                "databaseName:" + databaseName + "_" +
                "port:" + port + "_" +
                "region:" + region;
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
        RedshiftDataSourceSpecificationKey that = (RedshiftDataSourceSpecificationKey) o;
        return Objects.equals(clusterID, that.clusterID) &&
                Objects.equals(clusterName, that.clusterName) &&
                Objects.equals(databaseName, that.databaseName) &&
                Objects.equals(port, that.port) &&
                Objects.equals(region, that.region);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(clusterID, clusterName, databaseName, port, region);
    }
}
