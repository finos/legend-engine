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

package org.finos.legend.engine.language.hostedService.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.code.core.CoreFunctionActivatorCodeRepositoryProvider;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentOwner;
import org.finos.legend.engine.protocol.hostedService.metamodel.HostedService;
import org.finos.legend.engine.protocol.hostedService.metamodel.HostedServiceDeploymentConfiguration;
import org.finos.legend.engine.protocol.functionActivator.metamodel.Ownership;
import org.finos.legend.engine.protocol.hostedService.metamodel.control.UserList;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ExecutionEnvironmentInstance;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_DeploymentOwnership_Impl;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_hostedService_HostedService;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_hostedService_HostedServiceDeploymentConfiguration;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_hostedService_HostedServiceDeploymentConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_hostedService_HostedService_Impl;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_Ownership;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_hostedService_UserList_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;


public class HostedServiceCompilerExtension implements CompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Hosted_Service");
    }

    // Here only for dependency check error ...
    CoreFunctionActivatorCodeRepositoryProvider forDependencies;

    @Override
    public CompilerExtension build()
    {
        return new HostedServiceCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        HostedServiceDeploymentConfiguration.class,
                        this::buildDeploymentConfig
                ),
                Processor.newProcessor(
                        HostedService.class,
                        org.eclipse.collections.impl.factory.Lists.fixedSize.with(HostedServiceDeploymentConfiguration.class, ExecutionEnvironmentInstance.class),
                        this::buildHostedService
                )//,
//                Processor.newProcessor(
//                        ExecutionEnvironmentInstance.class,
//                        org.eclipse.collections.impl.factory.Lists.fixedSize.with(PackageableConnection.class, PackageableRuntime.class),
//                        (execEnv, context) -> new Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance_Impl(execEnv.name, null, context.pureModel.getClass("meta::legend::service::metamodel::ExecutionEnvironmentInstance"))
//                                ._name(execEnv.name),
//                        (execEnv, context) ->
//                        {
//                            Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance pureExecEnv = (Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance) context.pureModel.getOrCreatePackage(execEnv._package)._children().detect(c -> execEnv.name.equals(c._name()));
//                            pureExecEnv._executionParameters(ListIterate.collect(execEnv.executionParameters, params -> HelperHostedServiceBuilder.processExecutionParameters(params, context)));
//                        })
        );
    }

    public Root_meta_external_function_activator_hostedService_HostedServiceDeploymentConfiguration buildDeploymentConfig(HostedServiceDeploymentConfiguration config, CompileContext context)
    {
        return new Root_meta_external_function_activator_hostedService_HostedServiceDeploymentConfiguration_Impl("", null, context.pureModel.getClass("meta::external::function::activator::hostedService::HostedServiceDeploymentConfiguration"));
              //  ._stage(context.pureModel.getEnumValue("meta::external::function::activator::DeploymentStage", config.stage.name()));
    }

    public Root_meta_external_function_activator_hostedService_HostedService buildHostedService(HostedService app, CompileContext context)
    {
        try
        {
            PackageableFunction<?> func = (PackageableFunction<?>) context.resolvePackageableElement(FunctionDescriptor.functionDescriptorToId(app.function.path), app.sourceInformation);
            return new Root_meta_external_function_activator_hostedService_HostedService_Impl(
                        app.name,
                        null,
                        context.pureModel.getClass("meta::external::function::activator::hostedService::HostedService")
                        )
                        ._pattern(app.pattern)
                        ._function(func)
                        ._documentation(app.documentation)
                        ._autoActivateUpdates(app.autoActivateUpdates)
                        ._generateLineage(app.generateLineage)
                        ._storeModel(app.storeModel)
                        ._ownership(buildHostedServiceOwner(app.ownership, context))
                        ._activationConfiguration(app.activationConfiguration != null ? buildDeploymentConfig((HostedServiceDeploymentConfiguration) app.activationConfiguration, context) : null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }


    public Root_meta_external_function_activator_Ownership buildHostedServiceOwner(Ownership owner, CompileContext context)
    {
        if (owner instanceof UserList)
        {
            return new Root_meta_external_function_activator_hostedService_UserList_Impl("")._users(Lists.mutable.withAll(((UserList) owner).users));
        }
        else
        {
            return new Root_meta_external_function_activator_DeploymentOwnership_Impl(" ")._id(((DeploymentOwner)owner).id);
        }
    }
}
