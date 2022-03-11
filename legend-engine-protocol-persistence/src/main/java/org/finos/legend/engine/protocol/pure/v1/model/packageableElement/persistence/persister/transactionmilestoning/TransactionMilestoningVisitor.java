package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning;

public interface TransactionMilestoningVisitor<T>
{
    T visit(BatchIdAndDateTimeTransactionMilestoning val);
    T visit(BatchIdTransactionMilestoning val);
    T visit(DateTimeTransactionMilestoning val);
    T visit(OpaqueTransactionMilestoning val);
}