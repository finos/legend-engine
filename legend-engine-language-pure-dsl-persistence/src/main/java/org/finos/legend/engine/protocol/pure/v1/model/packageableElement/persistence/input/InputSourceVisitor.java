package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.input;

public interface InputSourceVisitor<T>
{
    T visit(ServiceInputSource val);
}