package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.transactionmilestoning;

public class OpaqueTransactionMilestoning extends TransactionMilestoning
{
    public <T> T accept(TransactionMilestoningVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}