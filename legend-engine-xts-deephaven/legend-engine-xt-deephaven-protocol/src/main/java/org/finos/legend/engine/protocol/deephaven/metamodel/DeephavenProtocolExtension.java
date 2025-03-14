// Copyright 2025 Goldman Sachs
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
//

package org.finos.legend.engine.protocol.deephaven.metamodel;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.deephaven.metamodel.pure.DeephavenExecutionNode;
import org.finos.legend.engine.protocol.deephaven.metamodel.executionPlan.context.DeephavenExecutionContext;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.DeephavenStore;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;

import java.util.List;
import java.util.Map;

public class DeephavenProtocolExtension implements PureProtocolExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Deephaven");
    }

    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.with(() -> Lists.fixedSize.with(
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(DeephavenStore.class, "deephavenStore")
                        .build(),
                // Connection
                ProtocolSubTypeInfo.newBuilder(Connection.class)
                        .withSubtype(DeephavenConnection.class, "deephavenConnection")
                        .build(),
                // Execution Nodes
                ProtocolSubTypeInfo.newBuilder(ExecutionNode.class)
                        .withSubtype(DeephavenExecutionNode.class, "DeephavenExecutionNode")
                        .build(),
                // Execution Context
                ProtocolSubTypeInfo.newBuilder(ExecutionContext.class)
                        .withSubtype(DeephavenExecutionContext.class, "deephavenExecutionContext")
                        .build()
        ));
    }


    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Maps.mutable.with(DeephavenStore.class, "meta::external::store::deephaven::metamodel::store::DeephavenStore");
    }
}
