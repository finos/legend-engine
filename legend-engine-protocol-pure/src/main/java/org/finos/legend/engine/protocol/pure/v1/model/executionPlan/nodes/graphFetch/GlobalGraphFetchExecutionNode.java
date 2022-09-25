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

package org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.xStore.XStorePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.GraphFetchTree;

import java.util.List;

public class GlobalGraphFetchExecutionNode extends ExecutionNode
{
    public GraphFetchTree graphFetchTree;
    public String store;
    public List<GlobalGraphFetchExecutionNode> children;
    public LocalGraphFetchExecutionNode localGraphFetchExecutionNode;
    public Integer parentIndex;
    public XStorePropertyMapping xStorePropertyMapping;
    public Boolean enableConstraints;
    public Boolean checked;
    public XStorePropertyFetchDetails xStorePropertyFetchDetails;

    @Override
    public <T> T accept(ExecutionNodeVisitor<T> executionNodeVisitor)
    {
        return executionNodeVisitor.visit(this);
    }

    @Override
    @JsonIgnore
    public List<ExecutionNode> childNodes()
    {
        return this.children == null ?
                Lists.mutable.with(this.localGraphFetchExecutionNode) :
                Lists.mutable.with((ExecutionNode) this.localGraphFetchExecutionNode).withAll(this.children);
    }
}
