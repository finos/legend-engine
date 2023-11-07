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

public class GraphQLProdMappingRuntimeCacheKey extends GraphQLProdCacheKey
{
    private String mappingPath;
    private String runtimePath;

    public GraphQLProdMappingRuntimeCacheKey(String groupID, String artifactId, String versionId, String mappingPath, String runtimePath, String queryClassPath, String query)
    {
        super(groupID, artifactId, versionId, queryClassPath, query);
        this.mappingPath = mappingPath;
        this.runtimePath = runtimePath;
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
        GraphQLProdMappingRuntimeCacheKey that = (GraphQLProdMappingRuntimeCacheKey) o;
        return super.equals(that)
                && Objects.equals(mappingPath, that.mappingPath)
                && Objects.equals(runtimePath, that.runtimePath);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(groupID, artifactId, versionId, mappingPath, runtimePath, queryClassPath, query);
    }

    public String getMappingPath()
    {
        return mappingPath;
    }

    public String getRuntimePath()
    {
        return runtimePath;
    }
}
