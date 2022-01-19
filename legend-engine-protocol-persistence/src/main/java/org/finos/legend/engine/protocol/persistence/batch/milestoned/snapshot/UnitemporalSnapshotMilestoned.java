package org.finos.legend.engine.protocol.persistence.batch.milestoned.snapshot;

public class UnitemporalSnapshotMilestoned extends SnapshotMilestoned
{
    public <T> T accept(org.finos.legend.engine.protocol.persistence.batch.BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}