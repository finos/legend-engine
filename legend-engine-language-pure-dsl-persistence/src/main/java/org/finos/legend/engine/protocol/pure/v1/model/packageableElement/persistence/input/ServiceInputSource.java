package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.input;

import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;

public class ServiceInputSource extends InputSource
{
    public PackageableElementPointer service;

    @Override
    public <T> T accept(InputSourceVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}