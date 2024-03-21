// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.graphFetch;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphFetchExecutionConfiguration
{
    public static final long DEFAULT_BATCH_MEMORY_LIMIT = 104_849_600L; /* 100MB - 100 * 1024 * 1024 */
    public static final long DEFAULT_SOFT_MEMORY_LIMIT_PERCENTAGE = 50;
    public static final boolean DEFAULT_USE_ADAPTIVE_BATCHING = false;
    public static final long DEFAULT_BATCH_SIZE = 1000;

    public static final long SOFT_MEMORY_TO_USE_FULL_MEMORY_PERCENTAGE = 100;

    public static final boolean DEFAULT_PARALLELIZE_GRAPHFETCH_QUERIES = false;

    @JsonProperty
    private final long batchMemoryLimit;
    @JsonProperty
    private final long softMemoryLimitPercentage;
    @JsonProperty
    private final boolean useAdaptiveBatching;
    @JsonProperty
    private final long defaultBatchSize;
    @JsonProperty
    private final boolean parallelizeGraphFetchQueries;
    @JsonProperty
    private ParallelGraphFetchExecutionConfig parallelGraphFetchExecutionConfig;

    public GraphFetchExecutionConfiguration()
    {
        this.batchMemoryLimit = DEFAULT_BATCH_MEMORY_LIMIT;
        this.softMemoryLimitPercentage = DEFAULT_SOFT_MEMORY_LIMIT_PERCENTAGE;
        this.useAdaptiveBatching = DEFAULT_USE_ADAPTIVE_BATCHING;
        this.defaultBatchSize = DEFAULT_BATCH_SIZE;
        this.parallelizeGraphFetchQueries = DEFAULT_PARALLELIZE_GRAPHFETCH_QUERIES;
        this.parallelGraphFetchExecutionConfig = new ParallelGraphFetchExecutionConfig();
    }

    public GraphFetchExecutionConfiguration(ParallelGraphFetchExecutionConfig parallelGraphFetchExecutionConfig)
    {
        this.batchMemoryLimit = DEFAULT_BATCH_MEMORY_LIMIT;
        this.softMemoryLimitPercentage = DEFAULT_SOFT_MEMORY_LIMIT_PERCENTAGE;
        this.useAdaptiveBatching = DEFAULT_USE_ADAPTIVE_BATCHING;
        this.defaultBatchSize = DEFAULT_BATCH_SIZE;
        this.parallelizeGraphFetchQueries = true;
        this.parallelGraphFetchExecutionConfig = parallelGraphFetchExecutionConfig;
    }

    public GraphFetchExecutionConfiguration(long graphFetchBatchMemoryLimit, long graphFetchSoftMemoryLimitPercentage, boolean useAdaptiveBatching, long graphFetchDefaultBatchSize)
    {
        this.batchMemoryLimit = graphFetchBatchMemoryLimit;
        this.softMemoryLimitPercentage = graphFetchSoftMemoryLimitPercentage;
        this.useAdaptiveBatching = useAdaptiveBatching;
        this.defaultBatchSize = graphFetchDefaultBatchSize;
        this.parallelizeGraphFetchQueries = DEFAULT_PARALLELIZE_GRAPHFETCH_QUERIES;
        this.parallelGraphFetchExecutionConfig = new ParallelGraphFetchExecutionConfig();
    }

    public GraphFetchExecutionConfiguration(long graphFetchBatchMemoryLimit)
    {
        this.batchMemoryLimit = graphFetchBatchMemoryLimit;
        this.softMemoryLimitPercentage = SOFT_MEMORY_TO_USE_FULL_MEMORY_PERCENTAGE;
        this.useAdaptiveBatching = DEFAULT_USE_ADAPTIVE_BATCHING;
        this.defaultBatchSize = DEFAULT_BATCH_SIZE;
        this.parallelizeGraphFetchQueries = DEFAULT_PARALLELIZE_GRAPHFETCH_QUERIES;
        this.parallelGraphFetchExecutionConfig = new ParallelGraphFetchExecutionConfig();
    }

    public long getGraphFetchBatchMemoryHardLimit()
    {
        return batchMemoryLimit;
    }

    public long getGraphFetchBatchMemorySoftLimit()
    {
        return (batchMemoryLimit * softMemoryLimitPercentage) / 100;
    }

    public long getGraphFetchSoftMemoryLimitPercentage()
    {
        return softMemoryLimitPercentage;
    }

    public boolean shouldUseAdaptiveBatching()
    {
        return useAdaptiveBatching;
    }

    public long getGraphFetchDefaultBatchSize()
    {
        return defaultBatchSize;
    }

    public ParallelGraphFetchExecutionConfig getParallelGraphFetchExecutionConfig()
    {
        return parallelGraphFetchExecutionConfig;
    }

    public boolean canExecuteInParallel()
    {
        return parallelizeGraphFetchQueries;
    }
}
