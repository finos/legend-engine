package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge;

public class NoDeletesMergeStrategy extends MergeStrategy
{
    public <T> T accept(MergeStrategyVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}