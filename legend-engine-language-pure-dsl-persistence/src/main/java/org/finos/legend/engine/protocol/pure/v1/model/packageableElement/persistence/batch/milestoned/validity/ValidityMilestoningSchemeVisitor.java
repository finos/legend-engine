package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.validity;

public interface ValidityMilestoningSchemeVisitor<T>
{
    T visit(DateTimeValidityMilestoningScheme val);
    T visit(OpaqueValidityMilestoningScheme val);
}