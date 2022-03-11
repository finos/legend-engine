package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetshape;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.DeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestMode;

import java.util.List;

public class FlatTarget extends TargetShape
{
    public String modelClass;
    public String targetName;
    public List<String> partitionProperties;
    public DeduplicationStrategy deduplicationStrategy;
    public IngestMode ingestMode;

    @Override
    public <T> T accept(TargetShapeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}