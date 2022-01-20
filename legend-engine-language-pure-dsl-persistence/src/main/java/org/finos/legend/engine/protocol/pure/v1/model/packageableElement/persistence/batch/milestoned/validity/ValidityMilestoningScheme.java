package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.validity;

public abstract class ValidityMilestoningScheme
{
    public abstract <T> T accept(ValidityMilestoningSchemeVisitor<T> visitor);
}