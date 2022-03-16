package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger;

public class CronTrigger extends Trigger
{
    public String minutes;
    public String hours;
    public String dayOfMonth;
    public String month;
    public String dayOfWeek;

    @Override
    public <T> T accept(TriggerVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
