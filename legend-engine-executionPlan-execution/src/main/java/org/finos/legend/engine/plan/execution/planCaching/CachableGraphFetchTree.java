// Copyright 2023 Goldman Sachs
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


package org.finos.legend.engine.plan.execution.planCaching;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.GraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.GraphFetchTreeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.SubTypeGraphFetchTree;

public class CachableGraphFetchTree
{
    private HashMap<String, Object> parameterMap;  //map of the extracted values
    private GraphFetchTree graphFetchTree;

    private List<CachableValueSpecification> valueSpecificationListHelper(List<ValueSpecification> valueSpecificationList, String parameter)
    {
        return IntStream.range(0, valueSpecificationList.size())
                .mapToObj(index -> new CachableValueSpecification(valueSpecificationList.get(index), parameter + "_" + index))
                .collect(Collectors.toList());
    }

    private GraphFetchTree buildCachable(GraphFetchTree graphFetchTree, String parameterName)
    {

        return graphFetchTree.accept(new GraphFetchTreeVisitor<GraphFetchTree>()
        {
            @Override
            public GraphFetchTree visit(PropertyGraphFetchTree tree)
            {
                List<CachableValueSpecification> params = valueSpecificationListHelper(tree.parameters, parameterName + "_");
                List<ValueSpecification> paramList = Collections.EMPTY_LIST;
                params.forEach(p ->
                        {
                            paramList.add(p.getValueSpecification());
                            parameterMap.putAll(p.getParameterMap());
                        }
                );
                tree.parameters = paramList;
                return tree;
            }

            @Override
            public GraphFetchTree visit(RootGraphFetchTree tree)
            {
                tree.subTrees = IntStream.range(0, tree.subTrees.size())
                        .mapToObj(index -> buildCachable(tree.subTrees.get(index), parameterName + "_" + index))
                        .collect(Collectors.toList());
                return tree;
            }

            @Override
            public GraphFetchTree visit(SubTypeGraphFetchTree tree)
            {
                return tree;
            }
        });
    }


}
