package org.finos.legend.engine.protocol.persistence.batch.milestoned.transaction;

public class DateTimeTransactionMilestoningScheme extends TransactionMilestoningScheme
{
    public String transactionDateTimeInName;
    public String transactionDateTimeOutName;

    public <T> T accept(TransactionMilestoningSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}