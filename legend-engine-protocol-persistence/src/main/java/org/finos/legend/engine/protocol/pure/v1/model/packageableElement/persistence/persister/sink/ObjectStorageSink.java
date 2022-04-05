package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.sink;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;

public class ObjectStorageSink extends Sink
{
    public Connection connection;
    public String binding;

    @Override
    public <T> T accept(SinkVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}