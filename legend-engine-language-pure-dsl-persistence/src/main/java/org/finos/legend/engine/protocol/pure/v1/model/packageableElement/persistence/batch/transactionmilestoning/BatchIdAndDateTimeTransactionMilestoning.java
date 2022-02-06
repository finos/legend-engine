package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoning;

public class BatchIdAndDateTimeTransactionMilestoning extends TransactionMilestoning
{
    public String batchIdInName;
    public String batchIdOutName;
    public String dateTimeInName;
    public String dateTimeOutName;

    public <T> T accept(TransactionMilestoningVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}