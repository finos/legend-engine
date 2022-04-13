package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection;

public class AWS_CN extends AwsPartition
{
    @Override
    public <T> T accept(AwsPartitionVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}