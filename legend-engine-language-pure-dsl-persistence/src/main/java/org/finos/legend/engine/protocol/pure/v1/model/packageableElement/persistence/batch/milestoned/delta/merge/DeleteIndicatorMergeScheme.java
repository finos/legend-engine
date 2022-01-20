package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.delta.merge;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Property;

public class DeleteIndicatorMergeScheme extends MergeScheme
{
    public Property deleteProperty;
    public java.util.List<String> deleteValues = java.util.Collections.<String>emptyList();

    public <T> T accept(MergeSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}