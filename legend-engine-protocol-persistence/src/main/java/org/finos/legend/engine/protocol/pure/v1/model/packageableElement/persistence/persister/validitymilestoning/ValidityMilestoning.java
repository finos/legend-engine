package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.ValidityDerivation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DateTimeValidityMilestoning.class, name = "dateTimeValidityMilestoning"),
})
public abstract class ValidityMilestoning
{
    public SourceInformation sourceInformation;
    public ValidityDerivation derivation;

    public abstract <T> T accept(ValidityMilestoningVisitor<T> visitor);
}