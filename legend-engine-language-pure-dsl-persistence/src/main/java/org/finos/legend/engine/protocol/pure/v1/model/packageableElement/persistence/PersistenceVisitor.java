package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchPersistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.streaming.StreamingPersistence;

public interface PersistenceVisitor<T>
{
    T visit(BatchPersistence val);
    T visit(StreamingPersistence val);
}