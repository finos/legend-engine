package org.finos.legend.engine.protocol.persistence.batch.milestoned.delta;

public class UnitemporalDeltaMilestoned extends DeltaMilestoned
{
    public <T> T accept(org.finos.legend.engine.protocol.persistence.batch.BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}