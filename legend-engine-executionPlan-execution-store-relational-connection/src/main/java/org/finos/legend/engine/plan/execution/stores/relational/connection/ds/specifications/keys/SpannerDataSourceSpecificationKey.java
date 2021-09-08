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

public class SpannerDataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private final String projectId;
    private final String instanceId;
    private final String databaseId;

    public SpannerDataSourceSpecificationKey(String projectId, String instanceId, String databaseId)
    {
        this.projectId = projectId;
        this.instanceId = instanceId;
        this.databaseId = databaseId;
    }

    public String getProjectId()
    {
        return projectId;
    }

    public String getInstanceId()
    {
        return instanceId;
    }

    public String getDatabaseId() { return databaseId;}

    @Override
    public String toString()
    {
        return "SpannerSpecificationKey{" +
                "projectId='" + projectId + '\'' +
                ",instanceId='" + instanceId + '\'' +
                ",databaseId='" + databaseId + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "Spanner" +
                "projectId:" + projectId + "_" +
                "instanceId:" + instanceId + "_" +
                "databaseId:" + databaseId;
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
        SpannerDataSourceSpecificationKey that = (SpannerDataSourceSpecificationKey) o;
        return Objects.equals(projectId, that.projectId) &&
                Objects.equals(instanceId, that.instanceId) &&
                Objects.equals(databaseId, that.databaseId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(projectId, instanceId, databaseId);
    }
}
