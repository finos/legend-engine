package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection;

public interface AwsPartitionVisitor<T>
{
    T visit(AWS val);
    T visit(AWS_CN val);
    T visit(AWS_US_GOV val);
}