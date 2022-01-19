package org.finos.legend.engine.protocol.persistence.batch.milestoned.validity;

public abstract class ValidityMilestoningScheme
{
    public abstract <T> T accept(ValidityMilestoningSchemeVisitor<T> visitor);
}