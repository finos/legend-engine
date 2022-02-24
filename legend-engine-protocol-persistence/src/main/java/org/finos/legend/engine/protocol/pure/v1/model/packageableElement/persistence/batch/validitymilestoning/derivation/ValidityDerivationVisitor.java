package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation;

public interface ValidityDerivationVisitor<T>
{
    T visit(SourceSpecifiesFromAndThruDateTime val);
    T visit(SourceSpecifiesFromDateTime val);
}