package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.snapshot;

public class UnitemporalSnapshotMilestoned extends SnapshotMilestoned
{
    public <T> T accept(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}