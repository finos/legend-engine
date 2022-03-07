package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetshape;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.deduplication.DeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningMode;

import java.util.List;

public class SingleTargetShape extends TargetShape
{
    public String modelClass;
    public List<String> partitionProperties;
    public DeduplicationStrategy deduplicationStrategy;
    public BatchMilestoningMode milestoningMode;

    @Override
    public <T> T accept(TargetShapeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}