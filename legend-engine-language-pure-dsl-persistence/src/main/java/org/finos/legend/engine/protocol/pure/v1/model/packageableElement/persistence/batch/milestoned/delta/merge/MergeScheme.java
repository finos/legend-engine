package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.delta.merge;

public abstract class MergeScheme
{
    public abstract <T> T accept(MergeSchemeVisitor<T> visitor);
}