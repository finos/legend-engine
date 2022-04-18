package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.derivation;

public class SourceSpecifiesInAndOutDateTime extends TransactionDerivation
{
    public String sourceDateTimeInField;
    public String sourceDateTimeOutField;

    @Override
    public <T> T accept(TransactionDerivationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
