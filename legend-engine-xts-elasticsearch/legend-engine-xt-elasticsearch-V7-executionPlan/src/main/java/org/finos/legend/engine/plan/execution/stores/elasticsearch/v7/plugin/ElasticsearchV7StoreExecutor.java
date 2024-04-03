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
//

package org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.plugin;

import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreExecutor;

public class ElasticsearchV7StoreExecutor implements StoreExecutor
{
    private final ElasticsearchV7StoreState state = new ElasticsearchV7StoreState();

    private final ElasticsearchV7StoreExecutorConfiguration elasticsearchV7StoreExecutorConfiguration;

    public ElasticsearchV7StoreExecutor()
    {
        this.elasticsearchV7StoreExecutorConfiguration = ElasticsearchV7StoreExecutorConfiguration.newInstance().build();
    }

    public ElasticsearchV7StoreExecutor(ElasticsearchV7StoreExecutorConfiguration elasticsearchV7StoreExecutorConfiguration)
    {
        this.elasticsearchV7StoreExecutorConfiguration = elasticsearchV7StoreExecutorConfiguration;
    }

    @Override
    public StoreExecutionState buildStoreExecutionState()
    {
        return new ElasticsearchV7StoreExecutionState(this.state, this.elasticsearchV7StoreExecutorConfiguration);
    }

    @Override
    public ElasticsearchV7StoreState getStoreState()
    {
        return this.state;
    }
}
