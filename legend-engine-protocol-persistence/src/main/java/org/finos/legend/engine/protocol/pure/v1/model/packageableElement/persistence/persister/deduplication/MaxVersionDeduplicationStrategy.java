package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication;

public class MaxVersionDeduplicationStrategy extends DeduplicationStrategy
{
    public String versionProperty;

    @Override
    public <T> T accept(DeduplicationStrategyVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}