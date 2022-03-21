package org.finos.legend.engine.external.format.awspersistence.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.external.shared.format.generations.GenerationInput;

public class AwsPersistenceGenerationInput extends GenerationInput
{
    @JsonProperty(required = true)
    public AwsPersistenceGenerationConfig config;
}
