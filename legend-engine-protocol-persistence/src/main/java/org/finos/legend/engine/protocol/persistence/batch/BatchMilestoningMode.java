package org.finos.legend.engine.protocol.persistence.batch;

public abstract class BatchMilestoningMode
{
    public abstract <T> T accept(BatchMilestoningModeVisitor<T> visitor);
}