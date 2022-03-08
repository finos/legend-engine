package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.auditing;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = NoAuditing.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoAuditing.class, name = "noAuditing"),
        @JsonSubTypes.Type(value = BatchDateTimeAuditing.class, name = "batchDateTimeAuditing"),
        @JsonSubTypes.Type(value = OpaqueAuditing.class, name = "opaqueAuditing")
})
public abstract class Auditing
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(AuditingVisitor<T> visitor);
}