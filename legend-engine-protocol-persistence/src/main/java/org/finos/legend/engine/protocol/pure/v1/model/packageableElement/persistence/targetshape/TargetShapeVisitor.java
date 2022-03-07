package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetshape;

public interface TargetShapeVisitor<T>
{
    T visit(SingleTargetShape val);
    T visit(MultiTargetShape val);
    T visit(OpaqueTargetShape val);
}