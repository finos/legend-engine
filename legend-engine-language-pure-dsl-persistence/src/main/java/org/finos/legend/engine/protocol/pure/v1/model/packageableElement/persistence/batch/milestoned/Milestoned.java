package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned;

public class Milestoned extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchMilestoningMode
{
    public org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.transaction.TransactionMilestoningScheme tranactionMilestoningScheme;

    public <T> T accept(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}