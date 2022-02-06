package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing;

public interface AuditingVisitor<T>
{
    T visit(BatchDateTimeAuditing val);
    T visit(NoAuditing val);
    T visit(OpaqueAuditing val);
}