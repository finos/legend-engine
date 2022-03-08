package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.delta.NonMilestonedDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.snapshot.NonMilestonedSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.snapshot.UnitemporalSnapshot;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NonMilestonedSnapshot.class, name = "nonMilestonedSnapshot"),
        @JsonSubTypes.Type(value = UnitemporalSnapshot.class, name = "unitemporalSnapshot"),
        @JsonSubTypes.Type(value = BitemporalSnapshot.class, name = "bitemporalSnapshot"),
        @JsonSubTypes.Type(value = NonMilestonedDelta.class, name = "nonMilestonedDelta"),
        @JsonSubTypes.Type(value = UnitemporalDelta.class, name = "unitemporalDelta"),
        @JsonSubTypes.Type(value = BitemporalDelta.class, name = "bitemporalDelta"),
        @JsonSubTypes.Type(value = AppendOnly.class, name = "appendOnly")
})
public abstract class BatchMilestoningMode
{
    public SourceInformation sourceInformation;
    
    public abstract <T> T accept(BatchMilestoningModeVisitor<T> visitor);
}