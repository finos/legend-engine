package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.validity;

public class OpaqueValidityMilestoningScheme extends ValidityMilestoningScheme
{
    public <T> T accept(ValidityMilestoningSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}