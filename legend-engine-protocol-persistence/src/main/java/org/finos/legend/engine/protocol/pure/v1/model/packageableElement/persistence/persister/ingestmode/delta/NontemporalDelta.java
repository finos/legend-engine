package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.Auditing;

public class NontemporalDelta extends IngestMode
{
    public Auditing auditing;

    public <T> T accept(IngestModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}