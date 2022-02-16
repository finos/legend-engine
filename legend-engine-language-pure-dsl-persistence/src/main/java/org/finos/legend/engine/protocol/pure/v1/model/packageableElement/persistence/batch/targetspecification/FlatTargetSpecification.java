package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.deduplication.DeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningMode;

import java.util.List;

public class FlatTargetSpecification extends TargetSpecification
{
    public List<String> partitionProperties;
    public DeduplicationStrategy deduplicationStrategy;
    public BatchMilestoningMode batchMode;

    @Override
    public <T> T accept(TargetSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}