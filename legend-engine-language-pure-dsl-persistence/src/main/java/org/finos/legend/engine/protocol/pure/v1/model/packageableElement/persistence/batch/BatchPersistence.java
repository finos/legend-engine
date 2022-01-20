package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceVisitor;

public class BatchPersistence extends Persistence
{
    public BatchTransactionMode transactionMode;
    public BatchDatastoreSpecification targetSpecification;

    public <T> T accept(PersistenceVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}