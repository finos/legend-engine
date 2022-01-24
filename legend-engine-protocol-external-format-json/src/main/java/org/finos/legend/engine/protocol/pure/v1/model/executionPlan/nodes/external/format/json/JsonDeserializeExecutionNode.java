package org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.format.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.shared.ExternalFormatDeserializeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;

public class JsonDeserializeExecutionNode extends ExternalFormatDeserializeExecutionNode
{
    @JsonProperty(required = false)
    public String binding;
    @JsonProperty(required = false)
    public RootGraphFetchTree tree;

    @Override
    public <T> T accept(ExecutionNodeVisitor<T> executionNodeVisitor)
    {
        return executionNodeVisitor.visit(this);
    }
}
