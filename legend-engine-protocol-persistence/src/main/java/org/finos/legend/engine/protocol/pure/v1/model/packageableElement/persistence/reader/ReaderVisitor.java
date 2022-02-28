package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader;

public interface ReaderVisitor<T>
{
    T visit(ServiceReader val);
}