package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public abstract class Persistence
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(PersistenceVisitor<T> visitor);
}