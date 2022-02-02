package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.streaming;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceVisitor;

public class StreamingPersistence extends Persistence
{
    public SourceInformation sourceInformation;

    public <T> T accept(PersistenceVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}