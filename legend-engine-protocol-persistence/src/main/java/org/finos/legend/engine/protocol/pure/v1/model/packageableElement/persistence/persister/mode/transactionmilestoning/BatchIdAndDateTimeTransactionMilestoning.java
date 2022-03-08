package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.transactionmilestoning;

public class BatchIdAndDateTimeTransactionMilestoning extends TransactionMilestoning
{
    public String batchIdInFieldName;
    public String batchIdOutFieldName;
    public String dateTimeInFieldName;
    public String dateTimeOutFieldName;

    public <T> T accept(TransactionMilestoningVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}