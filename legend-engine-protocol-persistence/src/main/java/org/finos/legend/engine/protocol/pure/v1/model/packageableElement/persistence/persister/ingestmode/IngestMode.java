package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.NontemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.NontemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.UnitemporalSnapshot;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NontemporalSnapshot.class, name = "nonTemporalSnapshot"),
        @JsonSubTypes.Type(value = UnitemporalSnapshot.class, name = "unitemporalSnapshot"),
        @JsonSubTypes.Type(value = BitemporalSnapshot.class, name = "bitemporalSnapshot"),
        @JsonSubTypes.Type(value = NontemporalDelta.class, name = "nontemporalDelta"),
        @JsonSubTypes.Type(value = UnitemporalDelta.class, name = "unitemporalDelta"),
        @JsonSubTypes.Type(value = BitemporalDelta.class, name = "bitemporalDelta"),
        @JsonSubTypes.Type(value = AppendOnly.class, name = "appendOnly")
})
public abstract class IngestMode
{
    public SourceInformation sourceInformation;
    
    public abstract <T> T accept(IngestModeVisitor<T> visitor);
}