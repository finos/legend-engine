package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification;

public interface TargetSpecificationVisitor<T>
{
    T visit(FlatTargetSpecification val);
    T visit(GroupedFlatTargetSpecification val);
    T visit(NestedTargetSpecification val);
}