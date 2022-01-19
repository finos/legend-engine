package org.finos.legend.engine.protocol.persistence.batch;

public class BatchPersistence extends org.finos.legend.engine.protocol.persistence.Persistence
{
    public BatchTransactionMode transactionMode;
    public BatchDatastoreSpecification targetSpecification;

    public <T> T accept(org.finos.legend.engine.protocol.persistence.PersistenceVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}