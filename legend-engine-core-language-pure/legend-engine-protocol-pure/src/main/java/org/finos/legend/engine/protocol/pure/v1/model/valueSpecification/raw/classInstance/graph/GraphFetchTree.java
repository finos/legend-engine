// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;

import java.util.Collections;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RootGraphFetchTree.class, name = "rootGraphFetchTree"),
        @JsonSubTypes.Type(value = PropertyGraphFetchTree.class, name = "propertyGraphFetchTree"),
        @JsonSubTypes.Type(value = SubTypeGraphFetchTree.class, name = "subTypeGraphFetchTree")
})
public abstract class GraphFetchTree
{
    public SourceInformation sourceInformation;
    public List<GraphFetchTree> subTrees = Collections.emptyList();
    public List<SubTypeGraphFetchTree> subTypeTrees = Collections.emptyList();

    public abstract <T> T accept(GraphFetchTreeVisitor<T> visitor);
}
