package org.finos.legend.engine.protocol.persistence.batch.milestoned.transaction;

public abstract class TransactionMilestoningScheme
{
    public abstract <T> T accept(TransactionMilestoningSchemeVisitor<T> visitor);
}