package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch;

public abstract class BatchMilestoningMode
{
    public abstract <T> T accept(BatchMilestoningModeVisitor<T> visitor);
}