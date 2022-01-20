package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event;

public abstract class EventType
{
    public abstract <T> T accept(EventTypeVisitor<T> visitor);
}