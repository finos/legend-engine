package org.finos.legend.engine.shared.core.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;

import java.util.List;

public class ExecuteMappingTestInput
{
    public String clientVersion;
    @JsonProperty(required = true)
    public String mapping;
    public List<String> testId;
    public ExecutionContext context;
    @JsonProperty(required = true)
    public PureModelContext model;
}
