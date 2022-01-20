package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event;

public class ScheduleTriggered extends EventType
{
    public <T> T accept(EventTypeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}