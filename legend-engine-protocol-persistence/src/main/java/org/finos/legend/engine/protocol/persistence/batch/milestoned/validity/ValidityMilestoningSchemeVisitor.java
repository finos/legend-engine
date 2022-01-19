package org.finos.legend.engine.protocol.persistence.batch.milestoned.validity;

public interface ValidityMilestoningSchemeVisitor<T>
{
    T visit(DateTimeValidityMilestoningScheme val);
    T visit(OpaqueValidityMilestoningScheme val);
}