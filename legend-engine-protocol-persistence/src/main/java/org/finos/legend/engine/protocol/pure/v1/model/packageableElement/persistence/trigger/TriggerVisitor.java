package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger;

public interface TriggerVisitor<T>
{
    T visit(OpaqueTrigger val);
}