package org.finos.legend.engine.external.format.cpb.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.external.shared.format.generations.GenerationInput;

public class CpbGenerationInput extends GenerationInput
{
    @JsonProperty(required = true)
    public CpbGenerationConfig config;
}
