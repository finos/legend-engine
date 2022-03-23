package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing;

public class NoAuditing extends Auditing
{
    @Override
    public <T> T accept(AuditingVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}