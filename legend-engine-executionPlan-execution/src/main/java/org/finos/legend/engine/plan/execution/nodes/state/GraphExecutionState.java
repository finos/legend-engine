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

package org.finos.legend.engine.plan.execution.nodes.state;

import org.finos.legend.engine.plan.execution.result.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphExecutionState extends ExecutionState
{
    private final int batchSize;
    private final Result rootResult;
    private final long maxMemoryBytesForGraphExecution;

    private long totalObjectMemoryUtilization;
    private Map<Integer, List<?>> nodeObjects;
    private List<?> objectsToGraphFetch;
    private long rowCount;

    public GraphExecutionState(ExecutionState executionState, int batchSize, Result rootResult, long maxMemoryBytesForGraphExecution)
    {
        super(executionState);
        this.batchSize = batchSize;
        this.rootResult = rootResult;
        this.maxMemoryBytesForGraphExecution = maxMemoryBytesForGraphExecution;

        this.totalObjectMemoryUtilization = 0;
        this.nodeObjects = new HashMap<>();
        this.rowCount = 0;
    }

    public int getBatchSize()
    {
        return this.batchSize;
    }

    public Result getRootResult()
    {
        return this.rootResult;
    }

    public void addObjectMemoryUtilization(long memoryBytes)
    {
        this.totalObjectMemoryUtilization += memoryBytes;
        if (this.totalObjectMemoryUtilization > this.maxMemoryBytesForGraphExecution)
        {
            throw new RuntimeException("Maximum memory reached when processing the graph. Try reducing batch size of graph fetch operation.");
        }
    }

    public long getTotalObjectMemoryUtilization()
    {
        return this.totalObjectMemoryUtilization;
    }

    public List<?> getObjectsForNodeIndex(int nodeIndex)
    {
        return this.nodeObjects.get(nodeIndex);
    }

    public void setObjectsForNodeIndex(int nodeIndex, List<?> objects)
    {
        this.nodeObjects.put(nodeIndex, objects);
    }

    public List<?> getObjectsToGraphFetch()
    {
        return this.objectsToGraphFetch;
    }

    public void setObjectsToGraphFetch(List<?> objects)
    {
        this.objectsToGraphFetch = objects;
    }

    public void incrementRowCount()
    {
        this.rowCount++;
    }

    public long getRowCount()
    {
        return this.rowCount;
    }
}
