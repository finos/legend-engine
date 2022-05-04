package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
public abstract class FinCloudTargetSpecification {
    public SourceInformation sourceInformation;
}
