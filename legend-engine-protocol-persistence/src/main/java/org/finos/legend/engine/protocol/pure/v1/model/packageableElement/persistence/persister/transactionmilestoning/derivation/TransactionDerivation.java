package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.derivation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SourceSpecifiesInDateTime.class, name = "sourceSpecifiesInDateTime"),
        @JsonSubTypes.Type(value = SourceSpecifiesInAndOutDateTime.class, name = "sourceSpecifiesInAndOutDateTime")
})
public abstract class TransactionDerivation
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(TransactionDerivationVisitor<T> visitor);
}
