package org.finos.legend.engine.protocol.persistence.batch.nonmilestoned.audit;

public class BatchDateTimeAuditScheme extends AuditScheme
{
    public String transactionDateTimePropertyName;

    public <T> T accept(AuditSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}