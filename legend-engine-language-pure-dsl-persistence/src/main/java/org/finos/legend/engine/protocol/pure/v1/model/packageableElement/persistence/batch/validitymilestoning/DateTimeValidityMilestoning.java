package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning;

public class DateTimeValidityMilestoning extends ValidityMilestoning
{
    public String dateTimeFromName;
    public String dateTimeThruName;

    public <T> T accept(ValidityMilestoningVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}