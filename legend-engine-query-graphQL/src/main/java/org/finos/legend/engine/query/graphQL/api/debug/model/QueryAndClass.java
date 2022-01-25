package org.finos.legend.engine.query.graphQL.api.debug.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryAndClass
{
    public String query;
    @JsonProperty(value = "class")
    public String _class;
}
