package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier;

public class PagerDutyNotifyee extends Notifyee
{
    public String url;

    @Override
    public <T> T acceptVisitor(NotifyeeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}