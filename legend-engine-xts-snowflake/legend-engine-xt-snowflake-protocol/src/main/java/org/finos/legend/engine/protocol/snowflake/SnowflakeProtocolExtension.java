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

package org.finos.legend.engine.protocol.snowflake;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentConfiguration;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorDeploymentContent;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentConfiguration;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.snowflake.snowflakeApp.deployment.SnowflakeAppArtifact;
import org.finos.legend.engine.protocol.snowflake.snowflakeApp.deployment.SnowflakeAppContent;
import org.finos.legend.engine.protocol.snowflake.snowflakeApp.metamodel.SnowflakeApp;
import org.finos.legend.engine.protocol.snowflake.snowflakeApp.metamodel.SnowflakeAppDeploymentConfiguration;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.SnowflakeM2MUdfArtifact;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.SnowflakeM2MUdfContent;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.metamodel.SnowflakeM2MUdf;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.metamodel.SnowflakeM2MUdfDeploymentConfiguration;

import java.util.List;
import java.util.Map;

public class SnowflakeProtocolExtension implements PureProtocolExtension
{
    public static String snowflakeAppPackageJSONType = "snowflakeApp";
    public static String snowflakeM2MUdfPackageJSONType = "snowflakeM2MUdf";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Snowflake");
    }

    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.with(() -> Lists.mutable.with(
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(SnowflakeApp.class, snowflakeAppPackageJSONType)
                        .build(),
                ProtocolSubTypeInfo.newBuilder(DeploymentConfiguration.class)
                        .withSubtype(SnowflakeAppDeploymentConfiguration.class, "snowflakeDeploymentConfiguration")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(FunctionActivatorDeploymentConfiguration.class)
                        .withSubtype(org.finos.legend.engine.protocol.snowflake.snowflakeApp.deployment.SnowflakeAppDeploymentConfiguration.class, "snowflakeDeploymentConfig")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(FunctionActivatorArtifact.class)
                        .withSubtype(SnowflakeAppArtifact.class, "snowflakeArtifact")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(FunctionActivatorDeploymentContent.class)
                        .withSubtype(SnowflakeAppContent.class, "snowflakeDeploymentContent")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(SnowflakeM2MUdf.class, snowflakeM2MUdfPackageJSONType)
                        .build(),
                ProtocolSubTypeInfo.newBuilder(DeploymentConfiguration.class)
                        .withSubtype(SnowflakeM2MUdfDeploymentConfiguration.class, "snowflakeM2MUdfDeploymentConfiguration")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(FunctionActivatorDeploymentConfiguration.class)
                        .withSubtype(org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.SnowflakeM2MUdfDeploymentConfiguration.class, "snowflakeM2MUdfDeploymentConfig")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(FunctionActivatorArtifact.class)
                        .withSubtype(SnowflakeM2MUdfArtifact.class, "snowflakeM2MUdfArtifact")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(FunctionActivatorDeploymentContent.class)
                        .withSubtype(SnowflakeM2MUdfContent.class, "snowflakeM2MUdfDeploymentContent")
                        .build()
        ));
    }

    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Maps.mutable.with(SnowflakeApp.class, "meta::external::function::activator::snowflakeApp::SnowflakeApp",
                                 SnowflakeM2MUdf.class, "meta::external::function::activator::snowflakeM2MUdf::SnowflakeM2MUdf");
    }
}
