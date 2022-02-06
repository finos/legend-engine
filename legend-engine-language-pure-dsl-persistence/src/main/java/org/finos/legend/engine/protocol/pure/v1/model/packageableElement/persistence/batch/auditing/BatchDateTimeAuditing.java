package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing;

public class BatchDateTimeAuditing extends Auditing
{
    public String dateTimePropertyName;

    public <T> T accept(AuditingVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}