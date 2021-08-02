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

package org.finos.legend.engine.language.pure.dsl.service.execution;

import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCrossAssociationKeys;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/* Work in progress, do not use */

public class OperationalContext
{
    private Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> graphFetchCrossAssociationKeysCacheConfig;

    public static OperationalContext newInstance()
    {
        return new OperationalContext();
    }

    public OperationalContext withGraphFetchCrossAssociationKeysCacheConfig(Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> cacheConfig)
    {
        Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> nonNullCacheConfig =
                Objects.requireNonNull(cacheConfig, "cacheConfig must not be null")
                        .entrySet()
                        .stream()
                        .filter(e -> (e.getKey() != null) && (e.getValue() != null))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertSharedCachesValidity(nonNullCacheConfig);
        this.graphFetchCrossAssociationKeysCacheConfig = nonNullCacheConfig;
        return this;
    }

    public Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> getGraphFetchCrossAssociationKeysCacheConfig()
    {
        return this.graphFetchCrossAssociationKeysCacheConfig;
    }

    private static void assertSharedCachesValidity(Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> cacheConfig)
    {
        Map<ExecutionCache<GraphFetchCacheKey, List<Object>>, List<GraphFetchCrossAssociationKeys>> reverseCacheMap =
                cacheConfig
                        .entrySet()
                        .stream()
                        .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        reverseCacheMap.forEach((c, keys) -> {
            GraphFetchCrossAssociationKeys first = keys.get(0);
            for (GraphFetchCrossAssociationKeys key : keys)
            {
                if (!first.isCompatible(key))
                {
                    throw new IllegalArgumentException("Cannot use shared cache for incompatible cross association keys '" + first.getName() + "' and '" + key.getName() + "'");
                }
            }
        });
    }
}
