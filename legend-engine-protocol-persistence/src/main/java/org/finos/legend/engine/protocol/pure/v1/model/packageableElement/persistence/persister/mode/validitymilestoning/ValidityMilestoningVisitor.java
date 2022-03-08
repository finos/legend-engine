package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.validitymilestoning;

public interface ValidityMilestoningVisitor<T>
{
    T visit(DateTimeValidityMilestoning val);
    T visit(OpaqueValidityMilestoning val);
}