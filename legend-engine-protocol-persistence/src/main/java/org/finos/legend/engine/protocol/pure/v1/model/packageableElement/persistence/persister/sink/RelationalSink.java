package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.sink;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;

public class RelationalSink extends Sink
{
    public Connection connection;

    @Override
    public <T> T accept(SinkVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}