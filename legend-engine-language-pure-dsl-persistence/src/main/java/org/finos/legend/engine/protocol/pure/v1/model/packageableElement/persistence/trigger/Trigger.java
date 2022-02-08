package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public abstract class Trigger
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(TriggerVisitor<T> visitor);
}