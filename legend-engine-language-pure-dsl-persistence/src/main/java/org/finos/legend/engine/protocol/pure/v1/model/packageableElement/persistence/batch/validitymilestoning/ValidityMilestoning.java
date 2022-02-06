package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public abstract class ValidityMilestoning
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(ValidityMilestoningVisitor<T> visitor);
}