package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.IdentifiedConnection;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.Binding;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BatchPersister.class, name = "batchPersister"),
        @JsonSubTypes.Type(value = StreamingPersister.class, name = "streamingPersister")
})
public abstract class Persister
{
    public IdentifiedConnection connection;
    public Binding binding;
    public SourceInformation sourceInformation;

    public abstract <T> T accept(PersisterVisitor<T> visitor);
}