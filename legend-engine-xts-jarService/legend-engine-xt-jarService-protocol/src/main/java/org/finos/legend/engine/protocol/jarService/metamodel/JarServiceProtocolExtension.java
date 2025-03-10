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

package org.finos.legend.engine.protocol.jarService.metamodel;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentContent;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentConfiguration;
import org.finos.legend.engine.protocol.functionActivator.metamodel.Ownership;
import org.finos.legend.engine.protocol.jarService.deployment.JarServiceArtifact;
import org.finos.legend.engine.protocol.jarService.deployment.JarServiceContent;
import org.finos.legend.engine.protocol.jarService.metamodel.control.UserList;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;

import java.util.List;
import java.util.Map;

public class JarServiceProtocolExtension implements PureProtocolExtension
{
    public static String packageJSONType = "jarService";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Jar_Service");
    }

    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.with(() -> Lists.mutable.with(
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(JarService.class, packageJSONType)
                        .build(),
                ProtocolSubTypeInfo.newBuilder(Ownership.class)
                        .withSubtype(UserList.class, "userList")
//                        .withSubtype(Deployment.class, "deployment")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(DeploymentConfiguration.class)
                        .withSubtype(JarServiceDeploymentConfiguration.class, "jarServiceDeploymentConfiguration")
                        .build(),
//                ProtocolSubTypeInfo.newBuilder(FunctionActivatorDeploymentConfiguration.class)
//                        .withSubtype(org.finos.legend.engine.protocol.jarService.deployment.JarServiceDeploymentConfiguration.class, "jarServiceDeploymentConfig")
//                        .build(),
                ProtocolSubTypeInfo.newBuilder(FunctionActivatorArtifact.class)
                        .withSubtype(JarServiceArtifact.class, "jarServiceArtifact")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(FunctionActivatorDeploymentContent.class)
                        .withSubtype(JarServiceContent.class, "jarServiceDeploymentContent")
                        .build()
        ));
    }

    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Maps.mutable.with(JarService.class, "meta::external::function::activator::jarService::JarService");
    }
}
