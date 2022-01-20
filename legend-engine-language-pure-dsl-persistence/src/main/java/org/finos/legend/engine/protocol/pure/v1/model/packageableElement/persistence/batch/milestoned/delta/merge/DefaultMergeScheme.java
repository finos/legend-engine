package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.delta.merge;

public class DefaultMergeScheme extends MergeScheme
{
    public <T> T accept(MergeSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}