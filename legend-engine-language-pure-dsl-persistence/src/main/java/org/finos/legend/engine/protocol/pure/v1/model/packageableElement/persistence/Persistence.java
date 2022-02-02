package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence;

public abstract class Persistence
{
    public DataShape inputShape;
    public String inputClassPath;

    public abstract <T> T accept(PersistenceVisitor<T> visitor);
}