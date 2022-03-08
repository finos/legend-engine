package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetspecification;

public class NestedTargetSpecification extends TargetSpecification
{
    @Override
    public <T> T accept(TargetSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}