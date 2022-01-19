package org.finos.legend.engine.protocol.persistence.batch.milestoned.delta.merge;

public interface MergeSchemeVisitor<T>
{
    T visit(DefaultMergeScheme val);
    T visit(DeleteIndicatorMergeScheme val);
}