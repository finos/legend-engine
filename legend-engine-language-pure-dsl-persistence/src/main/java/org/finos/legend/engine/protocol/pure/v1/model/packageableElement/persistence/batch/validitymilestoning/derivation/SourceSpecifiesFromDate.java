package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation;

public class SourceSpecifiesFromDate extends ValidityDerivation
{
    public String sourceDateTimeFromProperty;

    public <T> T accept(ValidityDerivationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}