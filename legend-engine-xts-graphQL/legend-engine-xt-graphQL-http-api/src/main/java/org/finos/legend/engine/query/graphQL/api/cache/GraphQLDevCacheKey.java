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

public class GraphQLDevCacheKey implements GraphQLCacheKey
{
    private String projectId;
    private String workspaceId;
    private String queryClassPath;
    private String mappingPath;
    private String runtimePath;
    private String query;

    public GraphQLDevCacheKey(String projectId, String workspaceId, String queryClassPath, String mappingPath, String runtimePath, String query)
    {
        this.projectId = projectId;
        this.workspaceId = workspaceId;
        this.queryClassPath = queryClassPath;
        this.mappingPath = mappingPath;
        this.runtimePath = runtimePath;
        this.query = query;
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
        GraphQLDevCacheKey that = (GraphQLDevCacheKey) o;
        return Objects.equals(projectId, that.projectId)
                && Objects.equals(workspaceId, that.workspaceId)
                && Objects.equals(queryClassPath, that.queryClassPath)
                && Objects.equals(mappingPath, that.mappingPath)
                && Objects.equals(runtimePath, that.runtimePath)
                && Objects.equals(query, that.query);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(projectId, workspaceId, queryClassPath, mappingPath, runtimePath, query);
    }

    public String getQueryClassPath()
    {
        return queryClassPath;
    }

    @Override
    public String getProjectBasePath()
    {
        return String.format("%s:%s", projectId, workspaceId);
    }

    public String getMappingPath()
    {
        return mappingPath;
    }

    public String getRuntimePath()
    {
        return runtimePath;
    }

    public String getQuery()
    {
        return query;
    }
}
