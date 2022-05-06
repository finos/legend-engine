package org.finos.legend.engine.authentication.provider;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
public class DatabaseAuthenticationFlowProviderConfiguration
{
    public String _type;
}
