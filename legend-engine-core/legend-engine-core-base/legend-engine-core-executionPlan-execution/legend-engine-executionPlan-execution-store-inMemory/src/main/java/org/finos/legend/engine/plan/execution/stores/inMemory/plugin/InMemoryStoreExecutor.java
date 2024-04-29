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

package org.finos.legend.engine.plan.execution.stores.inMemory.plugin;

import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreExecutor;

public class InMemoryStoreExecutor implements StoreExecutor
{
    static final InMemoryStoreExecutor INSTANCE = new InMemoryStoreExecutor(new InMemoryStoreState());

    private final InMemoryStoreState state;

    private InMemoryStoreExecutor(InMemoryStoreState state)
    {
        this.state = state;
    }

    @Override
    public StoreExecutionState buildStoreExecutionState()
    {
        return new InMemoryStoreExecutionState(this.state);
    }

    @Override
    public InMemoryStoreState getStoreState()
    {
        return this.state;
    }
}
