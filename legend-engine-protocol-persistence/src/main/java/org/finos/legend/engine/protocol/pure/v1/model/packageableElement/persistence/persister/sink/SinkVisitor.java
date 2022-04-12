package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.sink;

public interface SinkVisitor<T>
{
    T visit(RelationalSink val);
    T visit(ObjectStorageSink val);
}
