package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.derivation;

public interface TransactionDerivationVisitor<T>
{
    T visit(SourceSpecifiesInDateTime val);
    T visit(SourceSpecifiesInAndOutDateTime val);
}
