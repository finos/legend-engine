package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence;

public interface PersistenceVisitor<T>
{
    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchPersistence val);
    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.streaming.StreamingPersistence val);
}