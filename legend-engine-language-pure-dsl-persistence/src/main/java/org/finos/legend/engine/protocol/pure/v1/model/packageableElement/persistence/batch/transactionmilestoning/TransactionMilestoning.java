package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoning;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public abstract class TransactionMilestoning
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(TransactionMilestoningVisitor<T> visitor);
}