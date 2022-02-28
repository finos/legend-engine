package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersisterVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification.TargetSpecification;

public class BatchPersister extends Persister
{
    public TargetSpecification targetSpecification;

    public <T> T accept(PersisterVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}