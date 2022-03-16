package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.IdentifiedConnection;

import java.util.Collections;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BatchPersister.class, name = "batchPersister"),
        @JsonSubTypes.Type(value = StreamingPersister.class, name = "streamingPersister")
})
public abstract class Persister
{
    public List<IdentifiedConnection> connections = Collections.emptyList();
    public SourceInformation sourceInformation;

    public abstract <T> T accept(PersisterVisitor<T> visitor);
}