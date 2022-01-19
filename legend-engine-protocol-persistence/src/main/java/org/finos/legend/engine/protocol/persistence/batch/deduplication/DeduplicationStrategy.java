package org.finos.legend.engine.protocol.persistence.batch.deduplication;

public abstract class DeduplicationStrategy
{
    public abstract <T> T accept(DeduplicationStrategyVisitor<T> visitor);
}