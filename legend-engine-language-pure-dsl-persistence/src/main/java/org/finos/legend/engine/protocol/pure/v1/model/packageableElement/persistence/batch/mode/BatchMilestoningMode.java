package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public abstract class BatchMilestoningMode
{
    public SourceInformation sourceInformation;
    
    public abstract <T> T accept(BatchMilestoningModeVisitor<T> visitor);
}