package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.deduplication;

public abstract class DeduplicationStrategy
{
    public abstract <T> T accept(DeduplicationStrategyVisitor<T> visitor);
}