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
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.code.core.CoreFunctionActivatorCodeRepositoryProvider;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.SourceInformationHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentOwner;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.snowflakeApp.metamodel.SnowflakeApp;
import org.finos.legend.engine.protocol.snowflakeApp.metamodel.SnowflakeAppDeploymentConfiguration;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;

public class SnowflakeAppCompilerExtension implements CompilerExtension
{
    // Here only for dependency check error ...
    CoreFunctionActivatorCodeRepositoryProvider forDependencies;

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Snowflake");
    }

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
                        org.eclipse.collections.impl.factory.Lists.fixedSize.with(PackageableConnection.class, Function.class),
                        this::buildSnowflakeApp
                )
        );
    }

    public Root_meta_external_function_activator_snowflakeApp_SnowflakeApp buildSnowflakeApp(SnowflakeApp app, CompileContext context)
    {
        try
        {
            PackageableFunction<?> func = (PackageableFunction<?>) context.resolvePackageableElement(FunctionDescriptor.functionDescriptorToId(app.function.path), app.sourceInformation);
            return new Root_meta_external_function_activator_snowflakeApp_SnowflakeApp_Impl(
                    app.name,
                    SourceInformationHelper.toM3SourceInformation(app.sourceInformation),
                    context.pureModel.getClass("meta::external::function::activator::snowflakeApp::SnowflakeApp")
            )
                    ._applicationName(app.applicationName)
                    ._function(func)
                    ._description(app.description)
                    ._ownership(new Root_meta_external_function_activator_DeploymentOwnership_Impl("")._id(((DeploymentOwner)app.ownership).id))
                    ._activationConfiguration(app.activationConfiguration != null ? buildDeploymentConfig((SnowflakeAppDeploymentConfiguration) app.activationConfiguration, context) : null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public Root_meta_external_function_activator_snowflakeApp_SnowflakeDeploymentConfiguration buildDeploymentConfig(SnowflakeAppDeploymentConfiguration configuration, CompileContext context)
    {
        return new Root_meta_external_function_activator_snowflakeApp_SnowflakeDeploymentConfiguration_Impl("")
                ._target((Root_meta_external_store_relational_runtime_RelationalDatabaseConnection) context.resolveConnection(configuration.activationConnection.connection, configuration.sourceInformation));
        // ._stage(context.pureModel.getEnumValue("meta::external::function::activator::DeploymentStage", configuration.stage.name()));
    }
}
