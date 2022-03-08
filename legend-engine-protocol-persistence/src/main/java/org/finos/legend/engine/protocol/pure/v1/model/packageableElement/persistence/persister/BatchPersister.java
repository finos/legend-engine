package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersisterVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetspecification.TargetSpecification;

public class BatchPersister extends Persister
{
    public TargetSpecification targetSpecification;

    public <T> T accept(PersisterVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}