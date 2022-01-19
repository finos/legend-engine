package org.finos.legend.engine.protocol.persistence.batch.milestoned.validity;

public class DateTimeValidityMilestoningScheme extends ValidityMilestoningScheme
{
    public String validDateTimeFromName;
    public String validDateTimeThruName;

    public <T> T accept(ValidityMilestoningSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}