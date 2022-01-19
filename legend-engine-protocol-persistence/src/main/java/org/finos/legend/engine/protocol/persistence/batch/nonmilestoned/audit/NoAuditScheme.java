package org.finos.legend.engine.protocol.persistence.batch.nonmilestoned.audit;

public class NoAuditScheme extends AuditScheme
{
    public <T> T accept(AuditSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}