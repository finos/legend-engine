package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.nonmilestoned;

public class AppendOnlyNonMilestoned extends NonMilestoned
{
    public boolean filterDuplicates;

    public <T> T accept(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}