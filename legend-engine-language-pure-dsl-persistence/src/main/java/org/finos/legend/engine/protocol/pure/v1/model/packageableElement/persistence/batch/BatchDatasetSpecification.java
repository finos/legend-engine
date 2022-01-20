package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Property;

public class BatchDatasetSpecification
{
    public String datasetName;
    public java.util.List<Property> partitionProperties = java.util.Collections.<Property>emptyList();
    public org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.deduplication.DeduplicationStrategy deduplicationStrategy;
    public BatchMilestoningMode milestoningMode;
}