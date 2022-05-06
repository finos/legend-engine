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

package org.finos.legend.engine.plan.execution.stores.relational.result.graphFetch;

import org.finos.legend.engine.shared.core.collectionsExtensions.DoubleStrategyHashMap;
import org.finos.legend.engine.plan.execution.stores.relational.result.SQLExecutionResult;

import org.finos.legend.engine.plan.execution.result.graphFetch.GraphObjectsBatch;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelationalGraphObjectsBatch extends GraphObjectsBatch
{
    private Map<Integer, DoubleStrategyHashMap<Object, Object, SQLExecutionResult>> nodeObjectsHashMap;
    private Map<Integer, List<Method>> nodePrimaryKeyGetters;

    public RelationalGraphObjectsBatch(long batchIndex)
    {
        super(batchIndex);
        this.nodeObjectsHashMap = new HashMap<>();
        this.nodePrimaryKeyGetters = new HashMap<>();
    }

    public RelationalGraphObjectsBatch(GraphObjectsBatch graphObjectsBatch)
    {
        super(graphObjectsBatch);
        this.nodeObjectsHashMap = new HashMap<>();
        this.nodePrimaryKeyGetters = new HashMap<>();
    }

    public DoubleStrategyHashMap<Object, Object, SQLExecutionResult> getNodeObjectsHashMap(int nodeIndex)
    {
        return this.nodeObjectsHashMap.get(nodeIndex);
    }

    public void setNodeObjectsHashMap(int nodeIndex, DoubleStrategyHashMap<Object, Object, SQLExecutionResult> hashMap)
    {
        this.nodeObjectsHashMap.put(nodeIndex, hashMap);
    }

    public List<Method> getNodePrimaryKeyGetters(int nodeIndex)
    {
        return this.nodePrimaryKeyGetters.get(nodeIndex);
    }

    public void setNodePrimaryKeyGetters(int nodeIndex, List<Method> primaryKeyGetters)
    {
        this.nodePrimaryKeyGetters.put(nodeIndex, primaryKeyGetters);
    }
}
