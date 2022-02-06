package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification.TargetSpecification;

public class BatchPersistence extends Persistence
{
    public TargetSpecification targetSpecification;

    public <T> T accept(PersistenceVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}