package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoned.derivation;

public class SourceSpecifiesValidFromAndThruDate extends ValidityDerivation
{
    public String sourceValidDateTimeFromProperty;
    public String sourceValidDateTimeThruProperty;

    public <T> T accept(ValidityDerivationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}