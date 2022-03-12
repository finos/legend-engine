package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier;

public class EmailNotifyee extends Notifyee
{
    public String address;

    @Override
    public <T> T acceptVisitor(NotifyeeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}