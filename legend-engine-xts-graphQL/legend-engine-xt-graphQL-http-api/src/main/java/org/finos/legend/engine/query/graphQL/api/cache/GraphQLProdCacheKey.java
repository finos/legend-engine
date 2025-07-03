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

public abstract class GraphQLProdCacheKey implements GraphQLCacheKey
{
    protected String groupID;
    protected String artifactId;
    protected String versionId;
    protected String queryClassPath;
    protected String query;

    public GraphQLProdCacheKey(String groupID, String artifactId, String versionId, String queryClassPath, String query)
    {
        this.groupID = groupID;
        this.artifactId = artifactId;
        this.versionId = versionId;
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
        return Objects.equals(groupID, that.groupID)
                && Objects.equals(artifactId, that.artifactId)
                && Objects.equals(versionId, that.versionId)
                && Objects.equals(queryClassPath, that.queryClassPath)
                && Objects.equals(query, that.query);
    }

    public String getGroupID()
    {
        return groupID;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersionId()
    {
        return versionId;
    }

    public String getQueryClassPath()
    {
        return queryClassPath;
    }

    public String getQuery()
    {
        return query;
    }

    public String getProjectBasePath()
    {
        return String.format("%s:%s:%s", groupID, artifactId, versionId);
    }
}
