package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoned;

public abstract class TransactionMilestoningScheme
{
    public abstract <T> T accept(TransactionMilestoningSchemeVisitor<T> visitor);
}