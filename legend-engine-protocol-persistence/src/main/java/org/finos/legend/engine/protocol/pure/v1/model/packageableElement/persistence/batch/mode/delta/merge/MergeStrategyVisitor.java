package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge;

public interface MergeStrategyVisitor<T>
{
    T visit(DeleteIndicatorMergeStrategy val);
    T visit(NoDeletesMergeStrategy val);
    T visit(OpaqueMergeStrategy val);
}