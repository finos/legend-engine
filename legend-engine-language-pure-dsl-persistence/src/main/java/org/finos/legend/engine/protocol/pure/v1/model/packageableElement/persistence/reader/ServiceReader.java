package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader;

import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;

public class ServiceReader extends Reader
{
    public PackageableElementPointer service;

    @Override
    public <T> T accept(ReaderVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}