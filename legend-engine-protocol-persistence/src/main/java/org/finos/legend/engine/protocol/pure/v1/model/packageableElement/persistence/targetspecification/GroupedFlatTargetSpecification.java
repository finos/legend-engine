package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetspecification;

import java.util.List;

public class GroupedFlatTargetSpecification extends TargetSpecification
{
    public List<PropertyAndFlatTargetSpecification> components;
    public TransactionScope transactionScope;

    @Override
    public <T> T accept(TargetSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}