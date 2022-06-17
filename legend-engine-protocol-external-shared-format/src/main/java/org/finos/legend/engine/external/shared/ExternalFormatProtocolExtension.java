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

package org.finos.legend.engine.external.shared;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.shared.DataQualityExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.shared.UrlStreamExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.Binding;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatConnection;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatSchemaSet;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalSource;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.UrlStreamExternalSource;

import java.util.List;

public class ExternalFormatProtocolExtension implements PureProtocolExtension
{
    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.with(() -> Lists.fixedSize.with(
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(ExternalFormatSchemaSet.class, "externalFormatSchemaSet")
                        .withSubtype(Binding.class, "binding")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(Connection.class)
                        .withSubtype(ExternalFormatConnection.class, "ExternalFormatConnection")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(ExternalSource.class)
                        .withSubtype(UrlStreamExternalSource.class, "urlStream")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(ExecutionNode.class)
                        .withSubtype(DataQualityExecutionNode.class, "dataQuality")
                        .withSubtype(UrlStreamExecutionNode.class, "urlStream")
                        .build()
        ));
    }
}