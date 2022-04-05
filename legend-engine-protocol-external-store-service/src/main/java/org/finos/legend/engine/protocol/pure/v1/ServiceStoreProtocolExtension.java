// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ServiceStoreEmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.ContentPattern;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.EqualToJsonPattern;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.EqualToPattern;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RestServiceExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ServiceParametersResolutionExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.connection.ServiceStoreConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.mapping.RootServiceStoreClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ServiceStore;

import java.util.List;
import java.util.Map;

public class ServiceStoreProtocolExtension implements PureProtocolExtension
{
    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.mutable.with(() -> Lists.mutable.with(
                // Class mapping
                ProtocolSubTypeInfo.Builder
                        .newInstance(ClassMapping.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(RootServiceStoreClassMapping.class, "serviceStore")
                        )).build(),
                // Connection
                ProtocolSubTypeInfo.Builder
                        .newInstance(Connection.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(ServiceStoreConnection.class, "serviceStore")
                        )).build(),
                // Content pattern
                ProtocolSubTypeInfo.Builder
                        .newInstance(ContentPattern.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(EqualToPattern.class, "equalTo"),
                                Tuples.pair(EqualToJsonPattern.class, "equalToJson")
                        )).build(),
                // Embedded Data
                ProtocolSubTypeInfo.Builder
                        .newInstance(EmbeddedData.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(ServiceStoreEmbeddedData.class, "serviceStore")
                        )).build(),
                // Execution Nodes
                ProtocolSubTypeInfo.Builder
                        .newInstance(ExecutionNode.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(RestServiceExecutionNode.class, "restService"),
                                Tuples.pair(ServiceParametersResolutionExecutionNode.class, "serviceParametersResolution")
                        )).build(),
                // Packageable element
                ProtocolSubTypeInfo.Builder
                        .newInstance(PackageableElement.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(ServiceStore.class, "serviceStore")
                        )).build()
        ));
    }

    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Maps.mutable.with(ServiceStore.class, "meta::external::store::service::metamodel::ServiceStore");
    }
}
