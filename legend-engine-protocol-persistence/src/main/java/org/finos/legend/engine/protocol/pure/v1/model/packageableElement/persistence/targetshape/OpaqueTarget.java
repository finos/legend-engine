package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetshape;

public class OpaqueTarget extends TargetShape
{
    @Override
    public <T> T accept(TargetShapeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}