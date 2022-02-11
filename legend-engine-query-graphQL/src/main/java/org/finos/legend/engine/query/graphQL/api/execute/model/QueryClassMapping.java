package org.finos.legend.engine.query.graphQL.api.execute.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryClassMapping
{
    public String query;
    @JsonProperty(value = "class")
    public String _class;
    public String mapping;
}
