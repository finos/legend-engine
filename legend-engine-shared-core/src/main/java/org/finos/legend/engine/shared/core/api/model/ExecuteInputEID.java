package org.finos.legend.engine.shared.core.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;

public class ExecuteInputEID {
    public String clientVersion;
    public String eidString;
    @JsonProperty(required = true)
    public Lambda function;
    public String mapping;
    public Runtime runtime;
    public ExecutionContext context;
    @JsonProperty(required = true)
    public PureModelContext model;
}
