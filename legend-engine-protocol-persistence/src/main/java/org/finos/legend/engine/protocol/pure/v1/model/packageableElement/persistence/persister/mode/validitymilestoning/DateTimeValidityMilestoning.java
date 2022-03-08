package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.validitymilestoning;

public class DateTimeValidityMilestoning extends ValidityMilestoning
{
    public String dateTimeFromFieldName;
    public String dateTimeThruFieldName;

    public <T> T accept(ValidityMilestoningVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}