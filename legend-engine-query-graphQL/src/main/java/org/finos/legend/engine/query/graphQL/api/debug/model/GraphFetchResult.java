package org.finos.legend.engine.query.graphQL.api.debug.model;

import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.GraphFetchTree;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GraphFetchResult
{
    public GraphFetchTree graphFetchTree;
    public List<DomainUnit> domainUnits = Collections.EMPTY_LIST;

    public GraphFetchResult(GraphFetchTree graphFetchTree, List<DomainUnit> domainUnits)
    {
        this.graphFetchTree = graphFetchTree;
        this.domainUnits = domainUnits;
    }

    public static class DomainUnit
    {
        public String property;
        public ValueSpecification expression;

        public DomainUnit(String first, ValueSpecification second)
        {
            this.property = first;
            this.expression = second;
        }
    }
}
