package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OpaqueTrigger.class, name = "OpaqueTrigger")
})
public abstract class Trigger
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(TriggerVisitor<T> visitor);
}