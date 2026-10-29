// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.protocol.graphQLStore;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.graphQLStore.executionPlan.GraphQLExecutionNode;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLClassMapping;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLConnection;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLStore;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;

import java.util.List;
import java.util.Map;

public class GraphQLStoreProtocolExtension implements PureProtocolExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "GraphQL");
    }

    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.with(() -> Lists.fixedSize.with(
                ProtocolSubTypeInfo.newBuilder(ClassMapping.class)
                        .withSubtype(GraphQLClassMapping.class, "graphQLClassMapping")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(Connection.class)
                        .withSubtype(GraphQLConnection.class, "graphQLConnection")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(GraphQLStore.class, "graphQLStore")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(ExecutionNode.class)
                        .withSubtype(GraphQLExecutionNode.class, "graphQL")
                        .build()
        ));
    }

    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        Map<Class<? extends PackageableElement>, String> map = Maps.mutable.empty();
        map.put(GraphQLStore.class, "meta::external::store::graphQL::metamodel::pure::GraphQLStore");
        return map;
    }
}
