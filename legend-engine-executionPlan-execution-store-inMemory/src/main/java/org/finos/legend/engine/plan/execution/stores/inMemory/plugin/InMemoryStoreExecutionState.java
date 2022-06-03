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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreState;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.pac4j.core.profile.CommonProfile;

public class InMemoryStoreExecutionState implements StoreExecutionState
{
    private final InMemoryStoreState state;
    private RuntimeContext runtimeContext;

    public InMemoryStoreExecutionState(InMemoryStoreState state, RuntimeContext runtimeContext)
    {
        this.state = state;
        this.runtimeContext = runtimeContext;
    }

    public InMemoryStoreExecutionState(InMemoryStoreState state)
    {
        this(state, RuntimeContext.empty());
    }

    @Override
    public StoreState getStoreState()
    {
        return this.state;
    }

    @Override
    public ExecutionNodeVisitor<Result> getVisitor(MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        return new InMemoryExecutionNodeExecutor(profiles, executionState);
    }

    @Override
    public StoreExecutionState copy()
    {
        return new InMemoryStoreExecutionState(this.state, this.runtimeContext);
    }

    @Override
    public RuntimeContext getRuntimeContext() {
        return this.runtimeContext;
    }

    @Override
    public void setRuntimeContext(RuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }
}
