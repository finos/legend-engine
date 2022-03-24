package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister;

public interface PersisterVisitor<T>
{
    T visit(BatchPersister val);
    T visit(StreamingPersister val);
}