package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.input;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public abstract class InputSource
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(InputSourceVisitor<T> visitor);
}