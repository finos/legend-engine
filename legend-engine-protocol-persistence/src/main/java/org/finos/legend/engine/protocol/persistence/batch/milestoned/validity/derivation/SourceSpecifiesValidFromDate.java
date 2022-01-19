package org.finos.legend.engine.protocol.persistence.batch.milestoned.validity.derivation;

public class SourceSpecifiesValidFromDate extends ValidityDerivation
{
    public <T> T accept(ValidityDerivationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}