package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing;

public interface AuditingVisitor<T>
{
    T visit(DateTimeAuditing val);
    T visit(NoAuditing val);
    T visit(OpaqueAuditing val);
}