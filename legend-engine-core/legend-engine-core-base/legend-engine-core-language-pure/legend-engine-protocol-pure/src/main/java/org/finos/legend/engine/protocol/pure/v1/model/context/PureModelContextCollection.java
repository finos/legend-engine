package org.finos.legend.engine.protocol.pure.v1.model.context;

import java.util.Collection;
import java.util.List;

public class PureModelContextCollection extends PureModelContext
{
    private final Collection<PureModelContext> contexts;

    public PureModelContextCollection(List<PureModelContext> contexts)
    {
        this.contexts = contexts;
    }

    public Collection<PureModelContext> getContexts() {
        return contexts;
    }
}
