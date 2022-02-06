package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.deduplication;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public abstract class DeduplicationStrategy
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(DeduplicationStrategyVisitor<T> visitor);
}