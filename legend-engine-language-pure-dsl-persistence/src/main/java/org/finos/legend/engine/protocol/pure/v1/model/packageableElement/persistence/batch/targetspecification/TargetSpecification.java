package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FlatTargetSpecification.class, name = "FlatTargetSpecification"),
        @JsonSubTypes.Type(value = GroupedFlatTargetSpecification.class, name = "GroupedFlatTargetSpecification"),
        @JsonSubTypes.Type(value = NestedTargetSpecification.class, name = "NestedTargetSpecification")
})
public abstract class TargetSpecification
{
    public String targetName;
    public String modelClass;
    public SourceInformation sourceInformation;

    public abstract <T> T accept(TargetSpecificationVisitor<T> visitor);
}