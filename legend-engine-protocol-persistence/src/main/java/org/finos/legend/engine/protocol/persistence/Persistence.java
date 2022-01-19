package org.finos.legend.engine.protocol.persistence;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;

public abstract class Persistence
{
    public DataShape inputShape;
    public Class inputClass;

    public abstract <T> T accept(PersistenceVisitor<T> visitor);
}