package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoned;

public class BatchIdTransactionMilestoningScheme extends TransactionMilestoningScheme
{
    public String batchIdInName;
    public String batchIdOutName;

    public <T> T accept(TransactionMilestoningSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}