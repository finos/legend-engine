// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
