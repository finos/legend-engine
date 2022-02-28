package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoning.TransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.ValidityMilestoning;

public class BitemporalSnapshot extends BatchMilestoningMode
{
    public TransactionMilestoning transactionMilestoning;
    public ValidityMilestoning validityMilestoning;

    public <T> T accept(BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}