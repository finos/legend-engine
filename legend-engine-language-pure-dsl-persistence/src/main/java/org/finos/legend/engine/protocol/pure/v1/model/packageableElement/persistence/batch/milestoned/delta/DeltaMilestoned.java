package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.delta;

public class DeltaMilestoned extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.Milestoned
{
    public <T> T accept(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}