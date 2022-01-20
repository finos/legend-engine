package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.validity.derivation;

public abstract class ValidityDerivation
{
    public abstract <T> T accept(ValidityDerivationVisitor<T> visitor);
}