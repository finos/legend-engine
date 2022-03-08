package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.delta.merge;

public interface MergeStrategyVisitor<T>
{
    T visit(DeleteIndicatorMergeStrategy val);
    T visit(NoDeletesMergeStrategy val);
    T visit(OpaqueMergeStrategy val);
}