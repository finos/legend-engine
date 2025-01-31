// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.result;

import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalTdsInstantiationExecutionNode;
import org.finos.legend.engine.shared.core.identity.Identity;

public class DeferredRelationalResult extends Result
{
    private final ExecutionState executionState;
    private final Identity identity;
    private final RelationalTdsInstantiationExecutionNode tdsInstantiationExecutionNode;

    public DeferredRelationalResult(RelationalTdsInstantiationExecutionNode tdsInstantiationExecutionNode, Identity identity, ExecutionState executionState)
    {
        super("deferred");
        this.tdsInstantiationExecutionNode = tdsInstantiationExecutionNode;
        this.identity = identity;
        this.executionState = executionState;
    }

    @Override
    public <T> T accept(ResultVisitor<T> resultVisitor)
    {
        try (RelationalResult result = this.evaluate())
        {
            return result.accept(resultVisitor);
        }
    }

    public RelationalResult evaluate()
    {
        ExecutionNodeVisitor<Result> visitor = this.executionState.getStoreExecutionState(StoreType.Relational).getVisitor(this.identity, this.executionState);
        SQLExecutionResult sqlResult = (SQLExecutionResult) visitor.visit(this.tdsInstantiationExecutionNode.executionNodes.get(0));
        return new RelationalResult(sqlResult, this.tdsInstantiationExecutionNode);
    }

    @Override
    public Result realizeInMemory()
    {
        return this.evaluate().realizeInMemory();
    }
}
