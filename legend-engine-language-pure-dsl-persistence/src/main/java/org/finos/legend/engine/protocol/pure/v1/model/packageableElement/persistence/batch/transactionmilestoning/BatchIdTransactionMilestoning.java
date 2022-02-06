package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoning;

public class BatchIdTransactionMilestoning extends TransactionMilestoning
{
    public String batchIdInName;
    public String batchIdOutName;

    public <T> T accept(TransactionMilestoningVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}