package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.auditing;

public class BatchDateTimeAuditing extends Auditing
{
    public String dateTimeFieldName;

    @Override
    public <T> T accept(AuditingVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}