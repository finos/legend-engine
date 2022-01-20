package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.delta.merge;

public interface MergeSchemeVisitor<T>
{
    T visit(DefaultMergeScheme val);
    T visit(DeleteIndicatorMergeScheme val);
}