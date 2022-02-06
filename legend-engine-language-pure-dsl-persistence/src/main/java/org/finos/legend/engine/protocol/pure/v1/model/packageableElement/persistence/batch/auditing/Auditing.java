package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public abstract class Auditing
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(AuditingVisitor<T> visitor);
}