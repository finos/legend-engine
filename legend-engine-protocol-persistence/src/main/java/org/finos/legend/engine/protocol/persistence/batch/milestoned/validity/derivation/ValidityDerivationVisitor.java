package org.finos.legend.engine.protocol.persistence.batch.milestoned.validity.derivation;

public interface ValidityDerivationVisitor<T>
{
    T visit(SourceSpecifiesValidFromAndThruDate val);
    T visit(SourceSpecifiesValidFromDate val);
}