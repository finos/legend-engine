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

import com.google.common.base.Objects;

public class GraphQLProdCacheKey implements GraphQLCacheKey
{
    private String groupID;
    private String artifactId;
    private String versionId;
    private String mappingPath;
    private String runtimePath;
    private String queryClassPath;
    private String query;

    public GraphQLProdCacheKey(String groupID, String artifactId, String versionId, String mappingPath, String runtimePath, String queryClassPath, String query)
    {
        this.groupID = groupID;
        this.artifactId = artifactId;
        this.versionId = versionId;
        this.mappingPath = mappingPath;
        this.runtimePath = runtimePath;
        this.queryClassPath = queryClassPath;
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
        GraphQLProdCacheKey that = (GraphQLProdCacheKey) o;
        return Objects.equal(groupID, that.groupID) && Objects.equal(artifactId, that.artifactId) && Objects.equal(versionId, that.versionId) && Objects.equal(mappingPath, that.mappingPath) && Objects.equal(runtimePath, that.runtimePath) && Objects.equal(queryClassPath, that.queryClassPath) && Objects.equal(query, that.query);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(groupID, artifactId, versionId, mappingPath, runtimePath, queryClassPath, query);
    }
}
