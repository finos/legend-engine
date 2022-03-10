package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetshape;

import java.util.List;

public class MultiFlatTarget extends TargetShape
{
    public String modelClass;
    public TransactionScope transactionScope;
    public List<PropertyAndFlatTarget> parts;

    @Override
    public <T> T accept(TargetShapeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}