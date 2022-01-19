package org.finos.legend.engine.protocol.persistence.streaming;

public class StreamingPersistence extends org.finos.legend.engine.protocol.persistence.Persistence
{
    public <T> T accept(org.finos.legend.engine.protocol.persistence.PersistenceVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}