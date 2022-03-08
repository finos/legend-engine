package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetspecification;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.DeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.BatchMilestoningMode;

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