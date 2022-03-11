package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning;

public class OpaqueValidityMilestoning extends ValidityMilestoning
{
    public <T> T accept(ValidityMilestoningVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}