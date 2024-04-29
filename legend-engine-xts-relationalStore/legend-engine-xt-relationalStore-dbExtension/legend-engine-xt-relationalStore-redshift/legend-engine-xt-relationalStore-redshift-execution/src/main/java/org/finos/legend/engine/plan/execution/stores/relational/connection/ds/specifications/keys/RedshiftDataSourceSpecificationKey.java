//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys;

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;

public class RedshiftDataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private String host;
    private int port;
    private String databaseName;
    private String clusterID;

    private String region;
    private String endpointURL;

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public String getClusterID()
    {
        return clusterID;
    }

    public String getRegion()
    {
        return region;
    }

    public String getEndPointURL()
    {
        return endpointURL;
    }

    public RedshiftDataSourceSpecificationKey(String host, int port, String databaseName, String clusterID, String region, String endpointURL)
    {
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.clusterID = clusterID;
        this.region = region;
        this.endpointURL = endpointURL;
    }


    @Override
    public String toString()
    {
        return "RedshiftDataSourceSpecificationKey{" +
                "host='" + host + '\'' +
                ", region='" + region + '\'' +
                ", port='" + port + '\'' +
                ", databaseName='" + databaseName + '\'' +
                ", clusterID='" + clusterID + '\'' +
                ", endpointURL='" + endpointURL + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "Redshift_" +
                "host:" + host + "_" +
                "region:" + region + "_" +
                "port:" + port + "_" +
                "databaseName:" + databaseName + "_" +
                "clusterID:" + clusterID + "_" +
                "endpointURL:" + endpointURL;
    }
}