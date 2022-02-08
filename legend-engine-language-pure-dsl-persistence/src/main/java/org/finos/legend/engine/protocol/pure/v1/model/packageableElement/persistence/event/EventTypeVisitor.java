package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event;

public interface EventTypeVisitor<T>
{
    T visit(ScheduleFired val);
    T visit(OpaqueEventType val);
}