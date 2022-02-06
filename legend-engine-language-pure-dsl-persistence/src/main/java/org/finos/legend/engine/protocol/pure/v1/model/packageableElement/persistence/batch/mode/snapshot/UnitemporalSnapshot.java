package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoning.TransactionMilestoning;

public class UnitemporalSnapshot extends BatchMilestoningMode
{
    public TransactionMilestoning transactionMilestoning;

    public <T> T accept(BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}