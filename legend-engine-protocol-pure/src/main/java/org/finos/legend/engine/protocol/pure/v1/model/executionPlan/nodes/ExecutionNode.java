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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GlobalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryCrossStoreGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryPropertyGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryRootGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.StoreStreamReadingExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.DataTypeResultType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.ResultType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.VoidResultType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;

import java.util.Collections;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AllocationExecutionNode.class, name = "allocation"),
        @JsonSubTypes.Type(value = ConstantExecutionNode.class, name = "constant"),
        @JsonSubTypes.Type(value = PureExpressionPlatformExecutionNode.class, name = "platform"),
        @JsonSubTypes.Type(value = PureExpressionPlatformExecutionNode.class, name = "pureExp"),
        @JsonSubTypes.Type(value = SequenceExecutionNode.class, name = "sequence"),
        @JsonSubTypes.Type(value = AggregationAwareExecutionNode.class, name = "aggregationAware"),
        @JsonSubTypes.Type(value = FunctionParametersValidationNode.class, name = "function-parameters-validation"),
        @JsonSubTypes.Type(value = MultiResultSequenceExecutionNode.class, name = "multiResultSequence"),
        @JsonSubTypes.Type(value = ErrorExecutionNode.class, name = "error"),
        @JsonSubTypes.Type(value = GraphFetchM2MExecutionNode.class, name = "graphFetchM2M"),
        @JsonSubTypes.Type(value = GraphFetchExecutionNode.class, name = "graphFetchExecutionNode"),
        @JsonSubTypes.Type(value = GlobalGraphFetchExecutionNode.class, name = "globalGraphFetchExecutionNode"),
        @JsonSubTypes.Type(value = FreeMarkerConditionalExecutionNode.class, name = "freeMarkerConditionalExecutionNode"),
        @JsonSubTypes.Type(value = StoreStreamReadingExecutionNode.class, name = "storeStreamReading"),
        @JsonSubTypes.Type(value = InMemoryRootGraphFetchExecutionNode.class, name = "inMemoryRootGraphFetch"),
        @JsonSubTypes.Type(value = InMemoryPropertyGraphFetchExecutionNode.class, name = "inMemoryPropertyGraphFetch"),
        @JsonSubTypes.Type(value = InMemoryCrossStoreGraphFetchExecutionNode.class, name = "inMemoryCrossStoreGraphFetch")
})
public abstract class ExecutionNode
{
    public ResultType resultType;
    public List<ExecutionNode> executionNodes = Collections.emptyList();
    public Multiplicity resultSizeRange;
    public List<VariableInput> requiredVariableInputs;
    public PlatformImplementation implementation;

    public abstract <T> T accept(ExecutionNodeVisitor<T> executionNodeVisitor);

    @JsonIgnore
    public RichIterable<ExecutionNode> executionNodes()
    {
        return executionNodes == null ? Lists.mutable.empty() : Lists.mutable.withAll(executionNodes);
    }

    @JsonIgnore
    public List<ExecutionNode> childNodes()
    {
        return this.executionNodes().toList();
    }

    @JsonIgnore
    @BsonIgnore
    public MutableMap<String, Object> getExecutionStateParams(MutableMap<String, Object> parameterMap)
    {
        MutableMap<String, Object> result = parameterMap;
        for (ExecutionNode e : this.executionNodes)
        {
            result = e.getExecutionStateParams(result);
        }
        return result;
    }

    /* Void Result APIs */
    @JsonIgnore
    public boolean isResultVoid()
    {
        return (this.resultType instanceof VoidResultType);
    }

    /* Primitive result APIs */
    @JsonIgnore
    public boolean isResultPrimitiveType()
    {
        return this.resultType instanceof DataTypeResultType;
    }

    @JsonIgnore
    public String getDataTypeResultType()
    {
        return ((DataTypeResultType) this.resultType).dataType;
    }
}
