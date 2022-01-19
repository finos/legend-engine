package org.finos.legend.engine.protocol.persistence.batch.milestoned.validity;

public class OpaqueValidityMilestoningScheme extends ValidityMilestoningScheme
{
    public <T> T accept(ValidityMilestoningSchemeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}