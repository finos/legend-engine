package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetshape.TargetShape;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BatchPersister.class, name = "batchPersister"),
        @JsonSubTypes.Type(value = StreamingPersister.class, name = "streamingPersister")
})
public abstract class Persister
{
    //TODO: ledav -- runtime requires a mapping; consider a different construct
    public Runtime runtime;
    public TargetShape targetShape;
    public SourceInformation sourceInformation;

    public abstract <T> T accept(PersisterVisitor<T> visitor);
}