package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoned.derivation;

public interface ValidityDerivationVisitor<T>
{
    T visit(SourceSpecifiesValidFromAndThruDate val);
    T visit(SourceSpecifiesValidFromDate val);
}