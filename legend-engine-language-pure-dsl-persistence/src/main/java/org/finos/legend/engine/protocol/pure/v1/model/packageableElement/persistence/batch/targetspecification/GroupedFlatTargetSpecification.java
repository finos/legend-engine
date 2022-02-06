package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification;

import java.util.List;

public class GroupedFlatTargetSpecification extends FlatTargetSpecification
{
    public List<PropertyAndFlatTargetSpecification> components;
    public TransactionScope transactionScope;
}