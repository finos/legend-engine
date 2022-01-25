package org.finos.legend.engine.query.graphQL.api.execute.model.error;

import org.eclipse.collections.impl.factory.Lists;

import java.util.Collections;
import java.util.List;

public class GraphQLErrorMain
{
    public List<GraphQLError> errors = Collections.EMPTY_LIST;

    public GraphQLErrorMain(String message)
    {
        this.errors = Lists.mutable.with(new GraphQLError(message));
    }
}
