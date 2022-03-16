package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing;

public interface AuditingVisitor<T>
{
    T visit(NoAuditing val);
    T visit(DateTimeAuditing val);
}