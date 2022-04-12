package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge;

public class DeleteIndicatorMergeStrategy extends MergeStrategy
{
    public String deleteField;
    public java.util.List<String> deleteValues = java.util.Collections.emptyList();

    public <T> T accept(MergeStrategyVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}