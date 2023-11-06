package org.finos.legend.engine.query.graphQL.api.cache;

import com.google.common.base.Objects;

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
                && Objects.equal(dataspacePath, that.dataspacePath)
                && Objects.equal(executionContext, that.executionContext);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(groupID, artifactId, versionId, dataspacePath, executionContext, queryClassPath, query);
    }

    public String getDataspacePath()
    {
        return dataspacePath;
    }

    public String getExecutionContext()
    {
        return executionContext;
    }
}
