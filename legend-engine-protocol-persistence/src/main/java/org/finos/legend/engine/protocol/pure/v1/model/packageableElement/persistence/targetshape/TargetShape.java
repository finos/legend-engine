package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetshape;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SingleTargetShape.class, name = "singleTargetShape"),
        @JsonSubTypes.Type(value = MultiTargetShape.class, name = "multiTargetShape"),
        @JsonSubTypes.Type(value = OpaqueTargetShape.class, name = "opaqueTargetShape")
})
public abstract class TargetShape
{
    public String targetName;
    public SourceInformation sourceInformation;

    public abstract <T> T accept(TargetShapeVisitor<T> visitor);
}