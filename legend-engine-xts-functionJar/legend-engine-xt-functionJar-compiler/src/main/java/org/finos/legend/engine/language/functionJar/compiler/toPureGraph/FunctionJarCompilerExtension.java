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

package org.finos.legend.engine.language.functionJar.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.code.core.CoreFunctionActivatorCodeRepositoryProvider;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentOwner;
import org.finos.legend.engine.protocol.functionJar.metamodel.FunctionJar;
import org.finos.legend.engine.protocol.functionJar.metamodel.FunctionJarDeploymentConfiguration;
import org.finos.legend.engine.protocol.functionActivator.metamodel.Ownership;
import org.finos.legend.engine.protocol.functionJar.metamodel.control.UserList;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ExecutionEnvironmentInstance;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_DeploymentOwnership_Impl;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_functionJar_FunctionJar;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_functionJar_FunctionJarDeploymentConfiguration;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_functionJar_FunctionJarDeploymentConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_functionJar_FunctionJar_Impl;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_Ownership;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_functionJar_UserList_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_extension_TaggedValue_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;


public class FunctionJarCompilerExtension implements CompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Jar_Service");
    }

    // Here only for dependency check error ...
    CoreFunctionActivatorCodeRepositoryProvider forDependencies;

    @Override
    public CompilerExtension build()
    {
        return new FunctionJarCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        FunctionJarDeploymentConfiguration.class,
                        this::buildDeploymentConfig
                ),
                Processor.newProcessor(
                        FunctionJar.class,
                        org.eclipse.collections.impl.factory.Lists.fixedSize.with(FunctionJarDeploymentConfiguration.class, ExecutionEnvironmentInstance.class, Function.class),
                        this::buildFunctionJar
                )
        );
    }

    public Root_meta_external_function_activator_functionJar_FunctionJarDeploymentConfiguration buildDeploymentConfig(FunctionJarDeploymentConfiguration config, CompileContext context)
    {
        return new Root_meta_external_function_activator_functionJar_FunctionJarDeploymentConfiguration_Impl("", null, context.pureModel.getClass("meta::external::function::activator::functionJar::FunctionJarDeploymentConfiguration"));
    }

    public Root_meta_external_function_activator_functionJar_FunctionJar buildFunctionJar(FunctionJar app, CompileContext context)
    {
        try
        {
            PackageableFunction<?> func = (PackageableFunction<?>) context.resolvePackageableElement(FunctionDescriptor.functionDescriptorToId(app.function.path), app.sourceInformation);
            return new Root_meta_external_function_activator_functionJar_FunctionJar_Impl(
                    app.name,
                    null,
                    context.pureModel.getClass("meta::external::function::activator::functionJar::FunctionJar")
            )
                    ._stereotypes(ListIterate.collect(app.stereotypes, s -> context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)))
                    ._taggedValues(ListIterate.collect(app.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::extension::TaggedValue"))._tag(context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.tag.sourceInformation))._value(t.value)))
                    ._function(func)
                    ._documentation(app.documentation)
                    ._ownership(buildFunctionJarOwner(app.ownership, context))
                    ._activationConfiguration(app.activationConfiguration != null ? buildDeploymentConfig((FunctionJarDeploymentConfiguration) app.activationConfiguration, context) : null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public Root_meta_external_function_activator_Ownership buildFunctionJarOwner(Ownership owner, CompileContext context)
    {
        if (owner instanceof UserList)
        {
            return new Root_meta_external_function_activator_functionJar_UserList_Impl("")._users(Lists.mutable.withAll(((UserList) owner).users));
        }
        else
        {
            return new Root_meta_external_function_activator_DeploymentOwnership_Impl(" ")._id(((DeploymentOwner)owner).id);
        }
    }
}
