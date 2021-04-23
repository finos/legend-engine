// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.cache.graphFetch;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.cache.ExecutionCache;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GraphFetchCacheByTargetCrossKeys implements GraphFetchCache
{
    private final List<Pair<String, String>> sourceSetIds;
    private final String targetSetId;
    private final ExecutionCache<GraphFetchCacheKey, List<Object>> cache;
    private List<String> targetPropertiesOrdered;
    private String subTree;

    public GraphFetchCacheByTargetCrossKeys(String mappingId, String sourceSetId, String targetSetId, ExecutionCache<GraphFetchCacheKey, List<Object>> cache)
    {
        this(Collections.singletonList(Tuples.pair(mappingId, sourceSetId)), targetSetId, cache);
    }

    public GraphFetchCacheByTargetCrossKeys(List<Pair<String, String>> sourceSetIds, String targetSetId, ExecutionCache<GraphFetchCacheKey, List<Object>> cache)
    {
        Objects.requireNonNull(sourceSetIds, "Source Set IDs should be non null");
        sourceSetIds.forEach(s -> {
            Objects.requireNonNull(s.getOne(), "Mapping ID should be non null");
            Objects.requireNonNull(s.getOne(), "Source Set ID should be non null");
        });
        Objects.requireNonNull(targetSetId, "Target Set ID should be non null");
        Objects.requireNonNull(cache, "Cache should be non null");

        this.sourceSetIds = sourceSetIds;
        this.targetSetId = targetSetId;
        this.cache = cache;
    }

    public List<Pair<String, String>> getSourceSetIds()
    {
        return this.sourceSetIds;
    }

    public String getTargetSetId()
    {
        return this.targetSetId;
    }

    public void setSubTree(String subTree)
    {
        this.subTree = subTree;
    }

    public String getSubTree()
    {
        return this.subTree;
    }

    public void setTargetPropertiesOrdered(List<String> targetPropertiesOrdered)
    {
        this.targetPropertiesOrdered = targetPropertiesOrdered;
    }

    public List<String> getTargetPropertiesOrdered()
    {
        return this.targetPropertiesOrdered;
    }

    @Override
    public boolean isCacheUtilized()
    {
        return this.subTree != null;
    }

    @Override
    public ExecutionCache<GraphFetchCacheKey, List<Object>> getExecutionCache()
    {
        return this.cache;
    }
}
