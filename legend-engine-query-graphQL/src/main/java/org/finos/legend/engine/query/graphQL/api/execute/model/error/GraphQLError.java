package org.finos.legend.engine.query.graphQL.api.execute.model.error;

import java.util.Collections;
import java.util.List;

public class GraphQLError
{
    public String message;
    public List<GraphQLErrorLocation> location = Collections.emptyList();
    public List<String> path = Collections.emptyList();

    public GraphQLError(String message)
    {
        this.message = message;
    }
}
