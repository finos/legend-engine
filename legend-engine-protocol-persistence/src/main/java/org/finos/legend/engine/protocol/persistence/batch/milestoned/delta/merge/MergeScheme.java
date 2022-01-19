package org.finos.legend.engine.protocol.persistence.batch.milestoned.delta.merge;

public abstract class MergeScheme
{
    public abstract <T> T accept(MergeSchemeVisitor<T> visitor);
}