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

package org.finos.legend.engine.protocol.bigqueryFunction.metamodel;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.protocol.bigqueryFunction.deployment.BigQueryFunctionArtifact;
import org.finos.legend.engine.protocol.bigqueryFunction.deployment.BigQueryFunctionContent;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentContent;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;

import java.util.List;
import java.util.Map;

public class BigQueryFunctionProtocolExtension implements PureProtocolExtension
{
    public static String packageJSONType = "bigQueryFunction";

    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.with(() -> Lists.mutable.with(
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(BigQueryFunction.class, packageJSONType)
                        .build(),
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(BigQueryFunctionDeploymentConfiguration.class, packageJSONType + "Config")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(FunctionActivatorDeploymentConfiguration.class)
                        .withSubtype(org.finos.legend.engine.protocol.bigqueryFunction.deployment.BigQueryFunctionDeploymentConfiguration.class, "bigQueryFunctionDeploymentConfig")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(FunctionActivatorArtifact.class)
                        .withSubtype(BigQueryFunctionArtifact.class, "bigQueryFunctionArtifact")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(FunctionActivatorDeploymentContent.class)
                        .withSubtype(BigQueryFunctionContent.class, "bigQueryFunctionDeploymentContent")
                        .build()
        ));
    }

    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Maps.mutable.with(BigQueryFunction.class, "meta::external::function::activator::bigQueryFunction::BigQueryFunction");
    }
}
