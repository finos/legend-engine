package org.finos.legend.engine.protocol.persistence.batch.milestoned.delta.merge;

public class DeleteIndicatorMergeScheme extends MergeScheme
{
    public java.util.List<String> deleteValues = java.util.Collections.<String>emptyList();

    public <T> T accept(MergeSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}