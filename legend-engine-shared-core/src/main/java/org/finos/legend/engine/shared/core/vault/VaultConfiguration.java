package org.finos.legend.engine.shared.core.vault;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
public abstract class VaultConfiguration
{
    public String _type;
}
