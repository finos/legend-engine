package org.finos.legend.engine.protocol.persistence.batch.deduplication;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Property;

public class MaxVersionDeduplicationStrategy extends DeduplicationStrategy
{
    public Property versionProperty;

    public <T> T accept(DeduplicationStrategyVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}