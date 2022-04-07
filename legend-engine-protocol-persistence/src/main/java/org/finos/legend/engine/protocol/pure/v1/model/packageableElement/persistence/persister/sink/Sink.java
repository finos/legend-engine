package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.sink;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RelationalSink.class, name = "relationalSink"),
        @JsonSubTypes.Type(value = ObjectStorageSink.class, name = "objectStorageSink"),
})
public abstract class Sink
{
    public SourceInformation sourceInformation;
    public abstract <T> T accept(SinkVisitor<T> visitor);
}