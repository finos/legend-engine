package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public class BatchDatastoreSpecification extends TargetSpecification
{
    public String datastoreName;
    public java.util.List<BatchDatasetSpecification> datasets = java.util.Collections.<BatchDatasetSpecification>emptyList();
    public SourceInformation sourceInformation;
}