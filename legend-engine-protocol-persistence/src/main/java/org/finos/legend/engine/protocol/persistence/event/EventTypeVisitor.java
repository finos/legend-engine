package org.finos.legend.engine.protocol.persistence.event;

public interface EventTypeVisitor<T>
{
    T visit(RegistryDatasetAvailable val);
    T visit(ScheduleTriggered val);
}