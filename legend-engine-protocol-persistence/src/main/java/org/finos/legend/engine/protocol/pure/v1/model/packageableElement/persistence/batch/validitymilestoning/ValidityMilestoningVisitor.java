package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning;

public interface ValidityMilestoningVisitor<T>
{
    T visit(DateTimeValidityMilestoning val);
    T visit(OpaqueValidityMilestoning val);
}