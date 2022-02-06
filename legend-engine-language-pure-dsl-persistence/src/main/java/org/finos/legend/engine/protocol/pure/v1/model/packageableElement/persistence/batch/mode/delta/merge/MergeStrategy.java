package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public abstract class MergeStrategy
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(MergeSchemeVisitor<T> visitor);
}