package org.finos.legend.engine.protocol.persistence.batch.nonmilestoned;

public class DeltaNonMilestoned extends NonMilestoned
{
    public <T> T accept(org.finos.legend.engine.protocol.persistence.batch.BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}