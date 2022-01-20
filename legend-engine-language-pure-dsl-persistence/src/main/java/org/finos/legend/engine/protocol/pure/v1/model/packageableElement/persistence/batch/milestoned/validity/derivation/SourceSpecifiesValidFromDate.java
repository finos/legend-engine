package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.validity.derivation;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Property;

public class SourceSpecifiesValidFromDate extends ValidityDerivation
{
    public Property sourceValidDateTimeFromProperty;

    public <T> T accept(ValidityDerivationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}