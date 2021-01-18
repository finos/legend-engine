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

package org.finos.legend.engine.plan.execution.result.graphFetch;

import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphObjectsBatch
{
    protected final long batchIndex;
    protected Map<Integer, List<?>> nodeObjects;
    protected long totalObjectMemoryUtilization;
    protected long rowCount;

    public GraphObjectsBatch(long batchIndex)
    {
        this.batchIndex = batchIndex;
        this.nodeObjects = new HashMap<>();
        this.totalObjectMemoryUtilization = 0;
        this.rowCount = 0;
    }

    public GraphObjectsBatch(GraphObjectsBatch other)
    {
        this.batchIndex = other.batchIndex;
        this.nodeObjects = other.nodeObjects;
        this.totalObjectMemoryUtilization = other.totalObjectMemoryUtilization;
        this.rowCount = other.rowCount;
    }

    public long getBatchIndex()
    {
        return this.batchIndex;
    }

    public void setObjectsForNodeIndex(int index, List<?> objects)
    {
        this.nodeObjects.put(index, objects);
    }

    public List<?> getObjectsForNodeIndex(int index)
    {
        return this.nodeObjects.get(index);
    }

    public long getRowCount()
    {
        return this.rowCount;
    }

    public void incrementRowCount()
    {
        this.rowCount++;
    }

    public void addObjectMemoryUtilization(long memoryBytes)
    {
        this.totalObjectMemoryUtilization += memoryBytes;
        if (this.totalObjectMemoryUtilization > ExecutionNodeExecutor.MAX_MEMORY_BYTES_PER_GRAPH_BATCH)
        {
            throw new RuntimeException("Maximum memory reached when processing the graphFetch. Try reducing batch size of graphFetch fetch operation.");
        }
    }
}
