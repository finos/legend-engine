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

package org.finos.legend.engine.language.snowflakeApp.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.code.core.CoreFunctionActivatorCodeRepositoryProvider;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.snowflakeApp.metamodel.SnowflakeApp;
import org.finos.legend.engine.protocol.snowflakeApp.metamodel.SnowflakeDeploymentConfiguration;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_snowflakeApp_SnowflakeApp;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_snowflakeApp_SnowflakeApp_Impl;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_snowflakeApp_SnowflakeDeploymentConfiguration;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_snowflakeApp_SnowflakeDeploymentConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_relational_runtime_RelationalDatabaseConnection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;

import java.util.Collections;

public class SnowflakeAppCompilerExtension implements CompilerExtension
{
    // Here only for dependency check error ...
    CoreFunctionActivatorCodeRepositoryProvider forDependencies;

    @Override
    public CompilerExtension build()
    {
        return new SnowflakeAppCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        SnowflakeApp.class,
                        org.eclipse.collections.impl.factory.Lists.fixedSize.with(SnowflakeDeploymentConfiguration.class),
                        this::buildSnowflakeApp
                ),
                Processor.newProcessor(
                        SnowflakeDeploymentConfiguration.class,
                        this::buildDeploymentConfig
                )
        );
    }

    public Root_meta_external_function_activator_snowflakeApp_SnowflakeApp buildSnowflakeApp(SnowflakeApp app, CompileContext context)
    {
        try
        {
            PackageableFunction<?> func = (PackageableFunction<?>) context.resolvePackageableElement(FunctionDescriptor.functionDescriptorToId(app.function), app.sourceInformation);
            return new Root_meta_external_function_activator_snowflakeApp_SnowflakeApp_Impl(
                        app.name,
                        null,
                        context.pureModel.getClass("meta::external::function::activator::snowflakeApp::SnowflakeApp")
                        )
                        ._applicationName(app.applicationName)
                        ._function(func)
                        ._description(app.description)
                        ._owner(app.owner)
                        ._activationConfiguration(app.activationConfiguration != null ? buildDeploymentConfig((SnowflakeDeploymentConfiguration) app.activationConfiguration, context) : null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public Root_meta_external_function_activator_snowflakeApp_SnowflakeDeploymentConfiguration buildDeploymentConfig(SnowflakeDeploymentConfiguration configuration, CompileContext context)
    {
        return new Root_meta_external_function_activator_snowflakeApp_SnowflakeDeploymentConfiguration_Impl("")
                ._target((Root_meta_external_store_relational_runtime_RelationalDatabaseConnection) context.resolveConnection(configuration.activationConnection.connection, configuration.sourceInformation));
               // ._stage(context.pureModel.getEnumValue("meta::external::function::activator::DeploymentStage", configuration.stage.name()));
    }
}
