package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation;

public interface ValidityDerivationVisitor<T>
{
    T visit(SourceSpecifiesFromDateTime val);
    T visit(SourceSpecifiesFromAndThruDateTime val);
}