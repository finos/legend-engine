package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.deduplication.DeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningMode;

import java.util.List;

public class FlatTargetSpecification extends TargetSpecification
{
    public List<String> partitionPropertyPaths;
    public DeduplicationStrategy deduplicationStrategy;
    public BatchMilestoningMode milestoningMode;
}