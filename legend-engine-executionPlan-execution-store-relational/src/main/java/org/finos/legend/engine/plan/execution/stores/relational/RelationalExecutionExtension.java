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

package org.finos.legend.engine.plan.execution.stores.relational;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.extension.ExecutionExtension;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.CreateAndPopulateTempTableExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalBlockExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalInstantiationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.RelationalGraphFetchExecutionNode;
import org.pac4j.core.profile.CommonProfile;

import java.util.Collections;
import java.util.List;

public class RelationalExecutionExtension implements ExecutionExtension
{
    @Override
    public List<Function3<ExecutionNode, MutableList<CommonProfile>, ExecutionState, Result>> getExtraNodeExecutors()
    {
        return Collections.singletonList(((executionNode, profiles, executionState) ->
        {
            if (executionNode instanceof RelationalBlockExecutionNode
                    || executionNode instanceof CreateAndPopulateTempTableExecutionNode
                    || executionNode instanceof SQLExecutionNode
                    || executionNode instanceof RelationalExecutionNode
                    || executionNode instanceof RelationalInstantiationExecutionNode
                    || executionNode instanceof RelationalGraphFetchExecutionNode
            )
            {
                System.out.println(executionState);
                System.out.println(executionState.getStoreExecutionState(StoreType.Relational));
                return executionNode.accept(executionState.getStoreExecutionState(StoreType.Relational).getVisitor(profiles, executionState));
            }
            return null;
        }));
    }
}
