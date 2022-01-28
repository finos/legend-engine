package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoned;

public class DateTimeValidityMilestoningScheme extends ValidityMilestoningScheme
{
    public String validDateTimeFromName;
    public String validDateTimeThruName;

    public <T> T accept(ValidityMilestoningSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}