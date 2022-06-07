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

public class BigQueryDataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private final String projectId;
    private final String defaultDataset;
    private String proxyHost;
    private String proxyPort;

    public BigQueryDataSourceSpecificationKey(String projectId, String defaultDataset, String proxyHost, String proxyPort)
    {
        this.projectId = projectId;
        this.defaultDataset = defaultDataset;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    public String getProjectId()
    {
        return projectId;
    }

    public String getDefaultDataset()
    {
        return defaultDataset;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public String getProxyPort()
    {
        return proxyPort;
    }

    @Override
    public String toString()
    {
        return "BigQuerySpecificationKey{" +
                "projectId='" + projectId + '\'' +
                ",defaultDataset='" + defaultDataset + '\'' +
                ",proxyHost='" + proxyHost + '\'' +
                ",proxyPort='" + proxyPort + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "BigQuery" +
                "projectId:" + projectId + "_" +
                "defaultDataset:" + defaultDataset + "_" +
                "proxyHost:" + proxyHost + "_" +
                "proxyPort:" + proxyPort + "_";
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
        BigQueryDataSourceSpecificationKey that = (BigQueryDataSourceSpecificationKey) o;
        return Objects.equals(projectId, that.projectId) &&
                Objects.equals(defaultDataset, that.defaultDataset) &&
                Objects.equals(proxyHost, that.proxyHost) &&
                Objects.equals(proxyPort, that.proxyPort);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(projectId, defaultDataset, proxyHost, proxyPort);
    }
}
