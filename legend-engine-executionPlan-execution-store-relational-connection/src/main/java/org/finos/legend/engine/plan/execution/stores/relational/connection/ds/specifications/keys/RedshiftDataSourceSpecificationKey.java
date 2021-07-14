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

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.Region;

import java.util.Objects;

public class RedshiftDataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private final String clusterName;
    private final String clusterID;
    private final String databaseName;
    private final int port;
    private final Region region;


    public RedshiftDataSourceSpecificationKey(String clusterName, String clusterID, String databaseName, int port, Region region)
    {
        this.clusterName = clusterName;
        this.clusterID = clusterID;
        this.databaseName = databaseName;
        this.port = port;
        this.region = region;
    }

    public String getClusterName()
    {
        return clusterName;
    }

    public String getClusterID()
    {
        return clusterID;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public int getPort()
    {
        return port;
    }

    public Region getRegion()
    {
        return region;
    }

    @Override
    public String toString()
    {
        return "RedshiftDataSourceSpecificationKey{" +
                "clusterName='" + clusterName + '\'' +
                ", clusterID='" + clusterID + '\'' +
                ", databaseName='" + databaseName + '\'' +
                ", port='" + port + '\'' +
                ", region='" + region.name() + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "Redshift_" +
                "clusterName:" + clusterName + "_" +
                "clusterID:" + clusterID + "_" +
                "databaseName:" + databaseName + "_" +
                "port:" + port + "_" +
                "region:" + region.name();
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
        return Objects.equals(clusterName, that.clusterName) &&
                Objects.equals(clusterID, that.clusterID) &&
                Objects.equals(databaseName, that.databaseName) &&
                Objects.equals(port, that.port) &&
                Objects.equals(region, that.region);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(clusterName, clusterID, databaseName, port, region);
    }
}
