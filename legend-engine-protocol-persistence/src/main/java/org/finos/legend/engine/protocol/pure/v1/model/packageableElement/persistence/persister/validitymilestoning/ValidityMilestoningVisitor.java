package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning;

public interface ValidityMilestoningVisitor<T>
{
    T visit(DateTimeValidityMilestoning val);
    T visit(OpaqueValidityMilestoning val);
}