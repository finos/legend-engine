package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.AwsPartition;


public class S3Connection extends Connection
{

    public AwsPartition partition;
    public String region;
    public String bucket;

    @Override
    public <T> T accept(ConnectionVisitor<T> connectionVisitor)
    {
        return connectionVisitor.visit(this);
    }

}



