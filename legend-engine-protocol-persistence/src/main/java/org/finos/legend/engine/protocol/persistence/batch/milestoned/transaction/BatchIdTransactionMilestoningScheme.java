package org.finos.legend.engine.protocol.persistence.batch.milestoned.transaction;

public class BatchIdTransactionMilestoningScheme extends TransactionMilestoningScheme
{
    public String batchIdInName;
    public String batchIdOutName;

    public <T> T accept(TransactionMilestoningSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}