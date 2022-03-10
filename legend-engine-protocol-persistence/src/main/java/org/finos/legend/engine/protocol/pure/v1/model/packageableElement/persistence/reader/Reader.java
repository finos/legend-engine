package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ServiceReader.class, name = "serviceReader")
})
public abstract class Reader
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(ReaderVisitor<T> visitor);
}