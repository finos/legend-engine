package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.streaming;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersisterVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.Persister;

public class StreamingPersister extends Persister
{
    public <T> T accept(PersisterVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}