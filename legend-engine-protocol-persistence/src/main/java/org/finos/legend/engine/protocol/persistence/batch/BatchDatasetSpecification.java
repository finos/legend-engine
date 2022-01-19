package org.finos.legend.engine.protocol.persistence.batch;

public class BatchDatasetSpecification
{
    public String datasetName;
    public org.finos.legend.engine.protocol.persistence.batch.deduplication.DeduplicationStrategy deduplicationStrategy;
    public BatchMilestoningMode milestoningMode;
}