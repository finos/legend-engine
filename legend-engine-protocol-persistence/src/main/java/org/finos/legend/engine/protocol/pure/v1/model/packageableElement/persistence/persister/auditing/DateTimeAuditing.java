package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing;

public class DateTimeAuditing extends Auditing
{
    public String dateTimeFieldName;

    @Override
    public <T> T accept(AuditingVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}