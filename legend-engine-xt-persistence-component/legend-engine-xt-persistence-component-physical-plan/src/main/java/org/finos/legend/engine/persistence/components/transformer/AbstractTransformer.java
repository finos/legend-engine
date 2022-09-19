// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.transformer;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlan;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNodes;
import org.finos.legend.engine.persistence.components.sink.Sink;

public abstract class AbstractTransformer<C extends PhysicalPlanNode, P extends PhysicalPlan<C>> implements Transformer<C, P>
{
    private final Sink sink;
    private final TransformOptions options;

    protected AbstractTransformer(Sink sink, TransformOptions options)
    {
        this.sink = sink;
        this.options = options;
    }

    @Override
    public TransformOptions options()
    {
        return options;
    }

    protected abstract P createPhysicalPlan(PhysicalPlanNodes<C> physicalNodes);

    @Override
    public P generatePhysicalPlan(LogicalPlan plan)
    {
        VisitorContext context = createContext(options);
        PhysicalPlanNodes<C> currentPhysicalNode = new PhysicalPlanNodes<>();

        for (Operation op : plan.ops())
        {
            LogicalPlanVisitor.VisitorResult result = visit(currentPhysicalNode, op, context);
            constructPhysicalPlanForChildren(result, context);
            for (Operation otherOp : result.getOtherOps())
            {
                LogicalPlanVisitor.VisitorResult opResult = visit(currentPhysicalNode, otherOp, context);
                constructPhysicalPlanForChildren(opResult, context);
            }
        }

        return this.createPhysicalPlan(currentPhysicalNode);
    }

    protected VisitorContext createContext(TransformOptions options)
    {
        return VisitorContext.builder()
            .batchStartTimestampPattern(options.batchStartTimestampPattern())
            .batchEndTimestampPattern(options.batchEndTimestampPattern())
            .batchStartTimestamp(options.batchStartTimestampValue())
            .batchIdPattern(options.batchIdPattern())
            .addAllOptimizers(options.optimizers())
            .quoteIdentifier(sink.quoteIdentifier())
            .build();
    }

    private LogicalPlanVisitor.VisitorResult visit(PhysicalPlanNode currentPhysicalNode, Operation op, VisitorContext context)
    {
        LogicalPlanVisitor<Operation> visitor = sink.visitorForClass(op.getClass());
        return visitor.visit(currentPhysicalNode, op, context);
    }

    private void constructPhysicalPlanForChildren(LogicalPlanVisitor.VisitorResult result, VisitorContext context)
    {
        for (LogicalPlanNode logicalNode : result.getNextItems())
        {
            LogicalPlanVisitor<LogicalPlanNode> visitor = sink.visitorForClass(logicalNode.getClass());
            LogicalPlanVisitor.VisitorResult visitedResult = visitor.visit(result.getReturnValue(), logicalNode, context);
            constructPhysicalPlanForChildren(visitedResult, context);

            for (Operation otherOp : result.getOtherOps())
            {
                LogicalPlanVisitor.VisitorResult opResult = visit(result.getReturnValue(), otherOp, context);
                constructPhysicalPlanForChildren(opResult, context);
            }
        }
    }
}
