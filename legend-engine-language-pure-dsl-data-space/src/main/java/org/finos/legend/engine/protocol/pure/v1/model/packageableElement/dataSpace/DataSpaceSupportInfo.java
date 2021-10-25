package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DataSpaceSupportEmail.class, name = "email"),
})
public abstract class DataSpaceSupportInfo
{
}
