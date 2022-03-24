package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier;

public interface NotifyeeVisitor<T>
{
    T visit(EmailNotifyee val);
    T visit(PagerDutyNotifyee val);
}