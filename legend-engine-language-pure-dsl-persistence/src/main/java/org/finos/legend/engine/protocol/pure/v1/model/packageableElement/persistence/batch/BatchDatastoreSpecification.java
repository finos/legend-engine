package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;

public class BatchDatastoreSpecification
{
    public String datastoreName;
    public java.util.List<BatchDatasetSpecification> datasets = java.util.Collections.<BatchDatasetSpecification>emptyList();
    public Runtime runtime;
}