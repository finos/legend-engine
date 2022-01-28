package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoned;

public interface TransactionMilestoningSchemeVisitor<T>
{
    T visit(BatchIdAndDateTimeTransactionMilestoningScheme val);
    T visit(BatchIdTransactionMilestoningScheme val);
    T visit(DateTimeTransactionMilestoningScheme val);
    T visit(OpaqueTransactionMilestoningScheme val);
}