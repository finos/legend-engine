package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = NoDeletesMergeStrategy.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoDeletesMergeStrategy.class, name = "noDeletesMergeStrategy"),
        @JsonSubTypes.Type(value = DeleteIndicatorMergeStrategy.class, name = "deleteIndicatorMergeStrategy"),
})
public abstract class MergeStrategy
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(MergeStrategyVisitor<T> visitor);
}