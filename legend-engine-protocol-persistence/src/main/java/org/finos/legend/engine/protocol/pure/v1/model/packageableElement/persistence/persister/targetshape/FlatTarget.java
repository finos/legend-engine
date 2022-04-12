package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.DeduplicationStrategy;

import java.util.List;

public class FlatTarget extends TargetShape
{
    public String modelClass;
    public String targetName;
    public List<String> partitionFields;
    public DeduplicationStrategy deduplicationStrategy;

    @Override
    public <T> T accept(TargetShapeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}