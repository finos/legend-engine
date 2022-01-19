package org.finos.legend.engine.protocol.persistence.event;

public abstract class EventType
{
    public abstract <T> T accept(EventTypeVisitor<T> visitor);
}