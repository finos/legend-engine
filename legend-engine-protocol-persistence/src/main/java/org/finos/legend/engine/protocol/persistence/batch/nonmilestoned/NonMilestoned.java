package org.finos.legend.engine.protocol.persistence.batch.nonmilestoned;

public class NonMilestoned extends org.finos.legend.engine.protocol.persistence.batch.BatchMilestoningMode
{
    public org.finos.legend.engine.protocol.persistence.batch.nonmilestoned.audit.AuditScheme auditScheme;

    public <T> T accept(org.finos.legend.engine.protocol.persistence.batch.BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}