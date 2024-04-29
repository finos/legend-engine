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

package org.finos.legend.engine.plan.execution.stores.relational.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RelationalGraphFetchExecutionConfig
{
    public final boolean DEFAULT_PARALLELIZE_GRAPHFETCH_QUERIES = false;

    @JsonProperty
    private final boolean parallelizeGraphFetchQueries;
    @JsonProperty
    private final RelationalGraphFetchParallelExecutionConfig relationalGraphFetchParallelExecutionConfig;

    public RelationalGraphFetchExecutionConfig()
    {
        this.parallelizeGraphFetchQueries = DEFAULT_PARALLELIZE_GRAPHFETCH_QUERIES;
        this.relationalGraphFetchParallelExecutionConfig = new RelationalGraphFetchParallelExecutionConfig();
    }

    public boolean canExecuteInParallel()
    {
        return parallelizeGraphFetchQueries;
    }

    public RelationalGraphFetchParallelExecutionConfig getRelationalGraphFetchParallelExecutionConfig()
    {
        return relationalGraphFetchParallelExecutionConfig;
    }
}
