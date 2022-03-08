package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.delta;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.BatchMilestoningMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.BatchMilestoningModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.auditing.Auditing;

public class NonMilestonedDelta extends BatchMilestoningMode
{
    public Auditing auditing;

    public <T> T accept(BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}