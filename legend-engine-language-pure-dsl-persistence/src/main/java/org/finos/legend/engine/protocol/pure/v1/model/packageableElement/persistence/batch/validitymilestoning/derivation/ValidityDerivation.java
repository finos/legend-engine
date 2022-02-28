package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SourceSpecifiesFromDateTime.class, name = "sourceSpecifiesFromDateTime"),
        @JsonSubTypes.Type(value = SourceSpecifiesFromAndThruDateTime.class, name = "sourceSpecifiesFromAndThruDateTime")
})
public abstract class ValidityDerivation
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(ValidityDerivationVisitor<T> visitor);
}