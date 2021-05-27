package org.finos.legend.engine.server.core.configuration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PropertyVaultConfiguration.class, name = "property")
})
public abstract class VaultConfiguration
{
    public String _type;
}
