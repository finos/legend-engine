package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister;

public class BatchPersister extends Persister
{
    public <T> T accept(PersisterVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}