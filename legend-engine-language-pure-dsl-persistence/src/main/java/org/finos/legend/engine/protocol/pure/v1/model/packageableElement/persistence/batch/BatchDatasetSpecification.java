package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.deduplication.DeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningMode;

public class BatchDatasetSpecification
{
    public String datasetName;
    public java.util.List<String> partitionProperties = java.util.Collections.emptyList();
    public DeduplicationStrategy deduplicationStrategy;
    public BatchMilestoningMode milestoningMode;
    public SourceInformation sourceInformation;
}