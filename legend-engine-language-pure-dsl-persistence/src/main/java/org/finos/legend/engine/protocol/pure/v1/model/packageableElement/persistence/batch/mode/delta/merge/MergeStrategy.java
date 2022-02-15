package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = NoDeletesMergeStrategy.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoDeletesMergeStrategy.class, name = "noDeletesMergeStrategy"),
        @JsonSubTypes.Type(value = DeleteIndicatorMergeStrategy.class, name = "deleteIndicatorMergeStrategy"),
        @JsonSubTypes.Type(value = OpaqueMergeStrategy.class, name = "opaqueMergeStrategy")
})
public abstract class MergeStrategy
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(MergeStrategyVisitor<T> visitor);
}