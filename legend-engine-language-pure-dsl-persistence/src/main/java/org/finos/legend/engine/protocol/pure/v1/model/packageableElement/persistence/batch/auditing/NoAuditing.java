package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing;

public class NoAuditing extends Auditing
{
    public <T> T accept(AuditingVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}