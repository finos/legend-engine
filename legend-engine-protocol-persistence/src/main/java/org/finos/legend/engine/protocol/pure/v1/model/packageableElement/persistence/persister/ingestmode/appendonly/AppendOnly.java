package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.appendonly;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.Auditing;

public class AppendOnly extends IngestMode
{
    public Auditing auditing;
    public boolean filterDuplicates;

    public <T> T accept(IngestModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}