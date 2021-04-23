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

package org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes;

import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GlobalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.LocalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryPropertyGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryRootGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.StoreStreamReadingExecutionNode;

public interface ExecutionNodeVisitor<T>
{
    T visit(ExecutionNode executionNode);

    // Flow
    T visit(SequenceExecutionNode sequenceExecutionNode);

    T visit(MultiResultSequenceExecutionNode multiResultSequenceExecutionNode);

    T visit(FreeMarkerConditionalExecutionNode localGraphFetchExecutionNode);

    T visit(AllocationExecutionNode allocationExecutionNode);

    T visit(ErrorExecutionNode errorExecutionNode);

    T visit(FunctionParametersValidationNode functionParametersValidationNode);

    // Shared GraphFetch Superstructure
    @Deprecated
    T visit(GraphFetchExecutionNode graphFetchExecutionNode);

    T visit(GlobalGraphFetchExecutionNode globalGraphFetchExecutionNode);

    T visit(LocalGraphFetchExecutionNode localGraphFetchExecutionNode);

    // InMemory
    @Deprecated
    T visit(GraphFetchM2MExecutionNode graphFetchM2MExecutionNode);

    T visit(StoreStreamReadingExecutionNode storeStreamReadingExecutionNode);

    T visit(InMemoryRootGraphFetchExecutionNode inMemoryRootGraphFetchExecutionNode);

    T visit(InMemoryPropertyGraphFetchExecutionNode inMemoryPropertyGraphFetchExecutionNode);

    // Aggregation Aware
    T visit(AggregationAwareExecutionNode aggregationAwareExecutionNode);

    // Constant
    T visit(ConstantExecutionNode constantExecutionNode);

    // Replace with Serialization
    T visit(PureExpressionPlatformExecutionNode pureExpressionPlatformExecutionNode);
}
