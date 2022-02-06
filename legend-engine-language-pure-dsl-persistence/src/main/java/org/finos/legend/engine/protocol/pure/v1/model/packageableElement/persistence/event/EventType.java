package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public abstract class EventType
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(EventTypeVisitor<T> visitor);
}