package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event;

public interface EventTypeVisitor<T>
{
    T visit(RegistryDatasetAvailable val);
    T visit(ScheduleTriggered val);
}