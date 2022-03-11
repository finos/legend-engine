package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning;

public class DateTimeTransactionMilestoning extends TransactionMilestoning
{
    public String dateTimeInFieldName;
    public String dateTimeOutFieldName;

    public <T> T accept(TransactionMilestoningVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}