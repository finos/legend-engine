package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.deduplication;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = NoDeduplicationStrategy.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoDeduplicationStrategy.class, name = "NoDeduplicationStrategy"),
        @JsonSubTypes.Type(value = AnyVersionDeduplicationStrategy.class, name = "AnyVersionDeduplicationStrategy"),
        @JsonSubTypes.Type(value = MaxVersionDeduplicationStrategy.class, name = "MaxVersionDeduplicationStrategy"),
        @JsonSubTypes.Type(value = OpaqueDeduplicationStrategy.class, name = "OpaqueDeduplicationStrategy")
})
public abstract class DeduplicationStrategy
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(DeduplicationStrategyVisitor<T> visitor);
}