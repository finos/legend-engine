package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoned;

public class OpaqueTransactionMilestoningScheme extends TransactionMilestoningScheme
{
    public <T> T accept(TransactionMilestoningSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}