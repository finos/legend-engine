package org.finos.legend.engine.protocol.persistence.event;

public class RegistryDatasetAvailable extends EventType
{
    public <T> T accept(EventTypeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}