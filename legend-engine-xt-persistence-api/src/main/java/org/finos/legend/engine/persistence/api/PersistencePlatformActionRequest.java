package org.finos.legend.engine.persistence.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;

public class PersistencePlatformActionRequest
{
    @JsonProperty(required = true)
    public String clientVersion;

    @JsonProperty(required = true)
    public String persistenceContext;

    @JsonProperty(required = true)
    public PureModelContext model;
}
