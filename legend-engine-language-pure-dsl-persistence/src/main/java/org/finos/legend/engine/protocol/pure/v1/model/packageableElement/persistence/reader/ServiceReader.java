package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader;

public class ServiceReader extends Reader
{
    public String service;

    @Override
    public <T> T accept(ReaderVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}