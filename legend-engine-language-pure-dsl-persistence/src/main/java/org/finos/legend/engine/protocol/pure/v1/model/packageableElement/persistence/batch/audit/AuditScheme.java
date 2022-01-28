package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.audit;

public abstract class AuditScheme
{
    public abstract <T> T accept(AuditSchemeVisitor<T> visitor);
}