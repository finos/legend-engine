package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.nonmilestoned.audit;

public interface AuditSchemeVisitor<T>
{
    T visit(BatchDateTimeAuditScheme val);
    T visit(NoAuditScheme val);
    T visit(OpaqueAuditScheme val);
}