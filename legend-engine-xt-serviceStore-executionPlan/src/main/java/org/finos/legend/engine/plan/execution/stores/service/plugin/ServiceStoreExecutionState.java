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

package org.finos.legend.engine.plan.execution.stores.service.plugin;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreState;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.pac4j.core.profile.CommonProfile;

public class ServiceStoreExecutionState implements StoreExecutionState
{
    private final ServiceStoreState state;
    private RuntimeContext runtimeContext;

    public ServiceStoreExecutionState(ServiceStoreState state, RuntimeContext runtimeContext)
    {
        this.state = state;
        this.runtimeContext = runtimeContext;
    }

    public ServiceStoreExecutionState(ServiceStoreState state)
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
        return new ServiceExecutionNodeExecutor(profiles, executionState);
    }

    @Override
    public StoreExecutionState copy()
    {
        return new ServiceStoreExecutionState(this.state, this.runtimeContext);
    }

    @Override
    public RuntimeContext getRuntimeContext()
    {
        return this.runtimeContext;
    }

    @Override
    public void setRuntimeContext(RuntimeContext runtimeContext)
    {
        this.runtimeContext = runtimeContext;
    }
}
