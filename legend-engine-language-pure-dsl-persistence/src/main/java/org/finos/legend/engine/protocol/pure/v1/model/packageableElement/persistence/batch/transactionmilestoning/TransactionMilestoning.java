package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoning;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BatchIdTransactionMilestoning.class, name = "BatchIdTransactionMilestoning"),
        @JsonSubTypes.Type(value = BatchIdAndDateTimeTransactionMilestoning.class, name = "BatchIdAndDateTimeTransactionMilestoning"),
        @JsonSubTypes.Type(value = DateTimeTransactionMilestoning.class, name = "DateTimeTransactionMilestoning"),
        @JsonSubTypes.Type(value = OpaqueTransactionMilestoning.class, name = "OpaqueTransactionMilestoning")
})
public abstract class TransactionMilestoning
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(TransactionMilestoningVisitor<T> visitor);
}