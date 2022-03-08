package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.appendonly;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.BatchMilestoningMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.BatchMilestoningModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.auditing.Auditing;

public class AppendOnly extends BatchMilestoningMode
{
    public Auditing auditing;
    public boolean filterDuplicates;

    public <T> T accept(BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}