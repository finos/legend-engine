package org.finos.legend.engine.protocol.persistence.batch;

public class BatchDatastoreSpecification
{
    public String datastoreName;
    public java.util.List<BatchDatasetSpecification> datasets = java.util.Collections.<BatchDatasetSpecification>emptyList();
}