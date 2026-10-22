//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.query.graphQL.api.cache;

import java.util.Objects;

public class GraphQLProdDataspaceCacheKey extends GraphQLProdCacheKey
{
    private String dataspacePath;
    private String executionContext;

    public GraphQLProdDataspaceCacheKey(String groupID, String artifactId, String versionId, String dataspacePath, String executionContext, String queryClassPath, String query)
    {
        super(groupID, artifactId, versionId, queryClassPath, query);
        this.dataspacePath = dataspacePath;
        this.executionContext = executionContext;
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
        GraphQLProdDataspaceCacheKey that = (GraphQLProdDataspaceCacheKey) o;
        return super.equals(that)
                && Objects.equals(dataspacePath, that.dataspacePath)
                && Objects.equals(executionContext, that.executionContext);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(groupID, artifactId, versionId, dataspacePath, executionContext, queryClassPath, query);
    }

    public String getDataspacePath()
    {
        return dataspacePath;
    }

    public String getExecutionContext()
    {
        return executionContext;
    }

    @Override
    public String getMappingPath()
    {
        return dataspacePath;
    }

    @Override
    public String getRuntimePath()
    {
        return "NA";
    }
}
