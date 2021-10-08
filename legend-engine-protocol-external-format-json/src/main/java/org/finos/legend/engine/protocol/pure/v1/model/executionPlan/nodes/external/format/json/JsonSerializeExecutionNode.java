package org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.format.json;

import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.shared.ExternalFormatSerializeExecutionNode;

public class JsonSerializeExecutionNode extends ExternalFormatSerializeExecutionNode
{
    @Override
    public <T> T accept(ExecutionNodeVisitor<T> executionNodeVisitor)
    {
        return executionNodeVisitor.visit(this);
    }
}
