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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.GraphFetchTree;

import java.util.List;

@Deprecated
public class GraphFetchM2MExecutionNode extends ExecutionNode
{
    public GraphFetchTree tree;
    public List<GraphFetchTree> trees;
    @JsonProperty(required = true)
    public boolean enableConstraints = true;
    @JsonProperty
    public Store store;

    @Override
    public <T> T accept(ExecutionNodeVisitor<T> executionNodeVisitor)
    {
        return executionNodeVisitor.visit(this);
    }
}
