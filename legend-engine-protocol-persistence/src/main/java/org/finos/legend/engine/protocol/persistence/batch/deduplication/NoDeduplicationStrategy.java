package org.finos.legend.engine.protocol.persistence.batch.deduplication;

public class NoDeduplicationStrategy extends DeduplicationStrategy
{
    public <T> T accept(DeduplicationStrategyVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}