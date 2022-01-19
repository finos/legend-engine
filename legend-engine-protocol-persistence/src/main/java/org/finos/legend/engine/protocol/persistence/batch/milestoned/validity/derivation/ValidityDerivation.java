package org.finos.legend.engine.protocol.persistence.batch.milestoned.validity.derivation;

public abstract class ValidityDerivation
{
    public abstract <T> T accept(ValidityDerivationVisitor<T> visitor);
}