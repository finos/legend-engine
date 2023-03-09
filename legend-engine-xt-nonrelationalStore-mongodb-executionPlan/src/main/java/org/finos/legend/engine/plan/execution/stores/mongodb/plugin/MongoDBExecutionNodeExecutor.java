// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.mongodb.plugin;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.mongodb.MongoDBExecutor;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.*;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.LocalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.StoreMappingGlobalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryCrossStoreGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryPropertyGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryRootGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.StoreStreamReadingExecutionNode;
import org.pac4j.core.profile.CommonProfile;

public class MongoDBExecutionNodeExecutor implements ExecutionNodeVisitor<Result>
{
    MutableList<CommonProfile> profiles;
    ExecutionState executionState;

    public MongoDBExecutionNodeExecutor(MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        this.profiles = profiles;
        this.executionState = executionState;
    }

    @Override
    public Result visit(ExecutionNode executionNode)
    {
        if (executionNode instanceof MongoDBExecutionNode)
        {
            try (Scope scope = GlobalTracer.get().buildSpan("MongoDB Store Execution").startActive(true))
            {
                scope.span().setTag("databaseCommand", ((MongoDBExecutionNode) executionNode).databaseCommand.toString());
                MongoDBExecutionNode node = (MongoDBExecutionNode) executionNode;

                String databaseCommand = node.databaseCommand.toString();
                MongoDBConnection mongoDBConnection = node.mongoDBConnection;
                CredentialProviderProvider credentialProviderProvider = ((MongoDBStoreExecutionState) executionState.getStoreExecutionState(StoreType.NonRelational_MongoDB)).getCredentialProviderProvider();
                return new MongoDBExecutor(credentialProviderProvider).executeMongoDBQuery(databaseCommand, mongoDBConnection);
            }
        }
        else
        {
            return null;
        }
    }

    @Deprecated
    @Override
    public Result visit(GraphFetchM2MExecutionNode graphFetchM2MExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(StoreStreamReadingExecutionNode storeStreamReadingExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(InMemoryRootGraphFetchExecutionNode inMemoryRootGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(InMemoryCrossStoreGraphFetchExecutionNode inMemoryRootGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(InMemoryPropertyGraphFetchExecutionNode inMemoryPropertyGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Deprecated
    @Override
    public Result visit(GraphFetchExecutionNode graphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(StoreMappingGlobalGraphFetchExecutionNode storeMappingGlobalGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(ErrorExecutionNode errorExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(AggregationAwareExecutionNode aggregationAwareExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(MultiResultSequenceExecutionNode multiResultSequenceExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(SequenceExecutionNode sequenceExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(FunctionParametersValidationNode functionParametersValidationNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(AllocationExecutionNode allocationExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(PureExpressionPlatformExecutionNode pureExpressionPlatformExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(ConstantExecutionNode constantExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(LocalGraphFetchExecutionNode localGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(FreeMarkerConditionalExecutionNode localGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }
}
