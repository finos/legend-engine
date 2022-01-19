package org.finos.legend.engine.protocol.persistence.batch.milestoned.transaction;

public class OpaqueTransactionMilestoningScheme extends TransactionMilestoningScheme
{
    public <T> T accept(TransactionMilestoningSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}