package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DateTimeValidityMilestoning.class, name = "dateTimeValidityMilestoning"),
        @JsonSubTypes.Type(value = OpaqueValidityMilestoning.class, name = "opaqueValidityMilestoning")
})
public abstract class ValidityMilestoning
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(ValidityMilestoningVisitor<T> visitor);
}