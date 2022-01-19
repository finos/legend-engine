package org.finos.legend.engine.protocol.persistence.batch.nonmilestoned.audit;

public class OpaqueAuditScheme extends AuditScheme
{
    public <T> T accept(AuditSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}