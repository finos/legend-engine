package org.finos.legend.engine.protocol.persistence.batch.nonmilestoned.audit;

public abstract class AuditScheme
{
    public abstract <T> T accept(AuditSchemeVisitor<T> visitor);
}