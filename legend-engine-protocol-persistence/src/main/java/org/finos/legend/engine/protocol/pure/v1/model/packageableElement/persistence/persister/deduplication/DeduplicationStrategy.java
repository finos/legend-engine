package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = NoDeduplicationStrategy.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoDeduplicationStrategy.class, name = "noDeduplicationStrategy"),
        @JsonSubTypes.Type(value = AnyVersionDeduplicationStrategy.class, name = "anyVersionDeduplicationStrategy"),
        @JsonSubTypes.Type(value = MaxVersionDeduplicationStrategy.class, name = "maxVersionDeduplicationStrategy"),
        @JsonSubTypes.Type(value = OpaqueDeduplicationStrategy.class, name = "opaqueDeduplicationStrategy")
})
public abstract class DeduplicationStrategy
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(DeduplicationStrategyVisitor<T> visitor);
}