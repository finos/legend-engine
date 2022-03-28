package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape;

public interface TargetShapeVisitor<T>
{
    T visit(FlatTarget val);
    T visit(MultiFlatTarget val);
    T visit(OpaqueTarget val);
}