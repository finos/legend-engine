package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection;

public interface FinCloudTargetSpecificationVisitor<T>
{
    T visit(FinCloudTargetSpecification finCloudTargetSpecification);
}
