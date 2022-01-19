package org.finos.legend.engine.protocol.persistence.batch.milestoned.snapshot;

public class SnapshotMilestoned extends org.finos.legend.engine.protocol.persistence.batch.milestoned.Milestoned
{
    public <T> T accept(org.finos.legend.engine.protocol.persistence.batch.BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}