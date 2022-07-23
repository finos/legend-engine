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

package org.finos.legend.engine.plan.execution.stores.relational.plugin;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreState;
import org.finos.legend.engine.plan.execution.stores.relational.RelationalExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.blockConnection.BlockConnectionContext;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.pac4j.core.profile.CommonProfile;

public class RelationalStoreExecutionState implements StoreExecutionState
{
    private final RelationalStoreState state;
    private boolean retainConnection;
    private BlockConnectionContext blockConnectionContext;
    private RuntimeContext runtimeContext;

    private RelationalStoreExecutionState(RelationalStoreState storeState, boolean retainConnection, BlockConnectionContext blockConnectionContext, RuntimeContext runtimeContext)
    {
        this.state = storeState;
        this.retainConnection = retainConnection;
        this.blockConnectionContext = blockConnectionContext;
        this.runtimeContext = runtimeContext;
    }

    public RelationalStoreExecutionState(RelationalStoreState storeState)
    {
        this(storeState, false, new BlockConnectionContext(), StoreExecutionState.emptyRuntimeContext());
    }

    @Override
    public StoreState getStoreState()
    {
        return this.state;
    }

    @Override
    public ExecutionNodeVisitor<Result> getVisitor(MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        return new RelationalExecutionNodeExecutor(executionState, profiles);
    }

    @Override
    public StoreExecutionState copy()
    {
        return new RelationalStoreExecutionState(this.state, this.retainConnection, this.retainConnection ? this.blockConnectionContext : this.blockConnectionContext.copy(), this.runtimeContext);
    }

    @Override
    public StoreExecutionState.RuntimeContext getRuntimeContext()
    {
        return this.runtimeContext;
    }

    @Override
    public void setRuntimeContext(RuntimeContext runtimeContext)
    {
        this.runtimeContext = runtimeContext;
    }

    public RelationalExecutor getRelationalExecutor()
    {
        return this.state.getRelationalExecutor();
    }

    public boolean retainConnection()
    {
        return this.retainConnection;
    }

    public void setRetainConnection(boolean retainConnection)
    {
        this.retainConnection = retainConnection;
    }

    public BlockConnectionContext getBlockConnectionContext()
    {
        return this.blockConnectionContext;
    }

    public void setBlockConnectionContext(BlockConnectionContext blockConnectionContext)
    {
        this.blockConnectionContext = blockConnectionContext;
    }
}
