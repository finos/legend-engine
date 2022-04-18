package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.derivation;

public class SourceSpecifiesInDateTime extends TransactionDerivation
{
    public String sourceDateTimeInField;

    @Override
    public <T> T accept(TransactionDerivationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
