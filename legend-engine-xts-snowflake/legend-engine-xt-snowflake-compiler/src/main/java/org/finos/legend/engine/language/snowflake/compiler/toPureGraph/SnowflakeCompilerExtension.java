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

package org.finos.legend.engine.language.snowflake.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.code.core.CoreFunctionActivatorCodeRepositoryProvider;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.SourceInformationHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentOwner;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.snowflake.snowflakeApp.metamodel.SnowflakeApp;
import org.finos.legend.engine.protocol.snowflake.snowflakeApp.metamodel.SnowflakeAppDeploymentConfiguration;
import org.finos.legend.engine.protocol.snowflake.snowflakeApp.metamodel.SnowflakePermissionScheme;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.metamodel.SnowflakeM2MUdf;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.metamodel.SnowflakeM2MUdfDeploymentConfiguration;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;

public class SnowflakeCompilerExtension implements CompilerExtension
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
        return new SnowflakeCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        SnowflakeApp.class,
                        org.eclipse.collections.impl.factory.Lists.fixedSize.with(PackageableConnection.class, Function.class),
                        this::buildSnowflakeApp
                ),
                Processor.newProcessor(
                        SnowflakeM2MUdf.class,
                        org.eclipse.collections.impl.factory.Lists.fixedSize.with(PackageableConnection.class, Function.class),
                        this::buildSnowflakeM2MUdf
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
                    ._stereotypes(ListIterate.collect(app.stereotypes, s -> context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)))
                    ._taggedValues(ListIterate.collect(app.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::extension::TaggedValue"))._tag(context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.tag.sourceInformation))._value(t.value)))
                    ._applicationName(app.applicationName)
                    ._function(func)
                    ._description(app.description)
                    ._deploymentSchema(app.deploymentSchema != null ? app.deploymentSchema : "LEGEND_NATIVE_APPS")
                    ._usageRole(app.usageRole)
                    ._permissionScheme(app.permissionScheme != null ? context.pureModel.getEnumValue("meta::external::function::activator::snowflakeApp::SnowflakePermissionScheme", app.permissionScheme.toString()) : context.pureModel.getEnumValue("meta::external::function::activator::snowflakeApp::SnowflakePermissionScheme", SnowflakePermissionScheme.DEFAULT.toString()))
                    ._ownership(new Root_meta_external_function_activator_DeploymentOwnership_Impl("")._id(((DeploymentOwner)app.ownership).id))
                    ._activationConfiguration(app.activationConfiguration != null ? buildDeploymentConfig((SnowflakeAppDeploymentConfiguration) app.activationConfiguration, context) : null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf buildSnowflakeM2MUdf(SnowflakeM2MUdf udf, CompileContext context)
    {
        try
        {
            PackageableFunction<?> func = (PackageableFunction<?>) context.resolvePackageableElement(FunctionDescriptor.functionDescriptorToId(udf.function.path), udf.sourceInformation);
            return new Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf_Impl(
                    udf.name,
                    SourceInformationHelper.toM3SourceInformation(udf.sourceInformation),
                    context.pureModel.getClass("meta::external::function::activator::snowflakeM2MUdf::SnowflakeM2MUdf")
            )
                    ._stereotypes(ListIterate.collect(udf.stereotypes, s -> context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)))
                    ._taggedValues(ListIterate.collect(udf.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::extension::TaggedValue"))._tag(context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.tag.sourceInformation))._value(t.value)))
                    ._udfName(udf.udfName)
                    ._function(func)
                    ._description(udf.description)
                    ._deploymentSchema(udf.deploymentSchema)
                    ._deploymentStage(udf.deploymentStage)
                    ._ownership(new Root_meta_external_function_activator_DeploymentOwnership_Impl("")._id(((DeploymentOwner)udf.ownership).id))
                    ._activationConfiguration(udf.activationConfiguration != null ? buildDeploymentConfig((SnowflakeM2MUdfDeploymentConfiguration) udf.activationConfiguration, context) : null);
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
    }

    public Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdfDeploymentConfiguration buildDeploymentConfig(SnowflakeM2MUdfDeploymentConfiguration configuration, CompileContext context)
    {
        return new Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdfDeploymentConfiguration_Impl("")
                ._target((Root_meta_external_store_relational_runtime_RelationalDatabaseConnection) context.resolveConnection(configuration.activationConnection.connection, configuration.sourceInformation));
    }
}
