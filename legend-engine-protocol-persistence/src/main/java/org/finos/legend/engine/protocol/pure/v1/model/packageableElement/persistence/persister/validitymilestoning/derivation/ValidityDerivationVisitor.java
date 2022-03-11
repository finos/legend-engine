package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation;

public interface ValidityDerivationVisitor<T>
{
    T visit(SourceSpecifiesFromAndThruDateTime val);
    T visit(SourceSpecifiesFromDateTime val);
}