package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoned.derivation;

public class SourceSpecifiesValidFromDate extends ValidityDerivation
{
    public String sourceValidDateTimeFromProperty;

    public <T> T accept(ValidityDerivationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}