package org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.shared;

import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;

public class UrlStreamExecutionNode extends ExecutionNode
{
    public String url;

    @Override
    public <T> T accept(ExecutionNodeVisitor<T> executionNodeVisitor)
    {
        return executionNodeVisitor.visit(this);
    }
}
