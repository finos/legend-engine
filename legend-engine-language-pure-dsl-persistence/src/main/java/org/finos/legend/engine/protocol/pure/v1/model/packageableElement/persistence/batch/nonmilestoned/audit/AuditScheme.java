package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.nonmilestoned.audit;

public abstract class AuditScheme
{
    public abstract <T> T accept(AuditSchemeVisitor<T> visitor);
}