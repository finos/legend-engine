package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger;

public class ManualTrigger extends Trigger
{
    @Override
    public <T> T accept(TriggerVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
