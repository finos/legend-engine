package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.delta;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.delta.merge.MergeScheme;

public class DeltaMilestoned extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.Milestoned
{
    public MergeScheme mergeScheme;

    public <T> T accept(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}