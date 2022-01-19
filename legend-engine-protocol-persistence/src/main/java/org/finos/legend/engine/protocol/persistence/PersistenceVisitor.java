package org.finos.legend.engine.protocol.persistence;

public interface PersistenceVisitor<T>
{
    T visit(org.finos.legend.engine.protocol.persistence.batch.BatchPersistence val);
    T visit(org.finos.legend.engine.protocol.persistence.streaming.StreamingPersistence val);
}