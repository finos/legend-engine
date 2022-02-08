package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public abstract class Reader
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(ReaderVisitor<T> visitor);
}