package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetshape;

import java.util.List;

public class MultiTargetShape extends TargetShape
{
    public String modelClass;
    public List<PropertyAndSingleTargetShape> children;
    public TransactionScope transactionScope;

    @Override
    public <T> T accept(TargetShapeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}