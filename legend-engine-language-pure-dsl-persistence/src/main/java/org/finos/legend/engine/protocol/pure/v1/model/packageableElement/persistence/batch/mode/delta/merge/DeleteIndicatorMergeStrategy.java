package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge;

public class DeleteIndicatorMergeStrategy extends MergeStrategy
{
    public String deleteProperty;
    public java.util.List<String> deleteValues = java.util.Collections.<String>emptyList();

    public <T> T accept(MergeSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}