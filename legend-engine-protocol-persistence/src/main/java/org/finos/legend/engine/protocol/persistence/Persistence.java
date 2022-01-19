package org.finos.legend.engine.protocol.persistence;

public abstract class Persistence
{
    public DataShape inputShape;

    public abstract <T> T accept(PersistenceVisitor<T> visitor);
}