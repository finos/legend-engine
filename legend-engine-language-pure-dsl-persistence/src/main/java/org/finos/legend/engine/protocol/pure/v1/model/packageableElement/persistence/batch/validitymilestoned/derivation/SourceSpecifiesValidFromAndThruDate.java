package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoned.derivation;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Property;

public class SourceSpecifiesValidFromAndThruDate extends ValidityDerivation
{
    public Property sourceValidDateTimeFromProperty;
    public Property sourceValidDateTimeThruProperty;

    public <T> T accept(ValidityDerivationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}