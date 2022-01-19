package org.finos.legend.engine.protocol.persistence.batch.deduplication;

public class CountDeduplicationStrategy extends DeduplicationStrategy
{
    public String duplicateCountPropertyName;

    public <T> T accept(DeduplicationStrategyVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}