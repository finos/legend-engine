package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetshape;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.DeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.BatchMilestoningMode;

import java.util.List;

public class FlatTarget extends TargetShape
{
    public String modelClass;
    public List<String> partitionProperties;
    public DeduplicationStrategy deduplicationStrategy;
    public BatchMilestoningMode batchMode;

    @Override
    public <T> T accept(TargetShapeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}