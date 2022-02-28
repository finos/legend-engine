package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.deduplication;

public class NoDeduplicationStrategy extends DeduplicationStrategy
{
    @Override
    public <T> T accept(DeduplicationStrategyVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}