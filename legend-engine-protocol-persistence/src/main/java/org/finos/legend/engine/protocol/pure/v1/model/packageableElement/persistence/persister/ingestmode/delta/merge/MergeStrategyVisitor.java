package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge;

public interface MergeStrategyVisitor<T>
{
    T visit(NoDeletesMergeStrategy val);
    T visit(DeleteIndicatorMergeStrategy val);
}