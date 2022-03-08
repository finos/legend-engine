package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.snapshot;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.BatchMilestoningMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.BatchMilestoningModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.transactionmilestoning.TransactionMilestoning;

public class UnitemporalSnapshot extends BatchMilestoningMode
{
    public TransactionMilestoning transactionMilestoning;

    public <T> T accept(BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}