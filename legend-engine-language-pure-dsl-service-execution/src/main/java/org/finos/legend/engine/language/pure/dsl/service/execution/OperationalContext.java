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

/* Work in progress, do not use */

import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCrossAssociationKeys;

import java.util.List;
import java.util.Map;

public class OperationalContext
{
    private Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> graphFetchCrossAssociationKeysCacheConfig;

    private OperationalContext()
    {

    }

    public static OperationalContext newInstance()
    {
        return new OperationalContext();
    }

    public OperationalContext withGraphFetchCrossAssociationKeysCacheConfig(Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> cacheConfig)
    {
        this.graphFetchCrossAssociationKeysCacheConfig = cacheConfig;
        return this;
    }

    public Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> getGraphFetchCrossAssociationKeysCacheConfig()
    {
        return this.graphFetchCrossAssociationKeysCacheConfig;
    }
}
