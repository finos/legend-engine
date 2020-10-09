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
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.dependencies.store.inMemory.IGraphFetchM2MExecutionNodeContext;
import org.finos.legend.engine.plan.dependencies.store.inMemory.IStoreStreamReader;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.DefaultExecutionNodeContext;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.GraphFetchM2MExecutionNode;

import java.io.IOException;
import java.util.ServiceLoader;

public class GraphFetchM2MExecutionNodeContext extends DefaultExecutionNodeContext implements IGraphFetchM2MExecutionNodeContext
{
    public static ExecutionNodeJavaPlatformHelper.ExecutionNodeContextFactory factory(GraphFetchM2MExecutionNode node)
    {
        return (ExecutionState state, Result childResult) -> new GraphFetchM2MExecutionNodeContext(node, state, childResult);
    }

    private final GraphFetchM2MExecutionNode node;

    private GraphFetchM2MExecutionNodeContext(GraphFetchM2MExecutionNode node, ExecutionState state, Result childResult)
    {
        super(state, childResult);
        this.node = node;
    }

    @Override
    public IStoreStreamReader createReader(String s) 
    {
        MutableList<StoreStreamReaderBuilder> builders = Lists.mutable.empty();
        for (StoreStreamReaderBuilder desc : ServiceLoader.load(StoreStreamReaderBuilder.class))
        {
            builders.add(desc);
        }
        return builders.isEmpty()
               ? null
               : builders.getFirst().newStoreStreamReader(s, node.store);
    }
}
