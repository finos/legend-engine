package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.audit.AuditScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningModeVisitor;

public class NonMilestonedSnapshot extends BatchMilestoningMode
{
    public AuditScheme auditScheme;

    public <T> T accept(BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}