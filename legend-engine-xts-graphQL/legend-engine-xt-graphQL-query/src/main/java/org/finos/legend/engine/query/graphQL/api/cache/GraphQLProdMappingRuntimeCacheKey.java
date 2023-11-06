package org.finos.legend.engine.query.graphQL.api.cache;

import com.google.common.base.Objects;

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
                && Objects.equal(mappingPath, that.mappingPath)
                && Objects.equal(runtimePath, that.runtimePath);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(groupID, artifactId, versionId, mappingPath, runtimePath, queryClassPath, query);
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
