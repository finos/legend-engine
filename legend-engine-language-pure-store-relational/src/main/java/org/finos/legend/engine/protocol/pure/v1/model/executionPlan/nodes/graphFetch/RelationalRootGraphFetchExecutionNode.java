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

import java.util.List;

public class RelationalRootGraphFetchExecutionNode extends RelationalTempTableGraphFetchExecutionNode
{
    @Override
    @JsonIgnore
    public List<ExecutionNode> childNodes()
    {
        return this.children == null ?
                Lists.mutable.empty() :
                Lists.mutable.withAll(this.children);
    }

    @Override
    public <T> T accept(ExecutionNodeVisitor<T> executionNodeVisitor)
    {
        return executionNodeVisitor.visit((ExecutionNode) this);
    }
}
