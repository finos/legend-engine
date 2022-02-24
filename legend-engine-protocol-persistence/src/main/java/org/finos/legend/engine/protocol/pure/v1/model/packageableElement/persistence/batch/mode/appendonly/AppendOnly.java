package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.appendonly;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing.Auditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningModeVisitor;

public class AppendOnly extends BatchMilestoningMode
{
    public Auditing auditing;
    public boolean filterDuplicates;

    public <T> T accept(BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}