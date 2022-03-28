package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FlatTarget.class, name = "flatTarget"),
        @JsonSubTypes.Type(value = MultiFlatTarget.class, name = "multiFlatTarget"),
        @JsonSubTypes.Type(value = OpaqueTarget.class, name = "opaqueTarget")
})
public abstract class TargetShape
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(TargetShapeVisitor<T> visitor);
}