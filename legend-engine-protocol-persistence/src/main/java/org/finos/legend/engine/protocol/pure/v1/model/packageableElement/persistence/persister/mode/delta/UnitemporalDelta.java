package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.delta;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.BatchMilestoningMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.BatchMilestoningModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.delta.merge.MergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.transactionmilestoning.TransactionMilestoning;

public class UnitemporalDelta extends BatchMilestoningMode
{
    public MergeStrategy mergeStrategy;
    public TransactionMilestoning transactionMilestoning;

    public <T> T accept(BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}