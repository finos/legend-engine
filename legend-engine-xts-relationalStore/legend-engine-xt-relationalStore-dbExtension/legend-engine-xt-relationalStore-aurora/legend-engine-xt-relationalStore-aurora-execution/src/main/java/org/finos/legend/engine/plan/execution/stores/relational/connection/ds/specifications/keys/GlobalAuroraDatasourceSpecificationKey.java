// Copyright 2026 Goldman Sachs
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

import java.util.List;
import java.util.Objects;

public class GlobalAuroraDatasourceSpecificationKey implements DataSourceSpecificationKey
{
    private final String host;
    private final int port;
    private final String name;
    private final String region;
    private final List<String> globalClusterInstanceHostPatterns;

    public GlobalAuroraDatasourceSpecificationKey(String host, int port, String name, String region, List<String> globalClusterInstanceHostPatterns)
    {
        this.host = host;
        this.port = port;
        this.name = name;
        this.region = region;
        this.globalClusterInstanceHostPatterns = globalClusterInstanceHostPatterns;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getName()
    {
        return name;
    }

    public String getRegion()
    {
        return region;
    }

    public List<String> getGlobalClusterInstanceHostPatterns()
    {
        return globalClusterInstanceHostPatterns;
    }

    @Override
    public String shortId()
    {
        return "GlobalAurora_" +
                "host:" + host + "_" +
                "port:" + port + "_" +
                "name:" + name + "_" +
                "region:" + region;
    }

    @Override
    public String toString()
    {
        return "GlobalAuroraDatasourceSpecificationKey{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", name='" + name + '\'' +
                ", region='" + region + '\'' +
                ", globalClusterInstanceHostPatterns=" + globalClusterInstanceHostPatterns +
                '}';
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof GlobalAuroraDatasourceSpecificationKey))
        {
            return false;
        }
        GlobalAuroraDatasourceSpecificationKey that = (GlobalAuroraDatasourceSpecificationKey) o;
        return port == that.port && Objects.equals(host, that.host) && Objects.equals(name, that.name) && Objects.equals(region, that.region) && Objects.equals(globalClusterInstanceHostPatterns, that.globalClusterInstanceHostPatterns);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(host, port, name, region, globalClusterInstanceHostPatterns);
    }
}
