package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public abstract class ValidityDerivation
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(ValidityDerivationVisitor<T> visitor);
}